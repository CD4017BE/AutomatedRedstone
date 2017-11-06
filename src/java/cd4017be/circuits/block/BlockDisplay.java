package cd4017be.circuits.block;

import cd4017be.circuits.tileEntity.Display8bit;
import cd4017be.lib.block.OrientedBlock;
import cd4017be.lib.property.PropertyOrientation;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class BlockDisplay extends OrientedBlock {

	public static final PropertyInteger con_prop = PropertyInteger.create("con", 0, 2);

	public BlockDisplay(String id, Material m, SoundType sound, int flags) {
		super(id, m, sound, flags, Display8bit.class, PropertyOrientation.XY_12_ROT);
	}

	@Override
	protected BlockStateContainer createBlockState() {
		orientProp = PropertyOrientation.XY_12_ROT;
		return new BlockStateContainer(this, orientProp, con_prop);
	}

	@Override
	public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof Display8bit) {
			Display8bit dsp = (Display8bit)te;
			return state.withProperty(con_prop, (dsp.dspMode & 4) != 0 ? 1 + (dsp.dspMode & 1) : 0);
		} else return state;
	}

}
