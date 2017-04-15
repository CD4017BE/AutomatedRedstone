package cd4017be.circuits.tileEntity;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.lib.ModTileEntity;
import cd4017be.lib.Gui.DataContainer.IGuiData;

public class FluidValve extends ModTileEntity implements ITickable, IDirectionalRedstone, IGuiData {

	private TileEntity in, out;
	public int tickInt = 1;
	public boolean measure, update = true;
	public int flow, state;

	@Override
	public void update() {
		if (worldObj.isRemote) return;
		if (update) {
			EnumFacing dir = EnumFacing.VALUES[getOrientation()];
			in = getLoadedTile(pos.offset(dir));
			out = getLoadedTile(pos.offset(dir.getOpposite()));
		}
		if (measure) flow += transferFluid(Integer.MAX_VALUE);
		if (worldObj.getTotalWorldTime() % tickInt != 0) return;
		if (measure) {
			if (flow != state) {
				state = flow;
				worldObj.notifyNeighborsOfStateChange(pos, Blocks.REDSTONE_TORCH);
			}
			flow = 0;
		} else if (state > 0) {
			flow = transferFluid(state);
		} else flow = 0;
	}

	private int transferFluid(int max) {
		if (in == null || out == null) return 0;
		if (in.isInvalid() || out.isInvalid()) {
			in = out = null;
			update = true;
			return 0;
		}
		EnumFacing dir = EnumFacing.VALUES[getOrientation()];
		IFluidHandler accIn = in.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir.getOpposite());
		if (accIn == null) return 0;
		IFluidHandler accOut = out.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir);
		if (accOut == null) return 0;
		FluidStack fluid = accIn.drain(max, false);
		if (fluid == null) return 0;
		max = accOut.fill(fluid, true);
		if (max > 0) accIn.drain(max, true);
		return max;
	}

	@Override
	public void onNeighborBlockChange(Block b) {
		if (worldObj.isRemote || measure) return;
		state = 0;
		for (EnumFacing s : EnumFacing.VALUES)
			state |= worldObj.getRedstonePower(pos.offset(s), s);
	}

	@Override
	public void onNeighborTileChange(BlockPos pos) {
		update = true;
	}

	@Override
	public int redstoneLevel(int s, boolean str) {
		return str || !measure ? 0 : state;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		flow = nbt.getInteger("flow");
		state = nbt.getInteger("state");
		tickInt = nbt.getInteger("tickInt");
		measure = nbt.getBoolean("measure");
		update = true;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setInteger("flow", flow);
		nbt.setInteger("state", state);
		nbt.setInteger("tickInt", tickInt);
		nbt.setBoolean("measure", measure);
		return super.writeToNBT(nbt);
	}

	@Override
	public void onPlayerCommand(PacketBuffer data, EntityPlayerMP player) throws IOException {
		byte cmd = data.readByte();
		if (cmd == 0) {
			tickInt = data.readInt();
			if (tickInt < 1) tickInt = 1;
			else if (tickInt > 1200) tickInt = 1200;
		} else if (cmd == 1) {
			measure = !measure;
			if (measure) flow = 0;
			else onNeighborBlockChange(null);
		}
	}

	@Override
	public int[] getSyncVariables() {
		return new int[]{flow, state, tickInt, measure?1:0};
	}

	@Override
	public void setSyncVariable(int i, int v) {
		switch(i) {
		case 0: flow = v; break;
		case 1: state = v; break;
		case 2: tickInt = v; break;
		case 3: measure = v != 0; break;
		}
	}

	@Override
	public byte getRSDirection(EnumFacing s) {
		return measure ? (byte)2 : (byte)1;
	}

}
