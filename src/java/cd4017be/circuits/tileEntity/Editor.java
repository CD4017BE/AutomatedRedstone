package cd4017be.circuits.tileEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;

import cd4017be.circuits.Objects;
import cd4017be.circuits.editor.BoundingBox2D;
import cd4017be.circuits.editor.InvalidSchematicException;
import cd4017be.circuits.editor.InvalidSchematicException.ErrorType;
import cd4017be.circuits.editor.compile.Compiler;
import cd4017be.circuits.editor.op.IConfigurable;
import cd4017be.circuits.editor.op.OpNode;
import cd4017be.circuits.editor.op.OpType;
import cd4017be.circuits.editor.op.Pin;
import cd4017be.lib.BlockGuiHandler.ClientPacketReceiver;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.GlitchSaveSlot;
import cd4017be.lib.capability.LinkedInventory;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.ItemKey;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraftforge.common.util.Constants.NBT;
import scala.actors.threadpool.Arrays;

/**
 * @author CD4017BE
 *
 */
public class Editor extends BaseTileEntity implements IGuiData, ClientPacketReceiver {

	public static int CAPACITY = 256;
	public static final int[] OP_COSTS = new int[OpType.values().length];
	public static final HashMap<ItemKey, int[]> RECIPES = new HashMap<>();
	static {
		RECIPES.put(new ItemKey(new ItemStack(Items.REDSTONE)), new int[] {2, 0, 0});
		RECIPES.put(new ItemKey(new ItemStack(Blocks.REDSTONE_BLOCK)), new int[] {18, 0, 0});
		RECIPES.put(new ItemKey(new ItemStack(Items.GOLD_NUGGET)), new int[] {0, 0 ,2});
		RECIPES.put(new ItemKey(new ItemStack(Items.GOLD_INGOT)), new int[] {0, 0, 18});
		RECIPES.put(new ItemKey(new ItemStack(Blocks.GOLD_BLOCK)), new int[] {0, 0, 162});
		RECIPES.put(new ItemKey(new ItemStack(Items.QUARTZ)), new int[] {0, 4, 0});
		RECIPES.put(new ItemKey(new ItemStack(Blocks.QUARTZ_BLOCK)), new int[] {0, 16, 0});
		OP_COSTS[0] = OP_COSTS[1] = 0x01_00_04;
		OP_COSTS[2] = 0x00_00_01;
		OP_COSTS[3] = 0x02_00_04;
		OP_COSTS[4] = 0x01_00_00;
		for (int i = 5; i < OP_COSTS.length; i++)
			OP_COSTS[i] = 0x02_00_00;
	}

	public ArrayList<OpNode> operators = new ArrayList<OpNode>();
	BitSet toSync = new BitSet(), usedIdx = new BitSet();
	public boolean modified;
	public String name = "";
	/** 0:av A, 1:av B, 2:av C, 3:req A, 4:req B, 5:req C, 6:last Error */
	public int[] ingreds = {0,0,0, 0,0,0, InvalidSchematicException.NO_ERROR};
	public ItemStack inventory = ItemStack.EMPTY; 

	void clear() {
		if (world == null || !world.isRemote) {
			for (int i = 0, l = operators.size(); i < l; i++)
				if (operators.get(i) != null)
					toSync.set(i);
			usedIdx.clear();
		}
		operators.clear();
	}

	/**
	 * @param data Format: {B_opCount, {B_opId, extra...}[opCount], B_srcIdx[inPinCount]}
	 */
	void deserialize(ByteBuf data) {
		clear();
		ArrayList<Pin> pins = new ArrayList<Pin>();
		int n = data.readUnsignedByte();
		for (int i = 0; i < n; i++) {
			OpNode o = OpType.newOp(data.readByte() & 0xff, operators.size());
			if (o == null) continue;
			for (Pin p : o.outputs)
				pins.add(p);
			operators.add(o);
			o.read(data);
		}
		for (OpNode o : operators)
			for (int i = 0; i < o.inputs.length; i++) {
				int idx = data.readUnsignedByte();
				if (idx < pins.size())
					o.setInput(i, pins.get(idx));
			}
		usedIdx.set(0, n);
		if (world != null && !world.isRemote)
			toSync.set(0, operators.size());
	}

