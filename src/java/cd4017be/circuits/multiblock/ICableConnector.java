package cd4017be.circuits.multiblock;

import net.minecraft.util.EnumFacing;

/**
 * @author CD4017BE
 *
 */
public interface ICableConnector {

	/**
	 * @param side relative to asked block
	 * @param digital whether the cable handles signals in digital (bits as 32 individual logic channels) representation instead of analog (as strength value, vanilla: 0-15, basic cables: 0-255)
	 * @return whether a visual connection should be drawn to this block
	 */
	boolean canConnect(EnumFacing side, boolean digital);

}
