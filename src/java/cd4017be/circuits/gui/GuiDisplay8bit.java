package cd4017be.circuits.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import cd4017be.circuits.tileEntity.Display8bit;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.AdvancedGui;

public class GuiDisplay8bit extends AdvancedGui {

	private static final byte[]
			nextMode = {1, 2, 4, 5, 5, 0, 0, 0},
			prevMode = {5, 0, 1, 2, 2, 4, 4, 4};

	private final Display8bit tile;

	public GuiDisplay8bit(IGuiData tileEntity, EntityPlayer player) {
		super(new DataContainer(tileEntity, player));
		this.tile = (Display8bit) tileEntity;
		this.MAIN_TEX = new ResourceLocation("circuits", "textures/gui/circuit.png");
		this.bgTexY = 100;
	}

	@Override
	public void initGui() {
		this.xSize = 108;
		this.ySize = 53;
		super.initGui();
		guiComps.add(new Button(0, 7, 26, 18, 9, 0).texture(108, 100).setTooltip("display.mode#"));
		guiComps.add(new TextField(1, 39, 27, 30, 7, 8).setTooltip("display.format"));
		guiComps.add(new TextField(2, 8, 16, 92, 7, 32).allowFormat().setTooltip("display.text"));
		guiComps.add(new TextField(3, 8, 38, 92, 7, 32).allowFormat().setTooltip("display.text"));
		guiComps.add(new Button(4, 83, 26, 9, 9, 0).texture(126, 100).setTooltip("display.color"));
	}

	@Override
	protected Object getDisplVar(int id) {
		switch(id) {
		case 0: return (int)(tile.dspMode & 7);
		case 1: return tile.format;
		case 2: return tile.text0;
		case 3: return tile.text1;
		case 4: return (int)(tile.dspMode >> 8 & 15);
		default: return null;
		}
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer dos = BlockGuiHandler.getPacketTargetData(tile.pos());
		switch(id) {
		case 0: dos.writeByte(0).writeByte((Integer)obj == 0 ? nextMode[tile.dspMode & 7] : prevMode[tile.dspMode & 7]); break;
		case 1: dos.writeByte(1); dos.writeString(tile.format = (String)obj); break;
		case 2: dos.writeByte(2); dos.writeString(tile.text0 = (String)obj); break;
		case 3: dos.writeByte(3); dos.writeString(tile.text1 = (String)obj); break;
		case 4: dos.writeByte(4).writeByte((tile.dspMode >> 8) + ((Integer)obj == 0 ? 1 : 15) & 15); break;
		}
		if (send) BlockGuiHandler.sendPacketToServer(dos);
	}

}
