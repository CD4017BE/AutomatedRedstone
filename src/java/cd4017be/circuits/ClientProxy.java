/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits;

import cd4017be.circuits.gui.*;
import cd4017be.circuits.render.RSInterfaceRenderer;
import cd4017be.circuits.tileEntity.Display8bit;
import cd4017be.circuits.tileEntity.Lever8bit;
import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.TileBlockRegistry;
import cd4017be.lib.render.ModelPipe;
import cd4017be.lib.render.SpecialModelLoader;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import static cd4017be.circuits.Objects.*;

/**
 *
 * @author CD4017BE
 */
public class ClientProxy extends CommonProxy
{
    
    @Override
    public void registerBlocks() 
    {
    	super.registerBlocks();
        TileBlockRegistry.registerGui(programmer, GuiProgrammer.class);
        TileBlockRegistry.registerGui(assembler, GuiAssembler.class);
        TileBlockRegistry.registerGui(circuit, GuiCircuit.class);
        TileBlockRegistry.registerGui(logicConv, GuiLogicConverter.class);
        TileBlockRegistry.registerGui(calcConv, GuiArithmeticConverter.class);
        TileBlockRegistry.registerGui(invReader, GuiInvReader.class);
        TileBlockRegistry.registerGui(itemTranslocator, GuiItemTranslocator.class);
        
        invConnector.setBlockLayer(EnumWorldBlockLayer.CUTOUT);
        
        SpecialModelLoader.setMod("circuits");
        SpecialModelLoader.registerBlockModel(rsp1bit, new ModelPipe("circuits:rsp1bit", 3, 3));
        SpecialModelLoader.registerBlockModel(rsp8bit, new ModelPipe("circuits:rsp8bit", 1, 3));
        SpecialModelLoader.registerBlockModel(invConnector, new ModelPipe("circuits:invConnector", 1, 3));
    }

    @Override
    public void registerRenderers() 
    {
    	this.registerAdditionalModels();
        BlockItemRegistry.registerBlockRender("programmer:0");
        BlockItemRegistry.registerBlockRender("assembler:0");
        BlockItemRegistry.registerBlockRender("circuit:0");
        BlockItemRegistry.registerBlockRender("rsp8bit:0");
        BlockItemRegistry.registerBlockRender("rsp1bit:0");
        BlockItemRegistry.registerBlockRender("rsp1bit:1");
        BlockItemRegistry.registerBlockRender("rsp1bit:2");
        BlockItemRegistry.registerBlockRender("lever8bit:0");
        BlockItemRegistry.registerBlockRender("display8bit:0");
        BlockItemRegistry.registerBlockRender("logicConv:0");
        BlockItemRegistry.registerBlockRender("calcConv:0");
        BlockItemRegistry.registerBlockRender("wireless8bit:0");
        BlockItemRegistry.registerBlockRender("wireless8bit:1");
        BlockItemRegistry.registerBlockRender("wireless8bit:2");
        BlockItemRegistry.registerBlockRender("invReader:0");
        BlockItemRegistry.registerBlockRender("itemTranslocator:0");
        BlockItemRegistry.registerBlockRender("invConnector:0");
        BlockItemRegistry.registerItemRender("circuitPlan");
        ClientRegistry.bindTileEntitySpecialRenderer(Lever8bit.class, new RSInterfaceRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(Display8bit.class, new RSInterfaceRenderer());
    }
    
    private void registerAdditionalModels()
    {
    	BlockItemRegistry.registerModels(rsp1bit, "rsp1bit", "rsp1bit_1", "rsp1bit_2");
    	BlockItemRegistry.registerModels(wireless8bit, "wireless8bit", "wireless8bit_1", "wireless8bit_2");
    }
    
}
