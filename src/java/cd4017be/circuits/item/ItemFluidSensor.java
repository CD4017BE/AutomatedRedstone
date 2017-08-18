package cd4017be.circuits.item;

import java.io.IOException;
import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import cd4017be.api.circuits.ItemBlockSensor;
import cd4017be.circuits.gui.GuiFluidSensor;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.BlockGuiHandler.ClientItemPacketReceiver;
import cd4017be.lib.IGuiItem;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.ItemGuiData;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.Gui.TileContainer.TankSlot;
import cd4017be.lib.templates.ITankContainer;

public class ItemFluidSensor extends ItemBlockSensor implements IGuiItem, ClientItemPacketReceiver {

	public ItemFluidSensor(String id) {
		super(id, 20F);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		ItemStack item = player.getHeldItem(hand);
		if (hand != EnumHand.MAIN_HAND) return new ActionResult<ItemStack>(EnumActionResult.PASS, item);
		BlockGuiHandler.openItemGui(player, hand);
		return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, item);
	}

	@Override
	public void addInformation(ItemStack item, EntityPlayer player, List<String> list, boolean b) {
		if (item.hasTagCompound()) {
			String[] states = I18n.translateToLocal("gui.cd4017be.fluidSensor.tip").split(",");
			boolean inv = item.getTagCompound().getBoolean("inv");
			Fluid fluid = this.getFluid(item);
			if (states.length >= 3) {
				String s;
				if (fluid == null) s = states[inv ? 1 : 0];
				else s = (inv ? states[2] : "") + fluid.getLocalizedName(new FluidStack(fluid, 0));
				list.add(s);
			}
		}
		super.addInformation(item, player, list, b);
	}

	private Fluid getFluid(ItemStack inv) {
		return inv.hasTagCompound() ? FluidRegistry.getFluid(inv.getTagCompound().getString("type")) : null;
	}

	@Override
	public Container getContainer(ItemStack item, EntityPlayer player, World world, BlockPos pos, int slot) {
		return new TileContainer(new GuiData(), player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiContainer getGui(ItemStack item, EntityPlayer player, World world, BlockPos pos, int slot) {
		return new GuiFluidSensor(new TileContainer(new GuiData(), player));
	}

	@Override
	public void onPacketFromClient(PacketBuffer data, EntityPlayer sender, ItemStack item, int slot) throws IOException {
		if (!item.hasTagCompound()) item.setTagCompound(new NBTTagCompound());
		byte cmd = data.readByte();
		if (cmd == 0) item.getTagCompound().setBoolean("inv", !item.getTagCompound().getBoolean("inv"));
		else if (cmd == 1) item.getTagCompound().setString("type", data.readString(32));
	}

	class GuiData extends ItemGuiData implements ITankContainer {

		private InventoryPlayer player;
		public GuiData() {super(ItemFluidSensor.this);}

		@Override
		public void initContainer(DataContainer container) {
			TileContainer cont = (TileContainer)container;
			cont.addTankSlot(new TankSlot(this, 0, 62, 16, (byte)0x11));
			cont.addPlayerInventory(8, 50, false, true);
			player = cont.player.inventory;
		}

		@Override
		public int getTanks() {return 1;}

		@Override
		public FluidStack getTank(int i) {
			ItemStack item = player.mainInventory.get(player.currentItem);
			Fluid fluid = item != null ? getFluid(item) : null;
			return fluid != null ? new FluidStack(fluid, 0) : null;
		}

		@Override
		public int getCapacity(int i) {return 0;}

		@Override
		public void setTank(int i, FluidStack fluid) {}

	}

	@Override
	protected float measure(ItemStack sensor, NBTTagCompound nbt, World world, BlockPos pos, EnumFacing side) {
		IFluidHandler acc = FluidUtil.getFluidHandler(world, pos, side);
		if (acc == null) return 0F;
		Fluid filter = this.getFluid(sensor);
		boolean inv = nbt.getBoolean("inv");
		int n = 0;
		for (IFluidTankProperties prop : acc.getTankProperties()) {
			FluidStack fluid = prop.getContents();
			if (!inv && filter == null) n += prop.getCapacity() - (fluid != null ? fluid.amount : 0);
			else if (fluid != null && (inv ^ fluid.getFluid() == filter)) n += fluid.amount;
		}
		return n;
	}

}
