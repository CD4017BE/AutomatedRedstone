/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits.item;

import java.util.List;

import cd4017be.circuits.RedstoneCircuits;
import cd4017be.lib.DefaultItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 *
 * @author CD4017BE
 */
public class ItemProgramm extends DefaultItem
{
    
    public ItemProgramm(String id)
    {
        super(id);
        this.setCreativeTab(RedstoneCircuits.tabCircuits);
    }

    @Override
    public void addInformation(ItemStack item, EntityPlayer player, List list, boolean par4) 
    {
        if (item.getTagCompound() != null) {
            list.add(item.getTagCompound().getString("name"));
        }
        super.addInformation(item, player, list, par4);
    }
    
}
