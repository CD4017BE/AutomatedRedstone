package cd4017be.circuits.tileEntity;

import java.io.IOException;
import java.util.Arrays;

import cd4017be.circuits.Objects;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.templates.AutomatedTile;
import cd4017be.lib.templates.Inventory;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.items.SlotItemHandler;
import static cd4017be.circuits.tileEntity.Circuit.*;

/**
 *
 * @author CD4017BE
 */
public class Programmer extends AutomatedTile implements IGuiData {

	private static final int 
		E_Load = 1,
		E_Save = 2,
		E_Comp = 3,
		E_invalid = 4,
		E_cap = 5,
		E_gate = 6,
		E_calc = 7,
		E_tooBig = 8,
		E_num = 9,
		E_arg = 10,
		E_cmd = 11,
		E_err = 12,
		E_struc = 13;
	public String[] code = new String[]{""};
	public String[] label = new String[]{""};
	public String name = "";
	public int errorCode = 0, errorArg;

	public Programmer() {
		inventory = new Inventory(1, 0, null);
	}

	public String getCode(int i) {return i < code.length && code[i] != null ? code[i] : "";}
	public String getLabel(int i) {return i < label.length && label[i] != null ? label[i] : "";}

	private String serializeCode() {
		String s = "";
		for (int i = 0; i < code.length; i++)
			s += code[i] + (label[i].isEmpty() ? "" : "\"" + label[i]) + (i != code.length - 1 ? "\n" : "");
		return s;
	}
	
	private void deserializeCode(String s) {
		String[] c = s.split("\n", 256);
		code = new String[c.length];
		label = new String[c.length];
		for (int i = 0; i < c.length; i++) {
			int j = c[i].indexOf('\"');
			label[i] = j < 0 ? "" : c[i].substring(j + 1);
			code[i] = j < 0 ? c[i] : c[i].substring(0, j);
			if (label[i].length() > 8) label[i] = label[i].substring(0, 8);
			if (code[i].length() > 32) code[i] = code[i].substring(0, 32);
		}
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setString("name", name);
		nbt.setString("code", this.serializeCode());
		return super.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		name = nbt.getString("name");
		this.deserializeCode(nbt.getString("code"));
	}

