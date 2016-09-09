package cd4017be.circuits.item;

import java.util.List;

import cd4017be.circuits.tileEntity.Assembler;
import cd4017be.lib.DefaultItemBlock;
import cd4017be.lib.TooltipInfo;
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
	}

	@Override
	public void addInformation(ItemStack item, EntityPlayer player, List<String> list, boolean par4) {
		NBTTagCompound nbt = item.getTagCompound();
		if (nbt != null) {
			for (int i = 0; i < Assembler.tagNames.length; i++)
				list.add(TooltipInfo.format("gui.cd4017be.assembler.comp" + i, nbt.getByte(Assembler.tagNames[i]) & 0xff));
			if (nbt.hasKey("name"))
				list.add(nbt.getString("name"));
		}
		super.addInformation(item, player, list, par4);
	}

}
