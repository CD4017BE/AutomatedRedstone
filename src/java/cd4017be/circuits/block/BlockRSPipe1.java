package cd4017be.circuits.block;

import java.util.ArrayList;
import java.util.List;

import cd4017be.lib.templates.BlockPipe;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 *
 * @author CD4017BE
 */
public class BlockRSPipe1 extends BlockPipe {

	public static final PropertyInteger prop = PropertyInteger.create("type", 0, 2);

	public static final byte ID_Transport = 0;
	public static final byte ID_Extraction = 1;
	public static final byte ID_Injection = 2;

	public BlockRSPipe1(String id, Material m, SoundType sound) {
		super(id, m, sound, 0x30);
		this.setDefaultState(this.getBlockState().getBaseState().withProperty(prop, 0));
		this.size = 0.25F;
	}

	@Override
	public void getSubBlocks(Item par1, CreativeTabs par2CreativeTabs, List<ItemStack> list) {
		list.add(new ItemStack(this, 1, ID_Transport));
		list.add(new ItemStack(this, 1, ID_Extraction));
		list.add(new ItemStack(this, 1, ID_Injection));
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
