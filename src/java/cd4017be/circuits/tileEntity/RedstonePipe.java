package cd4017be.circuits.tileEntity;

import java.util.List;

import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.api.circuits.IQuickRedstoneHandler;
import cd4017be.circuits.multiblock.ICableConnector;
import cd4017be.circuits.multiblock.RedstoneNetwork;
import cd4017be.circuits.multiblock.RedstoneNode;
import cd4017be.lib.block.AdvancedBlock.IRedstoneTile;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.block.MultipartBlock.IModularTile;
import cd4017be.lib.templates.Cover;
import cd4017be.lib.tileentity.PassiveNetworkTile;
import cd4017be.lib.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 *
 * @author CD4017BE
 */
public abstract class RedstonePipe extends PassiveNetworkTile<RedstoneNode, RedstoneNetwork, RedstonePipe> implements ITilePlaceHarvest, IRedstoneTile, IModularTile, IQuickRedstoneHandler, IDirectionalRedstone, ICableConnector {

	protected Cover cover = new Cover();
	protected int[] inputs;

	protected abstract void combineInputs();
	protected abstract void checkCon(EnumFacing side);

	protected void initInputCache() {
		inputs = new int[6];
		for (int i = 0; i < 6; i++)
			if ((comp.rsIO >> (i<<1) & 1) != 0) {
				EnumFacing s = EnumFacing.VALUES[i];
				inputs[i] = world.getRedstonePower(pos.offset(s), s);
			}
		combineInputs();
	}

	@Override
	public void onRedstoneStateChange(EnumFacing side, int value, TileEntity src) {
		int i = side.ordinal();
		if ((comp.rsIO >> (i<<1) & 1) == 0) return;
		if (inputs == null) initInputCache();
		else if (inputs[i] != value) {
			inputs[i] = value;
			combineInputs();
		}
	}

	@Override
	public void neighborBlockChange(Block b, BlockPos pos) {
		EnumFacing side = Utils.getSide(pos, this.pos);
		if (side != null) {
			checkCon(side);
			super.neighborBlockChange(b, pos);
			int i = side.ordinal(), val;
			if ((comp.rsIO >> (i<<1) & 1) == 0) return;
			if (inputs == null) initInputCache();
			else if (inputs[i] != (val = world.getRedstonePower(pos, side))) {
				inputs[i] = val;
				combineInputs();
			}
		} else if ((comp.rsIO & RedstoneNetwork.AllIn) != 0) initInputCache();
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
		for (EnumFacing side : EnumFacing.values())
			checkCon(side);
		if ((comp.rsIO & RedstoneNetwork.AllIn) != 0) initInputCache();
	}

	@Override
	public int redstoneLevel(EnumFacing side, boolean strong) {
		return !strong && (comp.rsIO >> (side.ordinal() << 1) & 2) != 0 && !comp.invalid() ? comp.network.outputState : 0;
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
		return te instanceof ICableConnector && ((ICableConnector)te).canConnect(dir.getOpposite(), comp.digital);
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
	public boolean canConnect(EnumFacing side, boolean digital) {
		return digital == comp.digital && comp.canConnect((byte)side.ordinal());
	}

}
