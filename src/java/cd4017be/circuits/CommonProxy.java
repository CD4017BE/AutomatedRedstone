package cd4017be.circuits;

import static cd4017be.circuits.Objects.*;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.TileContainer;

/**
 *
 * @author CD4017BE
 */
public class CommonProxy {

	public void registerRenderers() {}

	public void registerBlocks() {
		BlockGuiHandler.registerContainer(ASSEMBLER, TileContainer.class);
		BlockGuiHandler.registerContainer(CIRCUIT, DataContainer.class);
		BlockGuiHandler.registerContainer(POTENTIOMETER, DataContainer.class);
		BlockGuiHandler.registerContainer(DISPLAY, DataContainer.class);
		BlockGuiHandler.registerContainer(SENSOR_READER, TileContainer.class);
		BlockGuiHandler.registerContainer(OSZILLOGRAPH, TileContainer.class);
		BlockGuiHandler.registerContainer(DESIGNER, TileContainer.class);
		BlockGuiHandler.registerContainer(FLUID_VALVE, DataContainer.class);
		BlockGuiHandler.registerContainer(ENERGY_VALVE, DataContainer.class);
		BlockGuiHandler.registerContainer(RSP_SHIFT, DataContainer.class);
	}

}
