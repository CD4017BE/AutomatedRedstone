package cd4017be.circuits.tileEntity;

import java.io.IOException;
import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.items.SlotItemHandler;
import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.api.circuits.ISensor;
import cd4017be.lib.BlockGuiHandler.ClientPacketReceiver;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.block.AdvancedBlock.IRedstoneTile;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.block.BaseTileEntity;
import cd4017be.lib.templates.LinkedInventory;
import cd4017be.lib.util.Utils;

public class BlockSensor extends BaseTileEntity implements ITilePlaceHarvest, IRedstoneTile, ITickable, IDirectionalRedstone, IGuiData, ClientPacketReceiver {

	public int tickInt = 20;
	private ItemStack sensor = ItemStack.EMPTY;
	public float mult = 1, ofs = 0;
	private int output;

	@Override
	public void update() {
		if (world.isRemote || world.getTotalWorldTime() % tickInt != 0) return;
		int nstate;
		if (sensor.getItem() instanceof ISensor) 
			nstate = (int)Math.floor(((ISensor)sensor.getItem()).measure(sensor, world, pos) * (double)mult + (double)ofs);
		else nstate = 0;
		if (output != nstate) {
			output = nstate;
			Utils.updateRedstoneOnSide(this, nstate, getOrientation().front);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		output = nbt.getInteger("out");
		mult = nbt.getFloat("sca");
		ofs = nbt.getFloat("ofs");
		tickInt = nbt.getShort("time");
		sensor = new ItemStack(nbt.getCompoundTag("item"));
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setInteger("out", output);
		nbt.setFloat("sca", mult);
		nbt.setFloat("ofs", ofs);
		nbt.setShort("time", (short)tickInt);
		nbt.setTag("item", sensor.writeToNBT(new NBTTagCompound()));
		return super.writeToNBT(nbt);
	}

	@Override
	public void onPacketFromClient(PacketBuffer data, EntityPlayer sender) throws IOException {
		byte cmd = data.readByte();
		switch(cmd) {
		case 0: mult = data.readFloat(); break;
		case 1: ofs = data.readFloat(); break;
		case 2: tickInt = data.readShort();
			if (tickInt < 1) tickInt = 1;
			else if (tickInt > 1200) tickInt = 1200;
		}
	}

	@Override
	public int redstoneLevel(EnumFacing side, boolean strong) {
		return strong || side != getOrientation().front ? 0 : output;
	}

	@Override
	public boolean connectRedstone(EnumFacing side) {
		return side == getOrientation().front;
	}

	@Override
	public byte getRSDirection(EnumFacing s) {
		return connectRedstone(s) ? (byte)2 : 0;
	}

	@Override
	public void initContainer(DataContainer container) {
		TileContainer cont = (TileContainer)container;
		cont.addItemSlot(new SlotItemHandler(new LinkedInventory(1, 1, (i) -> sensor, (item, i) -> sensor = item), 0, 44, 16));
		cont.addPlayerInventory(8, 140);
	}

	@Override
	public int[] getSyncVariables() {
		return new int[]{Float.floatToIntBits(mult), Float.floatToIntBits(ofs), tickInt};
	}

	@Override
	public void setSyncVariable(int i, int v) {
		switch(i) {
		case 0: mult = Float.intBitsToFloat(v); break;
		case 1: ofs = Float.intBitsToFloat(v); break;
		case 2: tickInt = v; break;
		}
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		List<ItemStack> list = makeDefaultDrops(null);
		if (!sensor.isEmpty()) list.add(sensor);
		return list;
	}

	@Override
	public boolean detectAndSendChanges(DataContainer container, PacketBuffer dos) {
		return false;
	}

	@Override
	public void updateClientChanges(DataContainer container, PacketBuffer dis) {
	}

}
