package cd4017be.circuits.item;

import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import java.io.IOException;
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
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import cd4017be.api.circuits.ItemBlockSensor;
import cd4017be.circuits.gui.GuiItemSensor;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.BlockGuiHandler.ClientItemPacketReceiver;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.IGuiItem;
import cd4017be.lib.Gui.ItemGuiData;
import cd4017be.lib.Gui.SlotHolo;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.capability.InventoryItem;
import cd4017be.lib.capability.InventoryItem.IItemInventory;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.lib.util.Utils.ItemType;

public class ItemItemSensor extends ItemBlockSensor implements IGuiItem, IItemInventory, ClientItemPacketReceiver {

	public ItemItemSensor(String id) {
		super(id, 20F);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack item, @Nullable World player, List<String> list, ITooltipFlag b) {
		if (item.hasTagCompound()) {
			String[] states = TooltipUtil.translate("gui.cd4017be.itemSensor.tip").split(",");
			byte mode = item.getTagCompound().getByte("mode");
			ItemStack stack = this.loadInventory(item, null)[0];
			if (states.length >= 6) {
				String s;
				if (stack.isEmpty()) s = states[mode & 1];
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
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		ItemStack item = player.getHeldItem(hand);
		if (hand != EnumHand.MAIN_HAND) return new ActionResult<ItemStack>(EnumActionResult.PASS, item);
		BlockGuiHandler.openItemGui(player, hand);
		return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, item);
	}

	@Override
	public Container getContainer(ItemStack item, EntityPlayer player, World world, BlockPos pos, int slot) {
		return new TileContainer(new GuiData(), player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiContainer getGui(ItemStack item, EntityPlayer player, World world, BlockPos pos, int slot) {
		return new GuiItemSensor(new TileContainer(new GuiData(), player));
	}

	@Override
	public void onPacketFromClient(PacketBuffer data, EntityPlayer sender, ItemStack item, int slot) throws IOException {
		if (!item.hasTagCompound()) item.setTagCompound(new NBTTagCompound());
		item.getTagCompound().setByte("mode", data.readByte());
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

	@Override
	public ItemStack[] loadInventory(ItemStack inv, EntityPlayer player) {
		ItemStack[] items = new ItemStack[1];
		if (inv.hasTagCompound() && inv.getTagCompound().hasKey("type", 10)) items[0] = new ItemStack(inv.getTagCompound().getCompoundTag("type"));
		else items[0] = ItemStack.EMPTY;
		return items;
	}

	@Override
	public void saveInventory(ItemStack inv, EntityPlayer player, ItemStack[] items) {
		if (!inv.hasTagCompound()) inv.setTagCompound(new NBTTagCompound());
		if (items[0].isEmpty()) inv.getTagCompound().removeTag("type");
		else inv.getTagCompound().setTag("type", items[0].writeToNBT(new NBTTagCompound()));
	}

	@Override
	protected float measure(ItemStack sensor, NBTTagCompound nbt, World world, BlockPos pos, EnumFacing side) {
		int mode = nbt.getByte("mode");
		TileEntity te =  world.getTileEntity(pos);
		IItemHandler acc = te != null ? te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side) : null;
		ItemStack filter = loadInventory(sensor, null)[0];
		boolean inv = (mode & 1) != 0;
		boolean empty = filter.isEmpty();
		ItemStack item;
		int n = 0;
		if (acc == null) {
			if (empty && !inv) return 0F;
			ItemType type = empty ? null : new ItemType((mode & 2) != 0, (mode & 4) != 0, (mode & 8) != 0, filter);
			for (EntityItem e : world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(pos))) {
				item = e.getItem();
				if (type == null || (inv ^ type.matches(item))) n += item.getCount();
			}
			IBlockState state = world.getBlockState(pos);
			for (ItemStack it : state.getBlock().getDrops(world, pos, state, 0))
				if (type == null || (inv ^ type.matches(it))) n += it.getCount();
		} else if (empty) {
			int m;
			for (int i = 0; i < acc.getSlots(); i++)
				if ((m = acc.getStackInSlot(i).getCount()) == 0 ^ inv)
					n += inv ? m : 1;
		} else {
			ItemType type = new ItemType((mode & 2) != 0, (mode & 4) != 0, (mode & 8) != 0, filter);
			int m;
			for (int i = 0; i < acc.getSlots(); i++) 
				if ((m = (item = acc.getStackInSlot(i)).getCount()) != 0 && (type.matches(item) ^ inv))
					n += m;
		}
		return n;
	}

}
