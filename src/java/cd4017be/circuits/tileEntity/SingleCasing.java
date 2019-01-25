package cd4017be.circuits.tileEntity;

import java.util.Arrays;
import java.util.function.IntConsumer;

import cd4017be.api.circuits.Chip;
import cd4017be.api.circuits.IChipItem;
import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.GlitchSaveSlot;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.block.AdvancedBlock.IRedstoneTile;
import cd4017be.lib.capability.AbstractInventory;
import cd4017be.lib.util.Utils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.Constants.NBT;

/**
 * @author CD4017BE
 *
 */
public class SingleCasing extends SyncronousRedstoneIO implements IDirectionalRedstone, IGuiData, IRedstoneTile {

	public static int PROPERTY_FLAGS = 0;

	private ItemStack inventory = ItemStack.EMPTY;
	private Chip chip = Chip.NULL_CHIP;
	/** indices: 0-5 rsOut, 6-11 chipIn, values: 0-5 rsIn, 6-11 chipOut */
	public final byte[] wiring = new byte[12];
	public final int[] rsOutput = new int[6];
	/** bits[0-5]: inputs, bits[8-13]: outputs */
	private int usedIO;
	private IntConsumer rsNotifier = Chip.NULL_OUTPUT;

	@Override
	protected void tick(int rsDirty) {
		if ((rsDirty & usedIO) != 0)
			rsNotifier.accept(rsDirty);
		Chip chip = this.chip;
		if (chip.dirty) chip.update();
	}

	public void setWiringDirty() {
		int chipIn = 0, usedIO = 0,
			ni = chip.inputs().length,
			no = chip.outputs().length;
		short[] cUpdates = new short[no];
		short[] rUpdates = new short[6];
		int n = 0;
		for (int i = 0; i < 6; i++) {
			int cfg = wiring[i];
			if (cfg < 0) continue;
			if (cfg < 6) {
				rUpdates[n++] = (short)(1 << cfg | cfg << 8 | i << 12);
				usedIO |= 1 << cfg | 0x100 << i;
			} else if ((cfg-=6) < no) {
				int u = cUpdates[cfg];
				if (u == 0) u = (short)(i << 8);
				cUpdates[cfg] = (short)(u | 1 << i);
				usedIO |= 0x100 << i;
			}
		}
		for (int i = 0; i < no; i++) {
			int u = cUpdates[i];
			if (u == 0) chip.connectOutput(i, Chip.NULL_OUTPUT);
			else {
				int s = u >> 8 & 7; u &= 0xcf;
				EnumFacing[] sides = new EnumFacing[Integer.bitCount(u)];
				for (int k = 0, j = 0; u != 0; u >>= 1, j++)
					if ((u & 1) != 0)
						sides[k++] = EnumFacing.VALUES[j];
				chip.connectOutput(i, (v)-> {
					if (v == rsOutput[s]) return;
					for (EnumFacing side : sides)
						Utils.updateRedstoneOnSide(this, v, side);
				});
			}
		}
		for (int i = 0; i < ni; i++) {
			int cfg = wiring[i + 6];
			if (cfg < 0 || cfg >= 6)
				chip.connectInput(i, Chip.NULL_INPUT);
			else {
				chipIn |= 1 << cfg;
				usedIO |= 1 << cfg;
				final int s = i;
				chip.connectInput(i, ()-> rsInput[s]);
			}
		}
		final short[] updates = Arrays.copyOf(rUpdates, n);
		final int chipU = chipIn;
		rsNotifier = (dirty)-> {
			if ((dirty & chipU) != 0) chip.dirty = true;
			for (short u : updates)
				if ((dirty & u) != 0) {
					int v = rsInput[u >> 8 & 7], s = u >> 12 & 7;
					rsOutput[s] = v;
					Utils.updateRedstoneOnSide(this, v, EnumFacing.VALUES[s]);
				}
		};
		int d = this.usedIO ^ usedIO;
		this.usedIO = usedIO;
		if (unloaded) return;
		for (int i = 0; i < 6; i++) {
			if ((usedIO >> i & 0x100) == 0)
				rsOutput[i] = 0;
			if ((d >> i & 0x101) != 0)
				world.neighborChanged(pos.offset(EnumFacing.VALUES[i]), blockType, pos);
		}
	}

	@Override
	public int redstoneLevel(EnumFacing side, boolean strong) {
		return strong ? 0 : rsOutput[side.ordinal()];
	}

	@Override
	public byte getRSDirection(EnumFacing s) {
		int i = s.ordinal(), j = usedIO;
		return (byte)(j >> i & 1 | j >> (i + 7) & 2);
	}

	@Override
	public boolean connectRedstone(EnumFacing side) {
		return getRSDirection(side) != 0;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		if (nbt.hasKey("chip", NBT.TAG_COMPOUND))
			inventory = new ItemStack(nbt.getCompoundTag("chip"));
		else inventory = ItemStack.EMPTY;
		{byte[] arr = nbt.getByteArray("wires");
		System.arraycopy(arr, 0, wiring, 0, Math.min(arr.length, wiring.length));}
		{int[] buff = nbt.getIntArray("rsOut");
		System.arraycopy(buff, 0, rsOutput, 0, Math.min(buff.length, rsOutput.length));}
		updateChipItem();
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		saveChip();
		if (!inventory.isEmpty())
			nbt.setTag("chip", inventory.writeToNBT(new NBTTagCompound()));
		nbt.setByteArray("wires", wiring);
		nbt.setIntArray("rsOut", rsOutput);
		return super.writeToNBT(nbt);
	}

	@Override
	protected void setupData() {
		chip.setHost(this);
		super.setupData();
	}

	@Override
	protected void clearData() {
		chip.setHost(null);
		super.clearData();
	}

	@Override
	public void initContainer(DataContainer container) {
		TileContainer cont = (TileContainer)container;
		Inventory inv = new Inventory();
		cont.addItemSlot(new GlitchSaveSlot(inv, 0, 8, 16, false));
		cont.addPlayerInventory(8, 86);
	}

	@Override
	public int[] getSyncVariables() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSyncVariable(int i, int v) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean detectAndSendChanges(DataContainer container, PacketBuffer dos) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void updateClientChanges(DataContainer container, PacketBuffer dis) {
		// TODO Auto-generated method stub

	}

	private void saveChip() {
		Item item = inventory.getItem();
		if (item instanceof IChipItem && chip != Chip.NULL_CHIP)
			((IChipItem)item).saveState(inventory, chip);
	}

	private void updateChipItem() {
		Item item = inventory.getItem();
		if (item instanceof IChipItem) {
			IChipItem cp = (IChipItem)item;
			if (cp.fitsInSocket(inventory, 6, 6, PROPERTY_FLAGS)) {
				chip = cp.provideChip(inventory);
				setWiringDirty();
				if (!unloaded) chip.setHost(SingleCasing.this);
				return;
			}
		}
		chip = Chip.NULL_CHIP;
		setWiringDirty();
	}

	private class Inventory extends AbstractInventory {

		@Override
		public void setStackInSlot(int slot, ItemStack stack) {
			inventory = stack;
			if (world.isRemote) return;
			updateChipItem();
		}

		@Override
		public int getSlots() {
			return 1;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return inventory;
		}

		@Override
		public int insertAm(int slot, ItemStack stack) {
			Item item = stack.getItem();
			return item instanceof IChipItem && ((IChipItem)item).fitsInSocket(stack, 6, 6, PROPERTY_FLAGS) ? 1 : 0;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			saveChip();
			return super.extractItem(slot, amount, simulate);
		}

	}

}
