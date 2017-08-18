package cd4017be.circuits.tileEntity;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.SlotItemHandler;
import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.api.circuits.ISensor;
import cd4017be.circuits.render.OszillographRenderer;
import cd4017be.lib.BlockGuiHandler.ClientPacketReceiver;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.block.BaseTileEntity;
import cd4017be.lib.templates.LinkedInventory;
import cd4017be.lib.util.Utils;

public class Oszillograph extends BaseTileEntity implements ITilePlaceHarvest, ITickable, IGuiData, IDirectionalRedstone, ClientPacketReceiver {

	private static final float[] DefTransf = {1, 0, 1, 0, 1, 0, 1, 0};
	public static final int Size = 60;
	public final ItemStack[] inventory = new ItemStack[4];
	public final int[][] points = new int[4][];
	public final float[] transf = Arrays.copyOf(DefTransf, 8);
	/** bits[0-63 4*(1+3+5+5+1+1)]: channel*(on + dir + bitOfs + bitSize + signed + emptyBit) */
	public long cfg = 0x3e003e003e003e00L;
	public int idx = 59, tickInt = 1, mode = 0;
	public final String[] info = new String[]{"", "", "", ""};
	public float triggerLevel = 0;

	@Override
	public void update() {
		if (world.isRemote) return;
		long t = world.getTotalWorldTime();
		if (t % tickInt != 0 || !checkTrigger(t)) return;
		idx++;
		idx %= 60;
		for (int i = 0; i < points.length; i++) {
			if (points[i] == null) continue;
			ItemStack item = inventory[i];
			double nstate;
			if (!item.isEmpty() && item.getItem() instanceof ISensor) {
				nstate = ((ISensor)item.getItem()).measure(item, world, pos);
			} else nstate = getRedstone((int)(cfg >> i * 16 + 1) & 0x3fff);
			nstate /= transf[i * 2];
			nstate += transf[i * 2 + 1];
			points[i][idx] = Float.floatToIntBits(Math.min(0.5F, Math.max(-0.5F, (float)nstate / 6F)));
		}
		markUpdate();
	}

	private double getRedstone(int cfg) {
		EnumFacing side = EnumFacing.VALUES[(cfg & 7) % 6];
		long r = world.getRedstonePower(pos.offset(side), side);
		int shift = cfg >> 3 & 0x1f, size = cfg >> 8 & 0x1f;
		r = r >> shift & 0xffffffffL >>> (31 - size);
		if ((cfg & 0x2000) != 0) {
			long sgnMask = 1L << size;
			if ((r & sgnMask) != 0) return (r & ~sgnMask) - sgnMask;
		}
		return r;
	}

	private boolean checkTrigger(long t) {
		if (idx > 0) return true;
		int m = mode & 3;
		if (m == 3) return false;
		if (m == 0) return t / (long)tickInt % 60L == 0;
		int s = mode >> 2 & 7;
		double x;
		ItemStack item;
		if (m == 1) {
			EnumFacing side = EnumFacing.VALUES[s % 6];
			x = world.getRedstonePower(pos.offset(side), side);
		} else if (!(item = inventory[s % 4]).isEmpty() && item.getItem() instanceof ISensor) {
			x = ((ISensor)item.getItem()).measure(item, world, pos);
		} else x = 0;
		return (mode & 0x20) != 0 ? x < triggerLevel : x > triggerLevel;
	}

	@Override
	public void onPacketFromClient(PacketBuffer dis, EntityPlayer sender) throws IOException {
		byte cmd = dis.readByte();
		if (cmd < 8) transf[cmd] = dis.readFloat();
		else if (cmd == 8) {
			tickInt = dis.readInt();
			if (tickInt < 1) tickInt = 1;
			else if (tickInt > 1200) tickInt = 1200;
		} else if (cmd == 9) {
			mode = dis.readInt();
		} else if (cmd < 14) {
			info[cmd - 10] = dis.readString(16);
		} else if (cmd == 14) {
			triggerLevel = dis.readFloat();
		} else if (cmd == 15) {
			cfg = dis.readLong();
			for (int i = 0; i < 4; i++) {
				boolean active = (cfg >> i * 16 & 1) != 0;
				if (active && points[i] == null) points[i] = new int[Size];
				else if (!active && points[i] != null) points[i] = null;
			}
		}
		markUpdate();
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		int[] arr = nbt.getIntArray("cfg");
		for (int i = 0; i < 8; i++) transf[i] = arr.length > i ? Float.intBitsToFloat(arr[i]) : DefTransf[i];
		tickInt = nbt.getShort("tick");
		if (tickInt <= 0) tickInt = 1;
		idx = nbt.getShort("px");
		mode = nbt.getInteger("mode");
		cfg = nbt.getLong("cfgI");
		triggerLevel = nbt.getFloat("level");
		for (int i = 0; i < 4; i++) {
			info[i] = nbt.getString("inf" + i);
			points[i] = nbt.getIntArray("py" + i);
			if (points[i].length != Size) points[i] = null;
		}
		super.readFromNBT(nbt);
		if (FMLCommonHandler.instance().getSide() == Side.CLIENT && (world != null ? world.isRemote : FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT))
			setupGraph();
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		int[] arr = new int[8];
		for (int i = 0; i < 8; i++) arr[i] = Float.floatToIntBits(transf[i]);
		nbt.setIntArray("cfg", arr);
		nbt.setShort("px", (short)idx);
		nbt.setShort("tick", (short)tickInt);
		nbt.setInteger("mode", mode);
		nbt.setLong("cfgI", cfg);
		nbt.setFloat("level", triggerLevel);
		for (int i = 0; i < 4; i++) {
			nbt.setString("inf" + i, info[i]);
			if (points[i] != null)
				nbt.setIntArray("py" + i, points[i]);
		}
		return super.writeToNBT(nbt);
	}

