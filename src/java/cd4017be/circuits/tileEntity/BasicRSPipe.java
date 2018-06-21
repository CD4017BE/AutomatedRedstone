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
import net.minecraftforge.common.capabilities.ICapabilityProvider;

/**
 *
 * @author CD4017BE
 */
public class BasicRSPipe extends RedstonePipe implements IInteractiveTile {

	public BasicRSPipe() {
		new RedstoneNetwork(comp = new RedstoneNode(this, false));
	}

	@Override
	protected void combineInputs() {
		int value = 0;
		for (int s : inputs)
			if (s > value) value = s;
		if (value > 255) value = 255;
		if (value != comp.inputState) {
			comp.inputState = value;
			comp.network.markStateDirty();
			markDirty();
		}
	}

	@Override
	protected void checkCon(EnumFacing side) {
		int i = side.ordinal();
		if (comp.canConnect((byte)i)) {
			i <<= 1;
			ICapabilityProvider te = Utils.neighborTile(this, side);
			int io = comp.rsIO >> i & 3, nio;
			byte d;
			if (te instanceof BasicRSPipe) nio = 0;
			else if (te instanceof IDirectionalRedstone && (d = ((IDirectionalRedstone)te).getRSDirection(side.getOpposite())) > 0 && d < 3)
				nio = d == 1 ? 2 : 1;
			else if (!world.getBlockState(pos.offset(side)).getMaterial().isReplaceable())
				nio = getBlockMetadata();
			else nio = 0;
			if (nio != io) {
				comp.network.setIO(comp, (short)(comp.rsIO ^ (nio ^ io) << i));
				markUpdate();
				if (inputs != null && nio != 1) inputs[i >> 1] = 0;
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
				connect(dir, con);
				ICapabilityProvider te = Utils.neighborTile(this, dir);
				if (te instanceof BasicRSPipe)
					((BasicRSPipe)te).connect(dir.getOpposite(), con);
				else world.neighborChanged(pos.offset(dir), blockType, pos);
			}
			return true;
		} else return false;
	}

	@Override
	public void onClicked(EntityPlayer player) {
		if (world.isRemote || cover.hit(this, player)) return;
		//TODO split pipes
	}

	private void connect(EnumFacing side, boolean flag) {
		int i = side.ordinal();
		comp.setConnect((byte)i, flag);
		if (flag) neighborBlockChange(null, pos.offset(side));
		else {
			comp.network.setIO(comp, (short)(comp.rsIO & ~(3 << (i << 1))));
			if (inputs != null && inputs[i] != 0) {
				inputs[i] = 0;
				combineInputs();
			}
		}
		markUpdate();
		markDirty();
	}

}
