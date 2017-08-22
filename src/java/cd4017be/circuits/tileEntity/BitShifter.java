package cd4017be.circuits.tileEntity;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import java.util.List;

import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.api.circuits.IQuickRedstoneHandler;
import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.IUpdatable;
import cd4017be.lib.block.AdvancedBlock.IInteractiveTile;
import cd4017be.lib.block.AdvancedBlock.INeighborAwareTile;
import cd4017be.lib.block.AdvancedBlock.IRedstoneTile;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.block.BaseTileEntity;
import cd4017be.lib.util.Utils;

public class BitShifter extends BaseTileEntity implements IInteractiveTile, INeighborAwareTile, ITilePlaceHarvest, IRedstoneTile, IDirectionalRedstone, IQuickRedstoneHandler, IUpdatable {

	public byte ofsI = 0, ofsO = 0, size = 1;
	public int state;
	private boolean update;

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing s, float X, float Y, float Z) {
		if (world.isRemote) return true;
		if (player.isSneaking()) {
			if (item.isEmpty()) {
				if (size > 1) {
					dropStack(BlockItemRegistry.stack("m.IORelay", size - 1));
					size = 1;
					neighborBlockChange(null, pos);
					markUpdate();
				} else return false;
			} else return false;
		} else if (item.isEmpty()) {
			EnumFacing dir = getOrientation().front;
			int t = s.ordinal() >> 1;
			float f;
			switch(dir) {
			case DOWN: f = Y; t = (4 - t) % 3; break;
			case UP: f = 1F-Y; t = (4 - t) % 3; break;
			case NORTH: f = Z; break;
			case SOUTH: f = 1F-Z; break;
			case WEST: f = X; t = (3 - t) % 3; break;
			default: f = 1F-X; t = (3 - t) % 3;
			}
			if (t == 1) return false;
			int n = 32 - size;
			int p = Math.round(f * 32F - (float)size / 2F);
			if (p < 0) p = 0;
			else if (p > n) p = n;
			if (t == 2) {
				ofsI = (byte)p;
				neighborBlockChange(null, pos);
			} else {
				ofsO = (byte)p;
				Utils.updateRedstoneOnSide(this, state << ofsO, dir.getOpposite());
			}
			markUpdate();
		} else if (item.isItemEqual(BlockItemRegistry.stack("m.IORelay", 1))) {
			int n = 32 - size;
			if (item.getCount() > n) {
				item.shrink(n);
				size += n;
			} else {
				size += item.getCount();
				item = ItemStack.EMPTY;
			}
			player.setHeldItem(hand, item);
			neighborBlockChange(null, pos);
			markUpdate();
		}
		return true;
	}

	@Override
	public void onClicked(EntityPlayer player) {
	}

	@Override
	public void neighborBlockChange(Block b, BlockPos src) {
		EnumFacing dir = getOrientation().front;
		setInput(world.getRedstonePower(pos.offset(dir), dir), dir);
	}

	@Override
	public void neighborTileChange(BlockPos src) {
	}

	@Override
	public void onRedstoneStateChange(EnumFacing side, int value, TileEntity src) {
		EnumFacing dir = getOrientation().front;
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
	public int redstoneLevel(EnumFacing side, boolean strong) {
		return strong || side != getOrientation().front.getOpposite() ? 0 : state << ofsO;
	}

	@Override
	public boolean connectRedstone(EnumFacing side) {
		return getRSDirection(side) != 0;
	}

	@Override
	public byte getRSDirection(EnumFacing s) {
		EnumFacing dir = getOrientation().front;
		return (byte)(s == dir ? 1 : s.getOpposite() == dir ? 2 : 0);
	}

	@Override
	public void process() {
		update = false;
		Utils.updateRedstoneOnSide(this, state << ofsO, getOrientation().front.getOpposite());
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
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		List<ItemStack> list = makeDefaultDrops(null);
		if (size > 1) list.add(BlockItemRegistry.stack("m.IORelay", size - 1));
		return list;
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
	}

}
