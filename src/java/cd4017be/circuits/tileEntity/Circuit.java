package cd4017be.circuits.tileEntity;

import java.util.ArrayList;
import java.util.Arrays;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.api.computers.ComputerAPI;
import cd4017be.lib.ModTileEntity;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.fml.common.Optional;

/**
 *
 * @author CD4017BE
 */
@Optional.Interface(modid = "OpenComputers", iface = "li.cil.oc.api.network.Environment")
public class Circuit extends ModTileEntity implements IDirectionalRedstone, IGuiData, ITickable, Environment {

	public static final byte 
		C_NULL = 0x00,	//empty gates
		C_CNT = 0x10,	//counter
		C_OR = 0x20,	//OR
		C_NOR = 0x30,	//NOR
		C_AND = 0x40,	//AND
		C_NAND = 0x50,	//NAND
		C_XOR = 0x60,	//XOR
		C_XNOR = 0x70,	//XNOR
		C_LS = (byte)0x80,	//lesser
		C_NLS = (byte)0x90,	//not lesser
		C_EQ = (byte)0xA0,	//equal
		C_NEQ = (byte)0xB0,	//not equal
		C_12 = (byte)0xC0,	//unused
		C_13 = (byte)0xD0,	//unused
		C_14 = (byte)0xE0,	//unused
		C_15 = (byte)0xF0;	//unused

	@Override
	public String getName() {
		return name.length() > 0 ? "\"" + name + "\"" : super.getName();
	}

	public final int[] output = new int[6];
	public byte[] ram = new byte[0];
	public byte[] code = new byte[0];
	public String name = "";
	/** var[0-7]: IO, var[8-15]: cap, var[16-23]: gates, var[24-31]: calc */
	public int var;
	/** cfgI[0-47 6*8]: side*ramAddr, cfgI[48-59 6*2]: side*dir, cfgI[60]: on/off, cfgE[0-59 6*(5+5)]: side*(offset+size) */
	public long cfgI, cfgE;
	public int tickInt = 1;
	private short timer = 0;
	private boolean update;

	@Override
	public void update() {
		if (worldObj.isRemote) return;
		if (node != null) ComputerAPI.update(this, node, 0);
		if ((cfgI & 1L<<60) != 0) timer++;
		if (timer >= tickInt && ram.length > 0) {
			timer = 0;
			int rs;
			if (update) {
				for (int i = 0; i < 6; i++) 
					if (getDir(i) == 1) {
						EnumFacing dir = EnumFacing.VALUES[i];
						rs = worldObj.getRedstonePower(pos.offset(dir), dir);
						rs >>>= getExtIdx(i);
						this.writeInt(getRamIdx(i), getSize(i), rs);
					}
				update = false;
			}
			cpuTick();
			for (int i = 0; i < 6; i++) 
				if (getDir(i) == 2) {
					rs = this.readInt(getRamIdx(i), getSize(i)) << getExtIdx(i);
					if (rs != output[i]) {
						output[i] = rs;
						worldObj.notifyBlockOfStateChange(pos.offset(EnumFacing.VALUES[i]), Blocks.REDSTONE_TORCH);
					}
				} else if (output[i] != 0) {
					output[i] = 0;
					worldObj.notifyBlockOfStateChange(pos.offset(EnumFacing.VALUES[i]), Blocks.REDSTONE_TORCH);
				}
		}
	}

	private int readInt(int pos, int size) {
		int e = (pos + size + 7) >> 3;
		long l = 0;
		for (int i = pos >> 3, j = 0; i < e; i++, j+=8) l |= (ram[i % ram.length] & 0xff) << j;
		return (int)(l >>> (pos & 7)) & 0xffffffff >>> (32 - size);
	}

	private void writeInt(int pos, int size, int val) {
		int i0, i = pos >> 3, i1 = (pos + size + 7) >> 3, j = pos & 7;
		long r = (0xffffffffL >>> (32 - size)) << j, v = (long)val << j & r; 
		for (j = 0; i < i1; i++, j += 8) 
			ram[i0 = i % ram.length] = (byte)(ram[i0] & ~(r >>> j) | v >>> j);
	}

