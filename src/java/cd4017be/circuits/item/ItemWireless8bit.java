package cd4017be.circuits.item;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import cd4017be.lib.DefaultItemBlock;

public class ItemWireless8bit extends DefaultItemBlock
{

	public ItemWireless8bit(Block id) {
		super(id);
		this.setHasSubtypes(true);
	}

	@Override
	public void addInformation(ItemStack item, EntityPlayer player, List<String> list, boolean f) {
		if (item.getItemDamage() != 0 && item.getTagCompound() != null) 
			list.add(String.format("Linked: x= %d ,y= %d ,z= %d in dim %d", item.getTagCompound().getInteger("lx"), item.getTagCompound().getInteger("ly"), item.getTagCompound().getInteger("lz"), item.getTagCompound().getInteger("ld")));
		super.addInformation(item, player, list, f);
	}

	@Override
	public int getMetadata(int d) {
		return d == 2 ? 1 : 0;
	}

}
