package cd4017be.circuits.gui;

import cd4017be.circuits.tileEntity.ItemValve;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.AdvancedGui;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

public class GuiItemValve extends AdvancedGui {

	private final ItemValve tile;

	public GuiItemValve(IGuiData tile, EntityPlayer player) {
		super(new DataContainer(tile, player));
		this.tile = (ItemValve) tile;
		this.bgTexY = 40;
		this.MAIN_TEX = new ResourceLocation("circuits", "textures/gui/valve.png");
	}

	@Override
	public void initGui() {
		xSize = 158;
		ySize = 49;
		super.initGui();
		guiComps.add(new NumberSel(0, 7, 15, 36, 18, "", 1, 1200, 20, ()-> tile.tickInt, (i)-> {
			PacketBuffer dos = BlockGuiHandler.getPacketTargetData(tile.pos());
			dos.writeByte(0).writeInt(tile.tickInt = i);
			BlockGuiHandler.sendPacketToServer(dos);
		}).setup(8, 0xff404040, 2, true).around());
		guiComps.add(new Text<Float>(1, 8, 20, 34, 8, "valve.tick", ()-> (float)tile.tickInt / 20F).center().setTooltip("valve.timer"));
		guiComps.add(new Button(2, 43, 15, 18, 18, 0, ()-> tile.mode >> 2 & 3, (b)-> sendCommand((Integer)b == 0 ? 1 : 2)).texture(176, 36).setTooltip("itemvalve.mode#"));
		guiComps.add(new Button(3, 61, 15, 18, 18, 0, ()-> tile.mode >> 1 & 1, (b)-> sendCommand(3)).texture(176, 0).setTooltip("itemvalve.slot#"));
		guiComps.add(new Button(4, 43, 33, 18, 9, 0, ()-> tile.mode >> 4 & 1, (b)-> sendCommand(4)).texture(158, 36).setTooltip("itemvalve.bit#"));
		guiComps.add(new Button(5, 61, 33, 18, 9, 0, ()-> tile.mode & 1, (b)-> sendCommand(5)).texture(158, 54).setTooltip("itemvalve.out"));
		guiComps.add(new GuiComp<>(6, 8, 35, 32, 6).setTooltip("itemvalve.rs"));
		guiComps.add(new Text<Object[]>(7, 79, 15, 72, 27, "itemvalve.flow", ()-> new Object[] {tile.flow + tile.limit, tile.limit}).center());
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
		mc.renderEngine.bindTexture(MAIN_TEX);
		int size = (tile.mode & 16) != 0 ? 16 : 8;
		if ((tile.mode & 1) != 0)
			drawTexturedModalRect(guiLeft + 8, guiTop + 38, 158, 92, size, 2);
		int ofs = 0;
		if ((tile.mode & 2) != 0)
			drawTexturedModalRect(guiLeft + 8, guiTop + 36, 158, 90, ofs = 8, 2);
		if ((tile.mode & 12) != 0)
			drawTexturedModalRect(guiLeft + 8 + ofs, guiTop + 36, 166, 90, size, 2);
	}

}
