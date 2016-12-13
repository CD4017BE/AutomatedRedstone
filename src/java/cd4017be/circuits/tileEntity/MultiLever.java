package cd4017be.circuits.tileEntity;

import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.lib.ModTileEntity;
import cd4017be.lib.TooltipInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

/**
 *
 * @author CD4017BE
 */
public class MultiLever extends ModTileEntity implements IDirectionalRedstone {

	public byte offset;
	public byte state;

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing s, float X, float Y, float Z) {
		if (worldObj.isRemote) return true;
		else if (player.isSneaking()) {
			offset++;
			offset &= 3;
			worldObj.notifyNeighborsOfStateChange(pos, Blocks.REDSTONE_TORCH);
			player.addChatMessage(new TextComponentString(TooltipInfo.format("tile.cd4017be.lever8bit.click" + offset)));
			return true;
		} else if (s.getIndex() == this.getOrientation()) {
			int i = Y < 0.5F ? 4 : 0;
			if (s == EnumFacing.SOUTH) i |= (int)Math.floor(X * 4F);
			else if (s == EnumFacing.NORTH) i |= (int)Math.floor((1F - X) * 4F);
			else if (s == EnumFacing.WEST) i |= (int)Math.floor(Z * 4F);
			else if (s == EnumFacing.EAST) i |= (int)Math.floor((1F - Z) * 4F);
			state ^= 1 << i;
			worldObj.notifyNeighborsOfStateChange(pos, Blocks.REDSTONE_TORCH);
			this.markUpdate();
			return true;
		} else return false;
	}

	@Override
	public int redstoneLevel(int s, boolean str) {
		return str ? 0 : (state & 0xff) << (offset * 8);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		state = pkt.getNbtCompound().getByte("state");
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setByte("state", state);
		return new SPacketUpdateTileEntity(pos, -1, nbt);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setByte("state", state);
		nbt.setByte("offs", offset);
		return super.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		state = nbt.getByte("state");
		offset = nbt.getByte("offs");
	}

	@Override
	public byte getRSDirection(EnumFacing s) {
		return 2;
	}

}
