package cd4017be.circuits.tileEntity;

import cd4017be.api.circuits.IDirectionalRedstone;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

/**
 *
 * @author CD4017BE
 */
public class BasicRSPipe extends IntegerPipe {

	@Override
	protected int bitSize() {return 4;}

	@Override
	protected void checkCons() {
		int type = getBlockMetadata();
		ICapabilityProvider te;
		byte d;
		short io = 0;
		for (EnumFacing s : EnumFacing.VALUES) {
			if (!comp.canConnect((byte)s.ordinal())) continue;
			if ((te = getTileOnSide(s)) != null) {
				if (te instanceof IntegerPipe) continue;
				if (te instanceof IDirectionalRedstone && (d = ((IDirectionalRedstone)te).getRSDirection(s.getOpposite())) >= 0 && d < 3) {
					io |= (d == 1 ? 2 : 1) << (s.ordinal() * 2);
					continue;
				}
			}
			if (!worldObj.getBlockState(pos.offset(s)).getMaterial().isReplaceable()) io |= type << (s.ordinal() * 2);
		}
		if (io != comp.rsIO) {
			comp.network.setIO(comp, io);
			this.markUpdate();
		}
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
				ICapabilityProvider te = this.getTileOnSide(dir);
				if (te != null && te instanceof BasicRSPipe) {
					BasicRSPipe pipe = (BasicRSPipe)te;
					pipe.comp.setConnect((byte)(s^1), con);
					pipe.checkCons();
					pipe.markUpdate();
				}
				checkCons();
				this.markUpdate();
			}
			return true;
		} else return false;
	}

	@Override
	public int redstoneLevel(int s, boolean str) {
		return !str && (comp.rsIO >> (s * 2) & 2) != 0 ? comp.convertSignal(comp.network.outputState) : 0;
	}

	@Override
	public int textureForSide(byte s) {
		if (s == -1) return getBlockMetadata();
		int c = comp.rsIO >> (s * 2) & 3;
		if (c != 0) return c;
		return comp.canConnect(s) && comp.getNeighbor(s) != null ? 0 : -1;
	}

}
