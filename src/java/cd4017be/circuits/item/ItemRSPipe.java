/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits.item;

import cd4017be.circuits.block.BlockRSPipe1;
import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.DefaultItemBlock;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

/**
 *
 * @author CD4017BE
 */
public class ItemRSPipe extends DefaultItemBlock
{
    
    public ItemRSPipe(Block id)
    {
        super(id);
        this.setHasSubtypes(true);
        BlockItemRegistry.registerItemStack(new ItemStack(this, 1, BlockRSPipe1.ID_Transport), "rsp1bitN");
        BlockItemRegistry.registerItemStack(new ItemStack(this, 1, BlockRSPipe1.ID_Extraction), "rsp1bitI");
        BlockItemRegistry.registerItemStack(new ItemStack(this, 1, BlockRSPipe1.ID_Injection), "rsp1bitO");
    }

    @Override
    public int getMetadata(int dmg) 
    {
        return dmg;
    }
    
    @Override
    public IIcon getIconFromDamage(int m) 
    {
    	return this.field_150939_a.getIcon(0, m);
    }
    
}
