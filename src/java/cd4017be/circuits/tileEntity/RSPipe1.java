/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits.tileEntity;

import java.util.ArrayList;

import cd4017be.api.circuits.IRedstone1bit;
import cd4017be.circuits.block.BlockRSPipe1;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;

/**
 *
 * @author CD4017BE
 */
public class RSPipe1 extends ModTileEntity implements IRedstone1bit, IPipe, ITickable
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
        byte type = (byte)this.getBlockMetadata();
        boolean lHasOut = getFlowBit(6);
        boolean lHasIn = getFlowBit(14);
        boolean nHasOut = false;
        boolean nHasIn = false;
        boolean lDirOut;
        boolean lDirIn;
        ArrayList<RSPipe1> updateList = new ArrayList<RSPipe1>();
        short lFlow = flow;
        for (int i = 0; i < 6; i++) {
            lDirOut = this.getFlowBit(i);
            lDirIn = this.getFlowBit(i | 8);
            if (lDirOut && lDirIn) continue;
            dir = EnumFacing.VALUES[i];
            te = worldObj.getTileEntity(pos.offset(dir));
            if (te != null && te instanceof RSPipe1) {
                RSPipe1 pipe = (RSPipe1)te;
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
            } else if (te != null && te instanceof IRedstone1bit) {
                byte d = ((IRedstone1bit)te).getBitDirection(i^1);
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
            } else if (type != BlockRSPipe1.ID_Transport && !worldObj.getBlockState(pos.offset(dir)).getMaterial().isReplaceable()) {
                setFlowBit(i, type == BlockRSPipe1.ID_Injection);
                setFlowBit(i | 8, type == BlockRSPipe1.ID_Extraction);
                nHasOut |= type == BlockRSPipe1.ID_Injection;
                nHasIn |= type == BlockRSPipe1.ID_Extraction;
            } else {
                setFlowBit(i, false);
                setFlowBit(i | 8, false);
            }
        }
        setFlowBit(6, nHasOut);
        setFlowBit(14, nHasIn);
        if (flow != lFlow) {
            this.markUpdate();
            for (RSPipe1 pipe : updateList) {
                pipe.onNeighborBlockChange(Blocks.AIR);
            }
            update = true;
        }
        updateCon = false;
    }
    
    private void transferSignal(int recursion)
    {
        IRedstone1bit[] rst = new IRedstone1bit[6];
        byte[] dir = new byte[6];
        EnumFacing fd;
        int n = 0;
        boolean dirOut;
        boolean dirIn;
        byte lstate = state;
        byte nstate;
        state = 0;
        for (int i = 0; i < 6; i++) {
            dirOut = this.getFlowBit(i);
            dirIn = this.getFlowBit(i | 8);
            if (!dirIn ^ dirOut) continue;
            TileEntity te = Utils.getTileOnSide(this, (byte)i);
            boolean rs = te != null && te instanceof IRedstone1bit;
            if (dirIn) {
                if (!rs){
                    fd = EnumFacing.VALUES[i];
                    if ((nstate = (byte)worldObj.getRedstonePower(pos.offset(fd), fd)) > state) state = nstate;
                } else if ((nstate = ((IRedstone1bit)te).getBitValue(i^1)) > state) state = nstate;
            } else if (dirOut) {
                if (rs) rst[n] = (IRedstone1bit)te;
                else rst[n] = null;
                dir[n++] = (byte)(i^1);
            }
        }
        if (state != lstate) {
            recursion++;
            for (int i = 0; i < n; i++) {
                if (rst[i] != null) rst[i].setBitValue(dir[i], state, recursion);
                else {
                    fd = EnumFacing.VALUES[dir[i]].getOpposite();
                    this.notifyStateChange(pos.offset(fd));
                }
            }
        }
        update = false;
    }
    
    private void notifyStateChange(BlockPos pos)
    {
        worldObj.notifyBlockOfStateChange(pos, this.getBlockType());
        worldObj.notifyNeighborsOfStateChange(pos, this.getBlockType());
    }

    @Override
    public void onNeighborBlockChange(Block b) 
    {
        updateCon = true;
        if (b != this.getBlockType()) update = true;
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
            if (te != null && te instanceof RSPipe1) {
                RSPipe1 pipe = (RSPipe1)te;
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
    public byte getBitValue(int s) 
    {
        return state;
    }

    @Override
    public byte getBitDirection(int s) 
    {
        boolean b1 = getFlowBit(s);
        boolean b2 = getFlowBit(s | 8);
        return (byte)(b1 ^ b2 ? b1 ? 1 : -1 : 0);
    }

    @Override
    public void setBitValue(int s, byte v, int recursion) 
    {
        if (recursion > 16) update = true;
        else this.transferSignal(recursion);
    }

    @Override
    public int redstoneLevel(int s, boolean str) 
    {
        if (state > 0 && this.getFlowBit(s) && !this.getFlowBit(s | 8) && this.getBlockMetadata() == BlockRSPipe1.ID_Injection) return state;
        else return 0;
    }
    
    @Override
    public Cover getCover() 
    {
        return cover;
    }

    @Override
    public int textureForSide(byte s) 
    {
        if (s == -1) return this.getBlockMetadata();
    	TileEntity p = Utils.getTileOnSide(this, s);
        boolean b0 = getFlowBit(s), b1 = getFlowBit(s | 8);
        if (b0 ^ b1 || (!b0 && !b1 && p != null && p instanceof IRedstone1bit)) return (b0?1:0)|(b1?2:0);
        else return -1;
    }
    
}
