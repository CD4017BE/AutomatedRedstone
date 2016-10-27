package cd4017be.circuits.tileEntity;

import java.util.ArrayList;

import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.circuits.block.BlockRSPipe1;
import cd4017be.lib.ModTileEntity;
import cd4017be.lib.templates.IPipe;
import cd4017be.lib.util.Utils;
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
import net.minecraft.util.ITickable;

/**
 *
 * @author CD4017BE
 */
public class RSPipe1 extends ModTileEntity implements IDirectionalRedstone, IPipe, ITickable {
	private byte state;
	private boolean update = false;
	/** bits[0-13 (6+1)*2]: (side + total) * dir{0:none, 1:in, 2:out, 3:lock} */
	private short flow;
	private boolean updateCon = true;
	private Cover cover = null;

	@Override
	public void update() {
		if (worldObj.isRemote) return;
		if (updateCon) this.updateConnections();
		if (update && (flow & 0x2000) != 0) this.transferSignal();
	}

	private void updateConnections() {
		EnumFacing dir;
		TileEntity te;
		ArrayList<RSPipe1> updateList = new ArrayList<RSPipe1>();
		byte type = (byte)this.getBlockMetadata();
		int lHasIO = getFlowBit(6), nHasIO = 0, lDirIO, nDirIO;
		short lFlow = flow;
		for (int i = 0; i < 6; i++) {
			lDirIO = getFlowBit(i);
			if (lDirIO == 3) continue;
			dir = EnumFacing.VALUES[i];
			te = worldObj.getTileEntity(pos.offset(dir));
			if (te != null && te instanceof RSPipe1) {
				RSPipe1 pipe = (RSPipe1)te;
				int pHasIO = pipe.getFlowBit(6);
				int pDirIO = pipe.getFlowBit(i ^ 1);
				if (pDirIO == 3) nDirIO = 3;
				else if ((nDirIO = pHasIO & ~pDirIO) == 3) 
					nDirIO = lHasIO == 1 && (lDirIO & 1) == 0 ? 2 : lHasIO == 2 && (lDirIO & 2) == 0 ? 1 : 0;
				setFlowBit(i, nDirIO);
				if (nDirIO != 3) nHasIO |= nDirIO;
				updateList.add(pipe);
			} else if (te != null && te instanceof IDirectionalRedstone) {
				byte d = ((IDirectionalRedstone)te).getRSDirection(EnumFacing.VALUES[i^1]);
				d = d == 1 ? 2 : d == 2 ? 1 : (byte)0;
				setFlowBit(i, d);
				nHasIO |= d;
			} else if (type != BlockRSPipe1.ID_Transport && !worldObj.getBlockState(pos.offset(dir)).getMaterial().isReplaceable()) {
				setFlowBit(i, type);
				nHasIO |= type;
			} else setFlowBit(i, 0);
		}
		setFlowBit(6, nHasIO);
		if (flow != lFlow) {
			this.markUpdate();
			for (RSPipe1 pipe : updateList) 
				pipe.onNeighborBlockChange(Blocks.AIR);
			update = true;
		}
		updateCon = false;
	}

	private void transferSignal() {
		byte lstate = state;
		byte nstate;
		state = 0;
		for (byte i = 0; i < 6; i++)
			if (getFlowBit(i) == 1) {
				EnumFacing fd = EnumFacing.VALUES[i];
				if ((nstate = (byte)worldObj.getRedstonePower(pos.offset(fd), fd)) > state) state = nstate;
			}
		if (state != lstate) {//TODO increase transport speed
			for (int i = 0; i < 6; i++)
				if (getFlowBit(i) == 2) {
					EnumFacing fd = EnumFacing.VALUES[i];
					worldObj.notifyBlockOfStateChange(pos.offset(fd), Blocks.REDSTONE_TORCH);
				}
		}
		update = false;
	}

	@Override
	public void onNeighborBlockChange(Block b) {
		if (b != Blocks.REDSTONE_TORCH) updateCon = true;
		update = true;
	}

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing dir, float X, float Y, float Z) {
		if (player.isSneaking() && item == null) {
			if (worldObj.isRemote) return true;
			if (cover != null) {
				this.dropStack(cover.item);
				cover = null;
				this.markUpdate();
				return true;
			}
			dir = this.getClickedSide(X, Y, Z);
			int s = dir.ordinal();
			int lock = getFlowBit(s) == 3 ? 0 : 3;
			setFlowBit(s, lock);
			this.onNeighborBlockChange(Blocks.AIR);
			this.markUpdate();
			TileEntity te = Utils.getTileOnSide(this, (byte)s);
			if (te != null && te instanceof RSPipe1) {
				RSPipe1 pipe = (RSPipe1)te;
				pipe.setFlowBit(s^1, lock);
				pipe.onNeighborBlockChange(Blocks.AIR);
				pipe.markUpdate();
			}
			return true;
		} else if (!player.isSneaking() && cover == null && item != null && (cover = Cover.create(item)) != null) {
			if (worldObj.isRemote) return true;
			item.stackSize--;
			if (item.stackSize <= 0) item = null;
			player.setHeldItem(hand, item);
			this.markUpdate();
			return true;
		} else return false;
	}

	private int getFlowBit(int b) {
		return flow >> (b * 2) & 3;
	}

	private void setFlowBit(int b, int v) {
		b *= 2;
		flow = (short)(flow & ~(3 << b) | (v & 3) << b);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setShort("flow", flow);
		nbt.setByte("state", state);
		if (cover != null) cover.write(nbt, "cover");
		return super.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		flow = nbt.getShort("flow");
		state = nbt.getByte("state");
		cover = Cover.read(nbt, "cover");
		updateCon = true;
		update = true;
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		flow = pkt.getNbtCompound().getShort("flow");
		cover = Cover.read(pkt.getNbtCompound(), "cover");
		this.markUpdate();
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setShort("flow", flow);
		if (cover != null) cover.write(nbt, "cover");
		return new SPacketUpdateTileEntity(pos, -1, nbt);
	}

	@Override
	public void breakBlock() {
		super.breakBlock();
		if (cover != null) {
			this.dropStack(cover.item);
			cover = null;
		}
	}

	@Override
	public byte getRSDirection(EnumFacing s) {
		int b = getFlowBit(s.ordinal());
		return b == 1 ? -1 : b == 2 ? (byte)1 : 0;
	}

	@Override
	public int redstoneLevel(int s, boolean str) {
		if (!str && state > 0 && getFlowBit(s) == 2) return state;
		else return 0;
	}

	@Override
	public Cover getCover() {
		return cover;
	}

	@Override
	public int textureForSide(byte s) {
		if (s == -1) return this.getBlockMetadata();
		TileEntity p;
		int b = getFlowBit(s);
		return b == 3 || (b == 0 && !((p = Utils.getTileOnSide(this, s)) != null && p instanceof IDirectionalRedstone)) ? -1 : b;
	}

}
