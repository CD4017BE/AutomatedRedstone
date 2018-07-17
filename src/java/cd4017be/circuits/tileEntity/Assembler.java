package cd4017be.circuits.tileEntity;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.logging.log4j.Level;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.items.ItemHandlerHelper;
import cd4017be.circuits.Objects;
import cd4017be.circuits.tileEntity.CircuitDesigner.ModuleType;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.BlockGuiHandler.ClientPacketReceiver;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.SlotItemType;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.Gui.TileContainer.ISlotClickHandler;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.capability.BasicInventory;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.Utils;

/**
 *
 * @author CD4017BE
 */
public class Assembler extends BaseTileEntity implements ITickable, IGuiData, ISlotClickHandler, ClientPacketReceiver, ITilePlaceHarvest {

	public static final String[] tagNames = {"IO", "Cap", "Gate", "Calc"};
	public static final ItemStack[] materials = new ItemStack[4];
	/**0-3:needed 4-7: provided 8:errCode{-1:clear 0:successful 1:outOfBounds 2:tooManyConstants 3:tooManyIO 4:dataSyntaxErr} */
	public int[] N = new int[9];
	public final BasicInventory inventory = new BasicInventory(8);
	NBTTagCompound code;
	private boolean recompile;

	public Assembler() {
		inventory.onModify = this::onSetSlot;
		inventory.restriction = this::insertAmount;
		N[8] = -1;
	}

	@Override
	public void update() {
		if (world.isRemote) return;
		if (recompile) {
			compile();
			recompile = false;
		}
		if (N[8] > 0) return;
		ItemStack item = inventory.items[1];
		if (!item.isEmpty()) {
			ItemStack stack;
			boolean done = true;
			for (int i = 0; i < 4; i++) {
				int n = N[i], m = N[i + 4];
				if (m > n) {
					stack = inventory.insertItem(3 + i, materials[i].copy(), false);
					if (stack.getCount() == 0) N[i + 4] = --m;
				} else if (m < n) {
					stack = inventory.extractItem(3 + i, 1, false);
					if (stack.getCount() != 0) N[i + 4] = ++m;
				}
				done &= m == n;
			}
			if (done) {
				item.setTagCompound(code);
				inventory.items[1] = inventory.insertItem(2, item, false);
				for (int i = 4; i < 8; i++) N[i] = 0;
			}
		} else if (inventory.items[0].getItem() == Objects.circuit) {
			inventory.items[1] = item = inventory.extractItem(0, 1, false);
			NBTTagCompound nbt = item.getTagCompound();
			if (nbt == null) for (int i = 4; i < 8; i++) N[i] = 0;
			else for (int i = 0; i < 4; i++) N[i + 4] = nbt.getByte(tagNames[i]);
		}
	}

