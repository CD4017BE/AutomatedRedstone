/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits.item;

import java.util.List;

import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.DefaultItemBlock;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 *
 * @author CD4017BE
 */
public class ItemCircuit extends DefaultItemBlock
{
    
    public ItemCircuit(Block id)
    {
        super(id);
        BlockItemRegistry.registerItemStack(new ItemStack(this), this.getUnlocalizedName());
    }

    @Override
    public void addInformation(ItemStack item, EntityPlayer player, List list, boolean par4) 
    {
        if (item.getTagCompound() != null) {
            list.add(String.format("InOut: %d", item.getTagCompound().getByte("InOut") & 0xff));
            list.add(String.format("Gates: %d", item.getTagCompound().getByte("Gates") & 0xff));
            list.add(String.format("Count: %d", item.getTagCompound().getByte("Count") & 0xff));
            if (item.getTagCompound().hasKey("Progr")) {
                list.add(item.getTagCompound().getCompoundTag("Progr").getString("name"));
            }
        }
        super.addInformation(item, player, list, par4);
    }
    
    
    
}