	private void cpuTick() {
		boolean x;
		int cmd, con;
		int n = 0;
		for (int i = 0, j = 0; i < code.length && n >> 3 < ram.length; j = ++i) {
			cmd = code[i] & 0xf0;
			con = code[i] & 0x0f;
			switch ((byte)cmd) {
			case C_NULL:
				n += con == 0 ? 16 : con;
			continue;
			case C_CNT:{
				int mask = (0xff >> con) << (n & 7) & 0xff, p = n >> 3;
				if (getBit(i + 3)) ram[p] = (byte)((ram[p] & ~mask) | (code[i + 4] & mask));
				else if (getBit(i + 1)) ram[p] = (byte)((ram[p] & ~mask) | (ram[p] + code[i + 2] & mask));
				i += 4;	n += 8 - con;
			} continue;
			case C_OR:
				for (x = false; !x && i < j + con;) x |= getBit(++i);
				i = j + con;
			break;
			case C_NOR:
				for (x = false; !x && i < j + con;) x |= getBit(++i);
				x = !x; i = j + con;
			break;
			case C_AND:
				for (x = true; x && i < j + con;) x &= getBit(++i);
				i = j + con;
			break;
			case C_NAND:
				for (x = true; x && i < j + con;) x &= getBit(++i);
				x = !x; i = j + con;
			break;
			case C_XOR:
				for (x = false; i < j + con;) x ^= getBit(++i);
				i = j + con;
			break;
			case C_XNOR:
				for (x = true; i < j + con;) x ^= getBit(++i);
				i = j + con;
			break;
			case C_LS:
				x = (((con & 1) != 0 ? getByte(++i) : code[++i]) & 0xff) <
					(((con & 2) != 0 ? getByte(++i) : code[++i]) & 0xff);
			break;
			case C_NLS:
				x = (((con & 1) != 0 ? getByte(++i) : code[++i]) & 0xff) >=
					(((con & 2) != 0 ? getByte(++i) : code[++i]) & 0xff);
			break;
			case C_EQ:
				x = ((con & 1) != 0 ? getByte(++i) : code[++i]) ==
					((con & 2) != 0 ? getByte(++i) : code[++i]);
			break;
			case C_NEQ:
				x = ((con & 1) != 0 ? getByte(++i) : code[++i]) !=
					((con & 2) != 0 ? getByte(++i) : code[++i]);
			break;
			default: continue;
			}
			if (x) ram[n >> 3] |= 1 << (n & 7);
			else ram[n >> 3] &= ~(1 << (n & 7));
			n++;
		}
	}

	private boolean getBit(int i) {
		int p = code[i] & 0xff;
		return (ram[(p >> 3) % ram.length] >> (p & 7) & 1) != 0;
	}
	private byte getByte(int i) {
		return ram[(code[i] & 0xff) % ram.length];
	}

	public int getDir(int s) {return (int)(cfgI >>> (48 + s * 2)) & 3;}
	public int getRamIdx(int s) {return (int)(cfgI >>> (s * 8)) & 0xff;}
	public int getExtIdx(int s) {return (int)(cfgE >>> (s * 10)) & 0x1f;}
	public int getSize(int s) {return ((int)(cfgE >>> (s * 10 + 5)) & 0x1f) + 1;}
	public void setDir(int s, int v) {int p = 48 + s * 2; cfgI &= ~(3L << p); cfgI |= (long)(v & 3) << p;}
	public void setRamIdx(int s, int v) {int p = s * 8; cfgI &= ~(0xffL << p); cfgI |= (long)(v & 0xff) << p;}
	public void setExtIdx(int s, int v) {int p = s * 10; cfgE &= ~(0x1fL << p); cfgE |= (long)(v & 0x1f) << p;}
	public void setSize(int s, int v) {int p = s * 10 + 5; cfgE &= ~(0x1fL << p); cfgE |= (long)(v - 1 & 0x1f) << p;}
	
	public int usedIO() {
		int n = 0;
		for (int i = 0; i < 6; i++) 
			if (getDir(i) != 0) n += getSize(i);
		return n;
	}