	public void compile() {
		ItemStack item = inventory.items[7];
		if (item.getItem() != Objects.circuit_plan || !item.hasTagCompound()) {
			for (int i = 0; i < 4; i++) N[i] = 0;
			N[8] = -1;
			code = null;
			return;
		}
		NBTTagCompound nbt = item.getTagCompound();
		long defined = 0, used = 0;
		int size = 0, logic = 0, calc = 0, io = 0;
		IntArrayList addrIdx = new IntArrayList();
		NBTTagList list = new NBTTagList();
		ByteBuf out = Unpooled.buffer();
		ByteBuffer data = ByteBuffer.wrap(nbt.getByteArray("data"));
		ByteBuffer cst = ByteBuffer.allocate(64);
		try {
			int p = 0, q = 0, nCst = 0;
			for (int n = data.get() & 0xff; n > 0; n--) {
				int t = data.get();
				if (t == -1) {
					int l = data.get() & 0xff;
					p += l;
					continue;
				}
				ModuleType mt = ModuleType.get(t & 0x3f);
				int sz = (t >> 6 & 3) + 1;
				if (mt == ModuleType.OUT) {
					NBTTagCompound tag = new NBTTagCompound();
					tag.setBoolean("d", true);
					byte addr = data.get();
					int l = addrSize(addr, (byte) 0);
					if (l >= 64 || (defined | set(addr, l)) != defined) {N[8] = 1; return;}
					int a = addr & 0x3f;
					tag.setByte("p", (byte)(addr & 0xc0 | Long.bitCount(defined << (63 - a)) - 1));
					byte[] str = new byte[data.get()];
					data.get(str);
					tag.setString("n", new String(str));
					list.appendTag(tag);
					io += (addr >> 6 & 3) * 4 + 8;
					p = 64;
					continue;
				}
				if (p >= 64) {N[8] = 3; return;}
				if (mt == ModuleType.CST) {
					byte[] str = new byte[data.get()];
					data.get(str);
					int c = Integer.parseInt(new String(str));
					nCst += sz;
					while(sz > 0) {
						defined |= 1L << p++;
						cst.put(q++, (byte)c);
						c >>= 8;
						sz--;
					}
				} else {
					if (nCst > 0) {
						if (nCst > 4) {
							out.writeByte(Circuit.C_SKIP);
							out.writeByte(nCst);
						} else {
							out.writeByte((nCst - 1) << 6);
						}
						nCst = 0;
					}
					out.writeByte(t);
					if (mt == ModuleType.IN) {
						NBTTagCompound tag = new NBTTagCompound();
						tag.setBoolean("d", false);
						tag.setByte("p", (byte)(q & 0x3f | t & 0xc0));
						io += sz * 8;
						byte[] str = new byte[data.get()];
						data.get(str);
						tag.setString("n", new String(str));
						list.appendTag(tag);
					} else {
						int w = mt.varInAm ? sz * mt.group : mt.cons();
						for (int i = 0; i < w; i++) {
							byte con = mt.conType(i);
							byte addr = data.get();
							addrIdx.add(out.writerIndex());
							out.writeByte(addr);
							if (extraByte(addr, con)) {
								out.writeByte(data.get());
								logic++;
							}
							int l = addrSize(addr, con);
							if (l >= 64) {N[8] = 1; return;}
							used |= set(addr, l);
						}
						logic += logicCost(mt, sz);
						calc += calcCost(mt, sz);
						data.position(data.get() + data.position());//skip labels
						if (!mt.isNum) sz = mt.size;
					}
					defined |= 0xfL >> (4 - sz) << p;
					p += sz; q += sz;
				}
			}
			if (data.position() != data.limit()) throw new IllegalStateException(String.format("Only read %d of %d bytes!", data.position(), data.limit()));
		} catch (Exception e) {
			String s = " ";
			for (byte b : data.array()) s += Integer.toHexString(b & 0xff) + " ";
			FMLLog.log("Circuit Assembler", Level.ERROR, e, "crashed while compiling schematic data: [%s]", s);
			N[8] = 4;
			return;
		}
		size = Long.bitCount(defined);
		if ((used & ~defined) != 0) {N[8] = 1; return;}
		if (list.tagCount() > 6) {N[8] = 2; return;}
		for (int i : addrIdx) {
			byte addr = out.getByte(i);
			out.setByte(i, addr & 0xc0 | Long.bitCount(defined << (63 - (addr & 0x3f))) - 1);
		}
		N[8] = 0;
		String name = nbt.getString("name");
		code = nbt = new NBTTagCompound();
		nbt.setByte(tagNames[0], (byte)(N[0] = io));
		nbt.setByte(tagNames[1], (byte)(N[1] = size));
		nbt.setByte(tagNames[2], (byte)(N[2] = logic));
		nbt.setByte(tagNames[3], (byte)(N[3] = calc));
		byte[] b = new byte[size + out.writerIndex()];
		cst.get(b, 0, size);
		out.readBytes(b, size, out.writerIndex());
		nbt.setByteArray("data", b);
		nbt.setTag("io", list);
		nbt.setString("name", name);
		nbt.setLong("used", defined);
	}

	public static boolean extraByte(byte val, byte ct) {
		return (val & 0xc0) >= 0x80 && ct >= 4;
	}

	private int addrSize(byte val, byte ct) {
		return (val & 0x3f) + (ct < 4 ? (val >> 6 & 3) : 0);
	}

	private static long set(byte val, int to) {
		val &= 0x3f;
		to -= val;
		return 0xfL >> (3 - to) << val;
	}

	public static int logicCost(ModuleType t, int sz) {
		switch(t) {
		case OR: case NOR: case AND: case NAND: case XOR: case XNOR: return (sz + 1) / 2;
		case NOT: return 1;
		case LS: case NLS: case EQ: case NEQ: return 2;
		case SWT: case MIN: case MAX: return (sz + 1) / 2 + 1;
		case CNT1: case COMB: case FRG: return 1;
		case CNT2: return 2;
		case RD: case WR: return 8;
		default: return 0;
		}
	}

