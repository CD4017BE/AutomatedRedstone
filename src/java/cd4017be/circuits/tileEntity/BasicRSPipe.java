package cd4017be.circuits.tileEntity;

import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.lib.util.Utils;
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
			if (!world.getBlockState(pos.offset(s)).getMaterial().isReplaceable()) io |= type << (s.ordinal() * 2);
		}
		if (io != comp.rsIO) {
			comp.network.setIO(comp, io);
			this.markUpdate();
		}
	}

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing dir, float X, float Y, float Z) {
		if (world.isRemote) return true;
		if (item == null) {
			dir = Utils.hitSide(X, Y, Z);
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
	public int redstoneLevel(EnumFacing s, boolean str) {
		return !str && (comp.rsIO >> (s.ordinal() * 2) & 2) != 0 ? comp.convertSignal(comp.network.outputState) : 0;
	}

}
