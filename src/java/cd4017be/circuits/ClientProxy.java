/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits;

import cd4017be.circuits.gui.GuiAssembler;
import cd4017be.circuits.gui.GuiCircuit;
import cd4017be.circuits.gui.GuiInvReader;
import cd4017be.circuits.gui.GuiItemTranslocator;
import cd4017be.circuits.gui.GuiProgrammer;
import cd4017be.circuits.gui.GuiRedstoneInterface;
import cd4017be.circuits.render.RSInterfaceRenderer;
import cd4017be.circuits.tileEntity.Display8bit;
import cd4017be.circuits.tileEntity.Lever8bit;
import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.TileBlock;
import cd4017be.lib.TileBlockRegistry;
import cd4017be.lib.TooltipInfo;
import cd4017be.lib.templates.PipeRenderer;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.ResourceLocation;

/**
 *
 * @author CD4017BE
 */
public class ClientProxy extends CommonProxy
{
    
    @Override
    public void registerBlocks() 
    {
        TileBlockRegistry.registerGui(BlockItemRegistry.blockId("tile.programmer"), GuiProgrammer.class);
        TileBlockRegistry.registerGui(BlockItemRegistry.blockId("tile.assembler"), GuiAssembler.class);
        TileBlockRegistry.registerGui(BlockItemRegistry.blockId("tile.circuit"), GuiCircuit.class);
        TileBlockRegistry.registerGui(BlockItemRegistry.blockId("tile.rsInterface"), GuiRedstoneInterface.class);
        TileBlockRegistry.registerGui(BlockItemRegistry.blockId("tile.invReader"), GuiInvReader.class);
        TileBlockRegistry.registerGui(BlockItemRegistry.blockId("tile.itemTranslocator"), GuiItemTranslocator.class);
    }

    @Override
    public void registerRenderers() 
    {
        pipeRenderer = new PipeRenderer();
        RenderingRegistry.registerBlockHandler(pipeRenderer);
        pipeRenderer.setRenderMachine((TileBlock)BlockItemRegistry.getBlock("tile.rsp8bit"));
        pipeRenderer.setRenderMachine((TileBlock)BlockItemRegistry.getBlock("tile.rsp1bit"));
        pipeRenderer.setRenderMachine((TileBlock)BlockItemRegistry.getBlock("tile.invConnector"));
        TileEntityRendererDispatcher.instance.mapSpecialRenderers.put(Lever8bit.class, new RSInterfaceRenderer());
        TileEntityRendererDispatcher.instance.mapSpecialRenderers.put(Display8bit.class, new RSInterfaceRenderer());
        TooltipInfo.loadInfoFile(new ResourceLocation("circuits", "lang/toolTips.txt"));
    }
    
}
