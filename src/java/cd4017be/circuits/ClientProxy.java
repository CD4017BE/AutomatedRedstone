package cd4017be.circuits;

import cd4017be.circuits.gui.*;
import cd4017be.circuits.render.BitShiftRenderer;
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
import net.minecraft.util.BlockRenderLayer;
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
		BlockGuiHandler.registerGui(assembler, GuiAssembler.class);
		BlockGuiHandler.registerGui(circuit, GuiCircuit.class);
		BlockGuiHandler.registerGui(potentiometer, GuiPotentiometer.class);
		BlockGuiHandler.registerGui(display, GuiDisplay8bit.class);
		BlockGuiHandler.registerGui(sensor_reader, GuiBlockSensor.class);
		BlockGuiHandler.registerGui(oszillograph, GuiOszillograph.class);
		BlockGuiHandler.registerGui(designer, GuiCircuitDesigner.class);
		BlockGuiHandler.registerGui(fluid_valve, GuiFluidValve.class);
		BlockGuiHandler.registerGui(energy_valve, GuiEnergyValve.class);
		
		inv_connector.setBlockLayer(BlockRenderLayer.CUTOUT);
		
		SpecialModelLoader.setMod("circuits");
		SpecialModelLoader.registerBlockModel(rsp_basic, new MultipartModel(rsp_basic).setPipeVariants(3));
		SpecialModelLoader.registerBlockModel(rsp_32bit, new MultipartModel(rsp_32bit).setPipeVariants(4));
		SpecialModelLoader.registerBlockModel(inv_connector, new MultipartModel(inv_connector).setPipeVariants(3));
	}

	@Override
	public void registerRenderers() {
		BlockItemRegistry.registerRender(designer);
		BlockItemRegistry.registerRender(assembler);
		BlockItemRegistry.registerRender(circuit, 0, 2);
		BlockItemRegistry.registerRender(rsp_32bit);
		BlockItemRegistry.registerRender(rsp_basic, 0, 2);
		BlockItemRegistry.registerRender(bit_shifter);
		BlockItemRegistry.registerRender(multilever);
		BlockItemRegistry.registerRender(potentiometer);
		BlockItemRegistry.registerRender(display);
		BlockItemRegistry.registerRender(sensor_reader);
		BlockItemRegistry.registerRender(inv_connector);
		BlockItemRegistry.registerRender(circuit_plan);
		BlockItemRegistry.registerRender(item_sensor);
		BlockItemRegistry.registerRender(fluid_sensor);
		BlockItemRegistry.registerRender(energy_sensor);
		BlockItemRegistry.registerRender(time_sensor);
		BlockItemRegistry.registerRender(oszillograph);
		BlockItemRegistry.registerRender(fluid_valve);
		BlockItemRegistry.registerRender(energy_valve);
		BlockItemRegistry.registerRender(wireless_con, 0, 1);
		ClientRegistry.bindTileEntitySpecialRenderer(MultiLever.class, new RSInterfaceRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(Display8bit.class, new RSInterfaceRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(Oszillograph.class, new OszillographRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(Potentiometer.class, new PotentiometerRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(BitShifter.class, new BitShiftRenderer());
	}

}
