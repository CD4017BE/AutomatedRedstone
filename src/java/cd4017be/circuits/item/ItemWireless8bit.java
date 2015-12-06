package cd4017be.circuits.item;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import cd4017be.lib.DefaultItemBlock;

public class ItemWireless8bit extends DefaultItemBlock
{
    
    public ItemWireless8bit(Block id)
    {
        super(id);
    }

    @Override
    public String getItemStackDisplayName(ItemStack item) 
    {
        return (item.getItemDamage() != 0 ? "Linked 8-bit wireless " + (item.getItemDamage() == 1 ? "Transmitter" : "Receiver") : super.getItemStackDisplayName(item));
    }

    @Override
    public void addInformation(ItemStack item, EntityPlayer player, List list, boolean f) 
    {
        if (item.getItemDamage() != 0 && item.stackTagCompound != null) {
            list.add(String.format("Linked: x= %d ,y= %d ,z= %d in dim %d", item.stackTagCompound.getInteger("lx"), item.stackTagCompound.getInteger("ly"), item.stackTagCompound.getInteger("lz"), item.stackTagCompound.getInteger("ld")));
        }
        super.addInformation(item, player, list, f);
    }

	@Override
	public int getMetadata(int d) 
	{
		return d == 2 ? 1 : 0;
	}
    
}
