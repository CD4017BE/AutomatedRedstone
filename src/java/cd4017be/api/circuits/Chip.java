package cd4017be.api.circuits;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import net.minecraft.tileentity.TileEntity;

/**
 * Redstone processor chip API<br>
 * Instances of this perform the actual logic.
 * @author CD4017BE
 */
public abstract class Chip {

	/** true when this chip should update */
	public boolean dirty = true;

	/** the TileEntity this Chip was placed in (used to interact with the world) */
	protected TileEntity host = null;

	/**
	 * sets the host TileEntity (usually called during initialization)
	 * @param tile the new host TileEntity or null when unloaded / destroyed
	 */
	public void setHost(TileEntity tile) {
		this.host = tile;
	}

	/** update the chips internal logic */
	public abstract void update();

	/**
	 * connect an input pin of the chip
	 * @param idx input pin index
	 * @param port signal supply function (must not be null)
	 * @see #NULL_INPUT
	 */
	public abstract void connectInput(int idx, IntSupplier port);

	/**
	 * connect an output pin of the chip
	 * @param idx output pin index
	 * @param port signal update receiver function (must not be null)
	 * @see #NULL_OUTPUT
	 */
	public abstract void connectOutput(int idx, IntConsumer port);

	/**
	 * @return labels of the provided input pins
	 */
	public abstract String[] inputs();

	/**
	 * @return labels of the provided input pins
	 */
	public abstract String[] outputs();

	/** default signal supply function for unconnected Pins */
	public static final IntSupplier NULL_INPUT = ()-> 0;

	/** default signal update receiver function for unconnected Pins */
	public static final IntConsumer NULL_OUTPUT = (v)-> {};

	/** default chip implementation for empty sockets */
	public static final Chip NULL_CHIP = new Chip() {
		@Override
		public void setHost(TileEntity tile) {}
		@Override
		public void update() { dirty = false; }
		@Override
		public void connectInput(int idx, IntSupplier port) {}
		@Override
		public void connectOutput(int idx, IntConsumer port) {}
		@Override
		public String[] inputs() { return new String[0]; }
		@Override
		public String[] outputs() { return new String[0]; }
	};

}
