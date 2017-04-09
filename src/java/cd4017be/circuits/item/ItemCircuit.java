package cd4017be.circuits.item;

import java.util.List;

import cd4017be.lib.DefaultItemBlock;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 *
 * @author CD4017BE
 */
public class ItemCircuit extends DefaultItemBlock {

	public ItemCircuit(Block id){
		super(id);
		setHasSubtypes(true);
	}

	@Override
	public int getMetadata(int damage) {
		return damage;
	}

	@Override
	public void addInformation(ItemStack item, EntityPlayer player, List<String> list, boolean par4) {
		NBTTagCompound nbt = item.getTagCompound();
		if (nbt != null) list.add(nbt.getString("name"));
		super.addInformation(item, player, list, par4);
	}

}
