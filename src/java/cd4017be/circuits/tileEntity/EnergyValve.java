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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.lib.BlockGuiHandler.ClientPacketReceiver;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.block.AdvancedBlock.INeighborAwareTile;
import cd4017be.lib.block.AdvancedBlock.IRedstoneTile;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.Utils;

/**
 * 
 * @author cd4017be
 */
public class EnergyValve extends BaseTileEntity implements INeighborAwareTile, IRedstoneTile, ITickable, IDirectionalRedstone, IGuiData, ClientPacketReceiver, IEnergyStorage {

	public static final IEnergyStorage NULL_RECEIVE = new IEnergyStorage() {
		@Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }
		@Override public int getMaxEnergyStored() { return 0; }
		@Override public int getEnergyStored() { return 0; }
		@Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
		@Override public boolean canReceive() { return false; }
		@Override public boolean canExtract() { return true; }
	};

	private IEnergyStorage out;
	public int tickInt = 1;
	public boolean measure;
	public int flow, state;

	@Override
	public void update() {
		if (world.isRemote || world.getTotalWorldTime() % tickInt != 0) return;
		if (measure) {
			flow = Integer.MAX_VALUE - flow;
			if (flow != state) {
				state = flow;
				world.notifyNeighborsOfStateChange(pos, Blocks.REDSTONE_TORCH, false);
			}
			flow = Integer.MAX_VALUE;
		} else if (state > 0) {
			flow = state;
		} else flow = 0;
		markDirty();
	}

	@Override
	public void neighborBlockChange(Block b, BlockPos src) {
		if (measure || world.isRemote) return;
		int ls = state;
		state = 0;
		for (EnumFacing s : EnumFacing.VALUES)
			state |= world.getRedstonePower(pos.offset(s), s);
		if (state != ls) markDirty();
	}

	@Override
	public void neighborTileChange(TileEntity te, EnumFacing side) {
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
	public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
		EnumFacing front = getOrientation().front;
		return cap == CapabilityEnergy.ENERGY && (facing == front || facing == front.getOpposite());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> cap, EnumFacing facing) {
		if (cap == CapabilityEnergy.ENERGY) {
			EnumFacing front = getOrientation().front;
			if (facing == front) {
				out = Utils.neighborCapability(this, front.getOpposite(), CapabilityEnergy.ENERGY);
				return (T) this;
			} else if (facing == front.getOpposite()) return (T) NULL_RECEIVE;
		}
		return null;
	}

	@Override
	public int receiveEnergy(int am, boolean sim) {
		if (am > flow) am = flow;
		if (am > 0 && out != null) {
			am = out.receiveEnergy(am, sim);
			if (!sim) {
				flow -= am;
				markDirty();
			}
			return am;
		}
		return 0;
	}

	@Override
	public int extractEnergy(int am, boolean sim) {
		return 0;
	}

	@Override
	public int getEnergyStored() {
		return measure ? Integer.MAX_VALUE - flow : state - flow;
	}

	@Override
	public int getMaxEnergyStored() {
		return measure ? Integer.MAX_VALUE : state;
	}

	@Override
	public boolean canExtract() {
		return false;
	}

	@Override
	public boolean canReceive() {
		return true;
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
