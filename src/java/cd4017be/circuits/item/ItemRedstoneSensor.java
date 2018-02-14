package cd4017be.circuits.item;

import cd4017be.api.circuits.ItemBlockSensor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;


/**
 * @author CD4017BE
 *
 */
public class ItemRedstoneSensor extends ItemBlockSensor {

	public ItemRedstoneSensor(String id) {
		super(id, 20F);
	}

	@Override
	protected float measure(ItemStack sensor, NBTTagCompound nbt, World world, BlockPos pos, EnumFacing side) {
		return world.getBlockState(pos).getComparatorInputOverride(world, pos);
	}

}
