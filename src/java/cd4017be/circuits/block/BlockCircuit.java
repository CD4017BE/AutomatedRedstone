package cd4017be.circuits.block;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import cd4017be.circuits.tileEntity.Circuit;
import cd4017be.lib.TileBlock;

public class BlockCircuit extends TileBlock {

	public static final PropertyInteger prop = PropertyInteger.create("type", 0, Circuit.ClockSpeed.length - 1);

	public BlockCircuit(String id, Material m, SoundType sound) {
		super(id, m, sound, 0x50);
		setDefaultState(getBlockState().getBaseState().withProperty(prop, 0));
	}

	@Override
	public void getSubBlocks(Item par1, CreativeTabs par2CreativeTabs, List<ItemStack> list) {
		for (int i = 0; i < Circuit.ClockSpeed.length; i++)
			list.add(new ItemStack(this, 1, i));
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return this.getBlockState().getBaseState().withProperty(prop, meta);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(prop);
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected void addProperties(ArrayList<IProperty> main) {
		main.add(prop);
		super.addProperties(main);
	}

	@Override
	public int damageDropped(IBlockState state) {
		return this.getMetaFromState(state);
	}

}
