package cd4017be.circuits.tileEntity;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.Level;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.items.ItemHandlerHelper;
import cd4017be.circuits.Objects;
import cd4017be.circuits.tileEntity.CircuitDesigner.ModuleType;
import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.SlotItemType;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.Gui.TileContainer.ISlotClickHandler;
import cd4017be.lib.templates.AutomatedTile;
import cd4017be.lib.templates.Inventory;
import cd4017be.lib.templates.Inventory.IAccessHandler;

/**
 *
 * @author CD4017BE
 */
public class Assembler extends AutomatedTile implements IAccessHandler, IGuiData, ISlotClickHandler {

	
	private static final Item circuit = Item.getItemFromBlock(Objects.circuit);
	public static final String[] tagNames = {"IO", "Cap", "Gate", "Calc"};
	public static final String[] materials = {"m.IORelay", "m.RAMPlate", "m.LogicPrc", "m.CalcPrc"};
	/**0-3:needed 4-7: provided 8:errCode{-1:clear 0:successful 1:outOfBounds 2:tooManyConstants 3:tooManyIO 4:dataSyntaxErr} */
	public int[] N = new int[9];
	NBTTagCompound code;
	private boolean recompile;

	public Assembler() {
		inventory = new Inventory(8, 3, this).group(0, 0, 1, -1).group(1, 2, 3, 1).group(2, 3, 7, 0);
		N[8] = -1;
	}

	@Override
	public void update() {
		super.update();
		if (worldObj.isRemote) return;
		if (recompile) {
			compile();
			recompile = false;
		}
		if (N[8] > 0) return;
		ItemStack item = inventory.items[1];
		if (item != null) {
			ItemStack stack;
			boolean done = true;
			for (int i = 0; i < 4; i++) {
				int n = N[i], m = N[i + 4];
				if (m > n) {
					stack = inventory.insertItem(3 + i, BlockItemRegistry.stack(materials[i], 1), false);
					if (stack == null) N[i + 4] = --m;
				} else if (m < n) {
					stack = inventory.extractItem(3 + i, 1, false);
					if (stack != null) N[i + 4] = ++m;
				}
				done &= m == n;
			}
			if (done) {
				item.setTagCompound(code);
				inventory.items[1] = inventory.insertItem(2, item, false);
				for (int i = 4; i < 8; i++) N[i] = 0;
			}
		} else if (inventory.items[0] != null && inventory.items[0].getItem() == circuit) {
			inventory.items[1] = item = inventory.extractItem(0, 1, false);
			NBTTagCompound nbt = item.getTagCompound();
			if (nbt == null) for (int i = 4; i < 8; i++) N[i] = 0;
			else for (int i = 0; i < 4; i++) N[i + 4] = nbt.getByte(tagNames[i]);
		}
	}

