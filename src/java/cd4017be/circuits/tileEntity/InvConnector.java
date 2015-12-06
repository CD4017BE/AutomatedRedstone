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
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import cd4017be.api.circuits.ILinkedInventory;
import cd4017be.lib.ModTileEntity;
import cd4017be.lib.templates.IPipe;
import cd4017be.lib.util.Utils;

public class InvConnector extends ModTileEntity implements ILinkedInventory, IPipe 
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
	public void onNeighborTileChange(int tx, int ty, int tz) 
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
	public void writeToNBT(NBTTagCompound nbt) 
	{
		super.writeToNBT(nbt);
		nbt.setByte("dir", conDir);
		if (cover != null) cover.write(nbt, "cover");
	}

	@Override
	public void updateEntity() 
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
				linkObj = worldObj.getTileEntity(linkPos[0], linkPos[1], linkPos[2]);
				if (linkObj == null) linkPos[1] = -1;
			}
			if (linkObj != null && (linkObj instanceof ILinkedInventory || !(linkObj instanceof IInventory))) {
				linkObj = null;
				linkPos[1] = -1;
			}
		} else {
			linkObj = te;
			linkPos = new int[]{te.xCoord, te.yCoord, te.zCoord};
		}
		if (linkObj != last) {
			worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, getBlockType());
			worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
		}
		linkUpdate = false;
	}
	
	@Override
    public boolean onActivated(EntityPlayer player, int s, float X, float Y, float Z) 
    {
        ItemStack item = player.getCurrentEquippedItem();
        if (player.isSneaking() && item == null) {
            if (worldObj.isRemote) return true;
            if (cover != null) {
                player.setCurrentItemOrArmor(0, cover.item);
                cover = null;
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                return true;
            }
            IInventory inv = this.getLinkInv();
            if (inv == null) player.addChatMessage(new ChatComponentText("Not Linked!"));
            else player.addChatMessage(new ChatComponentText(String.format("Linked to %s at %d, %d, %d", inv.getInventoryName(), linkPos[0], linkPos[1], linkPos[2])));
            return true;
        } else if (item == null) {
        	if (!worldObj.isRemote) this.connect();
        	return true;
        } else if (!player.isSneaking() && cover == null && item != null && (cover = Cover.create(item)) != null) {
            if (worldObj.isRemote) return true;
            item.stackSize--;
            if (item.stackSize <= 0) item = null;
            player.setCurrentItemOrArmor(0, item);
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
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
				worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
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
	public ItemStack getStackInSlotOnClosing(int i) 
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
	public String getInventoryName() 
	{
		IInventory inv = this.getLinkInv();
		return inv == null ? "No Connection" : inv.getInventoryName();
	}

	@Override
	public boolean hasCustomInventoryName() 
	{
		IInventory inv = this.getLinkInv();
		return inv == null ? true : inv.hasCustomInventoryName();
	}

	@Override
	public int getInventoryStackLimit() 
	{
		IInventory inv = this.getLinkInv();
		return inv == null ? 0 : inv.getInventoryStackLimit();
	}

	@Override
	public void openInventory() {}

	@Override
	public void closeInventory() {}

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
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) 
    {
        conDir = pkt.func_148857_g().getByte("dir");
        linkPos[1] = pkt.func_148857_g().getBoolean("link") ? 0 : -1;
        cover = Cover.read(pkt.func_148857_g(), "cover");
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public Packet getDescriptionPacket() 
    {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setShort("dir", conDir);
        nbt.setBoolean("link", linkPos[1] >= 0);
        if (cover != null) cover.write(nbt, "cover");
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, -1, nbt);
    }
    
    @Override
    public void breakBlock() 
    {
        super.breakBlock();
        if (cover != null) {
            EntityItem entity = new EntityItem(worldObj, xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, cover.item);
            cover = null;
            worldObj.spawnEntityInWorld(entity);
        }
    }

	@Override
	public int[] getAccessibleSlotsFromSide(int s) 
	{
		IInventory inv = this.getLinkInv();
		if (inv == null) return new int[0];
		if (inv instanceof ISidedInventory) return ((ISidedInventory)inv).getAccessibleSlotsFromSide(s);
		int[] ret = new int[inv.getSizeInventory()];
		for (int i = 0; i < ret.length; i++) ret[i] = i;
		return ret;
	}

	@Override
	public boolean canInsertItem(int i, ItemStack stack, int s) 
	{
		if (s == conDir) return false;
		IInventory inv = this.getLinkInv();
		return inv != null && (!(inv instanceof ISidedInventory) || ((ISidedInventory)inv).canInsertItem(i, stack, s));
	}

	@Override
	public boolean canExtractItem(int i, ItemStack stack, int s) 
	{
		if (s == conDir) return false;
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

}
