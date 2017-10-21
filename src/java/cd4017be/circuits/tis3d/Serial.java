package cd4017be.circuits.tis3d;

import cd4017be.circuits.tileEntity.BitShiftPipe;
import li.cil.tis3d.api.serial.SerialInterface;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

/**
 * 
 * @author CD4017BE
 */
public class Serial implements SerialInterface {

	private final EnumFacing side;
	private final BitShiftPipe adapter;

	Serial(BitShiftPipe adapter, EnumFacing side) {
		this.adapter = adapter;
		this.side = side;
	}

	public boolean valid() {
		return !this.adapter.isInvalid();
	}

	@Override
	public boolean canRead() {
		return true;
	}

	@Override
	public boolean canWrite() {
		return true;
	}

	@Override
	public short peek() {
		return (short) this.adapter.redstoneLevel(side, false);
	}

	@Override
	public void reset() {
	}

	@Override
	public void skip() {
	}

	@Override
	public void write(short v) {
		this.adapter.setInput(v, side);
	}

	@Override
	public void writeToNBT(NBTTagCompound arg0) {
	}

	@Override
	public void readFromNBT(NBTTagCompound arg0) {
	}

}