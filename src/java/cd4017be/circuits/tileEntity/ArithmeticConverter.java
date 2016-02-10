package cd4017be.circuits.tileEntity;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import cd4017be.api.circuits.IRedstone1bit;
import cd4017be.api.circuits.IRedstone8bit;
import cd4017be.lib.TileEntityData;
import cd4017be.lib.templates.AutomatedTile;
import cd4017be.lib.util.Utils;

/**
 *
 * @author CD4017BE
 */
public class ArithmeticConverter extends AutomatedTile implements IRedstone8bit, IRedstone1bit
{
    
    public ArithmeticConverter()
    {
        netData = new TileEntityData(1, 0, 0, 0);
    }
    
    private short state;
    private boolean update;
    private boolean updateCon;

    @Override
    protected void customPlayerCommand(byte cmd, PacketBuffer dis, EntityPlayerMP player) throws IOException 
    {
        if (cmd == 0) {
            netData.longs[0] = dis.readLong();
            update = updateCon = true;
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) 
    {
        super.writeToNBT(nbt);
        nbt.setLong("cfg", netData.longs[0]);
        nbt.setShort("state", state);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) 
    {
        super.readFromNBT(nbt);
        netData.longs[0] = nbt.getLong("cfg");
        state = nbt.getShort("state");
        update = true;
    }

    @Override
    public void update() 
    {
        if (worldObj.isRemote) return;
        if (updateCon) {
        	worldObj.notifyNeighborsOfStateChange(pos, this.getBlockType());
        	updateCon = false;
        }
        if (update) this.update(0);
    }
    
    private void update(int recursion)
    {
    	update = false;
        byte d8;
        short lstate = state;
        state = (short)this.getConfig(6);
        int v;
        for (int i = 0; i < 6; i++) {
            EnumFacing dir = EnumFacing.VALUES[i];
            d8 = this.getDirection(i);
            if (d8 >= 0) continue;
            TileEntity te = Utils.getTileOnSide(this, (byte)i);
            if ((te != null && te instanceof IRedstone8bit && ((IRedstone8bit)te).getDirection(i^1) > 0 && (v = (((IRedstone8bit)te).getValue(i^1) & 0xff)) > 0)
            		|| (te != null && te instanceof IRedstone1bit && ((IRedstone1bit)te).getBitDirection(i^1) > 0 && (v = ((IRedstone1bit)te).getBitValue(i^1)) > 0)
                    || (v = worldObj.getRedstonePower(pos.offset(dir), dir)) > 0) {
                state += v * this.getConfig(i);
            }
        }
        if (state != lstate) {
            recursion++;
            for (int i = 0; i < 6; i++) {
                EnumFacing dir = EnumFacing.VALUES[i];
                d8 = this.getDirection(i);
                if (d8 <= 0) continue;
                TileEntity te = Utils.getTileOnSide(this, (byte)i);
                if (te != null && te instanceof IRedstone8bit && ((IRedstone8bit)te).getDirection(i^1) < 0) {
                    ((IRedstone8bit)te).setValue(i^1, this.getValue(i), recursion);
                } else if (te != null && te instanceof IRedstone1bit && ((IRedstone1bit)te).getBitDirection(i^1) < 0) {
                	((IRedstone1bit)te).setBitValue(i^1, this.getBitValue(i), recursion);
                } else {
                	this.notifyStateChange(pos.offset(dir), dir.getOpposite());
                }
            }
        }
    }
    
    private void notifyStateChange(BlockPos pos, EnumFacing except)
    {
        worldObj.notifyBlockOfStateChange(pos, this.getBlockType());
        worldObj.notifyNeighborsOfStateExcept(pos, this.getBlockType(), except);
    }
    
    @Override
    public int redstoneLevel(int s, boolean str)
    {
        return this.getConfig(s | 8) == 0 ? 0 : this.getBitValue(s);
    }

    @Override
    public void onNeighborBlockChange(Block b) 
    {
        update = true;
    }
    
    @Override
    public byte getValue(int s) 
    {
    	int c = this.getConfig(s);
        return (byte)(c == 0 ? 0 : (int)state / c & 0xff);
    }

    @Override
    public byte getDirection(int s) 
    {
        return this.getConfig(s) == 0 ? 0 : this.getConfig(s | 8) == 0 ? (byte)-1 : (byte)1;
    }

    @Override
    public void setValue(int s, byte v, int recursion) 
    {
        update = true;
    }

    @Override
    public byte getBitDirection(int s) 
    {
        return this.getDirection(s);
    }

    @Override
    public byte getBitValue(int s) 
    {
        int c = this.getConfig(s);
        return (byte)(c == 0 ? 0 : (int)state / c & 0xf);
    }

    @Override
    public void setBitValue(int s, byte v, int recursion) 
    {
        update = true;
    }
    
    public int getConfig(int s)
    {
        int c, d;
        if (s < 6) {c = 0xff; s *= 8;}
        else if (s == 6) {c = 0x3ff; s = 48;}
        else {c = 1; s += 50;}
    	d = (int)(netData.longs[0] >> s) & c;
    	if (s < 58 && (d & Integer.highestOneBit(c)) != 0) d |= ~c;
    	return d;
    }
    
    public void setConfig(int s, int v)
    {
    	long c;
        if (s < 6) {c = 0xff; s *= 8;}
        else if (s == 6) {c = 0x3ff; s = 48;}
        else {c = 1; s += 50;}
        netData.longs[0] &= ~(c << s);
        netData.longs[0] |= ((long)v & c) << s;
    }
    
}
