package cd4017be.circuits.tileEntity;

import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.circuits.multiblock.RedstoneNetwork;
import cd4017be.circuits.multiblock.RedstoneNode;
import cd4017be.lib.block.AdvancedBlock.IInteractiveTile;
import cd4017be.lib.util.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

/**
 * @author CD4017BE
 *
 */
public class IntegerPipe extends RedstonePipe implements IInteractiveTile {

	public IntegerPipe() {
		new RedstoneNetwork(comp = new RedstoneNode(this, true));
	}

	@Override
	protected void combineInputs() {
		int value = 0;
		for (int s : inputs) value |= s;
		if (value != comp.inputState) {
			comp.inputState = value;
			comp.network.markStateDirty();
			markDirty();
		}
	}

	protected void checkCon(EnumFacing side) {
		int i = side.ordinal();
		if (comp.canConnect((byte)i) && (comp.rsIO >> (i << 1) & 3) == 0) {
			ICapabilityProvider te = Utils.neighborTile(this, side);
			byte d;
			if (te instanceof IDirectionalRedstone && (d = ((IDirectionalRedstone)te).getRSDirection(side.getOpposite())) != 0) {
				short io = (short)(comp.rsIO | ((d & 1) << 1 | (d & 2) >> 1) << (side.ordinal() * 2));
				comp.network.setIO(comp, io);
				this.markUpdate();
				if ((d == 1 || d == 3) && comp.network.outputState != 0)
					world.neighborChanged(pos.offset(side), blockType, pos);
			}
		}
	}

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing dir, float X, float Y, float Z) {
		if (world.isRemote) return true;
		if (cover.interact(this, player, hand, item, dir, X, Y, Z)) return true;
		if (item.isEmpty()) {
			dir = Utils.hitSide(X, Y, Z);
			byte s = (byte)dir.getIndex();
			if (player.isSneaking()) {
				boolean con = !comp.canConnect(s);
				ICapabilityProvider te = Utils.neighborTile(this, dir);
				if (te instanceof IntegerPipe) {
					IntegerPipe pipe = (IntegerPipe)te;
					if (pipe.getRSDirection(dir.getOpposite()) != 0) con = false;
					else {
						pipe.comp.setConnect((byte)(s^1), con);
						pipe.markUpdate();
						pipe.markDirty();
					}
				}
				comp.setConnect(s, con);
			} else {
				setIO(dir, (comp.rsIO >> (s<<1)) + 1);
			}
			markUpdate();
			markDirty();
			return true;
		} else return false;
	}

	@Override
	public void onClicked(EntityPlayer player) {
		if (!world.isRemote) cover.hit(this, player);
	}

	public void setIO(EnumFacing side, int io) {
		io &= 3;
		int i = side.getIndex(), s = i << 1;
		comp.network.setIO(comp, (short)((comp.rsIO & ~(3 << s)) | io << s));
		ICapabilityProvider te = Utils.neighborTile(this, side);
		if (te instanceof IntegerPipe) {
			IntegerPipe pipe = (IntegerPipe)te;
			pipe.comp.setConnect((byte)(side.ordinal()^1), false);
			io = (io << 1 | io >> 1) & 3;
			s ^= 2;
			pipe.comp.network.setIO(pipe.comp, (short)((pipe.comp.rsIO & ~(3 << s)) | io << s));
			pipe.markUpdate();
			pipe.markDirty();
		}
		BlockPos p1 = pos.offset(side);
		if ((io & 1) == 0 && inputs != null && inputs[i] != 0) {
			inputs[i] = 0;
			combineInputs();
		} else neighborBlockChange(null, p1);
		world.neighborChanged(p1, blockType, pos);
	}

}
