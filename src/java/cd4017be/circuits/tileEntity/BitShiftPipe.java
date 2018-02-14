package cd4017be.circuits.tileEntity;

import java.io.IOException;

import cd4017be.api.computers.ComputerAPI;
import cd4017be.lib.BlockGuiHandler.ClientPacketReceiver;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraftforge.fml.common.Optional.*;

/**
 * 
 * @author CD4017BE
 */
@InterfaceList(value = {
	@Interface(iface = "li.cil.oc.api.network.Environment", modid = "opencomputers"),
	@Interface(iface = "net.minecraft.util.ITickable", modid = "opencomputers")})
public class BitShiftPipe extends IntegerPipe implements IGuiData, ClientPacketReceiver, Environment, ITickable {

	/**[0-5]: out ofs, [6-11]: out size, [12-17]: in ofs, [18-23]: in size */
	public final byte[] shifts = new byte[24];
	public int internal;

	@Override
	protected void updateInput() {
		int newIn = internal;
		for (EnumFacing s : EnumFacing.values()) {
			int i = s.ordinal(), k = shifts[i + 18];
			if (k > 0)
				newIn |= (world.getRedstonePower(pos.offset(s), s) & 0xffffffff >>> (32 - k)) << shifts[i + 12];
		}
		if (newIn != comp.inputState) {
			comp.inputState = newIn;
			comp.network.markStateDirty();
			markDirty();
		}
	}

	@Override
	protected void checkCons() {
		short pre = comp.rsIO;
		super.checkCons();
		pre ^= comp.rsIO;
		if (pre != 0) {
			for (int i = 0; i < 12 ; i++)
				if ((pre >> i & 1) != 0) {
					int j = i / 2 - (i & 1) * 12 + 18;
					if (shifts[j] <= 0) shifts[j] = (byte)(32 - shifts[j - 6]);
				}
		}
	}

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing dir, float X, float Y, float Z) {
		if (player.isSneaking()) return super.onActivated(player, hand, item, dir, X, Y, Z);
		if (player.getHeldItemOffhand().isEmpty()) return false;
		return world.isRemote || cover.interact(this, player, hand, item, dir, X, Y, Z);
	}

	@Override
	public int redstoneLevel(EnumFacing side, boolean strong) {
		int i = side.ordinal(), k = shifts[i + 6];
		if (strong || k == 0) return 0;
		return comp.network.outputState >> shifts[i] & 0xffffffff >>> (32 - k);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		if (node != null) ComputerAPI.saveNode(node, nbt);
		nbt.setInteger("int", internal);
		nbt.setByteArray("shift", shifts);
		return super.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		if (node != null) ComputerAPI.readNode(node, nbt);
		internal = nbt.getInteger("int");
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
			if (v < 0) v = 0;
			else if (v > 32) v = 32;
			shifts[cmd] = v;
			int s = cmd % 12 - 6;
			if (s >= 0) {
				byte b = (byte) (32 - v);
				if (shifts[s] > b) shifts[s] = b;
				s <<= 1;
				int t = cmd < 12 ? 1 : 0;
				if ((comp.rsIO >> (s+t) & 1) != 0 ^ v > 0) {
					setIO(EnumFacing.VALUES[s >> 1], comp.rsIO >> s ^ 1 << t);
					markUpdate();
				}
			}
			if (cmd < 12) comp.onStateChange();
			else updateInput();
		} else {
			internal = 0;
			updateInput();
		}
		markDirty();
	}

	@Override
	public void initContainer(DataContainer container) {
	}

	@Override
	public int[] getSyncVariables() {
		int[] data = new int[7];
		for (int i = 0; i < shifts.length; i++)
			data[i % 6] |= (shifts[i] & 0xff) << (i / 6 * 8);
		data[6] = internal;
		return data;
	}

	@Override
	public void setSyncVariable(int i, int v) {
		if (i < 6)
			for (int j = 0; j < 4; j++)
				shifts[i + j * 6] = (byte)(v >> j * 8);
		else internal = v;
	}

	@Override
	public boolean detectAndSendChanges(DataContainer container, PacketBuffer dos) {
		return false;
	}

	@Override
	public void updateClientChanges(DataContainer container, PacketBuffer dis) {
	}

	private Object node = ComputerAPI.newOCnode(this, "analog_port", false);

	@Override
	public void update() {
		if (!world.isRemote && node != null) ComputerAPI.update(this, node, 0);
	}

	@Override
	public Node node() {
		return (Node) node;
	}

	@Override
	public void onConnect(Node node) {
	}

	@Override
	public void onDisconnect(Node node) {
	}

	@Override
	public void onMessage(Message message) {
	}

	@Method(modid = "opencomputers")
	@Callback
	public Object[] read(Context context, Arguments args) throws Exception {
		return new Object[] {comp.network.outputState};
	}

	@Method(modid = "opencomputers")
	@Callback
	public Object[] write(Context context, Arguments args) throws Exception {
		internal = args.checkInteger(0);
		updateInput();
		return null;
	}

	public void setInput(int v, EnumFacing side) {
		int i = side.ordinal() + 12, o = shifts[i];
		int mask = 0xffffffff >>> (32 - shifts[i + 6]) << o;
		internal &= ~mask;
		internal |= v << o & mask;
		updateInput();
	}

}
