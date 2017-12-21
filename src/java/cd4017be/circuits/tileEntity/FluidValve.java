package cd4017be.circuits.tileEntity;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
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
import cd4017be.lib.BlockGuiHandler.ClientPacketReceiver;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.block.AdvancedBlock.INeighborAwareTile;
import cd4017be.lib.block.AdvancedBlock.IRedstoneTile;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.Utils;

public class FluidValve extends BaseTileEntity implements INeighborAwareTile, IRedstoneTile, ITickable, IDirectionalRedstone, IGuiData, ClientPacketReceiver {

	private TileEntity in, out;
	public int tickInt = 1;
	public boolean measure, update = true;
	public int flow, state;

	@Override
	public void update() {
		if (world.isRemote) return;
		if (update) {
			EnumFacing dir = getOrientation().front;
			in = Utils.neighborTile(this, dir);
			out = Utils.neighborTile(this, dir.getOpposite());
		}
		if (measure) {
			int d = transferFluid(Integer.MAX_VALUE);
			if (d != 0) {
				flow += d;
				markDirty();
			}
		}
		if (world.getTotalWorldTime() % tickInt != 0) return;
		if (measure) {
			if (flow != state) {
				state = flow;
				world.notifyNeighborsOfStateChange(pos, Blocks.REDSTONE_TORCH, false);
			}
			flow = 0;
		} else if (state > 0) {
			flow = transferFluid(state);
		} else flow = 0;
		markDirty();
	}

	private int transferFluid(int max) {
		if (in == null || out == null) return 0;
		if (in.isInvalid() || out.isInvalid()) {
			in = out = null;
			update = true;
			return 0;
		}
		EnumFacing dir = getOrientation().front;
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
	public void neighborBlockChange(Block b, BlockPos src) {
		if (world.isRemote || measure) return;
		int ls = state;
		state = 0;
		for (EnumFacing s : EnumFacing.VALUES)
			state |= world.getRedstonePower(pos.offset(s), s);
		if (state != ls) markDirty();
	}

	@Override
	public void neighborTileChange(BlockPos src) {
		update = true;
	}

	@Override
	public int redstoneLevel(EnumFacing side, boolean strong) {
		return strong || !measure ? 0 : state;
	}

	@Override
	public boolean connectRedstone(EnumFacing side) {
		return getRSDirection(side) != 0;
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
	public void onPacketFromClient(PacketBuffer data, EntityPlayer sender) throws IOException {
		byte cmd = data.readByte();
		if (cmd == 0) {
			tickInt = data.readInt();
			if (tickInt < 1) tickInt = 1;
			else if (tickInt > 1200) tickInt = 1200;
		} else if (cmd == 1) {
			measure = !measure;
			if (measure) flow = 0;
			else neighborBlockChange(null, pos);
		}
		markDirty();
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

	@Override
	public void initContainer(DataContainer container) {
	}

	@Override
	public boolean detectAndSendChanges(DataContainer container, PacketBuffer dos) {
		return false;
	}

	@Override
	public void updateClientChanges(DataContainer container, PacketBuffer dis) {
	}

}
