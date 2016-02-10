package cd4017be.circuits.tileEntity;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import cd4017be.api.circuits.ILinkedInventory;
import cd4017be.api.circuits.IRedstone8bit;
import cd4017be.lib.TileContainer;
import cd4017be.lib.TileEntityData;
import cd4017be.lib.templates.AutomatedTile;
import cd4017be.lib.templates.Inventory;
import cd4017be.lib.templates.SlotHolo;
import cd4017be.lib.util.Utils;
import cd4017be.lib.util.Utils.ItemType;

public class ItemTranslocator extends AutomatedTile implements ILinkedInventory, IRedstone8bit 
{
	private int[] linkPos = {0, -1, 0};
	private TileEntity linkObj;
	private byte state;
	private boolean update1 = true;
	private boolean linkUpdate = true;
	private short counter = 0;
	
	public ItemTranslocator() 
	{
		inventory = new Inventory(this, 8);
		/* long0 = inv cfg
		 * long1 = 0-23,24-47: sides cfg in
		 * long2 = 0-23,24-47: sides cfg out
		 * long3 = 0-31: modes; 32-43: rsIO; 44-46: redirect; 47:on/off
		 */
		netData = new TileEntityData(4, 9, 0, 0);
		netData.ints[8] = 1;
	}
	
	@Override
	public void update() 
	{
		if (worldObj.isRemote) return;
		if (update1) this.output(0);
		if (linkUpdate) this.link();
		counter++;
		if ((netData.longs[3] >> 47L & 1L) == 0) counter = 0;
		if (counter >= netData.ints[8]) {
			counter = 0;
			this.update1();
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) 
	{
		super.readFromNBT(nbt);
		state = nbt.getByte("in");
		update1 = true;
		int[] data = nbt.getIntArray("ref");
		System.arraycopy(data, 0, netData.ints, 0, Math.min(data.length, netData.ints.length));
		netData.longs[1] = nbt.getLong("cfg1");
		netData.longs[2] = nbt.getLong("cfg2");
		netData.longs[3] = nbt.getLong("cfg3");
		counter = nbt.getShort("count");
		linkUpdate = true;
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) 
	{
		super.writeToNBT(nbt);
		nbt.setByte("in", state);
		nbt.setIntArray("ref", netData.ints);
		nbt.setLong("cfg1", netData.longs[1]);
		nbt.setLong("cfg2", netData.longs[2]);
		nbt.setLong("cfg3", netData.longs[3]);
		nbt.setShort("count", counter);
	}

	@Override
	protected void customPlayerCommand(byte cmd, PacketBuffer dis, EntityPlayerMP player) throws IOException 
	{
		if (cmd < 8) netData.ints[cmd] = dis.readInt();
		else if (cmd < 16) {
			byte s = dis.readByte();
			if (s >= 0 && s < 4) {
				long p = ((s & 1) == 0 ? cmd - 8 : cmd) * 3;
				int v = s < 2 ? 1 : 2;
				long x = ((netData.longs[v] >> p & 0x7L) + 1L) % 6L;
				netData.longs[v] = netData.longs[v] & ~(0x7L << p) | x << p;
			} else if (s >= 4 && s < 8) {
				netData.longs[3] ^= 1L << ((long)(cmd - 8) * 4L + (long)s - 4L);
			}
		} else if (cmd == 16) {
			byte s = dis.readByte();
			if (s < 0 || s >= 6) return;
			long p = 32 + s * 2;
			long x = ((netData.longs[3] >> p & 0x3L) + 1L) % 3L;
			netData.longs[3] = netData.longs[3] & ~(0x3L << p) | x << p;
			worldObj.notifyNeighborsOfStateChange(pos, this.getBlockType());
		} else if (cmd == 17) {
			netData.ints[8] = dis.readInt();
			 if (netData.ints[8] < 1) netData.ints[8] = 1;
	         else if (netData.ints[8] > 1200) netData.ints[8] = 1200;
		} else if (cmd == 18) {
			netData.longs[3] ^= 1L << 47L;
		} else if (cmd == 19) {
			long p = 44;
			long x = ((netData.longs[3] >> p & 0x7L) + 1L) % 6L;
			netData.longs[3] = netData.longs[3] & ~(0x7L << p) | x << p;
			linkUpdate = true;
		}
	}

	@Override
	public void initContainer(TileContainer container) 
	{
		container.addPlayerInventory(8, 122);
		
		for (int i = 0; i < 2; i++)
			for (int j = 0; j < 4; j++)
				container.addEntitySlot(new SlotHolo(this, i * 4 + j, 26 + i * 99, 16 + j * 18, false, false));
	}
	
	private void link()
	{
		TileEntity last = linkObj;
		byte s = (byte)(netData.longs[3] >> 44L & 0x7L);
		TileEntity te = Utils.getTileOnSide(this, s);
		if (te == null || !(te instanceof IInventory)) {
			linkPos[1] = -1;
			linkObj = null;
		} else if (te instanceof ILinkedInventory) {
			if ((((ILinkedInventory)te).getLinkDir() ^ s) == 1) {
				linkObj = null;
				linkPos[1] = -1;
			} else {
				linkPos = ((ILinkedInventory)te).getLinkPos();
				linkObj = worldObj.getTileEntity(new BlockPos(linkPos[0], linkPos[1], linkPos[2]));
			}
			if (linkObj != null && (linkObj instanceof ILinkedInventory || !(linkObj instanceof IInventory))) {
				linkObj = null;
				linkPos[1] = -1;
			}
		} else {
			linkObj = te;
			linkPos = new int[]{te.getPos().getX(), te.getPos().getY(), te.getPos().getZ()};
		}
		if (linkObj != last) worldObj.notifyNeighborsOfStateChange(pos, getBlockType());
		linkUpdate = false;
	}
	
	private void update1()
	{
		byte mode;
		boolean neg;
		TileEntity te;
		IInventory inv0, inv1;
		ItemType filter;
		int[] slots0, slots1;
		byte side0, side1;
		for (int i = 0; i < 8; i++) {
			if (netData.ints[i] <= 0 || (state >> i & 1) == 0) continue;
			mode = this.getMode(i);
			neg = (mode & 0x1) != 0;
			if (!neg && inventory.items[i] == null) continue;
			te = Utils.getTileOnSide(this, this.getDir(i));
			if (te == null || !(te instanceof IInventory)) continue;
			if (te instanceof ILinkedInventory) {
				inv0 = ((ILinkedInventory)te).getLinkInv();
				if (inv0 == null) continue;
			} else inv0 = (IInventory)te;
			te = Utils.getTileOnSide(this, this.getDir(i | 16));
			if (te == null || !(te instanceof IInventory)) continue;
			if (te instanceof ILinkedInventory) {
				inv1 = ((ILinkedInventory)te).getLinkInv();
				if (inv1 == null) continue;
			} else inv1 = (IInventory)te;
			side0 = this.getDir(i | 8);
			side1 = this.getDir(i | 24);
			slots0 = Utils.accessibleSlots(inv0, side0);
			slots1 = Utils.accessibleSlots(inv1, side1);
			if (slots0.length == 0 || slots1.length == 0) continue;
			filter = new ItemType((mode & 0x2) != 0, (mode & 0x4) != 0, (mode & 0x8) != 0, inventory.items[i]);
			this.transfer(inv0, side0, slots0, inv1, side1, slots1, filter, neg, netData.ints[i]);
		}
	}
	
	private void transfer(IInventory src, int sideS, int[] sS, IInventory dst, int sideD, int[] sD, ItemType type, boolean neg, int am) 
	{
        ISidedInventory srcS = src instanceof ISidedInventory ? (ISidedInventory) src : null;
        ISidedInventory dstS = dst instanceof ISidedInventory ? (ISidedInventory) dst : null;
        int am0 = am;
        for (int i : sS) {
            ItemStack curItem = src.getStackInSlot(i);
            if (curItem != null && (type.matches(curItem) ^ neg) && (srcS == null || srcS.canExtractItem(i, curItem, EnumFacing.VALUES[sideS]))) {
                int m = Math.min(curItem.getMaxStackSize(), dst.getInventoryStackLimit());
                int p = -1;
                for (int j : sD) {
                    ItemStack stack = dst.getStackInSlot(j);
                    if (stack == null && p == -1 && (dstS == null || dstS.canInsertItem(j, curItem, EnumFacing.VALUES[sideD]))) p = j;
                    else if (Utils.itemsEqual(curItem, stack) && stack.stackSize < m) {
                    	int n = Math.min(m - stack.stackSize, am);
                    	ItemStack item = src.decrStackSize(i, n);
                    	if (item != null) {
                    		stack.stackSize += item.stackSize;
                    		am -= item.stackSize;
                    	} else break;
                    	dst.setInventorySlotContents(j, stack);
                        if (am <= 0) {
                        	src.markDirty();
                    		dst.markDirty();
                        	return;
                        }
                    	curItem = src.getStackInSlot(i);
                        if (curItem == null) break;
                    }
                }
                if (p >= 0 && curItem != null) {
                	ItemStack item = src.decrStackSize(i, Math.min(m, am));
                	dst.setInventorySlotContents(p, item);
                	if (item != null && (am -= item.stackSize) <= 0) break;
                }
            }
        }
        if (am < am0) {
        	src.markDirty();
    		dst.markDirty();
        }
    }
	
	private void output(int recursion)
	{
		byte d8;
        byte lstate = state;
        state = 0;
        for (int i = 0; i < 6; i++) {
            d8 = this.getDirection(i);
            if (d8 > -1) continue;
            TileEntity te = Utils.getTileOnSide(this, (byte)i);
            if (d8 < 0 && te != null && te instanceof IRedstone8bit && ((IRedstone8bit)te).getDirection(i^1) > 0)
                state |= ((IRedstone8bit)te).getValue(i^1);
        }
        if (state != lstate) {
			recursion++;
	        for (int i = 0; i < 6; i++) {
	            d8 = this.getDirection(i);
	            if (d8 < 1) continue;
	            TileEntity te = Utils.getTileOnSide(this, (byte)i);
	            if (d8 > 0 && te != null && te instanceof IRedstone8bit && ((IRedstone8bit)te).getDirection(i^1) < 0)
	                ((IRedstone8bit)te).setValue(i^1, state, recursion);
	        }
        }
        update1 = false;
	}

	public byte getDir(int b)
	{
		return (byte)(netData.longs[b >= 16 ? 2 : 1] >> (long)((b % 16) * 3) & 0x7L);
	}
	
	public byte getMode(int b)
	{
		return (byte)(netData.longs[3] >> (long)(b * 4) & 0xfL);
	}
	
	@Override
    public void onNeighborBlockChange(Block b) 
    {
        update1 = true;
        linkUpdate = true;
    }
	
	@Override
	public byte getValue(int s)
	{
		return state;
	}

	@Override
	public byte getDirection(int s) 
	{
		byte k = (byte)(netData.longs[3] >> (long)(32 + s * 2) & 0x3L);
		return k > 1 ? -1 : k;
	}

	@Override
    public void setValue(int s, byte v, int recursion) 
    {
        if (recursion < 16) output(recursion);
        else update1 = true;
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
		return (byte)(netData.longs[3] >> 44L & 0x7L);
	}

	@Override
	public ItemStack removeStackFromSlot(int i) 
	{
		return null;
	}

}