	@Override
	protected void customPlayerCommand(byte cmd, PacketBuffer dis, EntityPlayerMP player) throws IOException {
		errorCode = 0;
		if (cmd == 0 || cmd == 1) {
			int p = dis.readByte() & 0xff;
			String s = dis.readStringFromBuffer(32);
			if (p >= code.length) {
				if (s.isEmpty()) return;
				String[] arr = new String[p + 1];
				System.arraycopy(code, 0, arr, 0, code.length);
				Arrays.fill(arr, code.length, arr.length, "");
				code = arr; arr = new String[p + 1];
				System.arraycopy(label, 0, arr, 0, label.length);
				Arrays.fill(arr, label.length, arr.length, "");
				label = arr;
			}
			if (cmd == 0) code[p] = s;
			else label[p] = s.length() > 8 ? s.substring(0, 8) : s;
			if (p == code.length - 1 && s.isEmpty()) {
				for (;p > 0; p--)
					if (!code[p].isEmpty() || !label[p].isEmpty()) break;
				if (++p < code.length) {
					String[] arr = new String[p];
					System.arraycopy(code, 0, arr, 0, arr.length);
					code = arr; arr = new String[p];
					System.arraycopy(label, 0, arr, 0, arr.length);
					label = arr;
				}
			}
		} else if (cmd == 2) name = dis.readStringFromBuffer(24);
		else if (cmd == 3) {
			NBTTagCompound nbt;
			if (inventory.items[0] != null && inventory.items[0].getItem() == Objects.circuitPlan && (nbt = inventory.items[0].getTagCompound()) != null) {
				name = nbt.getString("name");
				this.deserializeCode(nbt.getString("code"));
				errorCode = E_Load; errorArg = code.length;
			} else if (inventory.items[0] == null || inventory.items[0].getItem() == Items.PAPER) {
				name = "";
				code = new String[]{""};
				label = new String[]{""};
				errorCode = E_Load; errorArg = 0;
			} else errorCode = E_invalid;
		} else if (cmd == 4 && inventory.items[0] != null) {
			if (inventory.items[0].getItem() == Items.PAPER) inventory.items[0] = new ItemStack(Objects.circuitPlan, inventory.items[0].stackSize);
			if (inventory.items[0].getItem() == Objects.circuitPlan) {
				NBTTagCompound nbt = new NBTTagCompound();
				nbt.setString("name", name);
				nbt.setString("code", this.serializeCode());
				inventory.items[0].setTagCompound(nbt);
				errorCode = E_Save; errorArg = code.length;
			} else if (inventory.items[0].getItem() == Item.getItemFromBlock(Objects.circuit)) {
				NBTTagCompound nbt = inventory.items[0].getTagCompound();
				if (nbt == null) nbt = new NBTTagCompound();
				int n = (nbt.getByte("Cap") & 0xff) * 8;
				for (int i = code.length - 1; i >= n; i--)
					if (!code[i].isEmpty()) {
						errorCode = E_cap;
						errorArg = i / 8 + 1;
						return;
					}
				byte[] data = this.compile(nbt.getByte("Gate") & 0xff, nbt.getByte("Calc") & 0xff);
				if (data == null) return;
				nbt.setString("name", name);
				nbt.setByteArray("code", data);
				inventory.items[0].setTagCompound(nbt);
			} else errorCode = E_invalid;
		} /*if (cmd == 2) { //setLine
			byte l = dis.readByte();
			if (l >= 0 && l < gates.length) gates[l] = dis.readStringFromBuffer(40);
		} else if (cmd == 3) { //addLine
			byte l = dis.readByte();
			if (l >= 0 && l <= gates.length){
				String[] buff = new String[gates.length + 1];
				if (l > 0) System.arraycopy(gates, 0, buff, 0, l);
				if (l < gates.length) System.arraycopy(gates, l, buff, l + 1, gates.length - l);
				gates = buff;
				gates[l] = "";
			}
		} else if (cmd == 4) { //deleteLine
			byte l = dis.readByte();
			if (l >= 0 && l < gates.length) {
				String[] buff = new String[gates.length - 1];
				if (l > 0) System.arraycopy(gates, 0, buff, 0, l);
				if (l < gates.length - 1) System.arraycopy(gates, l + 1, buff, l, gates.length - l - 1);
				gates = buff;
				if (gates.length == 0) gates = new String[]{""};
			}
		} else if (cmd == 5) { //setOutput
			byte l = dis.readByte();
			if (l >= 0 && l < outputs.length) outputs[l] = dis.readByte();
		} else if (cmd == 6) { //setCounter
			byte l = dis.readByte();
			if (l >= 0 && l < counter.length) counter[l] = dis.readByte();
		} else if (cmd == 7) {
			name = dis.readStringFromBuffer(40);
		}
		this.markUpdate();*/
	}
	
