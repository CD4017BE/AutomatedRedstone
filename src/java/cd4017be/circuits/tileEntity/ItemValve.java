package cd4017be.circuits.tileEntity;

import java.io.IOException;

import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.lib.BlockGuiHandler.ClientPacketReceiver;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.block.AdvancedBlock.INeighborAwareTile;
import cd4017be.lib.block.AdvancedBlock.IRedstoneTile;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public class ItemValve extends BaseTileEntity implements INeighborAwareTile, IRedstoneTile, ITickable, IDirectionalRedstone, IGuiData, ClientPacketReceiver {

	private Inventory inventory = new AllSlots();
	private TileEntity target;
	public int tickInt = 1;
	public boolean update = true;
	/**bit0: output signal, bit1: sel slot, bits[2,3]: mode{unlimited, flow, packet} bit4: 16-bit amount*/
	public byte mode;
	public int flow, limit, output, input;

	@Override
	public void update() {
		if (world.isRemote) return;
		if (update) {
			EnumFacing dir = getOrientation().front;
			target = Utils.neighborTile(this, dir);
			update = false;
		}
		if (world.getTotalWorldTime() % tickInt != 0) return;
		switch (mode & 12) {
		case 0:
			emitRS(flow);
			flow = 0;
			limit = 0;
			break;
		case 4:
			emitRS(flow + limit);
			limit = (mode & 12) != 0 ? readAm() : 0;
			flow = -limit;
			break;
		default:
			emitRS(flow == 0 ? 0 : flow < 0 ? -1 : 1);
		}
		markDirty();
	}

	private int readAm() {
		int n = (mode & 2) != 0 ? input >> 8 : input;
		return (mode & 16) == 0 ? (byte)n : (short)n;
	}

	private void emitRS(int nstate) {
		if ((mode & 1) == 0) return;
		nstate &= (mode & 16) == 0 ? 0xff : 0xffff;
		if (nstate != output) {
			output = nstate;
			world.notifyNeighborsOfStateChange(pos, Blocks.REDSTONE_TORCH, false);
		}
	}

	@Override
	public void neighborBlockChange(Block b, BlockPos src) {
		if (world.isRemote || (mode & 14) == 0) return;
		int li = input;
		input = 0;
		for (EnumFacing s : EnumFacing.VALUES)
			input |= world.getRedstonePower(pos.offset(s), s);
		if (input == li) return;
		if ((mode & 8) != 0) {
			int n = readAm();
			if (n != 0) {
				limit = n;
				flow = -limit;//TODO time sync
			}
		}
		if ((mode & 2) != 0) {
			inventory.setSlot(input & 0xff);
		}
		markDirty();
	}

	@Override
	public void neighborTileChange(BlockPos src) {
		update = true;
	}

	@Override
	public int redstoneLevel(EnumFacing side, boolean strong) {
		return strong || (mode & 1) == 0 ? 0 : output;
	}

	@Override
	public boolean connectRedstone(EnumFacing side) {
		return getRSDirection(side) != 0;
	}

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
		return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing != getOrientation().front;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> cap, EnumFacing facing) {
		if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing != getOrientation().front) {
			if (target != null && target.isInvalid()) {target = null; update = true;}
			inventory.setItemHandler(target);
			return (T) inventory;
		} else return null;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		flow = nbt.getInteger("flow");
		output = nbt.getInteger("state");
		input = nbt.getInteger("in");
		tickInt = nbt.getInteger("tickInt");
		mode = nbt.getByte("mode");
		inventory = (mode & 2) == 0 ? new AllSlots() : new SingleSlot();
		update = true;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setInteger("flow", flow);
		nbt.setInteger("out", output);
		nbt.setInteger("in", input);
		nbt.setInteger("tickInt", tickInt);
		nbt.setByte("mode", mode);
		return super.writeToNBT(nbt);
	}

	@Override
	public void onPacketFromClient(PacketBuffer data, EntityPlayer sender) throws IOException {
		byte cmd = data.readByte();
		switch(cmd) {
		case 0:
			tickInt = data.readInt();
			if (tickInt < 1) tickInt = 1;
			else if (tickInt > 1200) tickInt = 1200;
			break;
		case 1: case 2:
			mode = (byte) (mode & 0x13 | (((mode >> 2 & 3) + cmd) % 3) << 2);
			flow = 0;
			break;
		case 3:
			mode ^= 2;
			inventory = (mode & 2) == 0 ? new AllSlots() : new SingleSlot();
			break;
		case 4:
			mode ^= 16;
			break;
		case 5:
			mode ^= 1;
			emitRS(output);
			break;
		}
		markDirty();
	}

	@Override
	public int[] getSyncVariables() {
		return new int[]{flow, limit, tickInt, mode};
	}

	@Override
	public void setSyncVariable(int i, int v) {
		switch(i) {
		case 0: flow = v; break;
		case 1: limit = v; break;
		case 2: tickInt = v; break;
		case 3: mode = (byte)v; break;
		}
	}

	@Override
	public byte getRSDirection(EnumFacing s) {
		return (byte)((mode & 1) << 1 | ((mode & 14) != 0 ? 1 : 0));
	}

	@Override
	public void initContainer(DataContainer container) {
	}

	@Override
	public boolean detectAndSendChanges(DataContainer container, PacketBuffer dos) {
		return false;
	}

	@Override
	public void updateClientChanges(DataContainer container, PacketBuffer dis) {
	}

	abstract class Inventory implements IItemHandler {

		IItemHandler src;
		/**prevents dangerous recursive usage of this device */
		boolean inRecursion;

		public void setItemHandler(TileEntity src) {
			this.src = src == null ? null : src.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, getOrientation().front.getOpposite());
			this.inRecursion = src == null;
		}

		public void setSlot(int slot) {}

	}

	class AllSlots extends Inventory {

		@Override
		public int getSlots() {
			if (inRecursion || ((mode & 12) != 0 && flow == 0)) return 0;
			else try {
				inRecursion = true;
				return src.getSlots();
			} finally {inRecursion = false;}
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			if (inRecursion) return ItemStack.EMPTY;
			else try {
				inRecursion = true;
				return src.getStackInSlot(slot);
			} finally {inRecursion = false;}
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (inRecursion) return stack;
			else try {
				inRecursion = true;
				if ((mode & 12) != 0) {
					if (flow >= 0) return stack;
					int n = stack.getCount();
					if (-flow < n) {
						n += flow;
						stack = stack.copy();
						stack.setCount(-flow);
						int ret = src.insertItem(slot, stack, simulate).getCount();
						if (!simulate) {
							flow += stack.getCount() - ret;
							markDirty();
						}
						return ItemHandlerHelper.copyStackWithSize(stack, ret + n);
					}
				}
				ItemStack ret = src.insertItem(slot, stack, simulate);
				if (!simulate) {
					flow += stack.getCount() - ret.getCount();
					markDirty();
				}
				return ret;
			} finally {inRecursion = false;}
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			if (inRecursion) return ItemStack.EMPTY;
			else try {
				inRecursion = true;
				if ((mode & 12) != 0) {
					if (flow <= 0) return ItemStack.EMPTY;
					if (flow < amount) amount = flow;
				}
				ItemStack ret = src.extractItem(slot, amount, simulate);
				if (!simulate) {
					flow -= ret.getCount();
					markDirty();
				}
				return ret;
			} finally {inRecursion = false;}
		}

		@Override
		public int getSlotLimit(int slot) {
			if (inRecursion) return 0;
			else try {
				inRecursion = true;
				return src.getSlotLimit(slot);
			} finally {inRecursion = false;}
		}

	}

	class SingleSlot extends Inventory {

		int slot;

		@Override
		public void setSlot(int slot) {
			this.slot = slot & 0xff;
		}

		@Override
		public int getSlots() {
			return (mode & 12) != 0 && flow == 0 ? 0 : 1;
		}

		@Override
		public ItemStack getStackInSlot(int s) {
			if (inRecursion || slot >= src.getSlots()) return ItemStack.EMPTY;
			else try {
				inRecursion = true;
				return src.getStackInSlot(slot);
			} finally {inRecursion = false;}
		}

		@Override
		public ItemStack insertItem(int s, ItemStack stack, boolean simulate) {
			if (inRecursion || slot >= src.getSlots()) return stack;
			else try {
				inRecursion = true;
				if ((mode & 12) != 0) {
					if (flow >= 0) return stack;
					int n = stack.getCount();
					if (-flow < n) {
						n += flow;
						stack = stack.copy();
						stack.setCount(-flow);
						int ret = src.insertItem(slot, stack, simulate).getCount();
						if (!simulate) {
							flow += stack.getCount() - ret;
							markDirty();
						}
						return ItemHandlerHelper.copyStackWithSize(stack, ret + n);
					}
				}
				ItemStack ret = src.insertItem(slot, stack, simulate);
				if (!simulate) {
					flow += stack.getCount() - ret.getCount();
					markDirty();
				}
				return ret;
			} finally {inRecursion = false;}
		}

		@Override
		public ItemStack extractItem(int s, int amount, boolean simulate) {
			if (inRecursion || slot >= src.getSlots()) return ItemStack.EMPTY;
			else try {
				inRecursion = true;
				if ((mode & 12) != 0) {
					if (flow <= 0) return ItemStack.EMPTY;
					if (flow < amount) amount = flow;
				}
				ItemStack ret = src.extractItem(slot, amount, simulate);
				if (!simulate) {
					flow -= ret.getCount();
					markDirty();
				}
				return ret;
			} finally {inRecursion = false;}
		}

		@Override
		public int getSlotLimit(int s) {
			if (inRecursion || slot >= src.getSlots()) return 0;
			else try {
				inRecursion = true;
				return src.getSlotLimit(slot);
			} finally {inRecursion = false;}
		}

	}

}
