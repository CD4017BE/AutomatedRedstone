/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits.tileEntity;

import java.util.ArrayList;

import cd4017be.api.circuits.IRedstone8bit;
import cd4017be.lib.ModTileEntity;
import cd4017be.lib.templates.IPipe;
import cd4017be.lib.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
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
public class RSPipe8 extends ModTileEntity implements IRedstone8bit, IPipe, ITickable
{
    private byte state;
    private boolean update = false;
    private short flow;
    private boolean updateCon = true;
    private Cover cover = null;

    @Override
    public void update() 
    {
        if (worldObj.isRemote) return;
        if (updateCon) this.updateConnections();
        if (getFlowBit(6) && update) this.transferSignal(0);
    }
    
    private void updateConnections() 
    {
        EnumFacing dir;
        TileEntity te;
        boolean lHasOut = getFlowBit(6);
        boolean lHasIn = getFlowBit(14);
        boolean nHasOut = false;
        boolean nHasIn = false;
        boolean lDirOut;
        boolean lDirIn;
        ArrayList<RSPipe8> updateList = new ArrayList<RSPipe8>();
        short lFlow = flow;
        for (int i = 0; i < 6; i++) {
            lDirOut = this.getFlowBit(i);
            lDirIn = this.getFlowBit(i | 8);
            if (lDirOut && lDirIn) continue;
            dir = EnumFacing.VALUES[i];
            te = worldObj.getTileEntity(pos.offset(dir));
            if (te == null) {
                setFlowBit(i, false);
                setFlowBit(i | 8, false);
            } else if (te instanceof RSPipe8) {
                RSPipe8 pipe = (RSPipe8)te;
                boolean pHasOut = pipe.getFlowBit(6);
                boolean pHasIn = pipe.getFlowBit(14);
                boolean pDirOut = pipe.getFlowBit(i ^ 1);
                boolean pDirIn = pipe.getFlowBit((i ^ 1) | 8);
                boolean nDirOut = pHasOut && !pDirOut;
                boolean nDirIn = pHasIn && !pDirIn;
                if (pDirOut && pDirIn) {
                    nDirOut = true;
                    nDirIn = true;
                } else if (nDirOut && nDirIn) {
                    boolean s = lHasIn ^ lHasOut;
                    nDirOut = s && lHasIn && !lDirIn;
                    nDirIn = s && lHasOut && !lDirOut;
                }
                this.setFlowBit(i, nDirOut);
                this.setFlowBit(i | 8, nDirIn);
                nHasIn |= nDirIn && !nDirOut;
                nHasOut |= nDirOut && !nDirIn;
                updateList.add(pipe);
            } else if (te instanceof IRedstone8bit) {
                byte d = ((IRedstone8bit)te).getDirection(i^1);
                if (d == 0) {
                    setFlowBit(i, false);
                    setFlowBit(i | 8, false);
                } else if (d > 0) {
                    setFlowBit(i, false);
                    setFlowBit(i | 8, true);
                    nHasIn = true;
                } else if (d < 0) {
                    setFlowBit(i, true);
                    setFlowBit(i | 8, false);
                    nHasOut = true;
                }
            } else {
                setFlowBit(i, false);
                setFlowBit(i | 8, false);
            }
        }
        setFlowBit(6, nHasOut);
        setFlowBit(14, nHasIn);
        if (flow != lFlow) {
            this.markUpdate();
            for (RSPipe8 pipe : updateList) {
                pipe.onNeighborBlockChange(Blocks.AIR);
            }
            update = true;
        }
        updateCon = false;
    }
    
    private void transferSignal(int recursion)
    {
        IRedstone8bit[] rst = new IRedstone8bit[6];
        byte[] dir = new byte[6];
        int n = 0;
        boolean dirOut;
        boolean dirIn;
        byte lstate = state;
        state = 0;
        for (int i = 0; i < 6; i++) {
            dirOut = this.getFlowBit(i);
            dirIn = this.getFlowBit(i | 8);
            if (!dirIn ^ dirOut) continue;
            TileEntity te = Utils.getTileOnSide(this, (byte)i);
            if (te == null || !(te instanceof IRedstone8bit)) updateCon = true;
            else if (dirIn) {
                state |= ((IRedstone8bit)te).getValue(i^1);
            } else if (dirOut) {
                rst[n] = (IRedstone8bit)te;
                dir[n++] = (byte)(i^1);
            }
        }
        if (state != lstate) {
            recursion++;
            for (int i = 0; i < n; i++)
            {
                rst[i].setValue(dir[i], state, recursion);
            }
        }
        update = false;
    }

