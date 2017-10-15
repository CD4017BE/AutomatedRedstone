package cd4017be.circuits.item;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import cd4017be.lib.item.BaseItemBlock;
import cd4017be.lib.util.TooltipUtil;

public class ItemWirelessCon extends BaseItemBlock {

	public ItemWirelessCon(Block id) {
		super(id);
		this.setHasSubtypes(true);
	}

	@Override
	public void addInformation(ItemStack item, EntityPlayer player, List<String> list, boolean f) {
		if (item.getItemDamage() != 0 && item.hasTagCompound()) {
			NBTTagCompound nbt = item.getTagCompound();
			list.add(TooltipUtil.format("msg.cd4017be.wireless3", nbt.getInteger("lx"), nbt.getInteger("ly"), nbt.getInteger("lz"), nbt.getInteger("ld")));
		}
		super.addInformation(item, player, list, f);
	}

}
