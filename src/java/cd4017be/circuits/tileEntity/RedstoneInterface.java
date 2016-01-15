/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits.tileEntity;

import java.io.DataInputStream;
import java.io.IOException;

import cd4017be.api.circuits.IRedstone1bit;
import cd4017be.api.circuits.IRedstone8bit;
import cd4017be.lib.TileEntityData;
import cd4017be.lib.templates.AutomatedTile;
import cd4017be.lib.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

/**
 *
 * @author CD4017BE
 */
public class RedstoneInterface extends AutomatedTile implements IRedstone8bit, IRedstone1bit
{
    
    public RedstoneInterface()
    {
        netData = new TileEntityData(0, 1, 0, 0);
    }
    
    private byte state;
    private boolean update;

    @Override
    protected void customPlayerCommand(byte cmd, DataInputStream dis, EntityPlayerMP player) throws IOException 
    {
        if (cmd == 0) {
            netData.ints[0] = dis.readInt();
            update = true;
            worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, this.getBlockType());
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) 
    {
        super.writeToNBT(nbt);
        nbt.setInteger("cfg", netData.ints[0]);
        nbt.setByte("state", state);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) 
    {
        super.readFromNBT(nbt);
        netData.ints[0] = nbt.getInteger("cfg");
        state = nbt.getByte("state");
        update = true;
    }

    @Override
    public void updateEntity() 
    {
        if (worldObj.isRemote) return;
        if (update) this.update(0);
    }
    
    private void update(int recursion)
    {
        byte d8, d1;
        byte lstate = state;
        state = 0;
        for (int i = 0; i < 6; i++) {
            ForgeDirection dir = ForgeDirection.getOrientation(i);
            d1 = this.getBitDirection(i);
            d8 = this.getDirection(i);
            if (d1 > -1 && d8 > -1) continue;
            TileEntity te = Utils.getTileOnSide(this, (byte)i);
            if (d8 < 0 && te != null && te instanceof IRedstone8bit && ((IRedstone8bit)te).getDirection(i^1) > 0) {
                state |= ((IRedstone8bit)te).getValue(i^1);
            } else if (d1 < 0 && ((te != null && te instanceof IRedstone1bit && ((IRedstone1bit)te).getBitDirection(i^1) > 0 && ((IRedstone1bit)te).getBitValue(i^1))
                || worldObj.getIndirectPowerOutput(xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ, i))) {
                state |= 1 << ((this.getConfig(i) & 0xf) - 2);
            }
        }
        if (state != lstate) {
            recursion++;
            for (int i = 0; i < 6; i++)
            {
                ForgeDirection dir = ForgeDirection.getOrientation(i);
                d1 = this.getBitDirection(i);
                d8 = this.getDirection(i);
                if (d1 < 1 && d8 < 1) continue;
                TileEntity te = Utils.getTileOnSide(this, (byte)i);
                if (d8 > 0 && te != null && te instanceof IRedstone8bit && ((IRedstone8bit)te).getDirection(i^1) < 0) {
                    ((IRedstone8bit)te).setValue(i^1, state, recursion);
                } else if (d1 > 0) {
                    if (te != null && te instanceof IRedstone1bit && ((IRedstone1bit)te).getBitDirection(i^1) < 0) {
                        ((IRedstone1bit)te).setBitValue(i^1, (state >> ((this.getConfig(i) & 0xf) - 2) & 1) != 0, recursion);
                    } else {
                        this.notifyStateChange(xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ);
                    }
                }
            }
        }
        update = false;
    }
    
    private void notifyStateChange(int x, int y, int z)
    {
        worldObj.notifyBlockOfNeighborChange(x, y, z, this.getBlockType());
        worldObj.notifyBlocksOfNeighborChange(x, y, z, this.getBlockType());
    }
    
    @Override
    public int redstoneLevel(int s, boolean str) 
    {
        int c = this.getConfig(s);
        if ((c & 0xf) >= 2 && c >= 16) return (state & (1 << ((c & 0xf) - 2))) != 0 ? 15 : 0;
        else return 0;
    }

    @Override
    public void onNeighborBlockChange(Block b) 
    {
        update = true;
    }
    
    @Override
    public byte getValue(int s) 
    {
        return state;
    }

    @Override
    public byte getDirection(int s) 
    {
        int c = this.getConfig(s);
        if ((c & 0xf) == 1) return (c & 0x10) != 0 ? (byte)1 : (byte)-1;
        else return 0;
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
        int c = this.getConfig(s);
        if ((c & 0xf) >= 2) return (c & 0x10) != 0 ? (byte)1 : (byte)-1;
        else return 0;
    }

    @Override
    public boolean getBitValue(int s) 
    {
        int c = this.getConfig(s) & 0xf;
        if (c >= 2) return (state & (1 << (c - 2))) != 0;
        else return false;
    }

    @Override
    public void setBitValue(int s, boolean v, int recursion) 
    {
        if (recursion < 16) this.update(recursion);
        else update = true;
    }
    
    public int getConfig(int s)
    {
        return netData.ints[0] >> (s * 5) & 0x1f;
    }
    
    public void setConfig(int s, int v)
    {
        netData.ints[0] &= ~(0x1f << (s * 5));
        netData.ints[0] |= (v & 0x1f) << (s * 5);
    }
    
}
