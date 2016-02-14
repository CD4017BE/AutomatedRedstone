package cd4017be.circuits.tileEntity;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import cd4017be.api.circuits.IRedstone8bit;
import cd4017be.lib.ModTileEntity;
import cd4017be.lib.util.Utils;

public class Wireless8bit extends ModTileEntity implements IRedstone8bit, ITickable
{

	public int linkX = 0;
    public int linkY = -1;
    public int linkZ = 0;
    public int linkD = 0;
    private Wireless8bit linkTile;
    private boolean updateCon;
	private boolean update;
    public byte state;
    
    @Override
    public void onPlaced(EntityLivingBase entity, ItemStack item) 
    {
        if (worldObj.isRemote) return;
        if (item.getItemDamage() == 0) {
            ItemStack drop = new ItemStack(item.getItem(), 1, 2);
            drop.setTagCompound(new NBTTagCompound());
            drop.getTagCompound().setInteger("lx", pos.getX());
            drop.getTagCompound().setInteger("ly", pos.getY());
            drop.getTagCompound().setInteger("lz", pos.getZ());
            drop.getTagCompound().setInteger("ld", worldObj.provider.getDimensionId());
            EntityItem eitem = new EntityItem(worldObj, entity.posX, entity.posY, entity.posZ, drop);
            worldObj.spawnEntityInWorld(eitem);
            if (entity instanceof EntityPlayer) ((EntityPlayer)entity).addChatMessage(new ChatComponentText("The droped Receiver will link to this"));
        } else if (item.getTagCompound() != null) {
            linkX = item.getTagCompound().getInteger("lx");
            linkY = item.getTagCompound().getInteger("ly");
            linkZ = item.getTagCompound().getInteger("lz");
            linkD = item.getTagCompound().getInteger("ld");
            link(true);
            if (linkTile != null && entity instanceof EntityPlayer) ((EntityPlayer)entity).addChatMessage(new ChatComponentText(String.format("Link found in dimension %d at position %d , %d , %d", linkD, linkX, linkY, linkZ)));
            else if (entity instanceof EntityPlayer) ((EntityPlayer)entity).addChatMessage(new ChatComponentText("Error: Link not Found!"));
        }
    }

    @Override
    public ArrayList<ItemStack> dropItem(IBlockState m, int fortune) 
    {
        ArrayList<ItemStack> list = new ArrayList<ItemStack>();
        ItemStack drop = new ItemStack(this.getBlockType(), 1, this.getBlockMetadata() == 0 ? 1 : 2);
        drop.setTagCompound(new NBTTagCompound());
        drop.getTagCompound().setInteger("lx", linkX);
        drop.getTagCompound().setInteger("ly", linkY);
        drop.getTagCompound().setInteger("lz", linkZ);
        drop.getTagCompound().setInteger("ld", linkD);
        list.add(drop);
        return list;
    }

    @Override
    public boolean onActivated(EntityPlayer player, EnumFacing s, float X, float Y, float Z) 
    {
        if (worldObj.isRemote) return true;
        if (player.isSneaking() && player.getCurrentEquippedItem() == null && linkTile != null && !linkTile.isInvalid() && linkTile.linkTile == this) {
            ItemStack item = new ItemStack(this.getBlockType(), 1, 0);
            linkTile.worldObj.setBlockToAir(linkTile.pos);
            worldObj.setBlockToAir(pos);
            EntityItem eitem = new EntityItem(worldObj, player.posX, player.posY, player.posZ, item);
            worldObj.spawnEntityInWorld(eitem);
            player.addChatMessage(new ChatComponentText("Both linked 8-bit-Wireless devices removed"));
            return true;
        }
        return false;
    }

    @Override
    public int redstoneLevel(int s, boolean str) 
    {
        if (linkTile == null) return 0;
        EnumFacing dir = EnumFacing.VALUES[s];
        if (str) return 0;
        return linkTile.worldObj.getRedstonePower(new BlockPos(linkX, linkY, linkZ).offset(dir), EnumFacing.VALUES[s]);
    }

    @Override
    public void update() 
    {
    	if (worldObj.isRemote) return;
        if (updateCon) {
            link(false);
            updateCon = false;
        }
        if (update && this.getBlockMetadata() == 0) {
            byte lstate = state;
            state = 0;
            for (int i = 0; i < 6; i++) {
                TileEntity te = Utils.getTileOnSide(this, (byte)i);
                if (te != null && te instanceof IRedstone8bit && ((IRedstone8bit)te).getDirection(i^1) > 0) {
                    state |= ((IRedstone8bit)te).getValue(i^1);
                }
            }
            if (state != lstate && linkTile != null) {
                linkTile.setValue(-1, state, 1);
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) 
    {
        super.writeToNBT(nbt);
        nbt.setByte("state", state);
        nbt.setInteger("lx", linkX);
        nbt.setInteger("ly", linkY);
        nbt.setInteger("lz", linkZ);
        nbt.setInteger("ld", linkD);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) 
    {
        super.readFromNBT(nbt);
        state = nbt.getByte("state");
        update = true;
        linkX = nbt.getInteger("lx");
        linkY = nbt.getInteger("ly");
        linkZ = nbt.getInteger("lz");
        linkD = nbt.getInteger("ld");
        updateCon = true;
    }
    
    @Override
    public void validate() 
    {
        super.validate();
        updateCon = true;
    }

    @Override
    public void invalidate() 
    {
        super.invalidate();
        if (linkTile != null) {
            if (linkTile.linkTile == this) linkTile.linkTile = null;
            linkTile = null;
        }
    }
    
    public boolean hasLink()
    {
        return linkTile != null;
    }
    
    private void link(boolean force)
    {
        World world = DimensionManager.getWorld(linkD);
        if (world == null && force) {
            DimensionManager.initDimension(linkD);
            world = DimensionManager.getWorld(linkD);
        }
        linkTile = null;
        if (world != null) {
            TileEntity te = world.getTileEntity(new BlockPos(linkX, linkY, linkZ));
            if (te != null && te instanceof Wireless8bit) {
                linkTile = (Wireless8bit)te;
                linkTile.linkTile = this;
                linkTile.linkX = pos.getX();
                linkTile.linkY = pos.getY();
                linkTile.linkZ = pos.getZ();
                linkTile.linkD = worldObj.provider.getDimensionId();
            }
        }
    }
    
    private void transferSignal(int recursion)
    {
        recursion++;
        for (int i = 0; i < 6; i++) {
        	TileEntity te = Utils.getTileOnSide(this, (byte)i);
        	if (te != null && te instanceof IRedstone8bit)
        		((IRedstone8bit)te).setValue(i^1, state, recursion);
        }
        update = false;
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
        return this.getBlockMetadata() == 0 ? -1 : (byte)1;
    }

    @Override
    public void setValue(int s, byte v, int recursion) 
    {
        if (this.getBlockMetadata() != 0) {
        	if (v == state) return;
        	state = v;
        	this.transferSignal(recursion);
        } else update = true;
    }

}
