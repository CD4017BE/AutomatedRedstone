package cd4017be.circuits;

import static cd4017be.circuits.Objects.*;
import cd4017be.circuits.tileEntity.*;
import cd4017be.lib.TileBlockRegistry;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.TileContainer;

/**
 *
 * @author CD4017BE
 */
public class CommonProxy {

	public void registerRenderers() {}

	public void registerBlocks() {
		TileBlockRegistry.register(programmer, Programmer.class, TileContainer.class);
		TileBlockRegistry.register(assembler, Assembler.class, TileContainer.class);
		TileBlockRegistry.register(circuit, Circuit.class, DataContainer.class);
		TileBlockRegistry.register(rsp8bit, IntegerPipe.class, null);
		TileBlockRegistry.register(rsp1bit, RSPipe1.class, null);
		TileBlockRegistry.register(lever8bit, MultiLever.class, null);
		TileBlockRegistry.register(display8bit, Display8bit.class, DataContainer.class);
		TileBlockRegistry.register(invConnector, InvConnector.class, null);
		TileBlockRegistry.register(blockSensor, BlockSensor.class, TileContainer.class);
		TileBlockRegistry.register(oszillograph, Oszillograph.class, TileContainer.class);
		TileBlockRegistry.register(designer, CircuitDesigner.class, TileContainer.class);
	}

}
