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
		BlockGuiHandler.registerGui(display8bit, GuiDisplay8bit.class);
		BlockGuiHandler.registerGui(blockSensor, GuiBlockSensor.class);
		BlockGuiHandler.registerGui(oszillograph, GuiOszillograph.class);
		BlockGuiHandler.registerGui(designer, GuiCircuitDesigner.class);
		BlockGuiHandler.registerGui(fluidValve, GuiFluidValve.class);
		BlockGuiHandler.registerGui(energyValve, GuiEnergyValve.class);
		
		invConnector.setBlockLayer(BlockRenderLayer.CUTOUT);
		
		SpecialModelLoader.setMod("circuits");
		SpecialModelLoader.registerBlockModel(rsp1bit, new MultipartModel(rsp1bit).setPipeVariants(3));
		SpecialModelLoader.registerBlockModel(rsp8bit, new MultipartModel(rsp8bit).setPipeVariants(4));
		SpecialModelLoader.registerBlockModel(invConnector, new MultipartModel(invConnector).setPipeVariants(3));
	}

	@Override
	public void registerRenderers() {
		BlockItemRegistry.registerRender(designer);
		BlockItemRegistry.registerRender(assembler);
		BlockItemRegistry.registerRender(circuit, 0, 2);
		BlockItemRegistry.registerRender(rsp8bit);
		BlockItemRegistry.registerRender(rsp1bit, 0, 2);
		BlockItemRegistry.registerRender(bitShifter);
		BlockItemRegistry.registerRender(lever8bit);
		BlockItemRegistry.registerRender(potentiometer);
		BlockItemRegistry.registerRender(display8bit);
		BlockItemRegistry.registerRender(blockSensor);
		BlockItemRegistry.registerRender(invConnector);
		BlockItemRegistry.registerRender(circuitPlan);
		BlockItemRegistry.registerRender(itemSensor);
		BlockItemRegistry.registerRender(fluidSensor);
		BlockItemRegistry.registerRender(energySensor);
		BlockItemRegistry.registerRender(timeSensor);
		BlockItemRegistry.registerRender(oszillograph);
		BlockItemRegistry.registerRender(fluidValve);
		BlockItemRegistry.registerRender(energyValve);
		BlockItemRegistry.registerRender(wirelessCon, 0, 1);
		ClientRegistry.bindTileEntitySpecialRenderer(MultiLever.class, new RSInterfaceRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(Display8bit.class, new RSInterfaceRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(Oszillograph.class, new OszillographRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(Potentiometer.class, new PotentiometerRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(BitShifter.class, new BitShiftRenderer());
	}

}