	private byte[] compile(int g, int c) {
		byte[] data = new byte[512];
		int p = 0, l = 0, g1 = 0, c1 = 0;
		try {
			while (l < code.length) {
				if (code[l].isEmpty()) {
					int k = l + 1;
					while(k < code.length && code[k].isEmpty()) k++;
					k -= l; l += k;
					for (;k > 0; k -= 16) data[p++] = (byte)(C_NULL | (k >= 16 ? 0 : k));
					continue;
				}
				String s0 = code[l].substring(1);
				byte cmd = 0;
				switch (code[l].charAt(0)) {
				case '+': p = addBitParams(data, p, C_OR, s0); g1++; l++; break;
				case '-': p = addBitParams(data, p, C_NOR, s0); g1++; l++; break;
				case '&': p = addBitParams(data, p, C_AND, s0); g1++; l++; break;
				case '*': p = addBitParams(data, p, C_NAND, s0); g1++; l++; break;
				case '/': p = addBitParams(data, p, C_XOR, s0); g1++; l++; break;
				case '\\': p = addBitParams(data, p, C_XNOR, s0); g1++; l++; break;
				case '<': p = addParameters(data, p, C_COMP, 0, s0); g1++; l++; break;
				case '>': p = addParameters(data, p, C_COMP_1, 0, s0); g1++; l++; break;
				case '=': p = addParameters(data, p, C_COMP_2, 0, s0); g1++; l++; break;
				case '~': p = addParameters(data, p, C_COMP_3, 0, s0); g1++; l++; break;
				case 'i':
				case 'I': c1 += 2; cmd++;
				case 'm':
				case 'M': c1 += 2; cmd++;
				case 's':
				case 'S': c1 += 2; cmd++;
				case 'b':
				case 'B': c1 += 2;
					s0 = s0.substring(1);
					switch(code[l].charAt(1)) {
					case '$': p = addParameters(data, p, (byte)(C_CNT | cmd), 2, s0); c1+=1; break;
					case '?': p = addParameters(data, p, (byte)(C_MUX | cmd), 1, s0); break;
					case '+': p = addParameters(data, p, (byte)(C_ADD | cmd), 0, s0); c1+=1; break;
					case '-': p = addParameters(data, p, (byte)(C_SUB | cmd), 0, s0); c1+=1; break;
					case '*': p = addParameters(data, p, (byte)(C_MUL | cmd), 0, s0); c1+=2; break;
					case '/': p = addParameters(data, p, (byte)(C_DIV | cmd), 0, s0); c1+=2; break;
					case '%': p = addParameters(data, p, (byte)(C_MOD | cmd), 0, s0); c1+=2; break;
					default: errorCode = E_cmd; errorArg = l; return null;
					}
					l &= 0xf8; l++;
					for (int j = Math.min(code.length, l + (int)cmd * 8 + 7); l < j; l++)
						if (!code[l].isEmpty()) {errorCode = E_struc; errorArg = l; return null;}
					break;
				default: errorCode = E_cmd; errorArg = l; return null;
				}
			}
			if (g < g1) {errorCode = E_gate; errorArg = g1;}
			else if (c < c1) {errorCode = E_calc; errorArg = c1;}
			else {
				byte[] ret = new byte[p];
				System.arraycopy(data, 0, ret, 0, p);
				errorCode = E_Comp; 
				errorArg = p;
				return ret;
			}
		}
		catch (NumberFormatException e) {errorCode = E_num; errorArg = l;}
		catch (IllegalArgumentException e) {errorCode = E_arg; errorArg = l;}
		catch (ArrayIndexOutOfBoundsException e) {errorCode = E_tooBig; errorArg = l;}
		catch (Exception e) {errorCode = E_err; errorArg = l;}
		return null;
	}

	private int addBitParams(byte[] data, int p, byte cmd, String s) {
		if (s.isEmpty()) {
			data[p++] = cmd;
			return p;
		}
		String[] s1 = s.split(",");
		if (s1.length > 15) throw new IllegalArgumentException();
		data[p++] = (byte)(cmd | s1.length);
		for (String s2 : s1) {
			if ((s2 = s2.trim()).length() > 2) throw new NumberFormatException();
			data[p++] = (byte)Integer.parseInt(s2, 16);
		}
		return p;
	}

	private int addParameters(byte[] data, int p, byte cmd, int n, String s) {
		String[] s1 = s.split(",");
		if (s1.length != n + 2) throw new IllegalArgumentException();
		int i = p + 1;
		for (int j = 0; j < 2; j++)
			try {
				int x = Integer.parseInt(s1[j]);
				cmd |= 4 << j;
				data[i++] = (byte)x;
				data[i++] = (byte)(x >> 8);
				data[i++] = (byte)(x >> 16);
				data[i++] = (byte)(x >> 24);
			} catch (NumberFormatException e) {
				data[i++] = decodeVar(s1[j]);
			}
		data[p] = cmd;
		for (int j = 2; j < s1.length; j++) {
			String s2 = s1[j].trim();
			if (s2.length() > 2) throw new NumberFormatException();
			data[i++] = (byte)Integer.parseInt(s2, 16);
		}
		return i;
	}
	
	private byte decodeVar(String s) {
		byte x = Byte.parseByte(s.substring(1));
		x &= 0x1f;
		switch(s.charAt(0)) {
		case 'B': return x |= 0x00;
		case 'S': return x |= 0x20;
		case 'M': return x |= 0x40;
		case 'I': return x |= 0x60;
		case 'b': return x |= 0x80;
		case 's': return x |= 0xa0;
		case 'm': return x |= 0xc0;
		case 'i': return x |= 0xe0;
		default: throw new NumberFormatException();
		}
	}

