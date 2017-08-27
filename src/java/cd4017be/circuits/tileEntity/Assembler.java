package cd4017be.circuits.tileEntity;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.ByteBuffer;
import org.apache.logging.log4j.Level;

import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.items.ItemHandlerHelper;
import cd4017be.circuits.Objects;
import cd4017be.circuits.tileEntity.CircuitDesigner.ModuleType;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.SlotItemType;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.Gui.TileContainer.ISlotClickHandler;
import cd4017be.lib.block.BaseTileEntity;
import cd4017be.lib.templates.BasicInventory;

/**
 *
 * @author CD4017BE
 */
public class Assembler extends BaseTileEntity implements ITickable, IGuiData, ISlotClickHandler {

	private static final Item circuit = Item.getItemFromBlock(Objects.circuit);
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
		} else if (inventory.items[0].getItem() == circuit) {
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
		int size = 0, logic = 0, calc = 0, io = 0, maxAddr = 0;
		NBTTagList list = new NBTTagList();
		ByteBuf out = Unpooled.buffer();
		ByteBuffer data = ByteBuffer.wrap(nbt.getByteArray("data"));
		ByteBuffer cst = ByteBuffer.allocate(64);
		try {
			int p = 0, skip = 0;
			for (int n = data.get() & 0xff; n > 0; n--) {
				int t = data.get();
				if (t == -1) {
					int l = data.get() & 0xff;
					p += l;
					skip += l;
					continue;
				}
				ModuleType mt = ModuleType.values()[t & 0x3f];
				int sz = (t >> 6 & 3) + 1;
				if (mt == ModuleType.OUT) {
					NBTTagCompound tag = new NBTTagCompound();
					tag.setBoolean("d", true);
					byte addr = data.get();
					if (addrSize(addr, mt, 0) >= size) {N[8] = 1; return;}
					tag.setByte("p", addr);
					byte[] str = new byte[data.get()];
					data.get(str);
					tag.setString("n", new String(str));
					list.appendTag(tag);
					io += (addr >> 6 & 3) * 4 + 8;
					p = 64; skip = 0;
					continue;
				}
				if (p >= 64) {N[8] = 3; return;}
				if (mt == ModuleType.CST) {
					skip += sz;
					byte[] str = new byte[data.get()];
					data.get(str);
					int c = Integer.parseInt(new String(str));
					while(sz > 0) {
						cst.put(p++, (byte)c);
						c >>= 8;
						sz--;
					}
				} else {
					while (skip > 0) {
						int l = Math.min(skip, 4);
						out.writeByte((byte)((l - 1) << 6));
						skip -= l;
					}
					out.writeByte(t);
					if (mt == ModuleType.IN) {
						NBTTagCompound tag = new NBTTagCompound();
						tag.setBoolean("d", false);
						tag.setByte("p", (byte)(p & 0x3f | t & 0xc0));
						io += sz * 4 + 4;
						byte[] str = new byte[data.get()];
						data.get(str);
						tag.setString("n", new String(str));
						list.appendTag(tag);
						p += sz;
					} else {
						byte[] buf = new byte[mt.varInAm ? sz : mt.cons()];
						data.get(buf);
						out.writeBytes(buf);
						for (int i = 0; i < buf.length; i++)
							maxAddr = Math.max(maxAddr, addrSize(buf[i], mt, i));
						logic += logicCost(mt, sz);
						calc += calcCost(mt, sz);
						data.position(data.get() + data.position());//skip labels
						p += mt.isNum ? sz : 1;
					}
				}
				size = p;
			}
			if (data.position() != data.limit()) throw new IllegalStateException(String.format("Only read %d of %d bytes!", data.position(), data.limit()));
		} catch (Exception e) {
			String s = " ";
			for (byte b : data.array()) s += Integer.toHexString(b & 0xff) + " ";
			FMLLog.log("Circuit Assembler", Level.ERROR, e, "crashed while compiling schematic data: [%s]", s);
			N[8] = 4;
			return;
		}
		if (maxAddr >= size) {N[8] = 1; return;}
		if (list.tagCount() > 6) {N[8] = 2; return;}
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
	}

	private int addrSize(byte val, ModuleType mt, int i) {
		return (val & 0x3f) + (mt.conType(i) < 4 ? (val >> 6 & 3) : 0);
	}

	public static int logicCost(ModuleType t, int sz) {
		switch(t) {
		case OR: case NOR: case AND: case NAND: case XOR: case XNOR: return (sz + 1) / 2;
		case NOT: return 1;
		case LS: case NLS: case EQ: case NEQ: return 2;
		case SWT: case MIN: case MAX: return (sz + 1) / 2 + 1;
		case CNT1: return 1;
		case CNT2: return 2;
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
		c.addItemSlot(new SlotItemType(inventory, 0, 152, 16, new ItemStack(circuit, 64), new ItemStack(circuit, 64, 1), new ItemStack(circuit, 64, 2)));
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
			|| (s == 0 && insert.getItem() != circuit)) return 0;
		return Math.min(64, insert.getMaxStackSize());
	}

	public void onSetSlot(ItemStack item, int s) {
		recompile |= s == 7;
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

}
