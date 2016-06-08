package cd4017be.circuits.tileEntity;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ITickable;
import cd4017be.api.circuits.ILinkedInventory;
import cd4017be.lib.ModTileEntity;
import cd4017be.lib.templates.IPipe;
import cd4017be.lib.util.Utils;

public class InvConnector extends ModTileEntity implements ILinkedInventory, IPipe, ITickable
{
	private boolean linkUpdate = true;
	private int[] linkPos = new int[]{0, -1, 0};
	private TileEntity linkObj;
	private byte conDir;
	private Cover cover;
	
	public InvConnector() 
	{
	}

	@Override
	public void onNeighborBlockChange(Block b) 
	{
		linkUpdate = true;
	}

	@Override
	public void onNeighborTileChange(BlockPos pos) 
	{
		linkUpdate = true;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) 
	{
		super.readFromNBT(nbt);
		conDir = nbt.getByte("dir");
		cover = Cover.read(nbt, "cover");
		linkUpdate = true;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) 
	{
		nbt.setByte("dir", conDir);
		if (cover != null) cover.write(nbt, "cover");
        return super.writeToNBT(nbt);
	}

	@Override
	public void update() 
	{
		if (worldObj.isRemote || !linkUpdate) return;
		TileEntity last = linkObj;
		TileEntity te = Utils.getTileOnSide(this, conDir);
		if (te == null || !(te instanceof IInventory)) {
			linkPos[1] = -1;
			linkObj = null;
		} else if (te instanceof ILinkedInventory) {
			if ((((ILinkedInventory)te).getLinkDir() ^ conDir) == 1) {
				linkObj = null;
				linkPos[1] = -1;
			} else {
				linkPos = ((ILinkedInventory)te).getLinkPos();
				linkObj = worldObj.getTileEntity(new BlockPos(linkPos[0], linkPos[1], linkPos[2]));
				if (linkObj == null) linkPos[1] = -1;
			}
			if (linkObj != null && (linkObj instanceof ILinkedInventory || !(linkObj instanceof IInventory))) {
				linkObj = null;
				linkPos[1] = -1;
			}
		} else {
			linkObj = te;
			linkPos = new int[]{te.getPos().getX(), te.getPos().getY(), te.getPos().getZ()};
		}
		if (linkObj != last) {
			worldObj.notifyNeighborsOfStateChange(pos, getBlockType());
			this.markUpdate();
		}
		linkUpdate = false;
	}
	
