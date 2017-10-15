package cd4017be.circuits.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import cd4017be.circuits.tileEntity.Potentiometer;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.AdvancedGui;

public class GuiPotentiometer extends AdvancedGui {

	private final Potentiometer tile;

	public GuiPotentiometer(IGuiData tileEntity, EntityPlayer player) {
		super(new DataContainer(tileEntity, player));
		this.tile = (Potentiometer) tileEntity;
		this.MAIN_TEX = new ResourceLocation("circuits", "textures/gui/circuit.png");
		this.bgTexY = 160;
	}

	@Override
	public void initGui() {
		xSize = 80;
		ySize = 42;
		super.initGui();
		guiComps.add(new TextField(0, 8, 16, 64, 7, 11, ()-> "" + tile.max, (t)-> {
			try {
				PacketBuffer dos = BlockGuiHandler.getPacketTargetData(tile.pos());
				dos.writeByte(1).writeInt(Integer.parseInt(t));
				BlockGuiHandler.sendPacketToServer(dos);
			} catch(NumberFormatException e) {}}).setTooltip("potent.max"));
		guiComps.add(new TextField(1, 8, 27, 64, 7, 11, ()-> "" + tile.min, (t)-> {
			try {
				PacketBuffer dos = BlockGuiHandler.getPacketTargetData(tile.pos());
				dos.writeByte(0).writeInt(Integer.parseInt(t));
				BlockGuiHandler.sendPacketToServer(dos);
			} catch(NumberFormatException e) {}}).setTooltip("potent.min"));
	}

}
