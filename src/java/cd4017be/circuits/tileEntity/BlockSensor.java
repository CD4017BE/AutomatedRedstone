package cd4017be.circuits.tileEntity;

import java.io.IOException;
import java.util.Arrays;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.SlotItemHandler;
import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.api.circuits.ISensor;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.templates.AutomatedTile;
import cd4017be.lib.templates.Inventory;

public class BlockSensor extends AutomatedTile implements IDirectionalRedstone, IGuiData {

	private static final float[] DefTransf = {1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0};
	public int tickInt = 20;
	private short timer = 0;
	public final float[] transf = Arrays.copyOf(DefTransf, 12);
	private final int[] output = new int[6];

	public BlockSensor() {
		inventory = new Inventory(6, 0, null);
	}

	@Override
	public void update() {
		if (worldObj.isRemote || ++timer < tickInt) return;
		timer = 0;
		for (int i = 0; i < 6; i++) {
			ItemStack item;
			int nstate;
			if ((item = inventory.items[i]) != null && item.getItem() instanceof ISensor) 
				nstate = (int)Math.floor(((ISensor)item.getItem()).measure(item, worldObj, pos) * transf[i * 2] + transf[i * 2 + 1]);
			else nstate = 0;
			if (output[i] != nstate) {
				output[i] = nstate;
				worldObj.notifyBlockOfStateChange(pos.offset(EnumFacing.VALUES[i]), Blocks.REDSTONE_TORCH);
			}
		}
	}

	public EnumFacing getSide(int var, int id) {return EnumFacing.VALUES[(var >> (id * 4) & 0xf) % 6];}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		timer = nbt.getShort("timer");
		int[] arr = nbt.getIntArray("cfg");
		for (int i = 0; i < 12; i++) transf[i] = arr.length > i ? Float.intBitsToFloat(arr[i]) : DefTransf[i];
		tickInt = arr.length > 12 ? arr[12] : 1;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setShort("timer", timer);
		nbt.setIntArray("cfg", getSyncVariables());
		nbt.setIntArray("out", output);
		return super.writeToNBT(nbt);
	}

	@Override
	protected void customPlayerCommand(byte cmd, PacketBuffer dis, EntityPlayerMP player) throws IOException {
		if (cmd < 12) transf[cmd] = dis.readFloat();
		else if (cmd == 14) {
			tickInt = dis.readInt();
			if (tickInt < 1) tickInt = 1;
			else if (tickInt > 1200) tickInt = 1200;
		}
	}

	@Override
	public int redstoneLevel(int s, boolean str) {
		return str ? 0 : output[s];
	}

	@Override
	public byte getRSDirection(EnumFacing s) {
		return 2;
	}

	@Override
	public void initContainer(DataContainer container) {
		TileContainer cont = (TileContainer)container;
		for (int i = 0; i < 6; i++)
			cont.addItemSlot(new SlotItemHandler(inventory, i, 44, 16 + 18 * i));
		cont.addPlayerInventory(8, 140);
	}

	@Override
	public int[] getSyncVariables() {
		int[] arr = new int[13];
		for (int i = 0; i < 12; i++) arr[i] = Float.floatToIntBits(transf[i]);
		arr[12] = tickInt;
		return arr;
	}

	@Override
	public void setSyncVariable(int i, int v) {
		if (i < 12) transf[i] = Float.intBitsToFloat(v);
		else if (i == 12) tickInt = v;
	}

}
