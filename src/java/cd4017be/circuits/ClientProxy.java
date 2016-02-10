/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits;

import cd4017be.circuits.gui.GuiArithmeticConverter;
import cd4017be.circuits.gui.GuiAssembler;
import cd4017be.circuits.gui.GuiCircuit;
import cd4017be.circuits.gui.GuiInvReader;
import cd4017be.circuits.gui.GuiItemTranslocator;
import cd4017be.circuits.gui.GuiProgrammer;
import cd4017be.circuits.gui.GuiLogicConverter;
import cd4017be.circuits.render.RSInterfaceRenderer;
import cd4017be.circuits.tileEntity.Display8bit;
import cd4017be.circuits.tileEntity.InvConnector;
import cd4017be.circuits.tileEntity.Lever8bit;
import cd4017be.circuits.tileEntity.RSPipe1;
import cd4017be.circuits.tileEntity.RSPipe8;
import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.TileBlockRegistry;
import cd4017be.lib.render.PipeRenderer;
import net.minecraftforge.fml.client.registry.ClientRegistry;

/**
 *
 * @author CD4017BE
 */
public class ClientProxy extends CommonProxy
{
    
    @Override
    public void registerGUIs() 
    {
        TileBlockRegistry.registerGui(BlockItemRegistry.blockId("programmer"), GuiProgrammer.class);
        TileBlockRegistry.registerGui(BlockItemRegistry.blockId("assembler"), GuiAssembler.class);
        TileBlockRegistry.registerGui(BlockItemRegistry.blockId("circuit"), GuiCircuit.class);
        TileBlockRegistry.registerGui(BlockItemRegistry.blockId("logicConv"), GuiLogicConverter.class);
        TileBlockRegistry.registerGui(BlockItemRegistry.blockId("calcConv"), GuiArithmeticConverter.class);
        TileBlockRegistry.registerGui(BlockItemRegistry.blockId("invReader"), GuiInvReader.class);
        TileBlockRegistry.registerGui(BlockItemRegistry.blockId("itemTranslocator"), GuiItemTranslocator.class);
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
        ClientRegistry.bindTileEntitySpecialRenderer(RSPipe1.class, new PipeRenderer("circuits:RSPipe1", "N", "I", "O"));
        ClientRegistry.bindTileEntitySpecialRenderer(RSPipe8.class, new PipeRenderer("circuits:RSPipe8", "N", "I", "O"));
        ClientRegistry.bindTileEntitySpecialRenderer(InvConnector.class, new PipeRenderer("circuits:InvCon", "B", "C", "L"));
    }
    
    private void registerAdditionalModels()
    {
    	BlockItemRegistry.registerModels("rsp1bit", "rsp1bit", "rsp1bit_1", "rsp1bit_2", 
    			"RSPipe1N_con", "RSPipe1N_core", "RSPipe1I_con", "RSPipe1I_core", "RSPipe1O_con", "RSPipe1O_core");//Workaround for pipe models
    	BlockItemRegistry.registerModels("rsp8bit", "rsp8bit", 
    			"RSPipe8N_core", "RSPipe8N_con", "RSPipe8I_con", "RSPipe8O_con");
    	BlockItemRegistry.registerModels("invConnector", "invConnector", 
    			"InvConB_core", "InvConB_con", "InvConC_con", "InvConL_con");
    	BlockItemRegistry.registerModels("wireless8bit", "wireless8bit", "wireless8bit_1", "wireless8bit_2");
    }
    
}
