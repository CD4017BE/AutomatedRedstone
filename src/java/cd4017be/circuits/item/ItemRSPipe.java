/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits.item;

import java.util.Arrays;
import java.util.List;

import org.lwjgl.input.Keyboard;

import cd4017be.circuits.block.BlockRSPipe1;
import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.TooltipInfo;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

/**
 *
 * @author CD4017BE
 */
public class ItemRSPipe extends ItemBlock
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
    public String getItemStackDisplayName(ItemStack item) 
    {
        if (item.getItemDamage() == BlockRSPipe1.ID_Transport) return "1-bit solid Redstone-Wire";
        else if (item.getItemDamage() == BlockRSPipe1.ID_Extraction) return "1-bit Redstone-Wire input";
        else if (item.getItemDamage() == BlockRSPipe1.ID_Injection) return "1-bit Redstone-Wire output";
        else return "Invalid Item";
    }
    
    @Override
    public void addInformation(ItemStack item, EntityPlayer player, List list, boolean b) 
    {
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            String s = TooltipInfo.getInfo(this.getUnlocalizedName(item) + ":" + item.getItemDamage());
            if (s != null) list.addAll(Arrays.asList(s.split("\n")));
        } else list.add("<SHIFT for info>");
        super.addInformation(item, player, list, b);
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
