package cd4017be.circuits.item;

import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import cd4017be.api.circuits.ISensor;
import cd4017be.circuits.gui.GuiTimeSensor;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.DefaultItem;
import cd4017be.lib.IGuiItem;
import cd4017be.lib.TooltipInfo;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.ItemGuiData;

public class ItemTimeSensor extends DefaultItem implements ISensor, IGuiItem {

	private static double RangeSQ = 400D;

	public ItemTimeSensor(String id) {
		super(id);
	}

	@Override
	public void addInformation(ItemStack item, EntityPlayer player, List<String> list, boolean b) {
		if (item.hasTagCompound()) list.add(TooltipInfo.formatLink(BlockPos.fromLong(item.getTagCompound().getLong("link")), EnumFacing.getFront(item.getTagCompound().getByte("side"))));
		super.addInformation(item, player, list, b);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack item, World world, EntityPlayer player, EnumHand hand) {
		if (hand != EnumHand.MAIN_HAND) return new ActionResult<ItemStack>(EnumActionResult.PASS, item);
		BlockGuiHandler.openItemGui(player, world, 0, -1, 0);
		return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, item);
	}

	@Override
	public EnumActionResult onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
		if (!player.isSneaking()) return EnumActionResult.PASS;
		NBTTagCompound nbt = stack.getTagCompound();
		if (nbt == null) stack.setTagCompound(nbt = new NBTTagCompound());
		nbt.setLong("link", pos.toLong());
		nbt.setByte("side", (byte)side.ordinal());
		return EnumActionResult.SUCCESS;
	}

	@Override
	public double measure(ItemStack sensor, World world, BlockPos det) {
		if (!sensor.hasTagCompound()) return 0D;
		NBTTagCompound nbt = sensor.getTagCompound();
		BlockPos pos = BlockPos.fromLong(nbt.getLong("link"));
		EnumFacing side = EnumFacing.getFront(nbt.getByte("side"));
		long ref = nbt.getLong("ref");
		long interv = nbt.getLong("int");
		byte src = nbt.getByte("src");
		byte mode = nbt.getByte("mode");
		long time = this.getTime(src, world);
		if (mode == 1 && time >= ref + interv) nbt.setLong("ref", ref += interv);
		else if (mode > 1 && pos.distanceSq(det) < RangeSQ && world.getRedstonePower(pos, side.getOpposite()) > 0) {
			if (mode == 2) nbt.setLong("ref", ref = time);
			else {
				nbt.setLong("ref", ref += interv);
				nbt.setLong("int", time - ref);
			}
		}
		return (double)(mode == 3 ? interv : time - ref) / 1000D;
	}

	private long getTime(byte src, World world) {
		return src == 0 ? world.getTotalWorldTime() * 50L : src == 1 ? world.getWorldTime() * 50L : System.currentTimeMillis();
	}

	@Override
	public Container getContainer(World world, EntityPlayer player, int x, int y, int z) {
		return new DataContainer(new ItemGuiData(this), player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiContainer getGui(World world, EntityPlayer player, int x, int y, int z) {
		return new GuiTimeSensor(new DataContainer(new ItemGuiData(this), player));
	}

	@Override
	public void onPlayerCommand(ItemStack item, EntityPlayer player, PacketBuffer data) {
		if (!item.hasTagCompound()) item.setTagCompound(new NBTTagCompound());
		byte cmd = data.readByte();
		if (cmd == 0) {
			byte src = data.readByte();
			item.getTagCompound().setByte("src", src);
			item.getTagCompound().setLong("ref", this.getTime(src, player.worldObj));
		}
		else if (cmd == 1) item.getTagCompound().setByte("mode", data.readByte());
		else if (cmd == 2) item.getTagCompound().setLong("ref", data.readLong());
		else if (cmd == 3) item.getTagCompound().setLong("int", data.readLong());
	}

}
