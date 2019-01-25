package cd4017be.circuits.tileEntity;

import cd4017be.api.circuits.IQuickRedstoneHandler;
import cd4017be.lib.block.AdvancedBlock.INeighborAwareTile;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.tileentity.BaseTileEntity.ITickableServerOnly;
import cd4017be.lib.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;


/**
 * @author CD4017BE
 *
 */
public abstract class SyncronousRedstoneIO extends BaseTileEntity implements INeighborAwareTile, IQuickRedstoneHandler, ITickableServerOnly {

	public final int[] rsInput = new int[6];
	private int modified;
	public int timer, interval, phase;

	public SyncronousRedstoneIO() {}
	public SyncronousRedstoneIO(IBlockState state) { super(state); }

	@Override
	public void update() {
		if (++timer < 0) return;
		timer -= interval;
		int m = modified;
		tick(m & 0x3f);
		modified = m >>>= 6;
		for (int i = 0; m != 0; m >>>= 1)
			if ((m & 1) != 0) {
				EnumFacing side = EnumFacing.VALUES[i];
				rsInput[i] = world.getRedstonePower(pos.offset(side), side);
			}
	}

	/**
	 * periodic tick cycle
	 * @param rsDirty binary encoded redstone input dirty flags
	 */
	protected abstract void tick(int rsDirty);

	@Override
	public void onRedstoneStateChange(EnumFacing side, int value, TileEntity src) {
		int i = side.ordinal(), v = rsInput[i];
		if (value == v) return;
		if (timer == -1 && (int)(world.getTotalWorldTime() % interval) == phase)
			modified |= 0x40 << i;
		else {
			rsInput[i] = value;
			modified |= 1 << i;
		}
	}

	@Override
	public void neighborBlockChange(Block b, BlockPos src) {
		EnumFacing side = Utils.getSide(src, pos);
		if (side != null)
			onRedstoneStateChange(side, world.getRedstonePower(pos.offset(side), side), null);
	}

	@Override
	public void neighborTileChange(TileEntity te, EnumFacing side) {
	}

	@Override
	protected void setupData() {
		if (interval > 0)
			timer = (int)Math.floorMod(world.getTotalWorldTime() - phase, interval);
		else timer = Integer.MIN_VALUE;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.interval = nbt.getShort("int");
		this.phase = nbt.getShort("pha");
		{int[] buff = nbt.getIntArray("rsIn");
		System.arraycopy(buff, 0, rsInput, 0, Math.min(buff.length, rsInput.length));}
		this.modified = 0xfff;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setShort("int", (short)interval);
		nbt.setShort("pha", (short)phase);
		nbt.setIntArray("rsIn", rsInput);
		return super.writeToNBT(nbt);
	}

}
