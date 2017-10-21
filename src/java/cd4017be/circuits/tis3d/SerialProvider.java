package cd4017be.circuits.tis3d;

import cd4017be.circuits.tileEntity.BitShiftPipe;
import li.cil.tis3d.api.serial.SerialInterface;
import li.cil.tis3d.api.serial.SerialInterfaceProvider;
import li.cil.tis3d.api.serial.SerialProtocolDocumentationReference;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SerialProvider implements SerialInterfaceProvider {

	public SerialProvider() {
	}

	@Override
	public SerialProtocolDocumentationReference getDocumentationReference() {
		return null; //easier to document in GUI info tab
	}

	@Override
	public SerialInterface interfaceFor(World world, BlockPos pos, EnumFacing side) {
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof BitShiftPipe) return new Serial((BitShiftPipe) te, side);
		return null;
	}

	@Override
	public boolean isValid(World world, BlockPos pos, EnumFacing side, SerialInterface serial) {
		return serial instanceof Serial && ((Serial)serial).valid();
	}

	@Override
	public boolean worksWith(World world, BlockPos pos, EnumFacing side) {
		return world.getTileEntity(pos) instanceof BitShiftPipe;
	}

}
