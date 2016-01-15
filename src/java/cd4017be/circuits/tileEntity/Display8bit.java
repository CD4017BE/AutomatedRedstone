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
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.Optional.Interface;

import java.util.ArrayList;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import cd4017be.api.circuits.IRedstone8bit;
import cd4017be.api.computers.ComputerAPI;
import cd4017be.lib.ModTileEntity;
import cd4017be.lib.util.Utils;
import net.minecraft.block.Block;
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
@Optional.InterfaceList(value = {@Interface(iface = "dan200.computercraft.api.peripheral.IPeripheral", modid = "ComputerCraft"), @Interface(iface = "li.cil.oc.api.network.Environment", modid = "OpenComputers")})
public class Display8bit extends ModTileEntity implements IRedstone8bit, IPeripheral, Environment
{
    private boolean update;
    public byte state;
    public byte dspType;

    @Override
    public boolean onActivated(EntityPlayer player, int s, float X, float Y, float Z) 
    {
        if (!player.isSneaking() && s == worldObj.getBlockMetadata(xCoord, yCoord, zCoord)) {
            dspType = (byte)((dspType + 1) % 3); 
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            return true;
        } else return false;
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) 
    {
        state = pkt.func_148857_g().getByte("state");
        dspType = pkt.func_148857_g().getByte("dsp");
    }

    @Override
    public Packet getDescriptionPacket() 
    {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setByte("state", state);
        nbt.setByte("dsp", dspType);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, -1, nbt);
    }

    @Override
    public void updateEntity() 
    {
        super.updateEntity();
        if (worldObj.isRemote) return;
        if (update) {
            byte lstate = state;
            state = 0;
            for (int i = 0; i < 6; i++) {
                TileEntity te = Utils.getTileOnSide(this, (byte)i);
                if (te != null && te instanceof IRedstone8bit && ((IRedstone8bit)te).getDirection(i^1) > 0) {
                    state |= ((IRedstone8bit)te).getValue(i^1);
                }
            }
            if (state != lstate) {
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                this.updateEvent();
            }
        }
    }

    @Override
    public void onNeighborBlockChange(Block b) 
    {
        update = true;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) 
    {
        super.writeToNBT(nbt);
        nbt.setByte("state", state);
        nbt.setByte("mode", dspType);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) 
    {
        super.readFromNBT(nbt);
        state = nbt.getByte("state");
        dspType = nbt.getByte("mode");
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
        return -1;
    }

    @Override
    public void setValue(int s, byte v, int recursion) 
    {
        update = true;
    }
    
    //---------------- Computer APIs --------------------
    
    private ArrayList<Object> listeners = new ArrayList<Object>();
    
    private void updateEvent() {
        for (Object computer : listeners)
            ComputerAPI.sendEvent(computer, "in8bit", state);
    }
    
    //ComputerCraft:
    
    @Optional.Method(modid = "ComputerCraft")
    @Override
    public String getType() 
    {
        return "RedstoneCircuits-In8bit";
    }

    @Optional.Method(modid = "ComputerCraft")
    @Override
    public String[] getMethodNames() 
    {
        return new String[]{"getInput"};
    }

    @Optional.Method(modid = "ComputerCraft")
    @Override
    public Object[] callMethod(IComputerAccess computer, ILuaContext lua, int cmd, Object[] par) throws LuaException 
    {
        if (cmd == 0) {
            return new Object[]{Double.valueOf(state)};
        } else return null;
    }

    @Optional.Method(modid = "ComputerCraft")
    @Override
    public void attach(IComputerAccess computer) 
    {
        listeners.add(computer);
    }

    @Optional.Method(modid = "ComputerCraft")
    @Override
    public void detach(IComputerAccess computer) 
    {
        listeners.remove(computer);
    }

    @Optional.Method(modid = "ComputerCraft")
    @Override
    public boolean equals(IPeripheral peripheral) 
    {
        return this.hashCode() == peripheral.hashCode();
    }

    //OpenComputers:
    
    private Object node = ComputerAPI.newOCnode(this, "RedstoneCircuits-In8bit", false);
    
    @Override
	public void invalidate() {
		super.invalidate();
		ComputerAPI.removeOCnode(node);
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		ComputerAPI.removeOCnode(node);
	}

	@Optional.Method(modid = "OpenComputers")
	@Override
	public Node node() {
		return (Node)node;
	}

    @Optional.Method(modid = "OpenComputers")
	@Override
	public void onConnect(Node node) {
    	if (node.host() instanceof Context) listeners.add(node.host());
	}

    @Optional.Method(modid = "OpenComputers")
	@Override
	public void onDisconnect(Node node) {
    	if (node.host() instanceof Context) listeners.remove(node.host());
	}

    @Optional.Method(modid = "OpenComputers")
	@Override
	public void onMessage(Message message) {}
    
    @Optional.Method(modid = "OpenComputers")
    @Callback(doc = "" ,direct = true)
    public Object[] getInput(Context cont, Arguments args) {
    	return new Object[]{state};
    }
}