	/**
	 * @param data Format: {B_opCount, {B_opId, extra...}[opCount], B_srcIdx[inPinCount]}
	 */
	void serialize(ByteBuf data) {
		int i = data.writerIndex(), n = 0, pi = 0;
		data.writeByte(0);
		for (OpNode o : operators)
			if (o != null) {
				for (Pin p : o.outputs)
					p.listingIdx = pi++;
				data.writeByte(o.op.ordinal());
				o.write(data);
				n++;
			}
		data.setByte(i, n);
		for (OpNode o : operators)
			if (o != null)
				for (Pin p : o.inputs)
					data.writeByte(p == null ? -1 : p.listingIdx);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		name = nbt.getString("name");
		{int[] buf = nbt.getIntArray("ingred");
		System.arraycopy(buf, 0, ingreds, 0, buf.length < 7 ? buf.length : 7);}
		if (nbt.hasKey("inv", NBT.TAG_COMPOUND))
			inventory = new ItemStack(nbt.getCompoundTag("inv"));
		else inventory = ItemStack.EMPTY;
		if (nbt.hasKey("schematic", NBT.TAG_BYTE_ARRAY))
			deserialize(Unpooled.wrappedBuffer(nbt.getByteArray("schematic")));
		else clear();
		toSync.clear();
		modified = true;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setString("name", name);
		nbt.setIntArray("ingred", ingreds);
		if (!inventory.isEmpty())
			nbt.setTag("inv", inventory.writeToNBT(new NBTTagCompound()));
		ByteBuf buf = Unpooled.buffer();
		serialize(buf);
		byte[] data = new byte[buf.writerIndex()];
		buf.readBytes(data);
		nbt.setByteArray("schematic", data);
		return super.writeToNBT(nbt);
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		NBTTagList list = new NBTTagList();
		int j = -1;
		ByteBuf buf = Unpooled.buffer();
		while((j = toSync.nextSetBit(j + 1)) >= 0) {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setByte("i", (byte)j);
			OpNode op;
			if (j < operators.size() && (op = operators.get(j)) != null) {
				tag.setByte("t", (byte)op.op.ordinal());
				op.write(buf);
				byte[] arr = new byte[buf.writerIndex()];
				buf.readBytes(arr);
				tag.setByteArray("d", arr);
				buf.clear();
				arr = new byte[op.inputs.length * 2];
				int i = 0;
				for (Pin p : op.inputs)
					if (p != null) {
						arr[i++] = (byte)p.src.index;
						arr[i++] = (byte)p.index;
					} else {
						arr[i++] = -1;
						arr[i++] = -1;
					}
				tag.setByteArray("p", arr);
			} else tag.setByte("t", (byte)-1);
			list.appendTag(tag);
		}
		nbt.setTag("d", list);
		toSync.clear();
		return new SPacketUpdateTileEntity(pos, -1, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		NBTTagCompound nbt = pkt.getNbtCompound();
		NBTTagList list = nbt.getTagList("d", NBT.TAG_COMPOUND);
		ByteBuf buf = Unpooled.buffer();
		for (NBTBase t : list) {
			NBTTagCompound tag = (NBTTagCompound)t;
			int i = tag.getByte("i") & 0xff;
			OpNode op = i < operators.size() ? operators.get(i) : null;
			byte id = tag.getByte("t");
			if ((op == null ? -1 : op.op.ordinal()) != id) {
				if (op != null) op.remove();
				while (i > operators.size()) operators.add(null);
				op = OpType.newOp(id & 0xff, i);
				if (i < operators.size()) operators.set(i, op);
				else operators.add(op);
			}
			if (op != null) {
				buf.writeBytes(tag.getByteArray("d"));
				op.read(buf);
				buf.clear();
			}
		}
		for (NBTBase t : list) {
			NBTTagCompound tag = (NBTTagCompound)t;
			int idx = tag.getByte("i") & 0xff;
			OpNode op;
			if (idx < operators.size() && (op = operators.get(idx)) != null) {
				byte[] arr = tag.getByteArray("p");
				for (int i = 0; i < op.inputs.length; i++) {
					int j = arr[i<<1] & 0xff;
					OpNode op1;
					if (j < operators.size() && (op1 = operators.get(j)) != null)
						op.setInput(i, (j = arr[i<<1|1]) < op1.outputs.length ? op1.outputs[j] : null);
					else op.setInput(i, null);
				}
			}
		}
		modified = true;
		computeCost();
	}

	public static final BoundingBox2D<OpNode> BOARD_AREA = new BoundingBox2D<OpNode>(null, -1, -1, 120, 60);
	public static final byte A_NEW = 0, A_LOAD = 1, A_SAVE = 2, A_COMPILE = 3, A_NAME = 4, A_ADD = 5, A_REM = 6, A_MOVE = 7, A_CONNECT = 8, A_SET_LABEL = 9, A_SET_VALUE = 10;

	@Override
	public void onPacketFromClient(PacketBuffer pkt, EntityPlayer sender) throws IOException {
		ingreds[6] = InvalidSchematicException.NO_ERROR;
		byte cmd = pkt.readByte();
		switch(cmd) {
		case A_NEW:
			clear();
			name = "";
			break;
		case A_LOAD: return;
		case A_SAVE: return;
		case A_COMPILE:
			try {
				compile();
			} catch (InvalidSchematicException e) {
				ingreds[6] = e.compact();
			} return;
		case A_NAME: name = pkt.readString(64); return;
		case A_ADD: {
			int i = usedIdx.nextClearBit(0);
			if (i >= 256) return;
			OpNode op = OpType.newOp(pkt.readByte(), i);
			op.rasterX = pkt.readUnsignedByte();
			op.rasterY = pkt.readUnsignedByte();
			if (getCollision(op.getBounds()) != null) return;
			toSync.set(i);
			usedIdx.set(i);
			if (i == operators.size()) operators.add(op);
			else operators.set(i, op);
		}	break;
		case A_REM: {
			OpNode op;
			int i = pkt.readUnsignedByte();
			if (i >= operators.size() || (op = operators.get(i)) == null) return;
			toSync.set(i);
			usedIdx.clear(i);
			op.remove();
			operators.set(i, null);
		}	break;
		case A_MOVE: {
			OpNode op;
			int i = pkt.readUnsignedByte();
			if (i >= operators.size() || (op = operators.get(i)) == null) return;
			int prevX = op.rasterX, prevY = op.rasterY;
			op.rasterX = pkt.readUnsignedByte();
			op.rasterY = pkt.readUnsignedByte();
			if (getCollision(op.getBounds()) != null) {
				op.rasterX = prevX;
				op.rasterY = prevY;
				return;
			} else toSync.set(i);
		}	break;
		case A_CONNECT: {
			OpNode op, op1;
			int i = pkt.readUnsignedByte();
			if (i >= operators.size() || (op = operators.get(i)) == null) return;
			int i1 = pkt.readUnsignedByte(), pins = pkt.readUnsignedByte();
			if ((pins & 15) >= op.inputs.length) return;
			Pin p = null;
			if (i1 < operators.size() && (op1 = operators.get(i1)) != null && pins >> 4 < op1.outputs.length)
				p = op1.outputs[pins >> 4];
			op.setInput(pins & 15, p);
			toSync.set(i);
		}	break;
		case A_SET_LABEL: {
			OpNode op;
			int i = pkt.readUnsignedByte();
			if (i >= operators.size() || (op = operators.get(i)) == null) return;
			toSync.set(i);
			op.label = pkt.readString(32);
		}	break;
		case A_SET_VALUE: {
			OpNode op;
			int i = pkt.readUnsignedByte();
			if (i >= operators.size() || !((op = operators.get(i)) instanceof IConfigurable)) return;
			toSync.set(i);
			((IConfigurable)op).setCfg(pkt.readString(32));
		}	break;
		}
		markUpdate();
		markDirty();
	}

	public BoundingBox2D<OpNode> getCollision(BoundingBox2D<OpNode> box) {
		if (!box.enclosedBy(BOARD_AREA)) return BOARD_AREA;
		BoundingBox2D<OpNode> box1;
		for (OpNode op1 : operators)
			if (op1 != null && (box1 = op1.getBounds()).overlapsWith(box))
				return box1;
		return null;
	}

	private void computeCost() {
		int a = 0, b = 0, c = 0;
		for (OpNode op : operators)
			if (op != null) {
				int i = OP_COSTS[op.op.ordinal()];
				a += i >> 16 & 0xff;
				b += i >> 8 & 0xff;
				c += i & 0xff;
			}
		ingreds[3] = a;
		ingreds[4] = b;
		ingreds[5] = c;
	}

	void compile() throws InvalidSchematicException {
		ItemStack stack = inventory;
		if (stack.getItem() != Objects.processor)
			throw new InvalidSchematicException(ErrorType.noCircuitBoard, null, 0);
		computeCost();
		int[] cost = ingreds.clone(), ingr;
		int n = stack.getCount();
		if (stack.hasTagCompound())
			ingr = Arrays.copyOf(stack.getTagCompound().getIntArray("ingr"), 3);
		else ingr = new int[3];
		for (int i = 0; i < 3; i++)
			if (cost[i] < (cost[i+3] -= ingr[i]) * n)
				throw new InvalidSchematicException(ErrorType.missingMaterial, null, i);
		
		NBTTagCompound nbt = Compiler.assemble(Compiler.filterProgramm(operators));
		
		for (int i = 0, c; i < 3; i++)
			if ((c = cost[i + 3]) > 0) {
				ingr[i] += c;
				ingreds[i] -= c * n;
			}
		nbt.setIntArray("ingr", ingr);
		nbt.setString("name", name);
		stack.setTagCompound(nbt);
	}

	@Override
	public void initContainer(DataContainer container) {
		TileContainer cont = (TileContainer)container;
		LinkedInventory inv = new LinkedInventory(1, 64, (s)-> inventory, this::putItem);
		cont.addItemSlot(new GlitchSaveSlot(inv, 0, 174, 232, false));
		cont.addPlayerInventory(8, 174);
		if (world.isRemote) {
			modified = true;
			name = "";
		} else {
			computeCost();
			container.extraRef = "";
		}
	}

	@Override
	public int[] getSyncVariables() {
		return ingreds;
	}

	@Override
	public void setSyncVariable(int i, int v) {
		ingreds[i] = v;
	}

	@Override
	public boolean detectAndSendChanges(DataContainer container, PacketBuffer dos) {
		if (!name.equals(container.extraRef)) {
			dos.writeByte(1);
			dos.writeString(name);
			return true;
		} else {
			dos.writeByte(0);
			return false;
		}
	}

	@Override
	public void updateClientChanges(DataContainer container, PacketBuffer dis) {
		if (dis.readByte() == 1)
			name = dis.readString(64);
	}

	private void putItem(ItemStack stack, int slot) {
		inventory = stack;
		int n = stack.getCount();
		if (world.isRemote || n <= 0) return;
		ingreds[6] = InvalidSchematicException.NO_ERROR;
		markDirty();
		if (stack.getItem() == Objects.processor && stack.hasTagCompound()) {
			NBTTagCompound nbt = stack.getTagCompound();
			int[] ig = nbt.getIntArray("ingr");
			boolean empty = false;
			for (int i = 0; i < 3; i++) {
				int x = ig[i], d = (CAPACITY - ingreds[i]) / n;
				if (x > 0 && d > 0) {
					if (d > x) d = x;
					ig[i] = x -= d;
					ingreds[i] += d * n;
				}
				empty |= x <= 0;
			}
			if (empty) stack.setTagCompound(null);
			else nbt.getKeySet().removeIf((key)-> !key.equals("ingr"));
		} else {
			int[] c = RECIPES.get(new ItemKey(stack));
			if (c == null) return;
			for (int i = 0; i < 3; i++)
				if (CAPACITY - ingreds[i] < c[i] * n) return;
			for (int i = 0; i < 3; i++)
				ingreds[i] += c[i] * n;
			inventory = ItemStack.EMPTY;
		}
	}

}
