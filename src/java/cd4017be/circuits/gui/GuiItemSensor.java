package cd4017be.circuits.gui;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.AdvancedGui;
import cd4017be.lib.Gui.TileContainer;

public class GuiItemSensor extends AdvancedGui {

	private final InventoryPlayer inv;

	public GuiItemSensor(TileContainer cont) {
		super(cont);
		this.MAIN_TEX = new ResourceLocation("circuits", "textures/gui/item_sensor.png");
		this.inv = cont.player.inventory;
	}

	@Override
	public void initGui() {
		this.xSize = 176;
		this.ySize = 132;
		super.initGui();
		guiComps.add(new Button(0, 79, 15, 18, 18, 0).texture(176, 0).setTooltip("itemSensor.neg"));
		guiComps.add(new Button(1, 97, 15, 18, 9, 0).texture(194, 0).setTooltip("itemSensor.meta"));
		guiComps.add(new Button(2, 97, 24, 18, 9, 0).texture(194, 18).setTooltip("itemSensor.nbt"));
		guiComps.add(new Button(3, 115, 15, 18, 18, 0).texture(212, 0).setTooltip("itemSensor.ore"));
	}

	@Override
	protected Object getDisplVar(int id) {
		ItemStack item = inv.mainInventory.get(inv.currentItem);
		byte mode = item.hasTagCompound() ? item.getTagCompound().getByte("mode") : 0;
		return (int)mode >> id & 1;
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer dos = BlockGuiHandler.getPacketForItem(inv.currentItem);
		ItemStack item = inv.mainInventory.get(inv.currentItem);
		byte mode = item.hasTagCompound() ? item.getTagCompound().getByte("mode") : 0;
		dos.writeByte(mode ^ 1 << id);
		BlockGuiHandler.sendPacketToServer(dos);
	}

}
