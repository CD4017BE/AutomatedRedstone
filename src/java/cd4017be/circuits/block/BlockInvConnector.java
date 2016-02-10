package cd4017be.circuits.block;

import java.util.ArrayList;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import cd4017be.lib.DefaultItemBlock;
import cd4017be.lib.templates.BlockPipe;

public class BlockInvConnector extends BlockPipe 
{
	public static final PropertyDirection Facing = PropertyDirection.create("dir");
	
	public BlockInvConnector(String id, Material m) 
	{
		super(id, m, DefaultItemBlock.class, 0x20);
		this.size = 0.375F;
		this.setDefaultState(this.blockState.getBaseState().withProperty(Facing, EnumFacing.DOWN));
	}

	@Override
	protected void addProperties(ArrayList<IProperty> main) {
		main.add(Facing);
		super.addProperties(main);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return meta >= 0 && meta < 6 ? this.blockState.getBaseState().withProperty(Facing, EnumFacing.VALUES[meta]) : this.getDefaultState();
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(Facing).getIndex();
	}

}
