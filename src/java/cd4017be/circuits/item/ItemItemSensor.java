package cd4017be.circuits.item;

import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import cd4017be.api.circuits.ISensor;
import cd4017be.circuits.gui.GuiItemSensor;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.DefaultItem;
import cd4017be.lib.IGuiItem;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.ItemGuiData;
import cd4017be.lib.Gui.SlotHolo;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.templates.InventoryItem;
import cd4017be.lib.templates.InventoryItem.IItemInventory;
import cd4017be.lib.util.Utils.ItemType;

public class ItemItemSensor extends DefaultItem implements ISensor, IGuiItem, IItemInventory {

	public ItemItemSensor(String id) {
		super(id);
	}

	@Override
	public void addInformation(ItemStack item, EntityPlayer player, List<String> list, boolean b) {
		if (item.hasTagCompound()) {
			String[] states = I18n.translateToLocal("gui.cd4017be.itemSensor.tip").split(",");
			byte mode = item.getTagCompound().getByte("mode");
			ItemStack stack = this.getStack(item, 0);
			if (states.length >= 6) {
				String s;
				if (stack == null) s = states[mode & 1];
				else {
					s = (mode & 1) != 0 ? states[2] : "";
					s += stack.getDisplayName();
					if ((mode & 2) != 0) s += states[3];
					if ((mode & 4) != 0) s += states[4];
					if ((mode & 8) != 0) s += states[5];
				}
				list.add(s);
			}
		}
		super.addInformation(item, player, list, b);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack item, World world, EntityPlayer player, EnumHand hand) {
		if (hand != EnumHand.MAIN_HAND) return new ActionResult<ItemStack>(EnumActionResult.PASS, item);
		BlockGuiHandler.openItemGui(player, world, 0, -1, 0);
		return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, item);
	}

	@Override
	public float measure(ItemStack sensor, World world, BlockPos pos, EnumFacing side) {
		if (!sensor.hasTagCompound() || !world.isBlockLoaded(pos)) return 0F;
		TileEntity te =  world.getTileEntity(pos);
		IItemHandler acc = te != null ? te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side) : null;
		ItemStack filter = this.getStack(sensor, 0);
		int mode = sensor.getTagCompound().getByte("mode");
		boolean inv = (mode & 1) != 0;
		ItemStack item;
		int n = 0;
		if (acc == null) {
			if (filter == null && !inv) return 0F;
			ItemType type = filter == null ? null : new ItemType((mode & 2) != 0, (mode & 4) != 0, (mode & 8) != 0, filter);
			for (EntityItem e : world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(pos))) {
				item = e.getEntityItem();
				if (type == null || (inv ^ type.matches(item))) n += item.stackSize;
			}
			IBlockState state = world.getBlockState(pos);
			for (ItemStack it : state.getBlock().getDrops(world, pos, state, 0))
				if (type == null || (inv ^ type.matches(it))) n += it.stackSize;
		} else if (filter == null) {
			for (int i = 0; i < acc.getSlots(); i++)
				if ((item = acc.getStackInSlot(i)) == null ^ inv)
					n += inv ? item.stackSize : 1;
		} else {
			ItemType type = new ItemType((mode & 2) != 0, (mode & 4) != 0, (mode & 8) != 0, filter);
			for (int i = 0; i < acc.getSlots(); i++) 
				if ((item = acc.getStackInSlot(i)) != null && (type.matches(item) ^ inv))
					n += item.stackSize;
		}
		return n;
	}

	@Override
	public Container getContainer(World world, EntityPlayer player, int x, int y, int z) {
		return new TileContainer(new GuiData(), player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiContainer getGui(World world, EntityPlayer player, int x, int y, int z) {
		return new GuiItemSensor(new TileContainer(new GuiData(), player));
	}

	@Override
	public void onPlayerCommand(ItemStack item, EntityPlayer player, PacketBuffer data) {
		if (!item.hasTagCompound()) item.setTagCompound(new NBTTagCompound());
		item.getTagCompound().setByte("mode", data.readByte());
	}

	@Override
	public int getSlots(ItemStack inv) {
		return 1;
	}

	@Override
	public ItemStack getStack(ItemStack inv, int slot) {
		return inv.hasTagCompound() && inv.getTagCompound().hasKey("type", 10) ? ItemStack.loadItemStackFromNBT(inv.getTagCompound().getCompoundTag("type")) : null;
	}

	@Override
	public void setStack(ItemStack inv, int slot, ItemStack stack) {
		if (!inv.hasTagCompound()) inv.setTagCompound(new NBTTagCompound());
		if (stack == null) inv.getTagCompound().removeTag("type");
		else inv.getTagCompound().setTag("type", stack.writeToNBT(new NBTTagCompound()));
	}

	class GuiData extends ItemGuiData {

		public GuiData() {super(ItemItemSensor.this);}

		@Override
		public void initContainer(DataContainer container) {
			TileContainer cont = (TileContainer)container;
			InventoryItem inv = new InventoryItem(cont.player);
			cont.addItemSlot(new SlotHolo(inv, 0, 44, 16, false, false));
			cont.addPlayerInventory(8, 50, false, true);
		}

	}

}
