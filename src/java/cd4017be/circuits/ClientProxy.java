package cd4017be.circuits;

import cd4017be.circuits.gui.*;
import cd4017be.circuits.render.BitShiftRenderer;
import cd4017be.circuits.render.DisplayRenderer;
import cd4017be.circuits.render.OszillographRenderer;
import cd4017be.circuits.render.PotentiometerRenderer;
import cd4017be.circuits.render.RSInterfaceRenderer;
import cd4017be.circuits.tileEntity.BitShifter;
import cd4017be.circuits.tileEntity.Display8bit;
import cd4017be.circuits.tileEntity.MultiLever;
import cd4017be.circuits.tileEntity.Oszillograph;
import cd4017be.circuits.tileEntity.Potentiometer;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.render.SpecialModelLoader;
import cd4017be.lib.render.model.MultipartModel;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import static cd4017be.circuits.Objects.*;

/**
 *
 * @author CD4017BE
 */
public class ClientProxy extends CommonProxy {

	@Override
	public void registerBlocks() {
		super.registerBlocks();
		BlockGuiHandler.registerGui(ASSEMBLER, GuiAssembler.class);
		BlockGuiHandler.registerGui(CIRCUIT, GuiCircuit.class);
		BlockGuiHandler.registerGui(POTENTIOMETER, GuiPotentiometer.class);
		BlockGuiHandler.registerGui(DISPLAY, GuiDisplay8bit.class);
		BlockGuiHandler.registerGui(SENSOR_READER, GuiBlockSensor.class);
		BlockGuiHandler.registerGui(OSZILLOGRAPH, GuiOszillograph.class);
		BlockGuiHandler.registerGui(DESIGNER, GuiCircuitDesigner.class);
		BlockGuiHandler.registerGui(FLUID_VALVE, GuiFluidValve.class);
		BlockGuiHandler.registerGui(ENERGY_VALVE, GuiEnergyValve.class);
		BlockGuiHandler.registerGui(ITEM_VALVE, GuiItemValve.class);
		BlockGuiHandler.registerGui(RSP_SHIFT, GuiBitShiftPipe.class);
		
		SpecialModelLoader.setMod("circuits");
		SpecialModelLoader.registerBlockModel(RSP_BASIC, new MultipartModel(RSP_BASIC).setPipeVariants(3));
		SpecialModelLoader.registerBlockModel(RSP_32BIT, new MultipartModel(RSP_32BIT).setPipeVariants(4));
		SpecialModelLoader.registerBlockModel(RSP_SHIFT, new MultipartModel(RSP_SHIFT).setPipeVariants(4));
	}

	@Override
	public void registerRenderers() {
		BlockItemRegistry.registerRender(DESIGNER);
		BlockItemRegistry.registerRender(ASSEMBLER);
		BlockItemRegistry.registerRender(CIRCUIT, 0, 2);
		BlockItemRegistry.registerRender(RSP_32BIT);
		BlockItemRegistry.registerRenderBS(RSP_BASIC, 0, 2);
		BlockItemRegistry.registerRender(RSP_SHIFT);
		BlockItemRegistry.registerRender(BIT_SHIFTER);
		BlockItemRegistry.registerRender(MULTILEVER);
		BlockItemRegistry.registerRender(POTENTIOMETER);
		BlockItemRegistry.registerRender(DISPLAY);
		BlockItemRegistry.registerRender(SENSOR_READER);
		BlockItemRegistry.registerRender(circuit_plan);
		BlockItemRegistry.registerRender(item_sensor);
		BlockItemRegistry.registerRender(fluid_sensor);
		BlockItemRegistry.registerRender(energy_sensor);
		BlockItemRegistry.registerRender(time_sensor);
		BlockItemRegistry.registerRender(OSZILLOGRAPH);
		BlockItemRegistry.registerRender(FLUID_VALVE);
		BlockItemRegistry.registerRender(ENERGY_VALVE);
		BlockItemRegistry.registerRender(ITEM_VALVE);
		BlockItemRegistry.registerRender(WIRELESS_CON, 0, 1);
		ClientRegistry.bindTileEntitySpecialRenderer(MultiLever.class, new RSInterfaceRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(Display8bit.class, new DisplayRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(Oszillograph.class, new OszillographRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(Potentiometer.class, new PotentiometerRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(BitShifter.class, new BitShiftRenderer());
	}

}
