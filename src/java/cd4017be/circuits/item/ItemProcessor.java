package cd4017be.circuits.item;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import cd4017be.api.circuits.Chip;
import cd4017be.api.circuits.IAdjustable;
import cd4017be.api.circuits.IChipItem;
import cd4017be.circuits.editor.CircuitLoader;
import cd4017be.lib.item.BaseItem;
import cd4017be.lib.jvm_utils.NBT2Class;
import cd4017be.lib.util.TooltipUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;

/**
 * @author CD4017BE
 *
 */
public class ItemProcessor extends BaseItem implements IChipItem {

	/**
	 * @param id
	 */
	public ItemProcessor(String id) {
		super(id);
	}

	@Override
	public void addInformation(ItemStack stack, World player, List<String> list, ITooltipFlag b) {
		NBTTagCompound nbt = stack.getTagCompound();
		if (nbt != null) {
			if (nbt.hasKey("name", NBT.TAG_STRING))
				list.add("\u00a76\u00a7n" + nbt.getString("name"));
			if (nbt.hasKey("ingr", NBT.TAG_INT_ARRAY)) {
				int[] ing = nbt.getIntArray("ingr");
				if (ing.length >= 3)
					list.add(TooltipUtil.format("item.circuits.processor.ingreds", ing[0], ing[1], ing[2]));
			}
			if (b.isAdvanced()) {
				if (nbt.hasKey("class", NBT.TAG_COMPOUND)) {
					NBTTagCompound tag = nbt.getCompoundTag("class");
					if (tag.hasKey("uidM", NBT.TAG_LONG) && tag.hasKey("uidL", NBT.TAG_LONG))
						list.add("Class = " + new UUID(tag.getLong("uidM"), tag.getLong("uidL")));
					else {
						list.add("Class {cpt[" + tag.getTagList("cpt", NBT.TAG_BYTE_ARRAY).tagCount()
								+ "], fields[" + tag.getIntArray("fields").length
								+ "], methods[" + tag.getTagList("methods", NBT.TAG_BYTE_ARRAY).tagCount() + "]}");
					}
				}
				if (nbt.hasKey("state", NBT.TAG_INT_ARRAY)) {
					int[] state = nbt.getIntArray("state");
					list.add("State = " + Arrays.toString(state));
				}
			}
		}
		super.addInformation(stack, player, list, b);
	}

	@Override
	public boolean fitsInSocket(ItemStack stack, int maxIn, int maxOut, int flags) {
		NBTTagCompound nbt = stack.getTagCompound();
		return nbt != null && nbt.hasKey("class", NBT.TAG_COMPOUND)
			&& nbt.getByte("in") <= maxIn
			&& nbt.getByte("out") <= maxOut
			&& (nbt.getInteger("flags") & ~flags) == 0;
	}

	@Override
	public Chip provideChip(ItemStack stack) {
		NBTTagCompound nbt = stack.getTagCompound();
		if (nbt == null || !nbt.hasKey("class", NBT.TAG_COMPOUND)) return Chip.NULL_CHIP;
		Chip chip = CircuitLoader.create(new NBT2Class(nbt.getCompoundTag("class"), CircuitLoader.CHECKER, Chip.class, IAdjustable.class).addConstructor());
		if (chip instanceof IAdjustable) {
			IAdjustable adj = (IAdjustable)chip;
			int[] states = nbt.getIntArray("state");
			for (int i = states.length - 1; i >= 0; i--)
				adj.setParam(i, states[i]);
			return chip;
		} else return Chip.NULL_CHIP;
	}

	@Override
	public void saveState(ItemStack stack, Chip chip) {
		if (chip instanceof IAdjustable && stack.hasTagCompound())
			stack.getTagCompound().setIntArray("state", ((IAdjustable)chip).getStates());
	}

}
