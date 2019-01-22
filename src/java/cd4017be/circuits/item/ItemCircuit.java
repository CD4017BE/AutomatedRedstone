package cd4017be.circuits.item;

import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import java.util.List;

import cd4017be.lib.item.BaseItemBlock;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 *
 * @author CD4017BE
 */
public class ItemCircuit extends BaseItemBlock {

	public ItemCircuit(Block id){
		super(id);
		setHasSubtypes(true);
	}

	@Override
	public int getMetadata(int damage) {
		return damage;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack item, @Nullable World player, List<String> list, ITooltipFlag par4) {
		NBTTagCompound nbt = item.getTagCompound();
		if (nbt != null) list.add(nbt.getString("name"));
		super.addInformation(item, player, list, par4);
	}

}
