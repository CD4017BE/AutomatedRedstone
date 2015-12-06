/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits.block;

import java.util.List;

import cd4017be.circuits.item.ItemRSPipe;
import cd4017be.lib.templates.BlockPipe;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 *
 * @author CD4017BE
 */
public class BlockRSPipe1 extends BlockPipe
{
    public static final byte ID_Transport = 0;
    public static final byte ID_Extraction = 2;
    public static final byte ID_Injection = 1;
    
    public BlockRSPipe1(String id, Material m)
    {
        super(id, m, ItemRSPipe.class, 0x30, "RSPipe1N", "RSPipe1O", "RSPipe1I", "RSPipe1B");
        this.size = 0.25F;
    }
    
    @Override
    public void getSubBlocks(Item par1, CreativeTabs par2CreativeTabs, List list) 
    {
        list.add(new ItemStack(this, 1, ID_Transport));
        list.add(new ItemStack(this, 1, ID_Extraction));
        list.add(new ItemStack(this, 1, ID_Injection));
    }
    
    @Override
    public int damageDropped(int m) 
    {
        return m;
    }
    
}
