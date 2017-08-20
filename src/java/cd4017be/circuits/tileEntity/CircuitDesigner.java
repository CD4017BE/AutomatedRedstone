package cd4017be.circuits.tileEntity;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.util.ArrayList;
import com.mojang.authlib.GameProfile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.items.SlotItemHandler;
import cd4017be.circuits.Objects;
import cd4017be.lib.BlockGuiHandler.ClientPacketReceiver;
import cd4017be.lib.TooltipInfo;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.block.BaseTileEntity;
import cd4017be.lib.templates.LinkedInventory;

public class CircuitDesigner extends BaseTileEntity implements IGuiData, ClientPacketReceiver {

	public ItemStack dataItem;
	private GameProfile lastPlayer;
	public Module module0 = null, moduleL = null, selMod = null;
	public ArrayList<Module> outputs;
	public long modified = 1;
	public boolean renderAll, mode;
	ByteBuf data = Unpooled.buffer();
	public String name = "";

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		readNBT(nbt);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		writeNBT(nbt);
		return super.writeToNBT(nbt);
	}

	private NBTTagCompound writeNBT(NBTTagCompound nbt) {
		byte[] b = new byte[data.readableBytes()];
		data.readBytes(b);
		data.resetReaderIndex();
		nbt.setByteArray("data", b);
		nbt.setString("name", name);
		nbt.setByte("mode", (byte)((mode ? 1 : 0) | (renderAll ? 2 : 0)));
		return nbt;
	}

	private void readNBT(NBTTagCompound nbt) {
		data.clear();
		byte[] b = nbt.getByteArray("data");
		data.writeBytes(b);
		name = nbt.getString("name");
		byte m = nbt.getByte("mode");
		mode = (m & 1) != 0;
		renderAll = (m & 2) != 0;
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
			if (dataItem != null && dataItem.getItem() == Objects.circuit_plan) {
				dataItem.setTagCompound(writeNBT(new NBTTagCompound()));
			} break;
		case 2:
			if (dataItem != null && dataItem.getItem() == Objects.circuit_plan && dataItem.hasTagCompound()) {
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
		case 4: mode = false; break;
		case 5: mode = true; break;
		case 6: renderAll = false; break;
		case 7: renderAll = true; break;
		}
	}

	@Override
	public void initContainer(DataContainer container) {
		TileContainer cont = (TileContainer)container;
		cont.addItemSlot(new SlotItemHandler(new LinkedInventory(1, 1, (i) -> dataItem, (item, i) -> dataItem = item), 0, 194, 220));
		cont.addPlayerInventory(8, 162);
		if (world.isRemote) {
			modified = 0;
			if (outputs == null) outputs = new ArrayList<Module>(6);
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
		if (mode != ls.output) {
			chng |= mode ? 32 : 16;
			ls.output = mode;
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
		if ((chng & 48) != 0) mode = (chng & 32) != 0;
	}

	public ByteBuf serialize() {
		ByteBuf dos = Unpooled.buffer();
		int n = 0, p = dos.writerIndex();
		dos.writeByte(n);
		int pnp = 0;
		for (Module m = module0; m != null; m = m.next) {
			if (m.pos > pnp) {
				dos.writeByte(-1);
				dos.writeByte(m.pos - pnp);
				n++;
			}
			int t = m.type.ordinal();
			dos.writeByte(t);
			if (t < 6) {
				int l = 0, q = dos.writerIndex();
				dos.writeByte(l);
				for (Con c : m.cons)
					if (c != null) {
						dos.writeByte(c.getAddr());
						l++;
					}
				dos.setByte(q, l);
			} else {
				if (t >= 10) dos.writeByte(m.size);
				for (int i = 0; i < m.cons.length; i++) {
					Con c = m.cons[i];
					if (!m.type.conType(i)) {
						dos.writeByte(c == null ? m.pos : c.getAddr());
					} else if (c == null || c.type == 1) {
						dos.writeByte(-1);
						dos.writeInt(c == null ? 0 : c.size);
					} else {
						dos.writeByte(c.size / 8 - 1 | (c.type == 3 ? 4 : 0));
						dos.writeByte(c.getAddr());
					}
				}
			}
			byte[] b = m.label.getBytes();
			dos.writeByte(b.length);
			dos.writeBytes(b);
			pnp = m.nextPos;
			n++;
		}
		dos.setByte(p, n);
		dos.writeByte(outputs.size());
		for (Module m : outputs) {
			dos.writeByte(m.pos);
			dos.writeByte(m.size);
			byte[] b = m.label.getBytes();
			dos.writeByte(b.length);
			dos.writeBytes(b);
		}
		return dos;
	}

	public void deserialize(ByteBuf dis) {
		module0 = null; moduleL = null; selMod = null;
		outputs.clear();
		Module k = null;
		int p = 0;
		for (int n = dis.readByte() & 0xff; n > 0; n--) {
			int t = dis.readByte();
			if (t < 0 || t > 17) p += dis.readByte() & 0xff;
			else {
				Module m = new Module(ModuleType.values()[t]);
				if (t < 6) {
					int l = dis.readByte();
					for (int i = 0; i < l; i++) {
						m.cons[i] = new Con(null, dis.readByte() & 0xff, 1, (byte)0);
					}
				} else {
					if (t >= 10) m.size = dis.readByte();
					for (int i = 0; i < m.cons.length; i++)
						if (!m.type.conType(i)) {
							m.cons[i] = new Con(null, dis.readByte() & 0xff, 1, (byte)0);
						} else {
							int ct = dis.readByte();
							if (ct < 0) m.cons[i] = new Con(null, -1, dis.readInt(), (byte)1);
							else m.cons[i] = new Con(null, dis.readByte() & 0xff, 8 + 8 * (ct & 3), (byte)(2 + (ct >> 2)));
						}
				}
				m.setPos(p);
				p = m.nextPos;
				byte[] b = new byte[dis.readByte()];
				dis.readBytes(b);
				m.label = new String(b);
				if (k == null) module0 = moduleL = m;
				else k.insert(m);
				k = m;
			}
		}
		for (int n = dis.readByte() & 0x7; n > 0; n--) {
			int pos = dis.readByte() & 0xff;
			Module m = new Module(ModuleType.IN);
			m.size = dis.readByte();
			m.setPos(pos);
			byte[] b = new byte[dis.readByte()];
			dis.readBytes(b);
			m.label = new String(b);
			outputs.add(m);
		}
	}

	public void fixCons() {
		for (Module m = module0; m != null; m = m.next)
			for (Con c : m.cons)
				if (c != null && c.type != 1) {
					c.mod = find(m, c.addr, false);
					if (c.mod != null) c.addr -= c.mod.pos;
				}
	}

	public void modify() {
		modified++;
	}

	public void add(ModuleType t) {
		if (mode) {
			if (outputs.size() >= 6) return;
			int p = selMod == null ? 0 : selMod.nextPos;
			selMod = new Module(t);
			selMod.setPos(p);
			outputs.add(selMod);
		} else {
			Module tgt = selMod == null ? moduleL : selMod;
			selMod = new Module(t);
			if (tgt == null) module0 = moduleL = selMod;
			else tgt.insert(selMod);
		}
		modified++;
	}

	public void move(Module m, int pos) {
		if (mode) {
			m.setPos(pos);
		} else {
			Module m1 = find(m, pos, true);
			if (m1 != m) {
				remove(m);
				m.setPos(pos);
				if (m1 != null) m1.insert(m);
				else insertPre(m);
			} else m.setPos(pos);
		}
		modified++;
	}

	void insertPre(Module m) {
		if (module0 == null) module0 = moduleL = m;
		else {
			module0.prev = m;
			m.next = module0;
			module0 = m;
			if (m.nextPos > m.next.pos)  m.next.setPos(m.nextPos);
		}
	}

	public Module find(Module m, int mp, boolean closest) {
		if (m == null) return null;
		if (mp >= m.nextPos) {
			for (m = m.next; m != null; m = m.next) {
				if (m.pos > mp) return closest ? m.prev : null;
				else if (m.nextPos > mp) return m;
			}
			return closest ? moduleL : null;
		} else if (mp < m.pos){
			for (m = m.prev; m != null; m = m.prev) {
				if (m.nextPos <= mp) return closest ? m : null;
				else if (m.pos <= mp) return m;
			}
			return null;
		} else return m;
	}

	public void remove(Module m) {
		if (mode) {
			outputs.remove(m);
		} else {
			if (m.prev != null) m.prev.next = m.next;
			else module0 = m.next;
			if (m.next != null) m.next.prev = m.prev;
			else moduleL = m.prev;
			m.prev = null;
			m.next = null;
		}
	}

	public static class Con {
		public Con(Module m, int ad, int s, byte t) {mod = m; addr = ad; size = s; type = t;}
		public Module mod;
		public int addr, size;
		/** 0:bit 1:const 2:unsigned 3:signed  */
		public byte type;
		public int getAddr() {return mod != null ? mod.pos + addr : addr;}
	}

	public class Module {

		public final Con[] cons;
		public final ModuleType type;
		public int pos, nextPos, size, selCon;
		public Module next, prev;
		public String label = "";

		public Module(ModuleType type) {
			this.type = type;
			this.size = type.idxLoc > 0 ? 8 : 1;
			this.cons = new Con[type.defCon];
			for (int i = 0 ; i < type.defCon; i++)
				if (type.conType(i))
					cons[i] = new Con(null, -1, 0, (byte)1);
			this.nextPos = size;
		}

		public void setPos(int pos) {
			pos = (pos & 7) + size > 8 ? pos + 7 & 0xf8 : pos;
			if (pos != this.pos || pos + size != nextPos) {
				this.pos = pos;
				this.nextPos = pos + size;
				if (nextPos > 256 && prev != null) {
					prev.next = null;
					moduleL = prev;
					next = null;
					prev = null;
				} else if (next != null && nextPos > next.pos) next.setPos(nextPos);
			}
		}

		public void resize(int s) {
			size = s;
			setPos(pos);
		}

		public void insert(Module m) {
			m.next = next;
			if (next != null) next.prev = m;
			else moduleL = m;
			next = m;
			m.prev = this;
			if (nextPos > m.pos) m.setPos(nextPos);
			else if (m.next != null && m.nextPos > m.next.pos) m.next.setPos(m.nextPos);
		}

	}

	public enum ModuleType {
		OR(0,8), NOR(0,8), AND(0,8), NAND(0,8), XOR(0,8), XNOR(0,8),
		LS(0,2) {@Override public boolean conType(int i) {return true;}},
		NLS(0,2) {@Override public boolean conType(int i) {return true;}},
		EQ(0,2) {@Override public boolean conType(int i) {return true;}},
		NEQ(0,2) {@Override public boolean conType(int i) {return true;}},
		COUNT(1,4) {@Override public boolean conType(int i) {return i >= 2;}},
		SWITCH(5,3) {@Override public boolean conType(int i) {return i > 0;}},
		ADD(8,2), SUB(8,2), MUL(8,2), DIV(8,2), MOD(8,2), IN(0, 0);
		private ModuleType(int b, int n) {defCon = n; idxLoc = b;}
		public final int defCon, idxLoc;
		public boolean conType(int i) {return idxLoc > 0;}
	}

	public static class LastState {
		String name;
		boolean drawAll, output;
		long edited;
	}

	@Override
	public boolean canPlayerAccessUI(EntityPlayer player) {
		return !player.isDead;
	}

	@Override
	public int[] getSyncVariables() {
		return null;
	}

	@Override
	public void setSyncVariable(int i, int v) {
	}

	@Override
	public String getName() {
		return TooltipInfo.getLocFormat("gui.cd4017be.circuitDesigner.name");
	}

}
