package cd4017be.circuits.item;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import cd4017be.api.circuits.ISensor;
import cd4017be.api.circuits.ItemBlockSensor;
import cd4017be.api.energy.EnergyAPI;

public class ItemEnergySensor extends ItemBlockSensor implements ISensor {

	public ItemEnergySensor(String id) {
		super(id, 20F);
	}

	@Override
	protected float measure(ItemStack sensor, NBTTagCompound nbt, World world, BlockPos pos, EnumFacing side) {
		return EnergyAPI.get(world.getTileEntity(pos), side).getStorage() / 1000F;
	}

}