	public static int calcCost(ModuleType t, int sz) {
		switch(t) {
		case NEG: case ABS: case CNT1: case MIN: case MAX: return (sz + 1) / 2;
		case ADD: case SUB: return sz;
		case MUL: case DIV: case MOD: return 1 + sz;
		case CNT2: return sz;
		case RNG: return sz + 2;
		case SQRT: return 2 * sz + 6;
		case BSL: case BSR: return 1;
		default: return 0;
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		inventory.read(nbt.getTagList("items", Constants.NBT.TAG_COMPOUND));
		recompile = true;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setTag("items", inventory.write());
		return super.writeToNBT(nbt);
	}

	@Override
	public void initContainer(DataContainer container) {
		TileContainer c = (TileContainer)container;
		c.clickHandler = this;
		c.addItemSlot(new SlotItemType(inventory, 0, 152, 16, new ItemStack(Objects.circuit, 64), new ItemStack(Objects.circuit, 64, 1), new ItemStack(Objects.circuit, 64, 2)));
		c.addItemSlot(new SlotItemType(inventory, 2, 152, 52));
		c.addItemSlot(new SlotItemType(inventory, 3, 26, 16, ItemHandlerHelper.copyStackWithSize(materials[0], 64)));
		c.addItemSlot(new SlotItemType(inventory, 4, 8, 16, ItemHandlerHelper.copyStackWithSize(materials[1], 64)));
		c.addItemSlot(new SlotItemType(inventory, 5, 8, 52, ItemHandlerHelper.copyStackWithSize(materials[2], 64)));
		c.addItemSlot(new SlotItemType(inventory, 6, 26, 52, ItemHandlerHelper.copyStackWithSize(materials[3], 64)));
		c.addItemSlot(new SlotItemType(inventory, 7, 116, 34, new ItemStack(Objects.circuit_plan, 1)));
		c.addPlayerInventory(8, 86);
	}

	@Override
	public int[] getSyncVariables() {
		return N;
	}

	@Override
	public boolean transferStack(ItemStack item, int s, TileContainer container) {
		if (s < 7) return false;
		container.mergeItemStack(item, 2, 6, false);
		if (item.getCount() > 0) container.mergeItemStack(item, 0, 1, false);
		if (item.getCount() > 0) container.mergeItemStack(item, 6, 7, false);
		return true;
	}

	public int insertAmount(int s, ItemStack insert) {
		if ((s >= 3 && s < 7 && !insert.isItemEqual(materials[s - 3]))
			|| (s == 7 && insert.getItem() != Objects.circuit_plan)
			|| (s == 0 && insert.getItem() != Objects.circuit)) return 0;
		return Math.min(64, insert.getMaxStackSize());
	}

	public void onSetSlot(ItemStack item, int s) {
		recompile |= s == 7;
		markDirty();
	}

	@Override
	public boolean slotClick(ItemStack item, Slot slot, int b, ClickType c, TileContainer cont) {
		return false;
	}

	@Override
	public void setSyncVariable(int i, int v) {
	}

	@Override
	public boolean detectAndSendChanges(DataContainer container, PacketBuffer dos) {
		return false;
	}

	@Override
	public void updateClientChanges(DataContainer container, PacketBuffer dis) {
	}

	@Override
	public void onPacketFromClient(PacketBuffer data, EntityPlayer sender) throws IOException {
		byte cmd = data.readByte();
		if (cmd == 0) { //switch gui
			for (EnumFacing side : EnumFacing.HORIZONTALS) {
				TileEntity te = Utils.neighborTile(this, side);
				if (te instanceof CircuitDesigner) {
					CircuitDesigner dsg = (CircuitDesigner)te;
					ItemStack item = inventory.items[7];
					if (item.getItem() == Objects.circuit_plan) {
						inventory.setStackInSlot(7, dsg.dataItem);
						dsg.dataItem = item;
						dsg.markDirty();
					}
					BlockGuiHandler.openBlockGui(sender, world, pos.offset(side));
					return;
				}
			}
		} else if (cmd == 1 && sender.isCreative() && N[8] == 0) { //quick cheat assembled circuit
			if (inventory.items[1].isEmpty()) inventory.items[1] = new ItemStack(Objects.circuit, 1, 2);
			for (int i = 0; i < 4; i++) N[i + 4] = N[i];
		}
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		List<ItemStack> list = makeDefaultDrops(null);
		inventory.addToList(list);
		return list;
	}

}
