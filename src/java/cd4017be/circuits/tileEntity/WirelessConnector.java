package cd4017be.circuits.tileEntity;

import java.util.Arrays;
import java.util.List;

import multiblock.IntegerComp;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.capabilities.Capability;
import cd4017be.circuits.Objects;
import cd4017be.lib.ModTileEntity;
import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.IUpdatable;
import cd4017be.lib.TooltipInfo;

public class WirelessConnector extends ModTileEntity implements IUpdatable {

	protected BlockPos linkPos = BlockPos.ORIGIN;
	protected int linkDim;
	protected WirelessConnector linkTile;
	protected ModTileEntity conTile;
	private boolean updateLink, updateCon;

	protected void link(WirelessConnector tile) {
		if (tile != linkTile) {
			linkTile = tile;
			if (linkTile != null) {
				linkPos = linkTile.pos;
				linkDim = linkTile.dimensionId;
				if (!tileEntityInvalid && conTile != null && !conTile.isInvalid()) conTile.onNeighborTileChange(pos);
			} else if (conTile != null && !conTile.isInvalid()) {
				IntegerComp c = conTile.getCapability(Objects.RS_INTEGER_CAPABILITY, EnumFacing.VALUES[getOrientation()^1]);
				if (c != null) c.network.markDirty();
			}
		}
		updateLink = false;
	}

	private void checkLink(boolean forceLoad) {
		World world = DimensionManager.getWorld(linkDim);
		TileEntity te = world != null && (forceLoad || world.isBlockLoaded(linkPos)) ? world.getTileEntity(linkPos) : null;
		if (te == linkTile) return;
		if (linkTile != null && linkTile.linkTile == this) linkTile.link(null);
		if (te instanceof WirelessConnector) {
			link((WirelessConnector)te);
			linkTile.link(this);
		} else {
			link(null);
		}
	}

	@Override
	public void process() {
		if (updateLink) checkLink(false);
		if (updateCon) {
			TileEntity te = getLoadedTile(pos.offset(EnumFacing.VALUES[getOrientation()]));
			if (!(te instanceof ModTileEntity) || te instanceof WirelessConnector) te = null;
			if (te != conTile) {
				conTile = (ModTileEntity)te;
				if (linkTile != null && linkTile.conTile != null && !linkTile.conTile.isInvalid()) linkTile.conTile.onNeighborTileChange(linkTile.pos);
			}
			updateCon = false;
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		linkPos = new BlockPos(nbt.getInteger("lx"), nbt.getInteger("ly"), nbt.getInteger("lz"));
		linkDim = nbt.getInteger("ld");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setInteger("lx", linkPos.getX());
		nbt.setInteger("ly", linkPos.getY());
		nbt.setInteger("lz", linkPos.getZ());
		nbt.setInteger("ld", linkDim);
		return super.writeToNBT(nbt);
	}

	@Override
	public void invalidate() {
		if (linkTile != null && linkTile.linkTile == this) linkTile.link(null);
		linkTile = null;
		conTile = null;
		super.invalidate();
	}

	@Override
	public void onChunkUnload() {
		if (linkTile != null && linkTile.linkTile == this) linkTile.link(null);
		linkTile = null;
		conTile = null;
		super.onChunkUnload();
	}

	@Override
	public void validate() {
		updateLink = updateCon = true;
		TickRegistry.instance.updates.add(this);
		super.validate();
	}

	@Override
	public void onNeighborTileChange(BlockPos pos) {
		if (!updateCon) {
			updateCon = true;
			TickRegistry.instance.updates.add(this);
		}
	}

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
		return cap == Objects.RS_INTEGER_CAPABILITY && facing.ordinal() == getOrientation();
	}

	@Override
	public <T> T getCapability(Capability<T> cap, EnumFacing facing) {
		return cap == Objects.RS_INTEGER_CAPABILITY && facing.ordinal() == getOrientation()
			&& linkTile != null && linkTile.conTile != null ?
			linkTile.conTile.getCapability(cap, EnumFacing.VALUES[linkTile.getOrientation()^1]) : null;
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
		if (worldObj.isRemote) return;
		String msg;
		if (item.getItemDamage() == 0) {
			ItemStack drop = new ItemStack(item.getItem(), 1, 1);
			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setInteger("lx", pos.getX());
			nbt.setInteger("ly", pos.getY());
			nbt.setInteger("lz", pos.getZ());
			nbt.setInteger("ld", dimensionId);
			drop.setTagCompound(nbt);
			EntityItem eitem = new EntityItem(worldObj, entity.posX, entity.posY, entity.posZ, drop);
			worldObj.spawnEntityInWorld(eitem);
			msg = TooltipInfo.format("msg.cd4017be.wireless0");
		} else if (item.hasTagCompound()) {
			NBTTagCompound nbt = item.getTagCompound();
			linkPos = new BlockPos(nbt.getInteger("lx"), nbt.getInteger("ly"), nbt.getInteger("lz"));
			linkDim = nbt.getInteger("ld");
			checkLink(true);
			msg = TooltipInfo.format(linkTile != null ? "msg.cd4017be.wireless1" : "msg.cd4017be.wireless2", linkDim, linkPos.getX(), linkPos.getY(), linkPos.getZ());
		} else return;
		if (entity instanceof EntityPlayer) ((EntityPlayer)entity).addChatMessage(new TextComponentString(msg));
	}

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing s, float X, float Y, float Z) {
		if (worldObj.isRemote || !player.isSneaking() || item != null) return true;
		checkLink(true);
		String msg;
		if (linkTile != null && linkTile.linkTile == this) {
			item = new ItemStack(getBlockType());
			linkTile.worldObj.setBlockToAir(linkTile.pos);
			worldObj.setBlockToAir(getPos());
			EntityItem eitem = new EntityItem(worldObj, player.posX, player.posY, player.posZ, item);
			worldObj.spawnEntityInWorld(eitem);
			msg = TooltipInfo.format("msg.cd4017be.wireless4");
		} else msg = TooltipInfo.format("msg.cd4017be.wireless5");
		player.addChatMessage(new TextComponentString(msg));
		return true;
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		ItemStack item = new ItemStack(state.getBlock(), 1, 1);
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setInteger("lx", linkPos.getX());
		nbt.setInteger("ly", linkPos.getY());
		nbt.setInteger("lz", linkPos.getZ());
		nbt.setInteger("ld", linkDim);
		item.setTagCompound(nbt);
		return Arrays.asList(item);
	}

}
