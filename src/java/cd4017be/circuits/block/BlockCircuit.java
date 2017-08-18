package cd4017be.circuits.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import cd4017be.circuits.tileEntity.Circuit;
import cd4017be.lib.block.AdvancedBlock;

public class BlockCircuit extends AdvancedBlock {

	public static final PropertyInteger prop = PropertyInteger.create("type", 0, Circuit.ClockSpeed.length - 1);

	public BlockCircuit(String id, Material m, SoundType sound, Class<? extends TileEntity> tile) {
		super(id, m, sound, 0x50, tile);
		setDefaultState(getBlockState().getBaseState().withProperty(prop, 0));
	}

	@Override
	public void getSubBlocks(Item item, CreativeTabs tab, NonNullList<ItemStack> list) {
		for (int i = 0; i < Circuit.ClockSpeed.length; i++)
			list.add(new ItemStack(item, 1, i));
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return this.getBlockState().getBaseState().withProperty(prop, meta);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(prop);
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, prop);
	}

	@Override
	public int damageDropped(IBlockState state) {
		return this.getMetaFromState(state);
	}

}
