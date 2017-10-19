package cd4017be.circuits.tileEntity;

import java.io.IOException;

import cd4017be.lib.BlockGuiHandler.ClientPacketReceiver;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;

public class BitShiftPipe extends IntegerPipe implements IGuiData, ClientPacketReceiver {

	/**[0-5]: out ofs, [6-11]: out size, [12-17]: in ofs, [18-23]: in size */
	public final byte[] shifts = new byte[24];

	@Override
	protected void updateInput() {
		int newIn = 0;
		for (EnumFacing s : EnumFacing.values()) {
			int i = s.ordinal(), k = shifts[i + 18];
			if (k > 0)
				newIn |= (world.getRedstonePower(pos.offset(s), s) & ~(-1 << k)) << shifts[i + 12];
		}
		if (newIn != comp.inputState) {
			comp.inputState = newIn;
			comp.network.markStateDirty();
		}
	}

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing dir, float X, float Y, float Z) {
		if (player.isSneaking()) return super.onActivated(player, hand, item, dir, X, Y, Z);
		return false;
	}

	@Override
	public int redstoneLevel(EnumFacing side, boolean strong) {
		int i = side.ordinal(), k = shifts[i + 6];
		if (strong || k == 0) return 0;
		return comp.network.outputState >> shifts[i] & ~(-1 << k);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setByteArray("shift", shifts);
		return super.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		byte[] b = nbt.getByteArray("shift");
		System.arraycopy(b, 0, shifts, 0, Math.min(b.length, shifts.length));
		short x = 0;
		for (int i = 0; i < 6; i++) {
			if (shifts[i + 6] > 0) x |= 1 << (i * 2 + 1) | 0x2000;
			if (shifts[i + 18] > 0) x |= 1 << (i * 2) | 0x1000;
		}
		if (x != comp.rsIO) comp.network.setIO(comp, x);
	}

	@Override
	public void onPacketFromClient(PacketBuffer data, EntityPlayer sender) throws IOException {
		byte cmd = data.readByte();
		if (cmd >= 0 && cmd < shifts.length) {
			byte v = data.readByte();
			shifts[cmd] = v;
			int s = cmd % 12 - 6;
			if (s >= 0) {
				s *= 2;
				if (cmd < 12) s++;
				if ((comp.rsIO >> s & 1) != 0 ^ v > 0) {
					comp.network.setIO(comp, (short)(comp.rsIO ^ 1 << s));
					markUpdate();
				}
			}
		}
	}

	@Override
	public void initContainer(DataContainer container) {
	}

	@Override
	public int[] getSyncVariables() {
		int[] data = new int[6];
		for (int i = 0; i < shifts.length; i++)
			data[i % 6] |= (shifts[i] & 0xff) << (i / 6 * 8);
		return data;
	}

	@Override
	public void setSyncVariable(int i, int v) {
		if (i < 6)
			for (int j = 0; j < 4; j++)
				shifts[i + j * 6] = (byte)(v >> j * 8);
	}

	@Override
	public boolean detectAndSendChanges(DataContainer container, PacketBuffer dos) {
		return false;
	}

	@Override
	public void updateClientChanges(DataContainer container, PacketBuffer dis) {
	}

}
