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
import cd4017be.api.circuits.ILinkedInventory;
import cd4017be.api.circuits.IRedstone8bit;
import cd4017be.lib.TileContainer;
import cd4017be.lib.TileEntityData;
import cd4017be.lib.templates.AutomatedTile;
import cd4017be.lib.templates.Inventory;
import cd4017be.lib.templates.SlotHolo;
import cd4017be.lib.util.Utils;
import cd4017be.lib.util.Utils.ItemType;

public class InvReader extends AutomatedTile implements IRedstone8bit, ISidedInventory 
{
	private byte input;
	private byte output;
	private boolean update1;
	
	public InvReader() 
	{
		inventory = new Inventory(this, 8);
		netData = new TileEntityData(3, 8, 0, 0);
	}
	
	@Override
	public void update() 
	{
		if (worldObj.isRemote) return;
		if (update1) this.getIn();
		this.update1();
		if (update1) this.output(0);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) 
	{
		super.readFromNBT(nbt);
		input = nbt.getByte("in");
		output = nbt.getByte("out");
		update1 = true;
		int[] data = nbt.getIntArray("ref");
		System.arraycopy(data, 0, netData.ints, 0, Math.min(data.length, netData.ints.length));
		netData.longs[1] = nbt.getLong("cfg1");
		netData.longs[2] = nbt.getLong("cfg2");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) 
	{
		nbt.setByte("in", input);
		nbt.setByte("out", output);
		nbt.setIntArray("ref", netData.ints);
		nbt.setLong("cfg1", netData.longs[1]);
		nbt.setLong("cfg2", netData.longs[2]);
        return super.writeToNBT(nbt);
	}

	@Override
	protected void customPlayerCommand(byte cmd, PacketBuffer dis, EntityPlayerMP player) throws IOException 
	{
		if (cmd < 8) netData.ints[cmd] = dis.readInt();
		else if (cmd < 16) {
			byte s = dis.readByte();
			if (s == 0 || s == 1) {
				long p = (s == 0 ? cmd - 8 : cmd) * 3;
				long x = ((netData.longs[1] >> p & 0x7L) + 1L) % 6L;
				netData.longs[1] = netData.longs[1] & ~(0x7L << p) | x << p;
			} else if (s == 2) {
				long p = (cmd - 8) * 6;
				long x = ((netData.longs[2] >> p & 0x3L) + 1L) % 4L;
				netData.longs[2] = netData.longs[2] & ~(0x3L << p) | x << p;
			} else if (s > 2 && s < 7) {
				netData.longs[2] ^= 1L << ((long)(cmd - 8) * 6L + (long)s - 1L);
			}
		} else if (cmd == 16) {
			byte s = dis.readByte();
			if (s < 0 || s >= 6) return;
			long p = 48 + s * 2;
			long x = ((netData.longs[1] >> p & 0x3L) + 1L) % 3L;
			netData.longs[1] = netData.longs[1] & ~(0x3L << p) | x << p;
			worldObj.notifyNeighborsOfStateChange(pos, this.getBlockType());
		}
	}

	@Override
	public void initContainer(TileContainer container) 
	{
		container.addPlayerInventory(8, 104);
		
		for (int i = 0; i < 2; i++)
			for (int j = 0; j < 4; j++)
				container.addEntitySlot(new SlotHolo(this, i * 4 + j, 26 + i * 99, 16 + j * 18, false, false));
	}

	private void getIn()
    {
        byte d8;
        byte lstate = (byte)(input | output);
        input = 0;
        for (int i = 0; i < 6; i++) {
            d8 = this.getDirection(i);
            if (d8 > -1) continue;
            TileEntity te = Utils.getTileOnSide(this, (byte)i);
            if (d8 < 0 && te != null && te instanceof IRedstone8bit && ((IRedstone8bit)te).getDirection(i^1) > 0)
                input |= ((IRedstone8bit)te).getValue(i^1);
        }
        update1 = (input | output) != lstate;
    }
	
	private void update1()
	{
		byte lstate = (byte)(output | input);
		output = 0;
		byte mode;
		byte comp;
		boolean neg;
		TileEntity te;
		IInventory inv;
		ItemType filter;
		ItemStack item;
		int[] slots;
		int x;
		for (int i = 0; i < 8; i++) {
			mode = this.getMode(i);
			comp = (byte)(mode & 0x3);
			if (comp == 0) continue;
			neg = (mode & 0x4) != 0;
			te = Utils.getTileOnSide(this, this.getDir(i));
			if (te == null || !(te instanceof IInventory)) continue;
			if (te instanceof ILinkedInventory) {
				inv = ((ILinkedInventory)te).getLinkInv();
				if (inv == null) continue;
			} else inv = (IInventory)te;
			slots = Utils.accessibleSlots(inv, this.getDir(i | 8));
			x = 0;
			if (!neg && inventory.items[i] == null) {
				for (int s : slots) 
					if (inv.getStackInSlot(s) == null) x++;
			} else {
				filter = new ItemType((mode & 0x8) != 0, (mode & 0x10) != 0, (mode & 0x20) != 0, inventory.items[i]);
				for (int s : slots) {
					item = inv.getStackInSlot(s);
					if ((neg ^ filter.matches(item)) && item != null) x += item.stackSize;
				}
			}
			if ((comp == 1 && x == netData.ints[i]) || (comp == 2 && x < netData.ints[i]) || (comp == 3 && x > netData.ints[i]))
				output |= 1 << i;
		}
		update1 |= (output | input) != lstate;
	}
	
	private void output(int recursion)
	{
		recursion++;
		byte d8;
		byte state = (byte)(input | output);
        for (int i = 0; i < 6; i++) {
            d8 = this.getDirection(i);
            if (d8 < 1) continue;
            TileEntity te = Utils.getTileOnSide(this, (byte)i);
            if (d8 > 0 && te != null && te instanceof IRedstone8bit && ((IRedstone8bit)te).getDirection(i^1) < 0)
                ((IRedstone8bit)te).setValue(i^1, state, recursion);
        }
        update1 = false;
	}

	public byte getDir(int b)
	{
		return (byte)(netData.longs[1] >> (long)(b * 3) & 0x7L);
	}
	
	public byte getMode(int b)
	{
		return (byte)(netData.longs[2] >> (long)(b * 6) & 0x3fL);
	}
	
	@Override
    public void onNeighborBlockChange(Block b) 
    {
        update1 = true;
    }
	
	@Override
	public byte getValue(int s)
	{
		return (byte)(input | output);
	}

	@Override
	public byte getDirection(int s) 
	{
		byte k = (byte)(netData.longs[1] >> (long)(48 + s * 2) & 0x3L);
		return k > 1 ? -1 : k;
	}

	@Override
    public void setValue(int s, byte v, int recursion) 
    {
        if (recursion < 16) {
        	this.getIn();
        	if (update1) output(recursion);
        } else update1 = true;
    }

	@Override
	public ItemStack removeStackFromSlot(int i) 
	{
		return null;
	}
	
}
