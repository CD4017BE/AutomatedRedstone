package multiblock;

import net.minecraft.util.EnumFacing;

/**
 * @author CD4017BE
 *
 */
public interface ICableConnector {

	/**
	 * @param side relative to asked block
	 * @param signalMask the signal channels the cable supports
	 * @return whether a visual connection should be drawn to this block
	 */
	boolean canConnect(EnumFacing side, int signalMask);

}
