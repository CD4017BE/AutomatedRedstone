/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits;

import cd4017be.lib.BlockItemRegistry;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 *
 * @author CD4017BE
 */
public class CreativeTabCircuits extends CreativeTabs
{
    
    public CreativeTabCircuits(String name)
    {
        super(CreativeTabs.getNextID(), name);
    }
    
    @Override
    public ItemStack getIconItemStack() 
    {
        return BlockItemRegistry.stack("tile.circuit", 1);
    }

    @Override
    public String getTranslatedTabLabel() 
    {
        return "Automated Redstone";
    }
    
    @Override
	public Item getTabIconItem() 
	{
		return Item.getItemFromBlock(BlockItemRegistry.blockId("circuit"));
	}
    
}
