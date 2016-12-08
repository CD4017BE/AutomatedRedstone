package cd4017be.circuits.tileEntity;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.items.ItemHandlerHelper;
import cd4017be.circuits.Objects;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.SlotItemType;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.Gui.TileContainer.ISlotClickHandler;
import cd4017be.lib.templates.AutomatedTile;
import cd4017be.lib.templates.Inventory;
import cd4017be.lib.templates.Inventory.IAccessHandler;

/**
 *
 * @author CD4017BE
 */
public class Assembler extends AutomatedTile implements IAccessHandler, IGuiData, ISlotClickHandler {

	private static final Item circuit = Item.getItemFromBlock(Objects.circuit);
	public static final String[] tagNames = {"IO", "Cap", "Gate", "Calc"};
	public static final int[] maxCap = {192, 32, 255, 255};
	public static Item[] compItems = {Item.getItemFromBlock(Blocks.LEVER), Item.getItemFromBlock(Blocks.STONE_PRESSURE_PLATE), Items.REDSTONE, Items.QUARTZ};

	public Assembler() {
		inventory = new Inventory(8, 3, this).group(0, 0, 1, -1).group(1, 2, 3, 1).group(2, 3, 7, 0);
	}

	@Override
	public void update() {
		super.update();
		if (worldObj.isRemote) return;
		if (inventory.items[1] == null && inventory.items[0] != null && inventory.items[0].getItem() == circuit) inventory.items[1] = inventory.extractItem(0, 1, false);
		ItemStack item = inventory.items[1];
		if (item != null) {
			NBTTagCompound nbt = item.getTagCompound();
			boolean empty = true;
			if (nbt != null) {
				ItemStack stack;
				int n;
				for (int i = 0; i < 4; i++)
					if ((n = nbt.getByte(tagNames[i]) & 0xff) > 0) {
						n = (stack = inventory.insertItem(3 + i, new ItemStack(compItems[i], n), false)) == null ? 0 : stack.stackSize;
						nbt.setByte(tagNames[i], (byte)n);
						empty &= n == 0;
					}
			}
			if (empty) {
				if (inventory.items[2] == null) {
					inventory.items[2] = new ItemStack(circuit);
					inventory.items[1] = null;
				} else if (inventory.items[2].stackSize < 64 && inventory.items[2].getItem() == circuit) {
					inventory.items[2].stackSize++;
					inventory.items[1] = null;
				}
			}
		}
	}

	@Override
	protected void customPlayerCommand(byte cmd, PacketBuffer dis, EntityPlayerMP player) {
		if (cmd >= 4 || inventory.items[7] == null || !(inventory.items[7].getItem() == circuit)) return;
		NBTTagCompound nbt = inventory.items[7].getTagCompound();
		if (nbt == null) inventory.items[7].setTagCompound(nbt = new NBTTagCompound());
		int m = nbt.getByte(tagNames[cmd]) & 0xff;
		int n = Math.min(maxCap[cmd] - m, dis.readByte() & 0xff);
		if (n <= 0) return;
		ItemStack item = inventory.extractItem(3 + cmd, n, false);
		if (item != null) nbt.setByte(tagNames[cmd], (byte)(m + item.stackSize));
	}

	@Override
	public void initContainer(DataContainer container) {
		TileContainer c = (TileContainer)container;
		c.clickHandler = this;
		c.addItemSlot(new SlotItemType(inventory, 0, 8, 16, new ItemStack(circuit, 64)));
		c.addItemSlot(new SlotItemType(inventory, 2, 8, 52));
		c.addItemSlot(new SlotItemType(inventory, 3, 62, 16, new ItemStack(compItems[0], 64)));
		c.addItemSlot(new SlotItemType(inventory, 4, 44, 16, new ItemStack(compItems[1], 64)));
		c.addItemSlot(new SlotItemType(inventory, 5, 44, 52, new ItemStack(compItems[2], 64)));
		c.addItemSlot(new SlotItemType(inventory, 6, 62, 52, new ItemStack(compItems[3], 64)));
		c.addItemSlot(new SlotItemType(inventory, 7, 152, 34, new ItemStack(circuit, 1)));
		c.addPlayerInventory(8, 86);
	}

	@Override
	public int[] getSyncVariables() {
		if (inventory.items[7] == null || !inventory.items[7].hasTagCompound()) return new int[]{0, 0, 0, 0};
		NBTTagCompound nbt = inventory.items[7].getTagCompound();
		int[] ret = new int[4];
		for (int i = 0; i < ret.length; i++)
			ret[i] = nbt.getByte(tagNames[i]) & 0xff;
		return ret;
	}

	@Override
	public boolean transferStack(ItemStack item, int s, TileContainer container) {
		if (s < 7) return false;
		container.mergeItemStack(item, 2, 6, false);
		return true;
	}

	@Override
	public int insertAm(int g, int s, ItemStack item, ItemStack insert) {
		if (s >= 3 && s < 7 && insert.getItem() != compItems[s - 3]) return 0;
		int m = Math.min(insert.getMaxStackSize(), insert.stackSize); 
		return item == null ? m : item.stackSize < m && ItemHandlerHelper.canItemStacksStack(item, insert) ? m - item.stackSize : 0;
	}

	@Override
	public int extractAm(int g, int s, ItemStack item, int extract) {
		return item == null ? 0 : item.stackSize < extract ? item.stackSize : extract;
	}

	@Override
	public void setSlot(int g, int s, ItemStack item) {
		inventory.items[s] = item;
	}

}