	@Override
	public void onPlayerCommand(PacketBuffer data, EntityPlayerMP player) {
		byte cmd = data.readByte();
		if (cmd == 0) {
			cfgI = data.readLong();
			cfgE = data.readLong();
			if (this.usedIO() > (var & 0xff)) cfgI &= 0xf000ffffffffffffL;
			update = true;
		} else if (cmd == 1) {
			cfgI ^= 1L<<60;
		} else if (cmd == 2) {
			tickInt = data.readInt();
			if (tickInt < 1) tickInt = 1;
			else if (tickInt > 1200) tickInt = 1200;
		} else if (cmd == 3) {
			Arrays.fill(ram, (byte)0);
			update = true;
		} else if (cmd == 4) {
			byte i = data.readByte();
			ram[i >> 3 & 0x1f] ^= 1 << (i & 7);
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setByteArray("ram", ram);
		nbt.setByteArray("code", code);
		nbt.setInteger("var", var);
		nbt.setString("name", name);
		nbt.setInteger("tick", tickInt);
		nbt.setLong("cfgI", cfgI);
		nbt.setLong("cfgE", cfgE);
		nbt.setShort("timer", timer);
		nbt.setIntArray("out", output);
		if (node != null) ComputerAPI.saveNode(node, nbt);
		return super.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		var = nbt.getInteger("var");
		ram = new byte[Math.min(var >> 8 & 0xff, 32)];
		byte[] b = nbt.getByteArray("ram");
		System.arraycopy(b, 0, ram, 0, Math.min(ram.length, b.length));
		int[] i = nbt.getIntArray("out");
		System.arraycopy(i, 0, output, 0, Math.min(output.length, i.length));
		code = nbt.getByteArray("code");
		name = nbt.getString("name");
		timer = nbt.getShort("timer");
		tickInt = nbt.getInteger("tick");
		cfgI = nbt.getLong("cfgI");
		cfgE = nbt.getLong("cfgE");
		update = true;
		if (node != null) ComputerAPI.readNode(node, nbt);
	}

	@Override
	public ArrayList<ItemStack> dropItem(IBlockState m, int fortune) {
		ArrayList<ItemStack> list = new ArrayList<ItemStack>();
		ItemStack item = new ItemStack(this.getBlockType());
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setByte("IO", (byte)(var));
		nbt.setByte("Cap", (byte)(var >> 8));
		nbt.setByte("Gate", (byte)(var >> 16));
		nbt.setByte("Calc", (byte)(var >> 24));
		if (code.length > 0) {
			nbt.setString("name", name);
			nbt.setByteArray("code", code);
		}
		item.setTagCompound(nbt);
		list.add(item);
		return list;
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
		NBTTagCompound nbt = item.getTagCompound();
		if (nbt != null) {
			var = (nbt.getByte("IO") & 0xff) 
				| (nbt.getByte("Cap") & 0xff) << 8 
				| (nbt.getByte("Gate") & 0xff) << 16 
				| (nbt.getByte("Calc") & 0xff) << 24;
			ram = new byte[Math.min(var >> 8 & 0xff, 32)];
			if (nbt.hasKey("code")) {
				code = nbt.getByteArray("code");
				name = nbt.getString("name");
			}
		}
	}

	@Override
	public int redstoneLevel(int s, boolean str) {
		return str ? 0 : output[s];
	}

	@Override
	public void onNeighborBlockChange(Block b) {
		update = true;//TODO time synchronization
	}

	@Override
	public void initContainer(DataContainer container) {
		if (!worldObj.isRemote) container.extraRef = new LastState();
	}

	@Override
	public boolean detectAndSendChanges(DataContainer container, PacketBuffer dos) {
		LastState ls = (LastState)container.extraRef;
		int p = dos.writerIndex();
		dos.writeByte(0);
		byte chng = 0;
		if (tickInt != ls.tickInt) {dos.writeInt(ls.tickInt = tickInt); chng |= 1;}
		if (cfgI != ls.cfgI) {dos.writeLong(ls.cfgI = cfgI); chng |= 2;}
		if (cfgE != ls.cfgE) {dos.writeLong(ls.cfgE = cfgE); chng |= 4;}
		if (name != ls.name) {dos.writeString(ls.name = name); chng |= 8;}
		if (var != ls.var) {dos.writeShort(ls.var = var); chng |= 16;}
		int chng1 = 0;
		for (int i = 0; i < ram.length; i++)
			if (ram[i] != ls.ram[i]) chng1 |= 1 << i;
		if (chng1 != 0) {
			dos.writeInt(chng1);
			chng |= 32;
			for (int i = 0; chng1 != 0; i++, chng1 >>= 1)
				if ((chng1 & 1) != 0) dos.writeByte(ls.ram[i] = ram[i]);
		}
		if (chng == 0) return false;
		dos.setByte(p, chng);
		return true;
	}

	@Override
	public void updateClientChanges(DataContainer container, PacketBuffer dis) {
		byte chng = dis.readByte();
		if ((chng & 1) != 0) tickInt = dis.readInt();
		if ((chng & 2) != 0) cfgI = dis.readLong();
		if ((chng & 4) != 0) cfgE = dis.readLong();
		if ((chng & 8) != 0) name = dis.readStringFromBuffer(32);
		if ((chng & 16) != 0) {
			var = dis.readShort() & 0xffff;
			ram = new byte[Math.min(var >>> 8 & 0xff, 32)];
		}
		if ((chng & 32) != 0) {
			int chng1 = dis.readInt();
			for (int i = 0; chng1 != 0 && i < ram.length; i++, chng1 >>= 1)
				if ((chng1 & 1) != 0) ram[i] = dis.readByte();
		}
	}

	private static final class LastState {
		int tickInt, var;
		long cfgI, cfgE;
		String name;
		final byte[] ram = new byte[32];
	}

	@Override
	public byte getRSDirection(EnumFacing s) {
		return (byte)getDir(s.ordinal());
	}

	//---------------- OpenComputers integration ----------------//

	Object node = ComputerAPI.newOCnode(this, "circuit", false);

	@Override
	public void invalidate() {
		super.invalidate();
		ComputerAPI.removeOCnode(node);
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		ComputerAPI.removeOCnode(node);
	}

	@Override
	public Node node() {
		return (Node)node;
	}

	@Override
	public void onConnect(Node node) {}

	@Override
	public void onDisconnect(Node node) {}

	@Override
	public void onMessage(Message message) {}

	@Optional.Method(modid = "OpenComputers")
	@Callback(doc = "" , direct = true)
	public Object[] getBytes(Context cont, Arguments args) throws Exception {
		return new Object[]{ram};
	}

	@Optional.Method(modid = "OpenComputers")
	@Callback(doc = "" , direct = true)
	public Object[] setBytes(Context cont, Arguments args) throws Exception {
		byte[] data = args.checkByteArray(0);
		int ofs = args.optInteger(2, 0);
		int size = Math.min(data.length, ram.length - ofs);
		if (size > 0) System.arraycopy(data, 0, ram, ofs, size);
		return null;
	}

	@Optional.Method(modid = "OpenComputers")
	@Callback(doc = "" , direct = true)
	public Object[] getBit(Context cont, Arguments args) throws Exception {
		int p = args.checkInteger(0);
		return new Object[]{p >= 0 && p < ram.length * 8 && (ram[p >> 3] >> (p & 7) & 1) != 0};
	}

	@Optional.Method(modid = "OpenComputers")
	@Callback(doc = "" , direct = true)
	public Object[] setBit(Context cont, Arguments args) throws Exception {
		int p = args.checkInteger(0);
		if (p >= 0 && p < ram.length * 8) {
			if (args.checkBoolean(1)) ram[p >> 3] |= 1 << (p & 7);
			else ram[p >> 3] &= ~(1 << (p & 7));
		}
		return null;
	}

	@Optional.Method(modid = "OpenComputers")
	@Callback(doc = "" , direct = true)
	public Object[] getInt(Context cont, Arguments args) throws Exception {
		int pos = args.optInteger(0, 0), size = args.optInteger(1, 32);
		return new Object[]{this.readInt(pos, size)};
	}

	@Optional.Method(modid = "OpenComputers")
	@Callback(doc = "" , direct = true)
	public Object[] setInt(Context cont, Arguments args) throws Exception {
		int val = args.checkInteger(0);
		int pos = args.optInteger(1, 0), size = args.optInteger(2, 32);
		this.writeInt(pos, size, val);
		return null;
	}

}