	@Override
	public void initContainer(DataContainer container) {
		TileContainer c = (TileContainer)container;
		if (!worldObj.isRemote) c.extraRef = new LastState();
		c.addItemSlot(new SlotItemHandler(inventory, 0, 134, 91));
		c.addPlayerInventory(8, 125);
	}

	@Override
	public boolean detectAndSendChanges(DataContainer container, PacketBuffer dos) {
		LastState ls = (LastState)container.extraRef;
		int p = dos.writerIndex();
		dos.writeByte(0);
		byte chng = 0;
		if (errorCode != ls.errCode) {dos.writeByte(ls.errCode = errorCode); chng |= 1;}
		if (errorArg != ls.errArg) {dos.writeInt(ls.errArg = errorArg); chng |= 2;}
		if (name != ls.name) {dos.writeString(ls.name = name); chng |= 4;}
		if (code.length != ls.size) {dos.writeShort(ls.size = code.length); chng |= 8;}
		int p1 = dos.writerIndex(), chng1 = 0;
		dos.writeInt(chng1);
		for (int i = 0; i < code.length; i += 8) {
			int p2 = dos.writerIndex(); byte chng2 = 0;
			dos.writeByte(chng2);
			for (int j = i; j < i + 8 && j < code.length; j++)
				if (code[j] != ls.code[j]) 
				{chng2 |= 1 << (j & 7); dos.writeString(ls.code[j] = code[j]);}
			if (chng2 != 0) {dos.setByte(p2, chng2); chng1 |= 1 << (i >> 3);}
			else dos.writerIndex(p2);
		}
		if (chng1 != 0) {dos.setInt(p1, chng1); chng |= 16;}
		else dos.writerIndex(p1);
		p1 = dos.writerIndex(); chng1 = 0;
		dos.writeInt(chng1);
		for (int i = 0; i < label.length; i += 8) {
			int p2 = dos.writerIndex(); byte chng2 = 0;
			dos.writeByte(chng2);
			for (int j = i; j < i + 8 && j < label.length; j++)
				if (label[j] != ls.label[j]) 
				{chng2 |= 1 << (j & 7); dos.writeString(ls.label[j] = label[j]);}
			if (chng2 != 0) {dos.setByte(p2, chng2); chng1 |= 1 << (i >> 3);}
			else dos.writerIndex(p2);
		}
		if (chng1 != 0) {dos.setInt(p1, chng1); chng |= 32;}
		else dos.writerIndex(p1);
		if (chng == 0) return false;
		dos.setByte(p, chng);
		return true;
	}

	@Override
	public void updateClientChanges(DataContainer container, PacketBuffer dis) {
		byte chng = dis.readByte();
		if ((chng & 1) != 0) errorCode = dis.readByte();
		if ((chng & 2) != 0) errorArg = dis.readInt();
		if ((chng & 4) != 0) name = dis.readStringFromBuffer(24);
		if ((chng & 8) != 0) {
			int s = dis.readShort();
			String[] arr = new String[s];
			System.arraycopy(code, 0, arr, 0, Math.min(code.length, s));
			code = arr; arr = new String[s];
			System.arraycopy(label, 0, arr, 0, Math.min(label.length, s));
			label = arr;
		}
		if ((chng & 16) != 0) {
			int chng1 = dis.readInt();
			for (int i = 0; chng1 != 0; i++, chng1 >>>= 1)
				if ((chng1 & 1) != 0) {
					int chng2 = dis.readByte() & 0xff;
					for (int j = i * 8; chng2 != 0 && j < code.length; j++, chng2 >>= 1)
						if ((chng2 & 1) != 0) code[j] = dis.readStringFromBuffer(32);
				}
		}
		if ((chng & 32) != 0) {
			int chng1 = dis.readInt();
			for (int i = 0; chng1 != 0; i++, chng1 >>>= 1)
				if ((chng1 & 1) != 0) {
					int chng2 = dis.readByte() & 0xff;
					for (int j = i * 8; chng2 != 0 && j < label.length; j++, chng2 >>= 1)
						if ((chng2 & 1) != 0) label[j] = dis.readStringFromBuffer(8);
				}
		}
	}

	private static final class LastState {
		final String[] code = new String[256], label = new String[256];
		String name;
		int errCode, errArg, size = 1;
	}

}