	public void compile() {
		ItemStack item = inventory.items[7];
		if (item == null || item.getItem() != Objects.circuitPlan || !item.hasTagCompound()) {
			for (int i = 0; i < 4; i++) N[i] = 0;
			N[8] = -1;
			code = null;
			return;
		}
		NBTTagCompound nbt = item.getTagCompound();
		int size = 0, logic = 0, calc = 0, io = 0, n;
		NBTTagList list = new NBTTagList();
		HashMap<Integer, Constant> constants = new HashMap<Integer, Constant>();
		ByteBuf out = Unpooled.buffer(), cst = Unpooled.buffer();
		ByteBuffer data = ByteBuffer.wrap(nbt.getByteArray("data"));
		try {
			int p = 0;
			for (n = data.get() & 0xff; n > 0; n--) {
				int t = data.get();
				if (t < 0) {
					int l = (data.get() - 1 & 0xf);
					out.writeByte(Circuit.C_NULL | l);
					p += l + 1;
					continue;
				} else if (t < 6) {
					t = (t << 4) + Circuit.C_OR; 
					int l = data.get() & 0xf;
					out.writeByte(t|l);
					for (int i = 0; i < l; i++) {
						int a = data.get() & 0xff;
						if (a / 8 >= size) size = a / 8 + 1;
						out.writeByte(a);
					}
					p++; logic++;
				} else if (t == 17) {
					NBTTagCompound tag = new NBTTagCompound();
					tag.setBoolean("d", false);
					tag.setByte("p", (byte)p);
					int l = data.get() & 0x3f;
					tag.setByte("s", (byte)l);
					p += l;
					io += l;
					out.writeByte(Circuit.C_IN | (l >= 8 ? l / 8 - 1 : 0));
					byte[] str = new byte[data.get()];
					data.get(str);
					tag.setString("n", new String(str));
					list.appendTag(tag);
					continue;
				} else {
					int q = out.writerIndex(), r = 0;
					out.writeByte(r);
					if (t < 10) {
						r = t - 6 + Circuit.C_COMP;
						p++; calc++;
					} else {
						int s = data.get() / 8 - 1 & 3;
						r = (t - 10) * 16 + Circuit.C_CNT | s;
						p += s * 8 + 8; calc += s + (t == 11 ? 1 : t < 14 ? 2 : 3);
					}
					ModuleType tp = ModuleType.values()[t];
					for (int i = 0; i < tp.defCon; i++)
						if (!tp.conType(i)) {
							int a = data.get() & 0xff;
							if (a / 8 >= size) size = a / 8 + 1;
							out.writeByte(a);
						} else {
							int ct = data.get();
							if (ct < 0) {
								int x = data.getInt();
								Constant c = constants.get(x);
								if (c == null) {
									int k, s = cst.writerIndex();
									if (x < -8388608 || x >= 8388608) {cst.writeByte(x).writeByte(x >> 8).writeByte(x >> 16).writeByte(x >> 24); k = 0xc0;}
									else if (x < -32768 || x >= 32768) {cst.writeByte(x).writeByte(x >> 8).writeByte(x >> 16); k = 0x80;}
									else if (x < -128 || x >= 128) {cst.writeByte(x).writeByte(x >> 8); k = 0x40;}
									else {cst.writeByte(x); k = 0;}
									constants.put(x, c = new Constant(k, s));
								}
								c.ref.add(out.writerIndex());
								out.writeByte(0);
								r |= 16 >> (tp.defCon - i);
							} else {
								r |= (ct & 4) << (i + 2 - tp.defCon);
								ct &= 3;
								int x = (data.get() & 0xff) / 8;
								if (x + ct >= size) size = x + ct + 1;
								out.writeByte(x | ct << 6);
							}
						}
					out.setByte(q, r);
				}
				data.position(data.get() + data.position());//skip labels
			}
			p += 7;
			if (p / 8 > size) size = p / 8;
			if (size > 32) {N[8] = 1; return;}
			if (size + cst.writerIndex() > 64) {N[8] = 2; return;}
			for (Constant c : constants.values()) {
				int k = c.idx + size | c.type;
				for (int i : c.ref)
					out.setByte(i, k);
			}
			n = data.get();
			if (n + list.tagCount() > 6) {N[8] = 3; return;}
			for (int i = 0; i < n; i++) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setBoolean("d", true);
				tag.setByte("p", data.get());
				int l = data.get() & 0x3f;
				tag.setByte("s", (byte)l);
				io += l;
				byte[] str = new byte[data.get()];
				data.get(str);
				tag.setString("n", new String(str));
				list.appendTag(tag);
			}
			if (data.position() != data.limit()) throw new IllegalStateException(String.format("Only read %d of %d bytes!", data.position(), data.limit()));
		} catch (Exception e) {
			String s = " ";
			for (byte b : data.array()) s += Integer.toHexString(b & 0xff) + " ";
			FMLLog.log("Circuit Assembler", Level.ERROR, e, "crashed while compiling schematic data: [%s]", s);
			N[8] = 4;
			return;
		}
		int ofs = size;
		size += cst.writerIndex();
		N[8] = 0;
		String name = nbt.getString("name");
		code = nbt = new NBTTagCompound();
		nbt.setByte(tagNames[0], (byte)(N[0] = io));
		nbt.setByte(tagNames[1], (byte)(N[1] = size));
		nbt.setByte(tagNames[2], (byte)(N[2] = logic));
		nbt.setByte(tagNames[3], (byte)(N[3] = calc));
		byte[] b = new byte[out.writerIndex() + size];
		out.readBytes(b, size, out.writerIndex());
		cst.readBytes(b, ofs, cst.writerIndex());
		nbt.setByteArray("data", b);
		nbt.setTag("io", list);
		nbt.setByte("ofs", (byte)ofs);
		nbt.setString("name", name);
	}

	private static class Constant {
		Constant(int t, int i) {type = t; idx = i;}
		final int type, idx;
		final ArrayList<Integer> ref = new ArrayList<Integer>();
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		recompile = true;
	}

	@Override
	public void initContainer(DataContainer container) {
		TileContainer c = (TileContainer)container;
		c.clickHandler = this;
		c.addItemSlot(new SlotItemType(inventory, 0, 152, 16, new ItemStack(circuit, 64), new ItemStack(circuit, 64, 1), new ItemStack(circuit, 64, 2)));
		c.addItemSlot(new SlotItemType(inventory, 2, 152, 52));
		c.addItemSlot(new SlotItemType(inventory, 3, 26, 16, BlockItemRegistry.stack(materials[0], 64)));
		c.addItemSlot(new SlotItemType(inventory, 4, 8, 16, BlockItemRegistry.stack(materials[1], 64)));
		c.addItemSlot(new SlotItemType(inventory, 5, 8, 52, BlockItemRegistry.stack(materials[2], 64)));
		c.addItemSlot(new SlotItemType(inventory, 6, 26, 52, BlockItemRegistry.stack(materials[3], 64)));
		c.addItemSlot(new SlotItemType(inventory, 7, 116, 34, new ItemStack(Objects.circuitPlan, 1)));
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
		if (item.stackSize > 0) container.mergeItemStack(item, 0, 1, false);
		if (item.stackSize > 0) container.mergeItemStack(item, 6, 7, false);
		return true;
	}

	@Override
	public int insertAm(int g, int s, ItemStack item, ItemStack insert) {
		if ((s >= 3 && s < 7 && !insert.isItemEqual(BlockItemRegistry.stack(materials[s - 3], 1)))
			|| (s == 7 && insert.getItem() != Objects.circuitPlan)
			|| (s == 0 && insert.getItem() != circuit)) return 0;
		int m = insert.getMaxStackSize(); 
		return item == null ? Math.min(m, insert.stackSize) : item.stackSize < m && ItemHandlerHelper.canItemStacksStack(item, insert) ? Math.min(m - item.stackSize, insert.stackSize): 0;
	}

	@Override
	public int extractAm(int g, int s, ItemStack item, int extract) {
		return item == null ? 0 : item.stackSize < extract ? item.stackSize : extract;
	}

	@Override
	public void setSlot(int g, int s, ItemStack item) {
		inventory.items[s] = item;
		if (s == 7) recompile = true;
	}

}