    @Override
    public void onNeighborBlockChange(Block b) 
    {
        updateCon = true;
    }

    @Override
    public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing dir, float X, float Y, float Z) 
    {
        int s = dir.getIndex();
        if (player.isSneaking() && item == null) {
            if (worldObj.isRemote) return true;
            if (cover != null) {
                this.dropStack(cover.item);
                cover = null;
                this.markUpdate();
                return true;
            }
            X -= 0.5F;
            Y -= 0.5F;
            Z -= 0.5F;
            float dx = Math.abs(X);
            float dy = Math.abs(Y);
            float dz = Math.abs(Z);
            if (dy > dz && dy > dx) s = Y < 0 ? 0 : 1;
            else if (dz > dx) s = Z < 0 ? 2 : 3;
            else s = X < 0 ? 4 : 5;
            boolean lock = !(this.getFlowBit(s) && this.getFlowBit(s | 8));
            this.setFlowBit(s, lock);
            this.setFlowBit(s | 8, lock);
            this.onNeighborBlockChange(Blocks.AIR);
            this.markUpdate();
            TileEntity te = Utils.getTileOnSide(this, (byte)s);
            if (te != null && te instanceof RSPipe8) {
                RSPipe8 pipe = (RSPipe8)te;
                pipe.setFlowBit(s^1, lock);
                pipe.setFlowBit(s^1 | 8, lock);
                pipe.onNeighborBlockChange(Blocks.AIR);
                pipe.markUpdate();
            }
            return true;
        } else if (!player.isSneaking() && cover == null && item != null && (cover = Cover.create(item)) != null) {
            if (worldObj.isRemote) return true;
            item.stackSize--;
            if (item.stackSize <= 0) item = null;
            player.setHeldItem(hand, item);
            this.markUpdate();
            return true;
        } else return false;
    }
    
    public boolean getFlowBit(int b)
    {
        return (flow & (1 << b)) != 0;
    }
    
    private void setFlowBit(int b, boolean v)
    {
        if (v) flow |= 1 << b;
        else flow &= ~(1 << b);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) 
    {
        nbt.setShort("flow", flow);
        nbt.setByte("state", state);
        if (cover != null) cover.write(nbt, "cover");
        return super.writeToNBT(nbt);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) 
    {
        super.readFromNBT(nbt);
        flow = nbt.getShort("flow");
        state = nbt.getByte("state");
        cover = Cover.read(nbt, "cover");
        updateCon = true;
        update = true;
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) 
    {
        flow = pkt.getNbtCompound().getShort("flow");
        cover = Cover.read(pkt.getNbtCompound(), "cover");
        this.markUpdate();
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket()
    {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setShort("flow", flow);
        if (cover != null) cover.write(nbt, "cover");
        return new SPacketUpdateTileEntity(pos, -1, nbt);
    }
    
    @Override
    public void breakBlock() 
    {
        super.breakBlock();
        if (cover != null) {
            EntityItem entity = new EntityItem(worldObj, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, cover.item);
            cover = null;
            worldObj.spawnEntityInWorld(entity);
        }
    }
    
    @Override
    public byte getValue(int s) 
    {
        return state;
    }

    @Override
    public byte getDirection(int s) 
    {
        boolean b1 = getFlowBit(s);
        boolean b2 = getFlowBit(s | 8);
        return (byte)(b1 ^ b2 ? b1 ? 1 : -1 : 0);
    }

    @Override
    public void setValue(int s, byte v, int recursion) 
    {
        if (v != state) {
            if (recursion > 16) update = true;
            else this.transferSignal(recursion);
        }
    }
    
    @Override
    public Cover getCover() 
    {
        return cover;
    }

    @Override
    public int textureForSide(byte s) 
    {
    	if (s == -1) return 0;
        TileEntity p = Utils.getTileOnSide(this, s);
        boolean b0 = getFlowBit(s), b1 = getFlowBit(s | 8);
        if (b0 ^ b1 || (!b0 && !b1 && p != null && p instanceof IRedstone8bit)) return (b0?1:0)|(b1?2:0);
        else return -1;
    }
    
}
