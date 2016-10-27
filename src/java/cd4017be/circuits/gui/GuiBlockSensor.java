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
			guiComps.add(new TextField(i, 87, (t?15:16) + i * 9, 63, 8, 11).color(0xff40c0c0, 0xffff4040).setTooltip(t ? "sensor.add" : "sensor.mult"));
		}
		for (int i = 0; i < 6; i++) guiComps.add(new Button(i + 12, 43, 15 + i * 18, 9, 9, 1).texture(176, 0).setTooltip("sensor.in"));
		for (int i = 0; i < 6; i++) guiComps.add(new Button(i + 18, 43, 24 + i * 18, 9, 9, 0).texture(185, 0).setTooltip("sensor.acc"));
		guiComps.add(new Button(24, 7, 105, 18, 5, -1));//+1s
		guiComps.add(new Button(25, 7, 118, 18, 5, -1));//-1s
		guiComps.add(new Button(26, 25, 105, 18, 5, -1));//+1t
		guiComps.add(new Button(27, 25, 118, 18, 5, -1));//-1t
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mx, int my) {
		super.drawGuiContainerForegroundLayer(mx, my);
		if (this.isPointInRegion(152, 16, 16, 106, mx, my))
			this.drawSideCube(-64, tabsY + 63, (my - guiTop - 15) / 18, (byte)2);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
		this.drawStringCentered(String.format("%.2f", (float)tile.tickInt / 20F).concat("s"), this.guiLeft + 25, this.guiTop + 110, 0x404040);
		this.drawLocString(guiLeft + 7, guiTop + 88, 8, 0x404040, "sensor.timer");
	}

	@Override
	protected Object getDisplVar(int id) {
		if (id < 12) return Float.toString(tile.transf[id]);
		else if (id < 18) return tile.getSide(tile.input, id - 12);
		else if (id < 24) return tile.getSide(tile.access, id - 18);
		else return null;
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer dos = tile.getPacketTargetData();
		if (id < 12) try {
			dos.writeByte(AutomatedTile.CmdOffset + id);
			dos.writeFloat(Float.parseFloat((String)obj));
		} catch(NumberFormatException e){return;}
		else if (id < 18) {
			int p = (id - 12) * 4;
			dos.writeByte(AutomatedTile.CmdOffset + 12).writeInt(tile.input = tile.input & ~(0xf << p) | (((tile.input >> p & 0xf) + ((Integer)obj == 0 ? 1 : 5)) % 6) << p);
		} else if (id < 24) {
			int p = (id - 18) * 4;
			dos.writeByte(AutomatedTile.CmdOffset + 13).writeInt(tile.access = tile.access & ~(0xf << p) | (((tile.access >> p & 0xf) + ((Integer)obj == 0 ? 1 : 5)) % 6) << p);
		} else if (id == 24 || id == 26) dos.writeByte(AutomatedTile.CmdOffset + 14).writeInt(tile.tickInt = Math.min(1200, tile.tickInt + (id == 24 ? 20 : 1)));
		else if (id == 25 || id == 27) dos.writeByte(AutomatedTile.CmdOffset + 14).writeInt(tile.tickInt = Math.max(1, tile.tickInt - (id == 25 ? 20 : 1)));
		else return;
		if (send) BlockGuiHandler.sendPacketToServer(dos);
	}

}
