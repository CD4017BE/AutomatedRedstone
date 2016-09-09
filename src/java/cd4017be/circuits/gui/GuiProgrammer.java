package cd4017be.circuits.gui;

import java.io.IOException;
import java.util.Arrays;

import org.lwjgl.input.Keyboard;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import cd4017be.circuits.tileEntity.Programmer;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.TooltipInfo;
import cd4017be.lib.Gui.GuiMachine;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.templates.AutomatedTile;

/**
 *
 * @author CD4017BE
 */
public class GuiProgrammer extends GuiMachine {

	private final Programmer tile;
	private int selByte = 0;
	
	public GuiProgrammer(Programmer tileEntity, EntityPlayer player) {
		super(new TileContainer(tileEntity, player));
		this.MAIN_TEX = new ResourceLocation("circuits", "textures/gui/programmer.png");
		this.tile = tileEntity;
	}

	@Override
	public void initGui() {
		this.xSize = 176;
		this.ySize = 207;
		super.initGui();
		for (int i = 0; i < 8; i++) 
			guiComps.add(new TextField(i, 36, 25 + i * 8, 96, 8, 32).color(0xffc0c0c0, 0xffff0000));
		for (int i = 0; i < 8; i++)
			guiComps.add(new TextField(i + 8, 133, 25 + i * 8, 35, 8, 8).color(0xff40ff40, 0xffff0000));
		guiComps.add(new TextField(16, 89, 4, 80, 8, 24).setTooltip("program.n"));
		guiComps.add(new Button(17, 152, 91, 16, 16, -1).setTooltip("program.load"));
		guiComps.add(new Button(18, 116, 91, 16, 16, -1).setTooltip("program.save"));
		guiComps.add(new Button(19, 8, 91, 16, 8, -1).setTooltip("program.up"));
		guiComps.add(new Button(20, 8, 99, 16, 8, -1).setTooltip("program.down"));
		guiComps.add(new GuiComp(21, 8, 16, 28, 8).setTooltip("program.index"));
		guiComps.add(new GuiComp(22, 36, 16, 96, 8).setTooltip("program.code"));
		guiComps.add(new GuiComp(23, 132, 16, 36, 8).setTooltip("program.label"));
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mx, int my) {
		super.drawGuiContainerForegroundLayer(mx, my);
		if (this.isPointInRegion(9, 26, 14, 62, mx, my)) {
			int x = (mx - guiLeft - 8) / 2, y = (my - guiTop - 25) / 2, i = y * 8 + x;
			this.drawHoveringText(Arrays.asList(String.format("%d:%02X ", y, i) + tile.getLabel(i)), mx - guiLeft, my - guiTop);
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float t, int mx, int my) {
		super.drawGuiContainerBackgroundLayer(t, mx, my);
		mc.renderEngine.bindTexture(MAIN_TEX);
		if (focus >= 0 && focus < 8) this.showIO((TextField)guiComps.get(focus));
		else this.drawTexturedModalRect(guiLeft + 7, guiTop + 25 + 2 * selByte, 239, 0, 17, 1);
		for (int i = 0; i < 8; i++)
			fontRendererObj.drawString(String.format("%02X", i + selByte * 8), guiLeft + 24, guiTop + 25 + i * 8, 0xffffff40);
		fontRendererObj.drawString(tile.getName(), this.guiLeft + 8, this.guiTop + 4, 0x404040);
		String[] s = I18n.translateToLocal("gui.cd4017be.program.head").split("\\\\n");
		if (s.length > 0) fontRendererObj.drawString(s[0], guiLeft + 8, guiTop + 16, 0x404040);
		if (s.length > 1) fontRendererObj.drawString(s[1], guiLeft + 88 - fontRendererObj.getStringWidth(s[1]) / 2, guiTop + 16, 0x404040);
		if (s.length > 2) fontRendererObj.drawString(s[2], guiLeft + 168 - fontRendererObj.getStringWidth(s[2]), guiTop + 16, 0x404040);
		if (tile.errorCode > 0) {
			this.drawGradientRect(guiLeft + 8, guiTop + 112, guiLeft + 168, guiTop + 120, 0xffc6c6c6, 0xffc6c6c6);
			this.drawStringCentered(TooltipInfo.format("gui.cd4017be.err" + tile.errorCode, tile.errorArg), this.guiLeft + this.xSize / 2, this.guiTop + 112, 0xff4040);
		}
	}

	@Override
	protected Object getDisplVar(int id) {
		return id < 8 ? tile.getCode(id + 8 * selByte) : id < 16 ? tile.getLabel(id - 8 + 8 * selByte) : tile.name;
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer dos = tile.getPacketTargetData();
		if (id < 8) {dos.writeByte(AutomatedTile.CmdOffset).writeByte(id + selByte * 8); dos.writeString((String)obj);}
		else if (id < 16) {dos.writeByte(AutomatedTile.CmdOffset + 1).writeByte(id - 8 + selByte * 8); dos.writeString((String)obj);}
		else if (id == 16) {dos.writeByte(AutomatedTile.CmdOffset + 2); dos.writeString((String)obj);}
		else if (id == 17) dos.writeByte(AutomatedTile.CmdOffset + 3);
		else if (id == 18) dos.writeByte(AutomatedTile.CmdOffset + 4);
		else {
			this.setFocus(-1);
			if (id == 19) selByte -= (Integer)obj == 0 ? 1 : 8;
			else if (id == 20) selByte += (Integer)obj == 0 ? 1 : 8;
			selByte &= 0x1f;
			return;
		}
		if (send) BlockGuiHandler.sendPacketToServer(dos);
	}

	@Override
	protected void mouseClicked(int x, int y, int b) throws IOException {
		if (this.isPointInRegion(9, 26, 14, 62, x, y)) {
			int px = (x - guiLeft - 8) / 2, py = (y - guiTop - 25) / 2, i = py * 8 + px;
			if (focus >= 0 && focus < 8) this.addReqIndex((TextField)guiComps.get(focus), py, i);	
			else {
				this.setFocus(-1);
				selByte = py;
				this.setFocus(px);
			}
		} else super.mouseClicked(x, y, b);
	}

	@Override
	protected void keyTyped(char c, int k) throws IOException {
		TextField t;
		if (k == Keyboard.KEY_UP && (focus == 0 || focus == 8)) {int f = focus; this.setDisplVar(19, (int)0, false); this.setFocus(f + 7);}
		else if (k == Keyboard.KEY_DOWN && (focus == 7 || focus == 15)) {int f = focus; this.setDisplVar(20, (int)0, false); this.setFocus(f - 7);}
		else if (k == Keyboard.KEY_LEFT && focus >= 8 && focus < 16 && (t = (TextField)guiComps.get(focus)).cur == 0) this.setFocus(focus - 8);
		else if (k == Keyboard.KEY_RIGHT && focus >= 0 && focus < 8 && (t = (TextField)guiComps.get(focus)).cur == t.text.length()) this.setFocus(focus + 8);	
		else if (k == Keyboard.KEY_PRIOR) this.setDisplVar(19, isShiftKeyDown() ? (int)1 : (int)0, false);
		else if (k == Keyboard.KEY_NEXT) this.setDisplVar(20, isShiftKeyDown() ? (int)1 : (int)0, false);
		else if (k == Keyboard.KEY_S && isCtrlKeyDown()) this.setDisplVar(18, null, true);
		else super.keyTyped(c, k);
	}

	private void addReqIndex(TextField t, int by, int bi) {
		if (t.text.isEmpty()) return;
		int p0 = t.text.lastIndexOf(',', t.cur - 1) + 1, p1 = t.text.indexOf(',', t.cur);
		if (p0 <= 0) p0 = 1; if (p1 < 0) p1 = t.text.length();
		switch(t.text.charAt(0)) {
		case '+': case '-': case '&': case '*': case '/': case '\\': case '$':
			t.text = t.text.substring(0, p0) + Integer.toHexString(bi) + t.text.substring(p1);
			break;
		case '<': case '>': case '=': case '~':
			t.text = t.text.substring(0, p0) + "#" + Integer.toString(by) + t.text.substring(p1);
			break;
		default: return;
		}
		if (t.text.length() > t.maxL) t.text = t.text.substring(0, t.maxL);
		t.cur = p0;
	}

	private void showIO(TextField t) {
		this.drawTexturedModalRect(guiLeft + 7 + 2 * t.id, guiTop + 25 + 2 * selByte, 239 + 2 * t.id, 0, 2, 1);
		if (t.text.isEmpty()) return;
		String[] s = t.text.substring(1).split(",");
		switch(t.text.charAt(0)) {
		case '+': case '-': case '&': case '*': case '/': case '\\':
			for (String s1 : s)
				try {
					int i = Integer.parseInt(s1.trim().toLowerCase(), 16);
					this.drawTexturedModalRect(guiLeft + 7 + 2 * (i % 8), guiTop + 25 + 2 * (i / 8), 239, 1, 2, 1);
				} catch(NumberFormatException e) {}
			break;
		case '<': case '>': case '=': case '~':
			for (String s1 : s)
				if ((s1 = s1.trim()).startsWith("#"))
					try {
						int i = Integer.parseInt(s1.substring(1)) & 0x1f;
						this.drawTexturedModalRect(guiLeft + 7, guiTop + 25 + 2 * i, 239, 1, 17, 1);
					} catch(NumberFormatException e) {}
			break;
		case '$':
			for (int j = 0; j < s.length && j < 4; j += 2) 
				try {
					int i = Integer.parseInt(s[j].trim().toLowerCase(), 16);
					this.drawTexturedModalRect(guiLeft + 7 + 2 * (i % 8), guiTop + 25 + 2 * (i / 8), 239, 1, 2, 1);
				} catch(NumberFormatException e) {}
			for (int j = t.id + 8 * selByte + 1; tile.getCode(j).startsWith("."); j++)
				this.drawTexturedModalRect(guiLeft + 7 + 2 * (j % 8), guiTop + 25 + 2 * (j / 8), 239, 0, 2, 1);
		default: return;
		}
	}

}
