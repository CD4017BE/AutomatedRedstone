package cd4017be.circuits.tileEntity;

import java.util.List;

import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.api.circuits.IQuickRedstoneHandler;
import cd4017be.lib.block.AdvancedBlock.IInteractiveTile;
import cd4017be.lib.block.AdvancedBlock.IRedstoneTile;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.block.MultipartBlock.IModularTile;
import cd4017be.lib.templates.Cover;
import cd4017be.lib.tileentity.PassiveMultiblockTile;
import cd4017be.lib.util.Utils;
import multiblock.ICableConnector;
import multiblock.IntegerComp;
import multiblock.SharedInteger;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

/**
 *
 * @author CD4017BE
 */
public class IntegerPipe extends PassiveMultiblockTile<IntegerComp, SharedInteger> implements ITilePlaceHarvest, IRedstoneTile, IInteractiveTile, IModularTile, IQuickRedstoneHandler, IDirectionalRedstone, ICableConnector {

	protected Cover cover = new Cover();

	public IntegerPipe() {
		comp = new IntegerComp(this, bitSize());
		new SharedInteger(comp);
	}

	protected int bitSize() {return 32;}

	protected int convertSignal(int s) {
		return (s & ~comp.mask) == 0 ? s : s < 0 ? 0 : comp.mask;
	}

	protected void updateInput() {
		int newIn = 0;
		for (byte i = 0; i < 6; i++)
			if ((comp.rsIO >> (i * 2) & 1) != 0) {
				EnumFacing s = EnumFacing.VALUES[i];
				newIn |= world.getRedstonePower(pos.offset(s), s);
			}
		newIn = convertSignal(newIn);
		if (newIn != comp.inputState) {
			comp.inputState = newIn;
			comp.network.markStateDirty();
			markDirty();
		}
	}

	@Override
	public void onRedstoneStateChange(EnumFacing side, int value, TileEntity src) {
		updateInput();
	}

	@Override
	public void neighborBlockChange(Block b, BlockPos pos) {
		if (b != Blocks.REDSTONE_TORCH) checkCons();
		updateInput();
		super.neighborBlockChange(b, pos);
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
		checkCons();
	}

	protected void checkCons() {
		ICapabilityProvider te;
		byte d;
		short io = comp.rsIO;
		for (EnumFacing s : EnumFacing.VALUES)
			if ((te = this.getTileOnSide(s)) != null && te instanceof IDirectionalRedstone && (d = ((IDirectionalRedstone)te).getRSDirection(s.getOpposite())) != 0)
				io |= ((d & 1) << 1 | (d & 2) >> 1) << (s.ordinal() * 2);
		if (io != comp.rsIO) {
			comp.network.setIO(comp, io);
			this.markUpdate();
		}
	}

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing dir, float X, float Y, float Z) {
		if (world.isRemote) return true;
		if (cover.interact(this, player, hand, item, dir, X, Y, Z)) return true;
		if (item.isEmpty()) {
			dir = Utils.hitSide(X, Y, Z);
			byte s = (byte)dir.getIndex();
			if (player.isSneaking()) {
				boolean con = !comp.canConnect(s);
				ICapabilityProvider te = Utils.neighborTile(this, dir);
				if (te != null && te instanceof IntegerPipe) {
					IntegerPipe pipe = (IntegerPipe)te;
					if (pipe.getRSDirection(dir.getOpposite()) != 0) con = false;
					else {
						pipe.comp.setConnect((byte)(s^1), con);
						pipe.markUpdate();
						pipe.markDirty();
					}
				}
				comp.setConnect(s, con);
			} else {
				setIO(dir, (comp.rsIO >> (s<<1)) + 1);
			}
			markUpdate();
			markDirty();
			return true;
		} else return false;
	}

	public void setIO(EnumFacing side, int io) {
		io &= 3;
		int s = side.getIndex() << 1;
		comp.network.setIO(comp, (short)((comp.rsIO & ~(3 << s)) | io << s));
		ICapabilityProvider te = Utils.neighborTile(this, side);
		if (te != null && te instanceof IntegerPipe) {
			IntegerPipe pipe = (IntegerPipe)te;
			pipe.comp.setConnect((byte)(side.ordinal()^1), false);
			io = (io << 1 | io >> 1) & 3;
			s ^= 2;
			pipe.comp.network.setIO(pipe.comp, (short)((pipe.comp.rsIO & ~(3 << s)) | io << s));
			pipe.markUpdate();
			pipe.markDirty();
		}
	}

	@Override
	public void onClicked(EntityPlayer player) {
		if (!world.isRemote) cover.hit(this, player);
	}

	@Override
	public int redstoneLevel(EnumFacing side, boolean strong) {
		return !strong && (comp.rsIO >> (side.ordinal() * 2) & 2) != 0 && !comp.invalid() ? convertSignal(comp.network.outputState) : 0;
	}

	@Override
	public boolean connectRedstone(EnumFacing side) {
		return (comp.rsIO >> (side.ordinal() * 2) & 3) != 0;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		comp.writeToNBT(nbt);
		cover.writeNBT(nbt, "cover", false);
		return super.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		comp.readFromNBT(nbt);
		cover.readNBT(nbt, "cover", null);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		comp.readFromNBT(pkt.getNbtCompound());
		cover.readNBT(pkt.getNbtCompound(), "cv", this);
		this.markUpdate();
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		comp.writeToNBT(nbt);
		cover.writeNBT(nbt, "cv", true);
		return new SPacketUpdateTileEntity(pos, -1, nbt);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getModuleState(int m) {
		if (m == 6) return cover.module();
		int c = comp.rsIO >> (m * 2) & 3;
		return (T)Byte.valueOf(c != 0 ? (byte)c : isModulePresent(m) ? 0 : (byte)-1);
	}

	@Override
	public boolean isModulePresent(int m) {
		if (m == 6) return cover.state != null;
		if ((comp.rsIO >> (m * 2) & 3) != 0) return true;
		if (!comp.canConnect((byte)m)) return false;
		EnumFacing dir = EnumFacing.VALUES[m];
		TileEntity te = Utils.neighborTile(this, dir);
		return te instanceof ICableConnector && ((ICableConnector)te).canConnect(dir.getOpposite(), comp.mask);
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		List<ItemStack> list = makeDefaultDrops(null);
		if (cover.stack != null) list.add(cover.stack);
		return list;
	}

	@Override
	public byte getRSDirection(EnumFacing s) {
		return (byte)(comp.rsIO >> (s.getIndex() << 1) & 3);
	}

	@Override
	public boolean canConnect(EnumFacing side, int mask) {
		return (mask == comp.mask) && comp.canConnect((byte)side.ordinal());
	}

}
