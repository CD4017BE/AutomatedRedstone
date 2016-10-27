package cd4017be.circuits.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import cd4017be.circuits.tileEntity.Display8bit;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.GuiMachine;

public class GuiDisplay8bit extends GuiMachine {

	private final Display8bit tile;

	public GuiDisplay8bit(Display8bit tileEntity, EntityPlayer player) {
		super(new DataContainer(tileEntity, player));
		this.tile = tileEntity;
		this.MAIN_TEX = new ResourceLocation("circuits", "textures/gui/circuit.png");
		this.bgTexY = 100;
	}

	@Override
	public void initGui() {
		this.xSize = 108;
		this.ySize = 53;
		super.initGui();
		guiComps.add(new TextField(0, 8, 27, 11, 7, 2).setTooltip("circuit.ext"));
		guiComps.add(new TextField(1, 21, 27, 11, 7, 2).setTooltip("circuit.size"));
		guiComps.add(new Button(2, 34, 26, 18, 9, 0).texture(108, 100).setTooltip("display8bit.mode"));
		guiComps.add(new TextField(3, 54, 27, 46, 7, 8).setTooltip("display8bit.format"));
		guiComps.add(new TextField(4, 8, 16, 92, 7, 16).setTooltip("display8bit.text"));
		guiComps.add(new TextField(5, 8, 38, 92, 7, 16).setTooltip("display8bit.text"));
	}

	@Override
	protected Object getDisplVar(int id) {
		switch(id) {
		case 0: return Integer.toString(tile.dspType >> 2 & 0x1f);
		case 1: return Integer.toString((tile.dspType >> 7 & 0x1f) + 1);
		case 2: return (int)(tile.dspType & 3);
		case 3: return tile.format;
		case 4: return tile.text0;
		case 5: return tile.text1;
		default: return null;
		}
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer dos = tile.getPacketTargetData();
		switch(id) {
		case 0: try {
			int i = Integer.parseInt((String)obj) << 2;
			dos.writeByte(0).writeShort(tile.dspType = (short)(tile.dspType & 0xff83 | (i & 0x007c))); break;
		} catch (NumberFormatException e) {return;}
		case 1: try {
			int i = (Integer.parseInt((String)obj) - 1) << 7;
			dos.writeByte(0).writeShort(tile.dspType = (short)(tile.dspType & 0xf07f | (i & 0x0f80))); break;
		} catch (NumberFormatException e) {return;}
		case 2: dos.writeByte(0).writeShort(tile.dspType = (short)(tile.dspType & 0xfffc | ((tile.dspType & 0x0003) + 1) % 3)); break;
		case 3: dos.writeByte(1); dos.writeString(tile.format = (String)obj); break;
		case 4: dos.writeByte(2); dos.writeString(tile.text0 = (String)obj); break;
		case 5: dos.writeByte(3); dos.writeString(tile.text1 = (String)obj); break;
		}
		if (send) BlockGuiHandler.sendPacketToServer(dos);
	}

}
