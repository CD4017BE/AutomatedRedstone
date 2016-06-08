/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits.tileEntity;

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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;

/**
 *
 * @author CD4017BE
 */
@Optional.InterfaceList(value = {@Interface(iface = "dan200.computercraft.api.peripheral.IPeripheral", modid = "ComputerCraft"), @Interface(iface = "li.cil.oc.api.network.Environment", modid = "OpenComputers")})
public class Lever8bit extends ModTileEntity implements IRedstone8bit, ITickable, Environment //, IPeripheral //TODO reimplement
{
    private boolean update;
    public byte state;

    @Override
    public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing s, float X, float Y, float Z) 
    {
        if (!player.isSneaking() && s.getIndex() + 2 == this.getBlockMetadata()) {
            int i = Y < 0.5F ? 4 : 0;
            if (s == EnumFacing.SOUTH) {
                i |= (int)Math.floor(X * 4F);
            } else if (s == EnumFacing.NORTH) {
                i |= (int)Math.floor((1F - X) * 4F);
            } else if (s == EnumFacing.WEST) {
            	i |= (int)Math.floor(Z * 4F);
            } else if (s == EnumFacing.EAST) {
            	i |= (int)Math.floor((1F - Z) * 4F);
            }
            state ^= 1 << i;
            update = true;
            this.markUpdate();
            return true;
        } else return false;
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) 
    {
        state = pkt.getNbtCompound().getByte("state");
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket()
    {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setByte("state", state);
        return new SPacketUpdateTileEntity(pos, -1, nbt);
    }

    @Override
    public void update() 
    {
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
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) 
    {
        nbt.setByte("state", state);
        return super.writeToNBT(nbt);
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
    
    //---------------- Computer APIs --------------------
    
    private ArrayList<Object> listeners = new ArrayList<Object>();
    
    private void updateEvent() {
        for (Object computer : listeners) 
            ComputerAPI.sendEvent(computer, "out8bit", state);
    }
    
    /* TODO reimplement
    //ComputerCraft:
    
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
            this.markUpdate();
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
    */
    
    //OpenComputers:
    
    private Object node = ComputerAPI.newOCnode(this, "RedstoneCircuits-Out8bit", false);
    
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
    public Object[] getOutput(Context cont, Arguments args) {
    	return new Object[]{state};
    }
    
    @Optional.Method(modid = "OpenComputers")
    @Callback(doc = "" ,direct = true)
    public Object[] setOutput(Context cont, Arguments args) {
    	state = (byte)args.checkInteger(0);
    	update = true;
        this.markUpdate();
    	return new Object[0];
    }
    
}
