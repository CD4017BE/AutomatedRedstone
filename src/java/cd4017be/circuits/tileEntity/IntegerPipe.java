package cd4017be.circuits.tileEntity;

import java.util.List;

import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.api.circuits.IQuickRedstoneHandler;
import cd4017be.lib.block.AdvancedBlock.IInteractiveTile;
import cd4017be.lib.block.AdvancedBlock.IRedstoneTile;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.block.MultipartBlock.IModularTile;
import cd4017be.lib.tileentity.PassiveMultiblockTile;
import cd4017be.lib.util.Utils;
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
public class IntegerPipe extends PassiveMultiblockTile<IntegerComp, SharedInteger> implements ITilePlaceHarvest, IRedstoneTile, IInteractiveTile, IModularTile, IQuickRedstoneHandler {

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
		if (item.isEmpty()) {
			if (world.isRemote) return true;
			dir = Utils.hitSide(X, Y, Z);
			byte s = (byte)dir.getIndex();
			if (player.isSneaking()) {
				boolean con = !comp.canConnect(s);
				comp.setConnect(s, con);
				ICapabilityProvider te = this.getTileOnSide(dir);
				if (te != null && te instanceof IntegerPipe) {
					IntegerPipe pipe = (IntegerPipe)te;
					pipe.comp.setConnect((byte)(s^1), con);
					pipe.markUpdate();
				}
			} else {
				s *= 2;
				comp.network.setIO(comp, (short)((comp.rsIO & ~(3 << s)) | ((comp.rsIO >> s) + 1 & 3) << s));
			}
			this.markUpdate();
			return true;
		} else return false;
	}

	@Override
	public void onClicked(EntityPlayer player) {
	}

	@Override
	public int redstoneLevel(EnumFacing side, boolean strong) {
		return !strong && (comp.rsIO >> (side.ordinal() * 2) & 2) != 0 ? convertSignal(comp.network.outputState) : 0;
	}

	@Override
	public boolean connectRedstone(EnumFacing side) {
		return (comp.rsIO >> (side.ordinal() * 2) & 3) != 0;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		comp.writeToNBT(nbt);
		return super.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		comp.readFromNBT(nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		comp.readFromNBT(pkt.getNbtCompound());
		this.markUpdate();
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		comp.writeToNBT(nbt);
		return new SPacketUpdateTileEntity(pos, -1, nbt);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getModuleState(int m) {
		int c = comp.rsIO >> (m * 2) & 3;
		return (T)Byte.valueOf(c != 0 ? (byte)c : isModulePresent(m) ? 0 : (byte)-1);
	}

	@Override
	public boolean isModulePresent(int m) {
		if ((comp.rsIO >> (m * 2) & 3) != 0) return true;
		if (!comp.canConnect((byte)m)) return false;
		EnumFacing dir = EnumFacing.VALUES[m];
		ICapabilityProvider te = getTileOnSide(dir);
		return te != null && te.hasCapability(comp.getCap(), dir.getOpposite());
	}

	@Override
	public List<ItemStack> dropItem(IBlockState state, int fortune) {
		return makeDefaultDrops(null);
	}

}
