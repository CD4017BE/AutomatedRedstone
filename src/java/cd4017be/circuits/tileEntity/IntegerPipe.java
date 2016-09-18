package cd4017be.circuits.tileEntity;

import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.api.circuits.IntegerComp;
import cd4017be.api.circuits.SharedInteger;
import cd4017be.lib.templates.IPipe;
import cd4017be.lib.templates.MutiblockTile;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

/**
 *
 * @author CD4017BE
 */
public class IntegerPipe extends MutiblockTile<IntegerComp, SharedInteger> implements IPipe {

	private Cover cover = null;

	public IntegerPipe() {
		comp = new IntegerComp(this);
		new SharedInteger(comp);
	}

	@Override
	public void onNeighborBlockChange(Block b) {
		if (b != Blocks.REDSTONE_TORCH) {
			ICapabilityProvider te;
			byte d;
			short io = comp.rsIO;
			for (EnumFacing s : EnumFacing.VALUES)
				if ((te = this.getTileOnSide(s)) != null && te instanceof IDirectionalRedstone && (d = ((IDirectionalRedstone)te).getRSDirection(s.getOpposite())) != 0)
					io |= ((d & 1) << 1 | (d & 2) >> 1) << (s.ordinal() * 2);
			if (io != comp.rsIO) {
				comp.network.setIO(comp, io);
				this.markUpdate();
			}
		}
		comp.updateInput();
	}

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing dir, float X, float Y, float Z) {
		if (!player.isSneaking() && cover == null && item != null && (cover = Cover.create(item)) != null) {
			if (worldObj.isRemote) return true;
			item.stackSize--;
			if (item.stackSize <= 0) item = null;
			player.setHeldItem(hand, item);
			this.markUpdate();
			return true;
		} else if (item == null) {
			if (worldObj.isRemote) return true;
			if (player.isSneaking() && cover != null) {
				this.dropStack(cover.item);
				cover = null;
				this.markUpdate();
				return true;
			}
			dir = this.getClickedSide(X, Y, Z);
			byte s = (byte)dir.getIndex();
			if (player.isSneaking()) {
				boolean con = !comp.canConnect(s);
				comp.setConnect(s, con);
				this.updateCon = true;
				ICapabilityProvider te = this.getTileOnSide(dir);
				if (te != null && te instanceof IntegerPipe) {
					IntegerPipe pipe = (IntegerPipe)te;
					pipe.comp.setConnect((byte)(s^1), con);
					pipe.updateCon = true;
					pipe.markUpdate();
				}
			} else {
				s *= 2;
				comp.network.setIO(comp, (short)((comp.rsIO & ~(3 << s)) | ((comp.rsIO >> s) + 1 & 3) << s));
			}
			this.markUpdate();
			return true;
		} else return false;
	}

	@Override
	public int redstoneLevel(int s, boolean str) {
		return !str && (comp.rsIO >> (s * 2) & 2) != 0 ? comp.network.outputState : 0;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		if (cover != null) cover.write(nbt, "cover");
		comp.writeToNBT(nbt);
		return super.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		cover = Cover.read(nbt, "cover");
		comp.readFromNBT(nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		cover = Cover.read(pkt.getNbtCompound(), "cover");
		comp.readFromNBT(pkt.getNbtCompound());
		this.markUpdate();
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		comp.writeToNBT(nbt);
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
	public Cover getCover() {
		return cover;
	}

	@Override
	public int textureForSide(byte s) {
		if (s == -1) return 0;
		int c = comp.rsIO >> (s * 2) & 3;
		if (c != 0) return c;
		return comp.canConnect(s) && comp.getNeighbor(s) != null ? 0 : -1;
	}

}