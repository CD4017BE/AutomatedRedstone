package cd4017be.circuits.tileEntity;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.api.circuits.IQuickRedstoneHandler;
import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.ModTileEntity;
import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.IUpdatable;

public class BitShifter extends ModTileEntity implements IDirectionalRedstone, IQuickRedstoneHandler, IUpdatable {

	public byte ofsI = 0, ofsO = 0, size = 1;
	public int state;
	private boolean update;

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing s, float X, float Y, float Z) {
		if (worldObj.isRemote) return true;
		if (player.isSneaking()) {
			if (item == null) {
				if (size > 1) {
					dropStack(BlockItemRegistry.stack("m.IORelay", size - 1));
					size = 1;
					onNeighborBlockChange(null);
					markUpdate();
				} else return false;
			} else return false;
		} else if (item == null) {
			int t = s.ordinal() >> 1;
			float f;
			switch(getOrientation()) {
			case 0: f = Y; t = (4 - t) % 3; break;
			case 1: f = 1F-Y; t = (4 - t) % 3; break;
			case 2: f = Z; break;
			case 3: f = 1F-Z; break;
			case 4: f = X; t = (3 - t) % 3; break;
			default: f = 1F-X; t = (3 - t) % 3;
			}
			if (t == 1) return false;
			int n = 32 - size;
			int p = Math.round(f * 32F - (float)size / 2F);
			if (p < 0) p = 0;
			else if (p > n) p = n;
			if (t == 2) {
				ofsI = (byte)p;
				onNeighborBlockChange(null);
			} else {
				ofsO = (byte)p;
				worldObj.notifyBlockOfStateChange(pos.offset(EnumFacing.VALUES[getOrientation()^1]), Blocks.REDSTONE_TORCH);
			}
			markUpdate();
		} else if (item.isItemEqual(BlockItemRegistry.stack("m.IORelay", 1))) {
			int n = 32 - size;
			if (item.stackSize > n) {
				item.stackSize -= n;
				size += n;
			} else {
				size += item.stackSize;
				item = null;
			}
			player.setHeldItem(hand, item);
			onNeighborBlockChange(null);
			markUpdate();
		}
		return true;
	}

	@Override
	public void onNeighborBlockChange(Block b) {
		EnumFacing dir = EnumFacing.VALUES[getOrientation()];
		setInput(worldObj.getRedstonePower(pos.offset(dir), dir), dir);
	}

	@Override
	public void onRedstoneStateChange(EnumFacing side, int value, TileEntity src) {
		EnumFacing dir = EnumFacing.VALUES[getOrientation()];
		if (side == dir) setInput(value, dir);
	}

	private void setInput(int value, EnumFacing dir) {
		value >>>= ofsI;
		value &= 0xffffffff >>> (32 - size);
		if (value != state) {
			state = value;
			ICapabilityProvider te = getTileOnSide(dir.getOpposite());
			if (te != null && te instanceof IQuickRedstoneHandler)
				((IQuickRedstoneHandler)te).onRedstoneStateChange(dir, state << ofsO, this);
			else if (!update) {
				update = true;
				TickRegistry.instance.updates.add(this);
			}
		}
	}

	@Override
	public int redstoneLevel(int s, boolean str) {
		return str || s != (getOrientation()^1) ? 0 : state << ofsO;
	}

	@Override
	public byte getRSDirection(EnumFacing s) {
		byte dir = getOrientation();
		return (byte)(s.ordinal() == dir ? 1 : (s.ordinal()^1) == dir ? 2 : 0);
	}

	@Override
	public void process() {
		update = false;
		worldObj.notifyBlockOfStateChange(pos.offset(EnumFacing.VALUES[getOrientation()^1]), Blocks.REDSTONE_TORCH);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		ofsI = nbt.getByte("ofsI");
		ofsO = nbt.getByte("ofsO");
		size = nbt.getByte("size");
		state = nbt.getInteger("state");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setByte("ofsI", ofsI);
		nbt.setByte("ofsO", ofsO);
		nbt.setByte("size", size);
		nbt.setInteger("state", state);
		return super.writeToNBT(nbt);
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setByte("ofsI", ofsI);
		nbt.setByte("ofsO", ofsO);
		nbt.setByte("size", size);
		return new SPacketUpdateTileEntity(pos, -1, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		NBTTagCompound nbt = pkt.getNbtCompound();
		ofsI = nbt.getByte("ofsI");
		ofsO = nbt.getByte("ofsO");
		size = nbt.getByte("size");
	}

	@Override
	public double getMaxRenderDistanceSquared() {
		return 256.0;
	}

	@Override
	public void breakBlock() {
		if (size > 1) {
			dropStack(BlockItemRegistry.stack("m.IORelay", size - 1));
			size = 1;
		}
	}

}
