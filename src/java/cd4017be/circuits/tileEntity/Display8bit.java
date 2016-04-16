/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits.tileEntity;

import java.io.IOException;
import java.util.ArrayList;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.Optional.Interface;
import cd4017be.api.circuits.IRedstone8bit;
import cd4017be.api.computers.ComputerAPI;
import cd4017be.lib.ModTileEntity;
import cd4017be.lib.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;

/**
 *
 * @author CD4017BE
 */
@Optional.InterfaceList(value = {@Interface(iface = "dan200.computercraft.api.peripheral.IPeripheral", modid = "ComputerCraft"), @Interface(iface = "li.cil.oc.api.network.Environment", modid = "OpenComputers")})
public class Display8bit extends ModTileEntity implements IRedstone8bit, ITickable, Environment //, IPeripheral TODO reimplement
{
    private static String[] defaultFormat = {"", "##", "###"};
	private boolean update, textUpdate;
    public byte state;
    public byte dspType;
    public String text0 = "", text1 = "";
    public String format = "";

    @Override
	public void onPlayerCommand(PacketBuffer data, EntityPlayerMP player) throws IOException {
    	byte cmd = data.readByte();
    	if (cmd == 0) text0 = data.readStringFromBuffer(16);
    	else if (cmd == 1) format = data.readStringFromBuffer(3);
    	else if (cmd == 2) text1 = data.readStringFromBuffer(16);
    	else if (cmd == 3) {
    		dspType = (byte)(data.readByte() % 3);
    		format = defaultFormat[dspType];
    	}
    	this.markUpdate();
	}

	@Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) 
    {
        state = pkt.getNbtCompound().getByte("state");
        dspType = pkt.getNbtCompound().getByte("dsp");
        text0 = pkt.getNbtCompound().getString("t0");
        text1 = pkt.getNbtCompound().getString("t1");
        format = pkt.getNbtCompound().getString("form");
    }

    @Override
    public Packet getDescriptionPacket()
    {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setByte("state", state);
        nbt.setByte("dsp", dspType);
        nbt.setString("t0", text0);
        nbt.setString("t1", text1);
        nbt.setString("form", format);
        return new SPacketUpdateTileEntity(pos, -1, nbt);
    }

    @Override
    public void update() 
    {
        if (worldObj.isRemote) return;
        ComputerAPI.update(this, node, 0);
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
                textUpdate = true;
                this.updateEvent();
            }
            update = false;
        }
        if (textUpdate) {
        	this.markUpdate();
        	textUpdate = false;
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
        if (node != null) ComputerAPI.saveNode(node, nbt);
        nbt.setByte("state", state);
        nbt.setByte("mode", dspType);
        nbt.setString("t0", text0);
        nbt.setString("t1", text1);
        nbt.setString("form", format);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) 
    {
        super.readFromNBT(nbt);
        if (node != null) ComputerAPI.readNode(node, nbt);
        state = nbt.getByte("state");
        dspType = nbt.getByte("mode");
        text0 = nbt.getString("t0");
        text1 = nbt.getString("t1");
        format = nbt.getString("form");
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
    
    /* TODO reimplement
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
    */
    
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
    
    @Optional.Method(modid = "OpenComputers")
    @Callback(doc = "" ,direct = true)
    public Object[] print(Context cont, Arguments args) {
    	int tar = args.checkInteger(0);
    	String text = args.checkString(1);
    	if (tar == 0) {
    		if (text.length() > 3) throw new IllegalArgumentException("only max. 3 characters allowed for number format!");
    		format = text;
    	} else if (tar == 1) {
    		if (text.length() > 16) throw new IllegalArgumentException("only max. 16 characters allowed for description!");
    		text0 = text;
    	} else if (tar == 2) {
    		if (text.length() > 16) throw new IllegalArgumentException("only max. 16 characters allowed for description!");
    		text1 = text;
    	} else throw new IllegalArgumentException("invalid target index");
    	textUpdate = true;
    	return new Object[0];
    }
    
}
