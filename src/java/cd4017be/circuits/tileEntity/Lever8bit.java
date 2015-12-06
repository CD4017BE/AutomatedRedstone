/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits.tileEntity;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

import java.util.ArrayList;

import cd4017be.api.circuits.IRedstone8bit;
import cd4017be.lib.ModTileEntity;
import cd4017be.lib.util.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

/**
 *
 * @author CD4017BE
 */
public class Lever8bit extends ModTileEntity implements IRedstone8bit, IPeripheral
{
    private boolean update;
    public byte state;

    @Override
    public boolean onActivated(EntityPlayer player, int s, float X, float Y, float Z) 
    {
        if (!player.isSneaking() && s == worldObj.getBlockMetadata(xCoord, yCoord, zCoord)) {
            int i = Y < 0.5F ? 4 : 0;
            if (s == 3) {
                i |= (int)Math.floor(X * 4F);
            } else if (s == 2) {
                i |= (int)Math.floor((1F - X) * 4F);
            } else if (s == 5) {
                i |= (int)Math.floor((1F - Z) * 4F);
            } else if (s == 4) {
                i |= (int)Math.floor(Z * 4F);
            }
            state ^= 1 << i;
            update = true;
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            return true;
        } else return false;
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) 
    {
        state = pkt.func_148857_g().getByte("state");
    }

    @Override
    public Packet getDescriptionPacket() 
    {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setByte("state", state);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, -1, nbt);
    }

    @Override
    public void updateEntity() 
    {
        super.updateEntity();
        if (worldObj.isRemote) return;
        if (update) {
            for (int i = 0; i < 6; i++) {
                TileEntity te = Utils.getTileOnSide(this, (byte)i);
                if (te != null && te instanceof IRedstone8bit && ((IRedstone8bit)te).getDirection(i^1) < 0) {
                    ((IRedstone8bit)te).setValue(i^1, state, 1);
                }
            }
            this.updateEvent();
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) 
    {
        super.writeToNBT(nbt);
        nbt.setByte("state", state);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) 
    {
        super.readFromNBT(nbt);
        state = nbt.getByte("state");
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
        return 1;
    }

    @Override
    public void setValue(int s, byte v, int recursion) 
    {
    }
    
    private ArrayList<IComputerAccess> listeners = new ArrayList<IComputerAccess>();
    
    @Override
    public String getType() 
    {
        return "RedstoneCircuits-Out8bit";
    }

    @Override
    public String[] getMethodNames() 
    {
        return new String[]{"getOutput", "setOutput"};
    }

    @Override
    public Object[] callMethod(IComputerAccess computer, ILuaContext lua, int cmd, Object[] par) throws LuaException 
    {
        if (cmd == 0) {
            return new Object[]{Double.valueOf(state)};
        } else if (cmd == 1) {
            if (par.length != 1 || !(par[0] instanceof Double)) throw new LuaException("missing parameter [setOutput(Number state)]");
            state = ((Double)par[0]).byteValue();
            update = true;
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            return null;
        } else return null;
    }

    @Override
    public void attach(IComputerAccess computer) 
    {
        listeners.add(computer);
    }

    @Override
    public void detach(IComputerAccess computer) 
    {
        listeners.remove(computer);
    }

    @Override
    public boolean equals(IPeripheral peripheral) 
    {
        return this.hashCode() == peripheral.hashCode();
    }
    
    private void updateEvent()
    {
        for (IComputerAccess computer : listeners) {
            computer.queueEvent("out8bit", new Object[]{Double.valueOf(state)});
        }
    }
    
}
