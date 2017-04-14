package cd4017be.circuits.tileEntity;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.lib.ModTileEntity;
import cd4017be.lib.Gui.DataContainer.IGuiData;

public class Potentiometer extends ModTileEntity implements IDirectionalRedstone, IGuiData {

	public int min = 0, max = 15, cur = 0;

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing s, float X, float Y, float Z) {
		if (s.ordinal() != getOrientation()) return super.onActivated(player, hand, item, s, X, Y, Z);
		if (worldObj.isRemote) return true;
		boolean abs;
		switch(s) {
		case NORTH: abs = X >= 0.5F; break;
		case SOUTH: abs = X < 0.5F; break;
		case WEST: abs = Z < 0.5F; break;
		case EAST: abs = Z >= 0.5F; break;
		default: return false;
		}
		int state;
		if (abs) {
			Y -= 3.5F/32F; Y *= 32F/25F; //normalize to [0,1]
			state = Math.round((float)min + ((float)max - (float)min) * Y); //set to interpolated absolute
		} else {
			Y -= 0.5F; Y *= 2F; //normalize to [-1,1]
			state = cur + (Y > 0 ? (int)Math.pow((double)max - (double)min, Y) : -(int)Math.pow((double)max - (double)min, -Y)); //offset by logarithmic scaled
		}
		if (state < min) state = min;
		else if (state > max) state = max;
		if (state != cur) {
			cur = state;
			worldObj.notifyNeighborsOfStateChange(pos, Blocks.REDSTONE_TORCH);
			markUpdate();
		}
		return true;
	}

	@Override
	public int redstoneLevel(int s, boolean str) {
		return str ? 0 : cur;
	}

	@Override
	public void onPlayerCommand(PacketBuffer data, EntityPlayerMP player) throws IOException {
		byte cmd = data.readByte();
		switch(cmd) {
		case 0: min = data.readInt(); break;
		case 1: max = data.readInt(); break;
		}
		if (min > max) {
			int i = min;
			min = max;
			max = i;
		}
		if (cur > max) {
			cur = max;
			worldObj.notifyNeighborsOfStateChange(pos, Blocks.REDSTONE_TORCH);
		} else if (cur < min) {
			cur = min;
			worldObj.notifyNeighborsOfStateChange(pos, Blocks.REDSTONE_TORCH);
		}
		markUpdate();
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		min = nbt.getInteger("min");
		max = nbt.getInteger("max");
		cur = nbt.getInteger("cur");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setInteger("min", min);
		nbt.setInteger("max", max);
		nbt.setInteger("cur", cur);
		return super.writeToNBT(nbt);
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setInteger("min", min);
		nbt.setInteger("max", max);
		nbt.setInteger("cur", cur);
		return new SPacketUpdateTileEntity(pos, -1, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		NBTTagCompound nbt = pkt.getNbtCompound();
		min = nbt.getInteger("min");
		max = nbt.getInteger("max");
		cur = nbt.getInteger("cur");
	}

	@Override
	public byte getRSDirection(EnumFacing s) {
		return 2;
	}

}
