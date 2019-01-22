package cd4017be.circuits.item;

import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import cd4017be.lib.item.BaseItemBlock;
import cd4017be.lib.util.TooltipUtil;

/**
 * 
 * @author CD4017BE
 */
public class ItemWirelessCon extends BaseItemBlock {

	public ItemWirelessCon(Block id) {
		super(id);
		this.setHasSubtypes(true);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack item, @Nullable World player, List<String> list, ITooltipFlag f) {
		if (item.getItemDamage() != 0 && item.hasTagCompound()) {
			NBTTagCompound nbt = item.getTagCompound();
			list.add(TooltipUtil.format("msg.cd4017be.wireless3", nbt.getInteger("lx"), nbt.getInteger("ly"), nbt.getInteger("lz"), nbt.getInteger("ld")));
		}
		super.addInformation(item, player, list, f);
	}

}
