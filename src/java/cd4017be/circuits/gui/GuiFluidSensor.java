package cd4017be.circuits.gui;

import java.io.IOException;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.GuiMachine;
import cd4017be.lib.Gui.TileContainer;

public class GuiFluidSensor extends GuiMachine {

	private final InventoryPlayer inv;

	public GuiFluidSensor(TileContainer cont) {
		super(cont);
		this.MAIN_TEX = new ResourceLocation("circuits", "textures/gui/fluid_sensor.png");
		this.inv = cont.player.inventory;
	}

	@Override
	public void initGui() {
		this.xSize = 176;
		this.ySize = 132;
		super.initGui();
		guiComps.add(new Button(1, 97, 15, 18, 18, 0).texture(176, 0).setTooltip("itemSensor.neg"));
	}

	@Override
	protected void mouseClicked(int x, int y, int b) throws IOException {
		if (this.isPointInRegion(62, 16, 16, 16, x, y)) {
			PacketBuffer dos = BlockGuiHandler.getPacketForItem(inv.currentItem);
			FluidStack fluid = FluidUtil.getFluidContained(inv.getItemStack());
			dos.writeByte(1);
			dos.writeString(fluid != null ? fluid.getFluid().getName() : "");
			BlockGuiHandler.sendPacketToServer(dos);
		} else super.mouseClicked(x, y, b);
	}

	@Override
	protected Object getDisplVar(int id) {
		ItemStack item = inv.mainInventory.get(inv.currentItem);
		return item.hasTagCompound() && item.getTagCompound().getBoolean("inv") ? 1 : 0;
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer dos = BlockGuiHandler.getPacketForItem(inv.currentItem);
		dos.writeByte(0);
		BlockGuiHandler.sendPacketToServer(dos);
	}

}
