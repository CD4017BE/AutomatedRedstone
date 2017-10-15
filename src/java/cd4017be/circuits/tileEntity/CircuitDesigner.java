package cd4017be.circuits.tileEntity;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.util.Arrays;

import com.mojang.authlib.GameProfile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.SlotItemHandler;
import cd4017be.circuits.Objects;
import cd4017be.lib.BlockGuiHandler.ClientPacketReceiver;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.capability.LinkedInventory;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.tileentity.BaseTileEntity;

public class CircuitDesigner extends BaseTileEntity implements IGuiData, ClientPacketReceiver {

	public ItemStack dataItem = ItemStack.EMPTY;
	private GameProfile lastPlayer;
	public final Module[] modules = new Module[70];
	public int selMod = -1, lastPos = 0;
	public long modified = 1;
	public boolean renderAll;
	private final ByteBuf data = Unpooled.buffer();
	public String name = "";

	public CircuitDesigner() {
		data.writeByte(0); //fix IO-error when loading on client the first time
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		dataItem = new ItemStack(nbt.getCompoundTag("item"));
		readNBT(nbt);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		writeNBT(nbt);
		nbt.setTag("item", dataItem.writeToNBT(new NBTTagCompound()));
		return super.writeToNBT(nbt);
	}

	private NBTTagCompound writeNBT(NBTTagCompound nbt) {
		byte[] b = new byte[data.readableBytes()];
		data.readBytes(b);
		data.resetReaderIndex();
		nbt.setByteArray("data", b);
		nbt.setString("name", name);
		nbt.setBoolean("mode", renderAll);
		return nbt;
	}

	private void readNBT(NBTTagCompound nbt) {
		data.clear();
		byte[] b = nbt.getByteArray("data");
		data.writeBytes(b);
		name = nbt.getString("name");
		renderAll = nbt.getBoolean("mode");
	}

	@Override
	public void onPacketFromClient(PacketBuffer dis, EntityPlayer sender) throws IOException {
		byte cmd = dis.readByte();
		switch(cmd) {
		case 0:
			data.clear();
			data.writeBytes(dis);
			lastPlayer = sender.getGameProfile();
			modify();
			break;
		case 1:
			if (dataItem.getItem() == Objects.circuit_plan) {
				dataItem.setTagCompound(writeNBT(new NBTTagCompound()));
			} break;
		case 2:
			if (dataItem.getItem() == Objects.circuit_plan && dataItem.hasTagCompound()) {
				readNBT(dataItem.getTagCompound());
			} else {
				data.clear();
				data.writeByte(0).writeByte(0);
				name = "";
			}
			modify();
			lastPlayer = null;
			break;
		case 3:
			name = dis.readString(16);
			break;
		case 6: renderAll = false; break;
		case 7: renderAll = true; break;
		}
	}

	@Override
	public void initContainer(DataContainer container) {
		TileContainer cont = (TileContainer)container;
		cont.addItemSlot(new SlotItemHandler(new LinkedInventory(1, 1, (i) -> dataItem, (item, i) -> dataItem = item), 0, 202, 232));
		cont.addPlayerInventory(8, 174);
		if (world.isRemote) {
			modified = 0;
		} else {
			cont.extraRef = new LastState();
			if (lastPlayer != null && lastPlayer.equals(container.player.getGameProfile())) lastPlayer = null;
		}
	}

	@Override
	public boolean detectAndSendChanges(DataContainer cont, PacketBuffer dos) {
		LastState ls = (LastState)cont.extraRef;
		int chng = 0, p = dos.writerIndex();
		dos.writeByte(chng);
		if (modified > ls.edited && (lastPlayer == null || !lastPlayer.equals(cont.player.getGameProfile()))) {
			chng |= 1;
			dos.writeBytes(data, 0, data.writerIndex());
			ls.edited = modified;
		}
		if (!name.equals(ls.name)) {
			chng |= 2;
			dos.writeString(name);
			ls.name = name;
		}
		if (renderAll != ls.drawAll) {
			chng |= renderAll ? 8 : 4;
			ls.drawAll = renderAll;
		}
		if (chng == 0) return false;
		dos.setByte(p, chng);
		return true; 
	}

	@Override
	public void updateClientChanges(DataContainer container, PacketBuffer dis) {
		int chng = dis.readByte();
		if ((chng & 1) != 0) {
			deserialize(dis);
			fixCons();
			modified = 0;
		}
		if ((chng & 2) != 0) name = dis.readString(16);
		if ((chng & 12) != 0) renderAll = (chng & 8) != 0;
	}

	public ByteBuf serialize() {
		ByteBuf dos = Unpooled.buffer();
		int n = 0, p = dos.writerIndex();
		dos.writeByte(n);
		int pnp = 0;
		for (Module m : modules)
			if (m != null) {
				if (m.pos > pnp) {
					dos.writeByte(-1);
					dos.writeByte(m.pos - pnp);
					n++;
				}
				int t = m.type.ordinal();
				if (m.type.isNum) t |= (m.size - 1) << 6;
				else if (m.type.varInAm) t |= (m.cons() - 1) << 6;
				dos.writeByte(t);
				for (Con c : m.cons)
					if (c != null)
						dos.writeByte(c.getAddr() & 0x3f | (c.type & 3) << 6);
				byte[] b = m.label.getBytes();
				dos.writeByte(b.length);
				dos.writeBytes(b);
				pnp = m.pos + m.size;
				n++;
			}
		dos.setByte(p, n);
		return dos;
	}

