package cd4017be.circuits.tileEntity;

import java.io.IOException;

import cofh.api.energy.IEnergyReceiver;
import net.darkhax.tesla.api.ITeslaConsumer;
import net.darkhax.tesla.capability.TeslaCapabilities;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.Loader;
import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.lib.ModTileEntity;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import net.minecraftforge.fml.common.Optional;

public class EnergyValve extends ModTileEntity implements ITickable, IDirectionalRedstone, IGuiData, IEnergyReceiver {

	public static final Capability<?> TeslaConsumer;
	static {
		if (Loader.isModLoaded("Tesla")) {
			TeslaConsumer = TeslaCapabilities.CAPABILITY_CONSUMER;
		} else {
			TeslaConsumer = null;
		}
	}
	private TileEntity out;
	public int tickInt = 1;
	public boolean measure, update;
	public int flow, state;

	@Override
	public void update() {
		if (worldObj.isRemote) return;
		if (update) {
			EnumFacing dir = EnumFacing.VALUES[getOrientation()];
			out = getLoadedTile(pos.offset(dir.getOpposite()));
		}
		if (worldObj.getTotalWorldTime() % tickInt != 0) return;
		if (measure) {
			flow = Integer.MAX_VALUE - flow;
			if (flow != state) {
				state = flow;
				worldObj.notifyNeighborsOfStateChange(pos, Blocks.REDSTONE_TORCH);
			}
			flow = Integer.MAX_VALUE;
		} else if (state > 0) {
			flow = state;
		} else flow = 0;
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

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
		return cap == null && cap == TeslaConsumer && facing != null && facing.ordinal() == getOrientation();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> cap, EnumFacing facing) {
		if (cap != null && cap == TeslaConsumer && facing != null && facing.ordinal() == getOrientation()) {
			return (T)getTeslaConsumer();
		}
		return null;
	}

	@Optional.Method(modid = "Tesla")
	private ITeslaConsumer getTeslaConsumer() {
		return (am, sim) -> {
			if (am > flow) am = flow;
			if (am > 0 && out != null && !out.isInvalid()) {
				ITeslaConsumer acc = (ITeslaConsumer)out.getCapability(TeslaConsumer, EnumFacing.VALUES[getOrientation()]);
				if (acc == null) return 0;
				am = acc.givePower(am, sim);
				if (!sim) flow -= am;
				return am;
			} else return 0;
		};
	}

	@Override
	public boolean canConnectEnergy(EnumFacing from) {
		byte dir = getOrientation();
		return from.ordinal() == dir || from.ordinal() == (dir^1);
	}

	@Override
	public int getEnergyStored(EnumFacing from) {
		return 0;
	}

	@Override
	public int getMaxEnergyStored(EnumFacing from) {
		return 0;
	}

	@Override
	public int receiveEnergy(EnumFacing from, int am, boolean sim) {
		if (from.ordinal() == getOrientation()) {
			if (am > flow) am = flow;
			if (am > 0 && out != null && !out.isInvalid() && out instanceof IEnergyReceiver) {
				am = ((IEnergyReceiver)out).receiveEnergy(from, am, sim);
				if (!sim) flow -= am;
				return am;
			} else return 0;
		}
		return 0;
	}

}
