package multiblock;

import cd4017be.api.IAbstractTile;
import cd4017be.circuits.Objects;
import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.IUpdatable;
import cd4017be.lib.block.AdvancedBlock.IRedstoneTile;
import cd4017be.lib.templates.MultiblockComp;
import cd4017be.lib.util.Utils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

public class IntegerComp extends MultiblockComp<IntegerComp, SharedInteger> {

	public final int mask;

	public IntegerComp(IAbstractTile tile, int bitSize) {
		super(tile);
		this.mask = 0xffffffff >>> (32 - bitSize);
	}

	public int inputState;
	/** bits[0-13 8*(1+1)]: side*(in+out) */
	public short rsIO;

	public void setUID(long uid) {
		super.setUID(uid);
		if (network == null) new SharedInteger(this);
		network.setIO(this, rsIO);
	}

	public void onStateChange() {
		for (int i = 0; i < 6; i++)
			if ((rsIO >> (i * 2) & 2) != 0)
				Utils.updateRedstoneOnSide((TileEntity & IRedstoneTile) tile, EnumFacing.VALUES[i]);
	}

	@Override
	public void setConnect(byte side, boolean c) {
		boolean c0 = canConnect(side);
		if (!c && c0) {
			network.onDisconnect(this, side);
			con &= ~(1 << side);
		} else if (c && !c0) {
			con |= 1 << side;
			if (!updateCon) {
				updateCon = true;
				TickRegistry.instance.updates.add((IUpdatable)tile);
			}
		}
	}

	@Override
	public IntegerComp getNeighbor(byte side) {
		IntegerComp c = super.getNeighbor(side);
		return c == null || c.mask != mask ? null : c;
	}

	public void readFromNBT(NBTTagCompound nbt) {
		con = nbt.getByte("con");
		rsIO = nbt.getShort("io");
		if (network != null) network.setIO(this, rsIO);
		inputState = nbt.getInteger("state");
	}

	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setByte("con", con);
		nbt.setShort("io", rsIO);
		nbt.setInteger("state", inputState); //inputState is saved to ensure blocks don't get incomplete redstone states after chunkload.
	}

	@Override
	public Capability<IntegerComp> getCap() {
		return Objects.RS_INTEGER_CAPABILITY;
	}

}