	public void deserialize(ByteBuf dis) {
		Arrays.fill(modules, null);
		int p = 0;
		for (int n = dis.readByte() & 0xff; n > 0; n--) {
			int t = dis.readByte();
			if (t == -1) p += dis.readByte() & 0xff;
			else {
				Module m = new Module(ModuleType.values()[t & 0x3f]);
				int sz = (t >> 6 & 3) + 1;
				if (m.type.isNum) m.size = sz;
				else if (m.type.varInAm)
					for (int i = sz; i < m.cons.length; i++)
						m.cons[i] = null;
				for (Con c : m.cons)
					if (c != null) {
						byte b = dis.readByte();
						c.addr = b & 0x3f;
						c.type = (byte)(c.type & 4 | b >> 6 & 3);
					}
				byte[] b = new byte[dis.readByte() & 0xff];
				dis.readBytes(b);
				m.label = new String(b);
				m.setPos(p);
				p += m.size;
			}
		}
	}

	public void fixCons() {
		for (Module m : modules)
			if (m != null)
				for (Con c : m.cons)
					if (c != null) {
						int addr = c.getAddr();
						c.setAddr(find(addr), addr);
					}
	}

	public void modify() {
		modified++;
	}

	public void add(ModuleType t) {
		if (t == ModuleType.OUT) {
			for (int i = 64; i < modules.length; i++) {
				if (modules[i] != null) continue;
				Module m = new Module(t);
				m.setPos(selMod = i);
				modified++;
				break;
			}
		} else {
			if (selMod < 0)
				for (selMod = 63; selMod >= 0 && modules[selMod] == null; selMod--);
			selMod++;
			Module m = new Module(t);
			m.setPos(selMod);
			modified++;
		}
	}

	public void move(int cur, int pos) {
		Module m = modules[cur];
		if (m != null) {
			m.setPos(pos);
			modified++;
		}
	}

	public Module find(int mp) {
		if (mp >= modules.length) return null;
		for (int l = mp - 3; mp >= l && mp >= 0; mp--) {
			Module m = modules[mp];
			if (m != null)
				return m.size + mp > l + 3 ? m : null;
		}
		return null;
	}

	public static class Con {
		public Con(int ad, byte t) {addr = ad; type = t;}
		public Module mod;
		public int addr;
		/** 0-3: 8-32 bit num, 4: 1 bit bin, 5: 8 bit bin */
		public byte type;
		public int getAddr() {return mod != null ? mod.pos + addr : addr;}
		public void setAddr(Module m, int ad) {
			mod = m;
			addr = mod != null ? ad - mod.pos : ad;
		}
	}

	public class Module {

		public final Con[] cons;
		public final ModuleType type;
		public int pos, size, selCon;
		public String label = "";

		public Module(ModuleType type) {
			this.type = type;
			this.size = 1;
			this.pos = -1;
			this.cons = new Con[type.cons()];
			for (int i = 0 ; i < cons.length; i++)
				cons[i] = new Con(-1, type.conType(i));
			if (type == ModuleType.CST) label = "0";
		}

		public int cons() {
			if (type.varInAm) {
				int n = 0;
				for (Con c : cons)
					if (c != null) n++;
				return n;
			} else return cons.length;
		}
	
		public void setPos(int p) {
			if (pos >= 0 && modules[pos] == this) modules[pos] = null;
			Module m = find(p);
			if (m != null && m.pos < p) p = m.pos + m.size;
			if (type == ModuleType.OUT ? p < 64 || p + size > modules.length : p < 0 || p + size > 64) return;
			for (int i = p + size - 1; i >= p; i--) {
				m = modules[i];
				if (m != null) m.setPos(p + size);
			}
			modules[pos = p] = this;
		}

		public void resize(int s) {
			size = s;
			setPos(pos);
		}

		public void addCon() {
			for (int i = 0; i < cons.length; i++)
				if (cons[i] == null) {
					cons[i] = new Con(-1, type.conType(i));
					break;
				}
		}

		public void removeCon() {
			for (int i = cons.length - 1; i > 0; i--)
				if (cons[i] != null) {
					cons[i] = null;
					break;
				}
		}

	}

	public enum ModuleType {
		CST(0,0,1), IN(0,0,1), OR(4,0,6), NOR(4,0,6), AND(4,0,6), NAND(4,0,6), XOR(4,0,6), XNOR(4,0,6),
		BUF(0,1,1), NOT(1,0,2), LS(0,2,0), NLS(0,2,0), EQ(0,2,0), NEQ(0,2,0), NEG(0,1,1), ABS(0,1,1),
		ADD(0,2,1), SUB(0,2,1), MUL(0,2,1), DIV(0,2,1), MOD(0,2,1), MIN(0,2,1), MAX(0,2,1),
		SWT(1,2,1), CNT1(2,0,1), CNT2(2,2,1), RNG(0,1,1), SQRT(0,1,1), OUT(0,1,0);
		private ModuleType(int bc, int nc, int type) {
			binCon = bc;
			numCon = nc;
			isNum = (type & 1) != 0;
			can8bit = (type & 2) != 0;
			varInAm = (type & 4) != 0;
		}
		public final int binCon, numCon;
		public final boolean varInAm, can8bit, isNum;
		public int cons() {return binCon + numCon;}
		public byte conType(int i) {return i >= binCon ? (byte)0 : 4;}
		@SideOnly(Side.CLIENT)
		public int conRenderPos(int i) {
			if (this == OUT) return 2;
			switch(cons()) {
			case 1: return 6;
			case 2: return 3 + i * 7;
			case 3: return 2 + i * 4;
			case 4: return 2 + i * 3;
			default: return 0;
			}
		}
	}

	public static class LastState {
		String name;
		boolean drawAll, output;
		long edited;
	}

	@Override
	public int[] getSyncVariables() {
		return null;
	}

	@Override
	public void setSyncVariable(int i, int v) {
	}

}
