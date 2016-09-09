package cd4017be.circuits.item;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import cd4017be.api.circuits.ISensor;
import cd4017be.api.energy.EnergyAPI;
import cd4017be.lib.DefaultItem;

public class ItemEnergySensor extends DefaultItem implements ISensor {

	public ItemEnergySensor(String id) {
		super(id);
	}

	@Override
	public float measure(ItemStack sensor, World world, BlockPos pos, EnumFacing side) {
		return world.isBlockLoaded(pos) ? EnergyAPI.get(world.getTileEntity(pos), side).getStorage() / 1000F : 0F;
	}

}
