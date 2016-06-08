/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits.tileEntity;

import java.io.IOException;

import cd4017be.api.circuits.IRedstone1bit;
import cd4017be.api.circuits.IRedstone8bit;
import cd4017be.lib.TileEntityData;
import cd4017be.lib.templates.AutomatedTile;
import cd4017be.lib.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;

/**
 *
 * @author CD4017BE
 */
public class LogicConverter extends AutomatedTile implements IRedstone8bit, IRedstone1bit
{
    
    public LogicConverter()
    {
        netData = new TileEntityData(1, 0, 0, 0);
    }
    
    private byte state;
    private boolean update;
    private boolean updateCon;

    @Override
    protected void customPlayerCommand(byte cmd, PacketBuffer dis, EntityPlayerMP player) throws IOException 
    {
        if (cmd == 0) {
            netData.longs[0] = dis.readLong();
            updateCon = update = true;
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) 
    {
        nbt.setLong("cfg", netData.longs[0]);
        nbt.setByte("state", state);
        return super.writeToNBT(nbt);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) 
    {
        super.readFromNBT(nbt);
        netData.longs[0] = nbt.getLong("cfg");
        state = nbt.getByte("state");
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
        byte d8;
        byte lstate = state;
        state = 0;
        for (int i = 0; i < 6; i++) {
            EnumFacing dir = EnumFacing.VALUES[i];
            d8 = this.getDirection(i);
            if (d8 >= 0) continue;
            TileEntity te = Utils.getTileOnSide(this, (byte)i);
            if (te != null && te instanceof IRedstone8bit && ((IRedstone8bit)te).getDirection(i^1) > 0) {
                state |= ((IRedstone8bit)te).getValue(i^1) & this.getConfig(i);
            } else if ((te != null && te instanceof IRedstone1bit && ((IRedstone1bit)te).getBitDirection(i^1) > 0 && ((IRedstone1bit)te).getBitValue(i^1) > 0)
                || worldObj.getRedstonePower(pos.offset(dir), dir) > 0) {
                state |= this.getConfig(i);
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
        update = false;
    }
    
    private void notifyStateChange(BlockPos pos, EnumFacing except)
    {
        worldObj.notifyBlockOfStateChange(pos, this.getBlockType());
        worldObj.notifyNeighborsOfStateExcept(pos, this.getBlockType(), except);
    }
    
    @Override
    public int redstoneLevel(int s, boolean str) 
    {
        return (netData.longs[0] & 1L << (s + 48)) == 0 || this.getValue(s) == 0 ? 0 : 15;
    }

    @Override
    public void onNeighborBlockChange(Block b) 
    {
        update = true;
    }
    
    @Override
    public byte getValue(int s) 
    {
        return (byte)(state & this.getConfig(s));
    }

    @Override
    public byte getDirection(int s) 
    {
        return (netData.longs[0] & 0xffL << (s * 8)) == 0 ? (byte)0 : (netData.longs[0] & 1L << (s + 48)) == 0 ? (byte)-1 : (byte)1;
    }

    @Override
    public void setValue(int s, byte v, int recursion) 
    {
        if (recursion < 16) this.update(recursion);
        else update = true;
    }

    @Override
    public byte getBitDirection(int s) 
    {
        return this.getDirection(s);
    }

    @Override
    public byte getBitValue(int s) 
    {
        return this.getValue(s) != 0 ? (byte)15 : (byte)0;
    }

    @Override
    public void setBitValue(int s, byte v, int recursion) 
    {
        if (recursion < 16) this.update(recursion);
        else update = true;
    }
    
    public int getConfig(int s)
    {
        return (int)(netData.longs[0] >> (s * 8) & 0xffL);
    }
    
    public void setConfig(int s, int v)
    {
        netData.longs[0] &= ~(0xffL << (s * 8));
        netData.longs[0] |= (long)(v & 0xff) << (s * 8);
    }
    
}
