/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits;

import cd4017be.lib.templates.PipeRenderer;
import static cd4017be.lib.BlockItemRegistry.stack;
import cpw.mods.fml.common.registry.GameRegistry;
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
    protected PipeRenderer pipeRenderer;
    
    public void registerRenderers() {}
    
    public void registerBlocks() {}
    
    public void registerRecipes()
    {
        GameRegistry.addRecipe(new ShapelessOreRecipe(new ItemStack(Items.paper), stack("item.circuitPlan", 1)));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("tile.programmer", 1), "012", "343", "555", '0', new ItemStack(Items.feather), '1', new ItemStack(Items.paper), '2', "dyeBlack", '3', "dustRedstone", '4', new ItemStack(Blocks.crafting_table), '5', new ItemStack(Blocks.wooden_slab, 1, OreDictionary.WILDCARD_VALUE)));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("tile.assembler", 1), "010", "232", "010", '0', "dustRedstone", '1', new ItemStack(Blocks.piston), '2', new ItemStack(Blocks.chest), '3', new ItemStack(Blocks.crafting_table)));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("tile.circuit", 1), " 0 ", "010", " 0 ", '0', new ItemStack(Blocks.stone_slab), '1', "blockGlass"));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("tile.lever8bit", 1), "000", "121", "000", '0', new ItemStack(Blocks.lever), '1', new ItemStack(Blocks.stone_slab), '2', stack("tile.rsp8bit", 1)));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("tile.display8bit", 1), "000", "121", "000", '0', "dustGlowstone", '1', new ItemStack(Blocks.stone_slab), '2', stack("tile.rsp8bit", 1)));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("tile.rsp8bit", 4), "000", "010", "000", '0', stack("rsp1bitN", 1), '1', "ingotGold"));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("tile.rsInterface", 1), " 0 ", "121", '0', "dustRedstone", '1', new ItemStack(Blocks.stone_slab), '2', stack("tile.rsp8bit", 1)));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("rsp1bitN", 12), "000", "111", "000", '0', new ItemStack(Blocks.iron_bars), '1', "dustRedstone"));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("rsp1bitI", 2), "001", '0', stack("rsp1bitN", 1), '1', "dustRedstone"));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("rsp1bitO", 2), "001", '0', stack("rsp1bitN", 1), '1', new ItemStack(Blocks.redstone_torch)));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("tile.wireless8bit", 1), "010", "232", "010", '0', new ItemStack(Blocks.stone_slab), '1', new ItemStack(Items.ender_eye), '2', stack("tile.rsp8bit", 1), '3', "blockQuartz"));
        GameRegistry.addRecipe(new ShapelessOreRecipe(stack("tile.wireless8bit", 1), stack("tile.wireless8bit", 1, 1), stack("tile.wireless8bit", 1, 2)));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("tile.invConnector", 12), "000", "121", "000", '0', "gemQuartz", '1', "blockGlass", '2', new ItemStack(Items.ender_pearl)));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("tile.invReader", 1), "010", "232", "040", '0', new ItemStack(Blocks.stone_slab), '1', new ItemStack(Items.comparator), '2', "dustRedstone", '3', new ItemStack(Blocks.chest), '4', stack("tile.rsp8bit", 1)));
        GameRegistry.addRecipe(new ShapedOreRecipe(stack("tile.itemTranslocator", 1), "010", "232", "040", '0', new ItemStack(Blocks.stone_slab), '1', new ItemStack(Items.comparator), '2', new ItemStack(Blocks.piston), '3', stack("tile.invConnector", 1), '4', stack("tile.rsp8bit", 1)));
        
        ItemStack item = stack("RstMetall", 1);
        if (item != null) GameRegistry.addRecipe(new ShapedOreRecipe(stack("rsp1bitN", 8), "000", '0', item));
    }
    
}
