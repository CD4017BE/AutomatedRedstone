package cd4017be.circuits.tileEntity;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import cd4017be.api.circuits.ILinkedInventory;
import cd4017be.lib.ModTileEntity;
import cd4017be.lib.templates.IPipe;
import cd4017be.lib.util.Utils;

public class InvConnector extends ModTileEntity implements ILinkedInventory, IPipe, ITickable {

	private boolean linkUpdate = true;
	private BlockPos linkPos = Utils.NOWHERE;
	private TileEntity linkObj;
	private byte conDir;
	private Cover cover;

	@Override
	public void onNeighborBlockChange(Block b) {
		linkUpdate = true;
	}

	@Override
	public void onNeighborTileChange(BlockPos pos) {
		linkUpdate = true;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		conDir = nbt.getByte("dir");
		cover = Cover.read(nbt, "cover");
		linkUpdate = true;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setByte("dir", conDir);
		if (cover != null) cover.write(nbt, "cover");
		return super.writeToNBT(nbt);
	}

	@Override
	public void update() {
		if (worldObj.isRemote || !linkUpdate) return;
		TileEntity last = linkObj;
		TileEntity te = Utils.getTileOnSide(this, conDir);
		if (te == null) {
			linkPos = Utils.NOWHERE;
			linkObj = null;
		} else if (te instanceof ILinkedInventory) {
			if (((ILinkedInventory)te).getLinkDir() == EnumFacing.VALUES[conDir^1]) {
				linkObj = null;
				linkPos = Utils.NOWHERE;
			} else {
				linkPos = ((ILinkedInventory)te).getLinkPos();
				linkObj = worldObj.getTileEntity(linkPos);
				if (linkObj instanceof ILinkedInventory) linkObj = null;
				if (linkObj == null) linkPos = Utils.NOWHERE;
			}
		} else {
			linkObj = te;
			linkPos = te.getPos();
		}
		if (linkObj != last) {
			worldObj.notifyNeighborsOfStateChange(pos, getBlockType());
			this.markUpdate();
		}
		linkUpdate = false;
	}

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing s, float X, float Y, float Z) {
		if (player.isSneaking() && item == null) {
			if (worldObj.isRemote) return true;
			if (cover != null) {
				this.dropStack(cover.item);
				cover = null;
				this.markUpdate();
				return true;
			}
			if (linkObj == null) player.addChatMessage(new TextComponentString("Not Linked!"));
			else player.addChatMessage(new TextComponentString(String.format("Linked to %s @ %s", worldObj.getBlockState(linkPos).getBlock().getLocalizedName(), linkPos.toString())));
			return true;
		} else if (item == null) {
			if (!worldObj.isRemote) this.connect();
			return true;
		} else if (!player.isSneaking() && cover == null && item != null && (cover = Cover.create(item)) != null) {
			if (worldObj.isRemote) return true;
			item.stackSize--;
			if (item.stackSize <= 0) item = null;
			player.setHeldItem(hand, item);
			this.markUpdate();
			return true;
		} else return false;
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
		this.connect();
	}

	private void connect() {
		byte d;
		for (int i = 1; i < 6; i++) {
			d = (byte)((conDir + i) % 6);
			if (Utils.getTileOnSide(this, d) != null) {
				conDir = d;
				this.markUpdate();
				linkUpdate = true;
				return;
			}
		}
	}

	@Override
	public int textureForSide(byte s) {
		if (s == -1) return 0;
		if (s == conDir) return linkPos.getY() >= 0 ? 2 : 1;
		TileEntity te = Utils.getTileOnSide(this, s);
		return te != null && te instanceof ILinkedInventory && ((ILinkedInventory)te).getLinkDir() == EnumFacing.VALUES[s^1] ? 0 : -1;
	}

	@Override
	public Cover getCover() {
		return cover;
	}
	
	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		conDir = pkt.getNbtCompound().getByte("dir");
		linkPos = pkt.getNbtCompound().getBoolean("link") ? new BlockPos(0, 0, 0) : new BlockPos(0, -1, 0);
		cover = Cover.read(pkt.getNbtCompound(), "cover");
		this.markUpdate();
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setShort("dir", conDir);
		nbt.setBoolean("link", linkPos.getY() >= 0);
		if (cover != null) cover.write(nbt, "cover");
		return new SPacketUpdateTileEntity(pos, -1, nbt);
	}
	
	@Override
	public void breakBlock() {
		if (cover != null) {
			EntityItem entity = new EntityItem(worldObj, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, cover.item);
			cover = null;
			worldObj.spawnEntityInWorld(entity);
		}
	}

	@Override
	public BlockPos getLinkPos() {
		return linkPos;
	}

	@Override
	public EnumFacing getLinkDir() {
		return EnumFacing.VALUES[conDir];
	}
	
	@Override
	public TileEntity getLinkObj() {
		return linkObj;
	}

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing s) {
		return linkObj == null ? false : linkObj.hasCapability(cap, s);
	}

	@Override
	public <T> T getCapability(Capability<T> cap, EnumFacing s) {
		return linkObj == null ? null : linkObj.getCapability(cap, s);
	}

}
