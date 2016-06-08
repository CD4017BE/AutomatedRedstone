/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits;

import static cd4017be.circuits.Objects.*;
import static cd4017be.lib.BlockItemRegistry.stack;
import cd4017be.circuits.tileEntity.ArithmeticConverter;
import cd4017be.circuits.tileEntity.Assembler;
import cd4017be.circuits.tileEntity.Circuit;
import cd4017be.circuits.tileEntity.Display8bit;
import cd4017be.circuits.tileEntity.InvConnector;
import cd4017be.circuits.tileEntity.InvReader;
import cd4017be.circuits.tileEntity.ItemTranslocator;
import cd4017be.circuits.tileEntity.Lever8bit;
import cd4017be.circuits.tileEntity.LogicConverter;
import cd4017be.circuits.tileEntity.Programmer;
import cd4017be.circuits.tileEntity.RSPipe1;
import cd4017be.circuits.tileEntity.RSPipe8;
import cd4017be.circuits.tileEntity.Wireless8bit;
import cd4017be.lib.TileBlockRegistry;
import cd4017be.lib.TileContainer;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

/**
 *
 * @author CD4017BE
 */
public class CommonProxy 
{
    
    public void registerRenderers() {}
    
    public void registerBlocks() {
    	TileBlockRegistry.register(programmer, Programmer.class, TileContainer.class);
        TileBlockRegistry.register(assembler, Assembler.class, TileContainer.class);
        TileBlockRegistry.register(circuit, Circuit.class, TileContainer.class);
        TileBlockRegistry.register(rsp8bit, RSPipe8.class, null);
        TileBlockRegistry.register(rsp1bit, RSPipe1.class, null);
        TileBlockRegistry.register(lever8bit, Lever8bit.class, null);
        TileBlockRegistry.register(display8bit, Display8bit.class, TileContainer.class);
        TileBlockRegistry.register(logicConv, LogicConverter.class, TileContainer.class);
        TileBlockRegistry.register(calcConv, ArithmeticConverter.class, TileContainer.class);
        TileBlockRegistry.register(wireless8bit, Wireless8bit.class, null);
        TileBlockRegistry.register(invReader, InvReader.class, TileContainer.class);
        TileBlockRegistry.register(itemTranslocator, ItemTranslocator.class, TileContainer.class);
        TileBlockRegistry.register(invConnector, InvConnector.class, null);
    }
    
    public void registerRecipes()
    {
        GameRegistry.addRecipe(new ShapelessOreRecipe(new ItemStack(Items.PAPER), stack("item.circuitPlan", 1)));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("tile.programmer", 1), "012", "343", "555", '0', Items.FEATHER, '1', Items.PAPER, '2', "dyeBlack", '3', "dustRedstone", '4', Blocks.CRAFTING_TABLE, '5', new ItemStack(Blocks.WOODEN_SLAB, 1, OreDictionary.WILDCARD_VALUE)));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("tile.assembler", 1), "010", "232", "010", '0', "dustRedstone", '1', Blocks.PISTON, '2', Blocks.CHEST, '3', Blocks.CRAFTING_TABLE));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("tile.circuit", 1), " 0 ", "010", " 0 ", '0', Blocks.STONE_SLAB, '1', "blockGlass"));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("tile.lever8bit", 1), "000", "121", "000", '0', Blocks.LEVER, '1', Blocks.STONE_SLAB, '2', stack("tile.rsp8bit", 1)));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("tile.display8bit", 1), "000", "121", "000", '0', "dustGlowstone", '1', Blocks.STONE_SLAB, '2', stack("tile.rsp8bit", 1)));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("tile.rsp8bit", 4), "000", "010", "000", '0', stack("rsp1bitN", 1), '1', "nuggetGold"));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("tile.logicConv", 1), "010", "232", '0', Blocks.REDSTONE_TORCH, '1', "dustRedstone", '2', Blocks.STONE_SLAB, '3', stack("tile.rsp8bit", 1)));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("tile.calcConv", 1), "010", "232", '0', Blocks.REDSTONE_TORCH, '1', "dustRedstone", '2', Items.COMPARATOR, '3', stack("tile.rsp8bit", 1)));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("rsp1bitN", 12), "000", "111", "000", '0', Blocks.IRON_BARS, '1', "dustRedstone"));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("rsp1bitI", 2), "001", '0', stack("rsp1bitN", 1), '1', "dustRedstone"));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("rsp1bitO", 2), "001", '0', stack("rsp1bitN", 1), '1', Blocks.REDSTONE_TORCH));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("tile.wireless8bit", 1), "010", "232", "010", '0', Blocks.OBSIDIAN, '1', Items.ENDER_EYE, '2', stack("tile.rsp8bit", 1), '3', "blockQuartz"));
        GameRegistry.addRecipe(new ShapelessOreRecipe(stack("tile.wireless8bit", 1), stack("tile.wireless8bit", 1, 1), stack("tile.wireless8bit", 1, 2)));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("tile.invConnector", 12), "000", "121", "000", '0', "gemQuartz", '1', "blockGlass", '2', Items.ENDER_PEARL));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("tile.invReader", 1), "010", "232", "040", '0', Blocks.STONE_SLAB, '1', Items.COMPARATOR, '2', "dustRedstone", '3', Blocks.CHEST, '4', stack("tile.rsp8bit", 1)));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("tile.itemTranslocator", 1), "010", "232", "040", '0', Blocks.STONE_SLAB, '1', Items.COMPARATOR, '2', Blocks.PISTON, '3', stack("tile.invConnector", 1), '4', stack("tile.rsp8bit", 1)));
        
        ItemStack item = stack("RstMetall", 1);
        if (item != null) GameRegistry.addRecipe(new ShapedOreRecipe(stack("rsp1bitN", 8), "000", '0', item));
    }
    
}
