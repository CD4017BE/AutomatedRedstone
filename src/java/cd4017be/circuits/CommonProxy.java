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
		BlockGuiHandler.registerContainer(assembler, TileContainer.class);
		BlockGuiHandler.registerContainer(circuit, DataContainer.class);
		BlockGuiHandler.registerContainer(potentiometer, DataContainer.class);
		BlockGuiHandler.registerContainer(display8bit, DataContainer.class);
		BlockGuiHandler.registerContainer(blockSensor, TileContainer.class);
		BlockGuiHandler.registerContainer(oszillograph, TileContainer.class);
		BlockGuiHandler.registerContainer(designer, TileContainer.class);
		BlockGuiHandler.registerContainer(fluidValve, DataContainer.class);
		BlockGuiHandler.registerContainer(energyValve, DataContainer.class);
	}

}
