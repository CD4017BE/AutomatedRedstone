package cd4017be.circuits.tileEntity;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.mojang.authlib.GameProfile;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.SlotItemHandler;
import cd4017be.circuits.Objects;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.BlockGuiHandler.ClientPacketReceiver;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.capability.LinkedInventory;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.Utils;

public class CircuitDesigner extends BaseTileEntity implements IGuiData, ClientPacketReceiver, ITilePlaceHarvest {

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
		case 8:
			for (EnumFacing side : EnumFacing.HORIZONTALS) {
				TileEntity te = Utils.neighborTile(this, side);
				if (te instanceof Assembler) {
					Assembler ass = (Assembler)te;
					if (dataItem.getItem() == Objects.circuit_plan) {
						dataItem.setTagCompound(writeNBT(new NBTTagCompound()));
						ItemStack item = ass.inventory.items[7];
						ass.inventory.setStackInSlot(7, dataItem);
						dataItem = item;
					}
					BlockGuiHandler.openBlockGui(sender, world, pos.offset(side));
					return;
				}
			}
			break;
		}
		markDirty();
	}

	@Override
	public void initContainer(DataContainer container) {
		TileContainer cont = (TileContainer)container;
		cont.addItemSlot(new SlotItemHandler(new LinkedInventory(1, 1, (i) -> dataItem, (item, i) -> {dataItem = item; markDirty();}), 0, 202, 232));
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
		addMissingConsts();
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
				int t = m.type.id();
				if (m.type.isNum) t |= (m.size - 1) << 6;
				else if (m.type.varInAm) t |= ((m.cons() - 1) / m.type.group) << 6;
				dos.writeByte(t);
				for (Con c : m.cons)
					if (c != null) {
						if (c.type >= 6) dos.writeByte(c.getAddr() & 0x3f | 0x80).writeByte(1 << (c.type - 6));
						else dos.writeByte(c.getAddr() & 0x3f | (c.type & 3) << 6);
					}
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
				Module m = new Module(ModuleType.get(t & 0x3f));
				int sz = (t >> 6 & 3) + 1;
				if (m.type.isNum) m.size = sz;
				else if (m.type.varInAm)
					for (int i = sz * m.type.group; i < m.cons.length; i++)
						m.cons[i] = null;
				for (Con c : m.cons)
					if (c != null) {
						byte b = dis.readByte();
						c.addr = b & 0x3f;
						c.type = (byte)(c.type & 4 | b >> 6 & 3);
						if (c.type >= 6) c.type = (byte)(6 + Integer.numberOfTrailingZeros(dis.readByte()));
					}
				byte[] b = new byte[dis.readByte() & 0xff];
				dis.readBytes(b);
				m.label = new String(b);
				m.setPos(p);
				p += m.size;
			}
		}
	}

	private void addMissingConsts() {
		for (Module m : modules) {
			if (m == null) continue;
			boolean mem = m.type == ModuleType.RD || m.type == ModuleType.WR;
			for (Con c : m.cons)
				if (mem) {
					mem = false;
					int a = c.getAddr();
					if (a < 0) {
						c.setAddr(m, m.pos);
						continue;
					}
					Module m1 = find(a);
					if (m1 != null) continue;
					int n = m.size;
					int i0 = m.pos, i1 = (a - i0 - (a > i0 ? n - 1 : 0)) / n * n + i0;
					if (i0 > i1) {
						i0 = i1;
						i1 = m.pos;
						a = i0;
					} else {
						a = i1 + n - 1;
					}
					for (; i0 <= i1; i0 += n) {
						int i = m1 == null ? 0 : m1.pos + m1.size - i0;
						if (i <= 0) {m1 = null; i = 0;}
						Module cst = null;
						for (; i < n; i++) {
							m1 = modules[i + i0];
							if (m1 != null) {
								cst = m1.type == ModuleType.CST ? m1 : null;
								i += m1.size - 1;
								continue;
							}
							if (cst != null && cst.size < 4) cst.size++;
							else modules[i + i0] = cst = new Module(ModuleType.CST);
						}
					}
					c.setAddr(find(a), a);
				} else if (c != null) {
					int a = c.getAddr(), n = a < 0 ? 0 : c.type < 4 ? c.type + 1 : 1;
					Module cst = null;
					while(n > 0 && a < 64) {
						Module m1 = find(a);
						if (m1 != null) {
							int i = m1.pos + m1.size - a;
							a += i;
							n -= i;
							cst = m1.type == ModuleType.CST && m1.label.equals("0") ? m1 : null;
							continue;
						} else if (cst == null || cst.size >= 4) {
							cst = new Module(ModuleType.CST);
							cst.pos = a;
							modules[a] = cst;
						} else cst.size++;
						if (c.mod == null || modules[c.mod.pos] != c.mod) c.setAddr(cst, c.getAddr());
						a++;
						n--;
					}
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
			if (++selMod >= 64) selMod = -1;
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
		/** 0-3: 8-32 bit num, 4: 1 bit bin, 5: 8 bit bin, 6-14: sel bit 0-7 */
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
			this.size = type.size;
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
			int n = type.group;
			for (int i = 0; i < cons.length; i++)
				if (cons[i] == null) {
					cons[i] = new Con(-1, type.conType(i));
					if (--n <= 0) break;
				}
		}

		public void removeCon() {
			int n = type.group;
			for (int i = cons.length - 1; i > 0; i--)
				if (cons[i] != null) {
					cons[i] = null;
					if (--n <= 0) break;
				}
		}

	}

	public enum ModuleType {
		CST(0,0,1), IN(0,0,1), OR(4,0,6), NOR(4,0,6), AND(4,0,6), NAND(4,0,6), XOR(4,0,6), XNOR(4,0,6),
		BUF(0,1,1), NOT(1,0,2), LS(0,2,0), NLS(0,2,0), EQ(0,2,0), NEQ(0,2,0), NEG(0,1,1), ABS(0,1,1),
		ADD(0,2,1), SUB(0,2,1), MUL(0,2,1), DIV(0,2,1), MOD(0,2,1), MIN(0,2,1), MAX(0,2,1),
		SWT(1,2,1), CNT1(2,0,1), CNT2(2,2,1), RNG(0,1,1), SQRT(0,1,1), BSL(0,2,1), BSR(0,2,1), COMB(8,0,6), FRG(3,0,0x22),
		RD(2,0,11), WR(2,1,11),
		OUT(0,1,0);

		public final int binCon, numCon, group, size;
		public final boolean varInAm, can8bit, isNum, lockMode;

		private ModuleType(int bc, int nc, int type) {
			binCon = bc;
			numCon = nc;
			group = Math.max((bc + nc + 3) / 4, 1);
			isNum = (type & 1) != 0;
			can8bit = (type & 2) != 0;
			varInAm = (type & 4) != 0;
			lockMode = (type & 8) != 0;
			int s = type >> 4 & 15;
			size = s == 0 ? 1 : s;
		}

		public static ModuleType get(int i) {
			ModuleType[] arr = values();
			if (i >= 0 && i < arr.length) return arr[i];
			else return OUT;
		}

		public int cons() {return binCon + numCon;}
		public byte conType(int i) {return i >= binCon ? (byte)0 : can8bit && lockMode ? (byte)5 : (byte)4;}
		public int id() {return this == OUT ? 63 : ordinal();}

		@SideOnly(Side.CLIENT)
		public int conRenderY(int i) {
			if (this == OUT) return 2;
			switch(cons()) {
			case 1: return 6;
			case 2: return 3 + i * 7;
			case 3: return 2 + i * 4;
			case 4: return 2 + i * 3;
			default: return 2 + i % 4 * 3;
			}
		}

		@SideOnly(Side.CLIENT)
		public int conRenderX(int i) {
			return 1 + i / 4 * 3;
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

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		List<ItemStack> list = makeDefaultDrops(null);
		if (!dataItem.isEmpty()) list.add(dataItem);
		return list;
	}

}