	private ItemStack getItem(int slot) {
		return inventory[slot];
	}

	private void setItem(ItemStack item, int slot) {
		inventory[slot] = item;
	}

	@Override
	public void initContainer(DataContainer container) {
		TileContainer cont = (TileContainer)container;
		LinkedInventory inv = new LinkedInventory(inventory.length, 1, this::getItem, this::setItem);
		for (int i = 0; i < 4; i++)
			cont.addItemSlot(new SlotItemHandler(inv, i, 80, 16 + i * 18));
		cont.addPlayerInventory(8, 122);
	}

	@Override
	public int[] getSyncVariables() {
		int[] arr = new int[17];
		for (int i = 0; i < 8; i++) arr[i] = Float.floatToIntBits(transf[i]);
		for (int i = 0; i < 4; i++)
			if (points[i] != null) arr[i + 8] = points[i][idx];
		arr[12] = tickInt;
		arr[13] = mode;
		arr[14] = Float.floatToIntBits(triggerLevel);
		arr[15] = (int)cfg;
		arr[16] = (int)(cfg >> 32);
		return arr;
	}

	@Override
	public void setSyncVariable(int i, int v) {
		if (i < 8) transf[i] = Float.intBitsToFloat(v);
		else if (i == 12) tickInt = v;
		else if (i == 13) mode = v;
		else if (i == 14) triggerLevel = Float.intBitsToFloat(v);
		else if (i == 15) cfg = Utils.setState(cfg, 0, 0xffffffffL, v);
		else if (i == 16) cfg = Utils.setState(cfg, 32, 0xffffffffL, v);
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setShort("x", (short)idx);
		for (int i = 0; i < 4; i++) {
			nbt.setString("inf" + i, info[i]);
			if (points[i] != null)
				nbt.setFloat("y" + i, Float.intBitsToFloat(points[i][idx]));
		}
		return new SPacketUpdateTileEntity(pos, -1, nbt);
	}

	@SideOnly(Side.CLIENT)
	private void setupGraph() {
		for (int i = 0; i < 4; i++) {
			int[] data = points[i];
			if (data != null) {
				int[] vb = OszillographRenderer.newVertexData(Size);
				for (int j = 0; j < Size; j++)
					OszillographRenderer.setValue(vb, j, Float.intBitsToFloat(data[j]));
				vertexData[i] = vb;
				points[i] = null; //client doesn't need this
				OszillographRenderer.recolor(vertexData[i], idx, OszillographRenderer.colors[i]);
			}
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		NBTTagCompound nbt = pkt.getNbtCompound();
		idx = nbt.getShort("x");
		if (idx < 0) idx = 0;
		else if (idx >= Size) idx = Size - 1;
		for (int i = 0; i < 4; i++) {
			info[i] = nbt.getString("inf" + i);
			NBTBase tag = nbt.getTag("y" + i);
			if (tag == null) {
				vertexData[i] = null;
				continue;
			} else if (vertexData[i] == null) vertexData[i] = OszillographRenderer.newVertexData(Size);
			if (tag instanceof NBTTagFloat) OszillographRenderer.setValue(vertexData[i], idx, ((NBTTagFloat)tag).getFloat());
			OszillographRenderer.recolor(vertexData[i], idx, OszillographRenderer.colors[i]);
		}
	}

	public final int[][] vertexData = new int[4][];

	@Override
	public byte getRSDirection(EnumFacing s) {
		return 1;
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		List<ItemStack> list = makeDefaultDrops(null);
		for (ItemStack item : inventory)
			if (!item.isEmpty()) list.add(item);
		return list;
	}

	@Override
	public boolean canPlayerAccessUI(EntityPlayer player) {
		return !player.isDead;
	}

	@Override
	public boolean detectAndSendChanges(DataContainer container, PacketBuffer dos) {
		return false;
	}

	@Override
	public void updateClientChanges(DataContainer container, PacketBuffer dis) {
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {}

}
