package cd4017be.api.circuits;

import net.minecraft.item.ItemStack;

/**
 * Interface to be implemented by Items that support the Chip API.
 * @author CD4017BE
 */
public interface IChipItem {

	/**
	 * @param stack ItemStack representing the chip
	 * @param maxIn maximum supported input ports
	 * @param maxOut maximum supported output ports
	 * @param flags additional socket properties
	 * @return true if the provided chip is valid and has no more IO-ports than the given maximum counts
	 * @see #SOCKET_TIER
	 */
	boolean fitsInSocket(ItemStack stack, int maxIn, int maxOut, int flags);

	/**
	 * @param stack ItemStack representing the chip
	 * @return a new chip instance for the given item, must not be null!
	 * @see Chip#NULL_CHIP
	 */
	Chip provideChip(ItemStack stack);

	/**
	 * called to save internal state changes of a chip on its ItemStack for persistence
	 * @param stack ItemStack that provided the chip instance
	 * @param chip the chip instance
	 */
	void saveState(ItemStack stack, Chip chip);

	/** 
	 * chip socket property flags<br>
	 * These are generally used to specify what actions a socket allows a chip to perform.
	 * @see #fitsInSocket(ItemStack, int, int, int)
	 */
	public static final int
		SOCKET_TIER = 0x000f,
		INTERACT_BLOCKS = 0x0010,
		BREAK_BLOCKS = 0x0030,
		PLACE_BLOCKS = 0x0050,
		OBSERVE_BLOCKS = 0x0080,
		BLOCKS_ANY = 0x00f0,
		INTERACT_ENTITIES = 0x0100,
		KILL_ENTITIES = 0x0300,
		SPAWN_ENTITIES = 0x0500,
		MOVE_ENTITIES = 0x0800,
		OBSERVE_ENTITIES = 0x1000,
		ENTITIES_ANY = 0x1f00,
		HIGH_RANGE = 0x2000,
		USE_ENERGY = 0x4000;

}