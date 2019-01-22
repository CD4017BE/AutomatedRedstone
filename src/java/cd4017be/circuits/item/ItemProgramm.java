package cd4017be.circuits.item;

import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import java.util.List;

import cd4017be.lib.item.BaseItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 *
 * @author CD4017BE
 */
public class ItemProgramm extends BaseItem {

	public ItemProgramm(String id) {
		super(id);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack item, @Nullable World player, List<String> list, ITooltipFlag par4) {
		NBTTagCompound nbt = item.getTagCompound();
		if (nbt != null) {
			list.add(nbt.getString("name"));
			if (nbt.hasKey("code") && !nbt.hasKey("data")) {
				list.add("§cWarning: deprecated data format detected!");
				list.add("load and save in §6Circuit Programmer§7 to convert");
			}
		}
		super.addInformation(item, player, list, par4);
	}

}
