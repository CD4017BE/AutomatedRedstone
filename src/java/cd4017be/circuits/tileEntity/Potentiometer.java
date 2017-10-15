package cd4017be.circuits.tileEntity;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.BlockGuiHandler.ClientPacketReceiver;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.util.Orientation;
import cd4017be.lib.block.AdvancedBlock.IInteractiveTile;
import cd4017be.lib.block.AdvancedBlock.IRedstoneTile;
import cd4017be.lib.tileentity.BaseTileEntity;

public class Potentiometer extends BaseTileEntity implements IInteractiveTile, IRedstoneTile, IDirectionalRedstone, IGuiData, ClientPacketReceiver {

	public int min = 0, max = 15, cur = 0;

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing s, float X, float Y, float Z) {
		Orientation o = getOrientation();
		if (s != o.front) {
			BlockGuiHandler.openBlockGui(player, world, pos);
			return true;
		}
		if (world.isRemote) return true;
		Vec3d vec = o.reverse().rotate(new Vec3d(X - 0.5, Y - 0.5, Z - 0.5));
		boolean abs = vec.xCoord > 0;
		int state;
		if (abs) {
			Y = (float)vec.yCoord + 0.5F - 3.5F/32F; Y *= 32F/25F; //normalize to [0,1]
			state = Math.round((float)min + ((float)max - (float)min) * Y); //set to interpolated absolute
		} else {
			Y = (float)vec.yCoord; Y *= 2F; //normalize to [-1,1]
			state = cur + (Y > 0 ? (int)Math.pow((double)max - (double)min, Y) : -(int)Math.pow((double)max - (double)min, -Y)); //offset by logarithmic scaled
		}
		if (state < min) state = min;
		else if (state > max) state = max;
		if (state != cur) {
			cur = state;
			world.notifyNeighborsOfStateChange(pos, Blocks.REDSTONE_TORCH, false);
			markUpdate();
		}
		return true;
	}

	@Override
	public void onClicked(EntityPlayer player) {		
	}

	@Override
	public int redstoneLevel(EnumFacing side, boolean strong) {
		return strong ? 0 : cur;
	}

	@Override
	public boolean connectRedstone(EnumFacing side) {
		return true;
	}

	@Override
	public void onPacketFromClient(PacketBuffer data, EntityPlayer sender) throws IOException {
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
			world.notifyNeighborsOfStateChange(pos, Blocks.REDSTONE_TORCH, false);
		} else if (cur < min) {
			cur = min;
			world.notifyNeighborsOfStateChange(pos, Blocks.REDSTONE_TORCH, false);
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

	@Override
	public void initContainer(DataContainer container) {
	}

	@Override
	public int[] getSyncVariables() {
		return null;
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