	@Override
    public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing s, float X, float Y, float Z) 
    {
        if (player.isSneaking() && item == null) {
            if (worldObj.isRemote) return true;
            if (cover != null) {
                this.dropStack(cover.item);
                cover = null;
                this.markUpdate();
                return true;
            }
            IInventory inv = this.getLinkInv();
            if (inv == null) player.addChatMessage(new TextComponentString("Not Linked!"));
            else player.addChatMessage(new TextComponentString(String.format("Linked to %s at %d, %d, %d", inv.getName(), linkPos[0], linkPos[1], linkPos[2])));
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
	public void onPlaced(EntityLivingBase entity, ItemStack item) 
	{
		this.connect();
	}

	private void connect()
	{
		byte d;
		for (int i = 1; i <= 6; i++) {
			d = (byte)((conDir + i) % 6);
			TileEntity te = Utils.getTileOnSide(this, d);
			if (te != null && te instanceof IInventory) {
				conDir = d;
				this.markUpdate();
				linkUpdate = true;
				return;
			}
		}
	}

	@Override
	public int getSizeInventory() 
	{
		IInventory inv = this.getLinkInv();
		return inv == null ? 0 : inv.getSizeInventory();
	}

	@Override
	public ItemStack getStackInSlot(int i) 
	{
		IInventory inv = this.getLinkInv();
		return inv == null ? null : inv.getStackInSlot(i);
	}

	@Override
	public ItemStack decrStackSize(int i, int n) 
	{
		IInventory inv = this.getLinkInv();
		return inv == null ? null : inv.decrStackSize(i, n);
	}

	@Override
	public ItemStack removeStackFromSlot(int i) 
	{
		return null;
	}

	@Override
	public void setInventorySlotContents(int i, ItemStack stack) 
	{
		IInventory inv = this.getLinkInv();
		if (inv != null) inv.setInventorySlotContents(i, stack);
	}

	@Override
	public String getName() 
	{
		IInventory inv = this.getLinkInv();
		return inv == null ? "No Connection" : inv.getName();
	}

	@Override
	public boolean hasCustomName() 
	{
		IInventory inv = this.getLinkInv();
		return inv == null ? true : inv.hasCustomName();
	}

	@Override
	public int getInventoryStackLimit() 
	{
		IInventory inv = this.getLinkInv();
		return inv == null ? 0 : inv.getInventoryStackLimit();
	}

	@Override
	public void openInventory(EntityPlayer player) {}

	@Override
	public void closeInventory(EntityPlayer player) {}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack stack) 
	{
		IInventory inv = this.getLinkInv();
		return inv == null ? false : inv.isItemValidForSlot(i, stack);
	}

	@Override
	public int textureForSide(byte s) 
	{
		if (s == conDir) return linkPos[1] >= 0 ? 2 : 1;
		TileEntity te = Utils.getTileOnSide(this, s);
		return te != null && te instanceof IInventory ? 0 : -1;
	}

	@Override
	public Cover getCover() 
	{
		return cover;
	}
	
	@Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) 
    {
        conDir = pkt.getNbtCompound().getByte("dir");
        linkPos[1] = pkt.getNbtCompound().getBoolean("link") ? 0 : -1;
        cover = Cover.read(pkt.getNbtCompound(), "cover");
        this.markUpdate();
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket()
    {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setShort("dir", conDir);
        nbt.setBoolean("link", linkPos[1] >= 0);
        if (cover != null) cover.write(nbt, "cover");
        return new SPacketUpdateTileEntity(pos, -1, nbt);
    }
    
    @Override
    public void breakBlock() 
    {
        super.breakBlock();
        if (cover != null) {
            EntityItem entity = new EntityItem(worldObj, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, cover.item);
            cover = null;
            worldObj.spawnEntityInWorld(entity);
        }
    }

	@Override
	public int[] getSlotsForFace(EnumFacing s) 
	{
		IInventory inv = this.getLinkInv();
		if (inv == null) return new int[0];
		if (inv instanceof ISidedInventory) return ((ISidedInventory)inv).getSlotsForFace(s);
		int[] ret = new int[inv.getSizeInventory()];
		for (int i = 0; i < ret.length; i++) ret[i] = i;
		return ret;
	}

	@Override
	public boolean canInsertItem(int i, ItemStack stack, EnumFacing s) 
	{
		if (s.getIndex() == conDir) return false;
		IInventory inv = this.getLinkInv();
		return inv != null && (!(inv instanceof ISidedInventory) || ((ISidedInventory)inv).canInsertItem(i, stack, s));
	}

	@Override
	public boolean canExtractItem(int i, ItemStack stack, EnumFacing s) 
	{
		if (s.getIndex() == conDir) return false;
		IInventory inv = this.getLinkInv();
		return inv != null && (!(inv instanceof ISidedInventory) || ((ISidedInventory)inv).canExtractItem(i, stack, s));
	}

	@Override
	public int[] getLinkPos() 
	{
		return linkPos;
	}

	@Override
	public IInventory getLinkInv() 
	{
		if (linkObj == null) return null;
		else if (linkObj.isInvalid()) {
			linkUpdate = true;
			return null;
		} else return (IInventory)linkObj;
	}

	@Override
	public byte getLinkDir() 
	{
		return conDir;
	}

	@Override
	public int getField(int id) {
		return 0;
	}

	@Override
	public void setField(int id, int value) {}

	@Override
	public int getFieldCount() {
		return 0;
	}

	@Override
	public void clear() {}

	@Override
	public ITextComponent getDisplayName() {
		IInventory inv = this.getLinkInv();
		return inv == null ? new TextComponentString("No Connection") : inv.getDisplayName();
	}

}
