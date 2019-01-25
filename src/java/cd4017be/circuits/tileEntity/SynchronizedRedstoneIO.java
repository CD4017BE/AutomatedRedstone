package cd4017be.circuits.tileEntity;

import java.util.function.IntConsumer;

import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.api.circuits.IQuickRedstoneHandler;
import cd4017be.lib.block.AdvancedBlock.INeighborAwareTile;
import cd4017be.lib.block.AdvancedBlock.IRedstoneTile;
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
 * Template TileEntity that provides timing synchronized update ticking ({@link #tick()}) and Redstone interaction with the world ({@link #rsIn} and {@link #rsOut}). 
 * @author CD4017BE
 */
public abstract class SynchronizedRedstoneIO extends BaseTileEntity implements INeighborAwareTile, IQuickRedstoneHandler, IRedstoneTile, IDirectionalRedstone, ITickableServerOnly {

	/** redstone input handlers */
	public final IntConsumer[] rsIn = new IntConsumer[6];
	/** redstone output handlers */
	public final RSOutput[] rsOut = new RSOutput[6];
	/** tick timing controls */
	public int timer, interval, phase;
	private byte lateChng;

	public SynchronizedRedstoneIO() {}
	public SynchronizedRedstoneIO(IBlockState state) { super(state); }

	@Override
	public void update() {
		if (++timer < 0) return;
		tick();
		timer -= interval;
		int m = lateChng;
		lateChng = 0;
		IntConsumer in;
		for (int i = 0; m != 0; m >>>= 1, i++)
			if ((m & 1) != 0 && (in = rsIn[i]) != null) {
				EnumFacing side = EnumFacing.VALUES[i];
				in.accept(world.getRedstonePower(pos.offset(side), side));
			}
	}

	/**
	 * periodic tick cycle
	 */
	protected abstract void tick();

	@Override
	public void onRedstoneStateChange(EnumFacing side, int value, TileEntity src) {
		int i = side.ordinal();
		IntConsumer in = rsIn[i];
		if (in == null) return;
		if (timer == -1 && (int)(world.getTotalWorldTime() % interval) == phase)
			lateChng |= 1 << i;
		else in.accept(value);
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
	public int redstoneLevel(EnumFacing side, boolean strong) {
		if (strong) return 0;
		RSOutput out = rsOut[side.ordinal()];
		return out == null ? 0 : out.state;
	}

	@Override
	public boolean connectRedstone(EnumFacing side) {
		return getRSDirection(side) != 0;
	}

	@Override
	public byte getRSDirection(EnumFacing s) {
		int i = s.ordinal();
		return (byte) ((rsIn[i] != null ? 1 : 0) | (rsOut[i] != null ? 2 : 0));
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
		this.lateChng = 0x3f;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setShort("int", (short)interval);
		nbt.setShort("pha", (short)phase);
		return super.writeToNBT(nbt);
	}

	/**
	 * A Signal pipe for emitting redstone signals through a block face.
	 * @author cd4017be
	 */
	public class RSOutput implements IntConsumer {

		public int state;
		private final EnumFacing side;

		/**
		 * @param side block face to emit at
		 * @param initState the initial state of this output
		 */
		public RSOutput(EnumFacing side, int initState) {
			super();
			this.side = side;
			this.state = initState;
		}

		@Override
		public void accept(int value) {
			if (value != state) {
				state = value;
				TileEntity te = Utils.neighborTile(SynchronizedRedstoneIO.this, side);
				if (te != null && te instanceof IQuickRedstoneHandler)
					((IQuickRedstoneHandler)te).onRedstoneStateChange(side.getOpposite(), value, SynchronizedRedstoneIO.this);
				else world.neighborChanged(pos.offset(side), blockType, pos);
			}
		}

	}

}
