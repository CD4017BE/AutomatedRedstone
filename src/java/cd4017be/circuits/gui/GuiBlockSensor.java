package cd4017be.circuits.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import cd4017be.circuits.tileEntity.BlockSensor;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.GuiMachine;
import cd4017be.lib.Gui.TileContainer;

public class GuiBlockSensor extends GuiMachine {

	private final BlockSensor tile;

	public GuiBlockSensor(IGuiData tile, EntityPlayer player) {
		super(new TileContainer(tile, player));
		this.tile = (BlockSensor) tile;
		this.MAIN_TEX = new ResourceLocation("circuits", "textures/gui/sensor.png");
	}

	@Override
	public void initGui() {
		this.xSize = 176;
		this.ySize = 132;
		this.tabsY = 15 - 63;
		super.initGui();
		guiComps.add(new TextField(0, 87, 16, 63, 8, 11).color(0xff40c0c0, 0xffff4040).setTooltip("sensor.mult"));
		guiComps.add(new TextField(1, 87, 24, 63, 8, 11).color(0xff40c0c0, 0xffff4040).setTooltip("sensor.add"));
		guiComps.add(new NumberSel(2, 7, 15, 36, 18, "", 1, 1200, 20).setup(8, 0xff404040, 2, true).around());
		guiComps.add(new Text<Float>(3, 8, 20, 34, 8, "sensor.tick").center().setTooltip("sensor.timer"));
		guiComps.add(new Tooltip<Integer>(4, 152, 16, 16, 16, "sensor.out"));
	}

	@Override
	protected Object getDisplVar(int id) {
		switch(id) {
		case 0: return Float.toString(tile.mult);
		case 1: return Float.toString(tile.ofs);
		case 2: return tile.tickInt;
		case 3: return (float)tile.tickInt / 20F;
		case 4: return tile.output;
		default: return null;
		}
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer dos = BlockGuiHandler.getPacketTargetData(tile.pos());
		switch(id) {
		case 0:	case 1:
			try {
				dos.writeByte(id);
				dos.writeFloat(Float.parseFloat((String)obj));
			} catch(NumberFormatException e){return;}
			break;
		case 2: dos.writeByte(2).writeShort(tile.tickInt = (Integer)obj); break;
		default: return;
		}
		if (send) BlockGuiHandler.sendPacketToServer(dos);
	}

}
