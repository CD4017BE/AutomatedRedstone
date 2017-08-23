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
		this.ySize = 222;
		this.tabsY = 15 - 63;
		super.initGui();
		guiComps.add(new TextField(0, 69, 16, 63, 8, 11).color(0xff40c0c0, 0xffff4040).setTooltip("sensor.mult"));
		guiComps.add(new TextField(1, 69, 24, 63, 8, 11).color(0xff40c0c0, 0xffff4040).setTooltip("sensor.add"));
		guiComps.add(new Button(2, 7, 105, 18, 5, -1));//+1s
		guiComps.add(new Button(3, 7, 118, 18, 5, -1));//-1s
		guiComps.add(new Button(4, 25, 105, 18, 5, -1));//+1t
		guiComps.add(new Button(5, 25, 118, 18, 5, -1));//-1t
		guiComps.add(new Text<>(6, 8, 110, 34, 8, "tickInt").center());
		guiComps.add(new Text<>(7, 7, 88, 36, 8, "sensor.timer").center());
	}

	@Override
	protected Object getDisplVar(int id) {
		switch(id) {
		case 0: return Float.toString(tile.mult);
		case 1: return Float.toString(tile.ofs);
		case 6: return (float)tile.tickInt / 20F;
		default: return null;
		}
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer dos = BlockGuiHandler.getPacketTargetData(tile.pos());
		if (id < 2) try {
			dos.writeByte(id);
			dos.writeFloat(Float.parseFloat((String)obj));
		} catch(NumberFormatException e){return;}
		else if (id == 2 || id == 4) dos.writeByte(2).writeInt(tile.tickInt = Math.min(1200, tile.tickInt + (id == 2 ? 20 : 1)));
		else if (id == 3 || id == 5) dos.writeByte(2).writeInt(tile.tickInt = Math.max(1, tile.tickInt - (id == 3 ? 20 : 1)));
		else return;
		if (send) BlockGuiHandler.sendPacketToServer(dos);
	}

}
