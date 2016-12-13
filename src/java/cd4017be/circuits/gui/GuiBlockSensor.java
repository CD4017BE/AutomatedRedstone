package cd4017be.circuits.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import cd4017be.circuits.tileEntity.BlockSensor;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.GuiMachine;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.templates.AutomatedTile;

public class GuiBlockSensor extends GuiMachine {

	private final BlockSensor tile;

	public GuiBlockSensor(BlockSensor tile, EntityPlayer player) {
		super(new TileContainer(tile, player));
		this.tile = tile;
		this.MAIN_TEX = new ResourceLocation("circuits", "textures/gui/sensor.png");
	}

	@Override
	public void initGui() {
		this.xSize = 176;
		this.ySize = 222;
		this.tabsY = 15 - 63;
		super.initGui();
		for (int i = 0; i < 12; i++) {
			boolean t = (i & 1) != 0;
			guiComps.add(new TextField(i, 69, (t?15:16) + i * 9, 63, 8, 11).color(0xff40c0c0, 0xffff4040).setTooltip(t ? "sensor.add" : "sensor.mult"));
		}
		guiComps.add(new Button(12, 7, 105, 18, 5, -1));//+1s
		guiComps.add(new Button(13, 7, 118, 18, 5, -1));//-1s
		guiComps.add(new Button(14, 25, 105, 18, 5, -1));//+1t
		guiComps.add(new Button(15, 25, 118, 18, 5, -1));//-1t
		guiComps.add(new Text(16, 8, 110, 34, 8, "tickInt").center());
		guiComps.add(new Text(17, 7, 88, 36, 8, "sensor.timer").center());
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mx, int my) {
		super.drawGuiContainerForegroundLayer(mx, my);
		if (this.isPointInRegion(134, 16, 16, 106, mx, my))
			this.drawSideCube(-64, tabsY + 63, (my - guiTop - 15) / 18, (byte)2);
	}

	@Override
	protected Object getDisplVar(int id) {
		if (id < 12) return Float.toString(tile.transf[id]);
		else if (id == 16) return (float)tile.tickInt / 20F;
		else return null;
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer dos = tile.getPacketTargetData();
		if (id < 12) try {
			dos.writeByte(AutomatedTile.CmdOffset + id);
			dos.writeFloat(Float.parseFloat((String)obj));
		} catch(NumberFormatException e){return;}
		else if (id == 12 || id == 14) dos.writeByte(AutomatedTile.CmdOffset + 14).writeInt(tile.tickInt = Math.min(1200, tile.tickInt + (id == 12 ? 20 : 1)));
		else if (id == 13 || id == 15) dos.writeByte(AutomatedTile.CmdOffset + 14).writeInt(tile.tickInt = Math.max(1, tile.tickInt - (id == 13 ? 20 : 1)));
		else return;
		if (send) BlockGuiHandler.sendPacketToServer(dos);
	}

}
