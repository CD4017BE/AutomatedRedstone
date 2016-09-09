package cd4017be.circuits.item;

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
import cd4017be.api.circuits.ISensor;
import cd4017be.circuits.gui.GuiTimeSensor;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.DefaultItem;
import cd4017be.lib.IGuiItem;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.ItemGuiData;

public class ItemTimeSensor extends DefaultItem implements ISensor, IGuiItem {

	public ItemTimeSensor(String id) {
		super(id);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack item, World world, EntityPlayer player, EnumHand hand) {
		if (hand != EnumHand.MAIN_HAND) return new ActionResult<ItemStack>(EnumActionResult.PASS, item);
		BlockGuiHandler.openItemGui(player, world, 0, -1, 0);
		return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, item);
	}

	@Override
	public float measure(ItemStack sensor, World world, BlockPos pos, EnumFacing side) {
		if (sensor.stackTagCompound == null) return 0F;
		long ref = sensor.stackTagCompound.getLong("ref");
		long interv = sensor.stackTagCompound.getLong("int");
		byte src = sensor.stackTagCompound.getByte("src");
		byte mode = sensor.stackTagCompound.getByte("mode");
		long time = this.getTime(src, world);
		if (mode == 1 && time >= ref + interv) sensor.stackTagCompound.setLong("ref", ref += interv);
		else if (mode > 1 && world.getRedstonePower(pos, side.getOpposite()) > 0) {
			if (mode == 2) sensor.stackTagCompound.setLong("ref", ref = time);
			else {
				sensor.stackTagCompound.setLong("ref", ref += interv);
				sensor.stackTagCompound.setLong("int", time - ref);
			}
		}
		return (float)(mode == 3 ? interv : time - ref) / 1000F;
	}

	private long getTime(byte src, World world) {
		return src == 0 ? world.getTotalWorldTime() * 50L : src == 1 ? world.getWorldTime() * 50L : System.currentTimeMillis();
	}

	@Override
	public Container getContainer(World world, EntityPlayer player, int x, int y, int z) {
		return new DataContainer(new ItemGuiData(this), player);
	}

	@Override
	public GuiContainer getGui(World world, EntityPlayer player, int x, int y, int z) {
		return new GuiTimeSensor(new DataContainer(new ItemGuiData(this), player));
	}

	@Override
	public void onPlayerCommand(ItemStack item, EntityPlayer player, PacketBuffer data) {
		if (item.stackTagCompound == null) item.stackTagCompound = new NBTTagCompound();
		byte cmd = data.readByte();
		if (cmd == 0) {
			byte src = data.readByte();
			item.stackTagCompound.setByte("src", src);
			item.stackTagCompound.setLong("ref", this.getTime(src, player.worldObj));
		}
		else if (cmd == 1) item.stackTagCompound.setByte("mode", data.readByte());
		else if (cmd == 2) item.stackTagCompound.setLong("ref", data.readLong());
		else if (cmd == 3) item.stackTagCompound.setLong("int", data.readLong());
	}

}
