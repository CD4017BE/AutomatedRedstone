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
	private boolean view8 = false;

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
		this.titleX = xSize / 4;
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
		guiComps.add(new Button(24, 26, 91, 16, 16, 0).texture(176, 0).setTooltip("program.view#"));
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
		else this.drawTexturedModalRect(guiLeft + 7, guiTop + 25 + 2 * selByte, 239, 0, 17, view8 ? 16 : 2);
		for (int i = 0; i < 8; i++)
			fontRendererObj.drawString(String.format("%02X", lineIndex(i)), guiLeft + 24, guiTop + 25 + i * 8, 0xffffff40);
		String[] s = I18n.translateToLocal("gui.cd4017be.program.head").split("\\\\n");
		if (s.length > 0) fontRendererObj.drawString(s[0], guiLeft + 8, guiTop + 16, 0x404040);
		if (s.length > 1) fontRendererObj.drawString(s[1], guiLeft + 88 - fontRendererObj.getStringWidth(s[1]) / 2, guiTop + 16, 0x404040);
		if (s.length > 2) fontRendererObj.drawString(s[2], guiLeft + 168 - fontRendererObj.getStringWidth(s[2]), guiTop + 16, 0x404040);
		if (tile.errorCode > 0) {
			this.drawGradientRect(guiLeft + 8, guiTop + 111, guiLeft + 168, guiTop + 120, 0xffc6c6c6, 0xffc6c6c6);
			this.drawStringCentered(TooltipInfo.format("gui.cd4017be.err" + tile.errorCode, tile.errorArg), this.guiLeft + this.xSize / 2, this.guiTop + 112, 0xff4040);
		}
	}

	@Override
	protected Object getDisplVar(int id) {
		if (id < 8) return tile.getCode(lineIndex(id));
		else if(id < 16) return tile.getLabel(lineIndex(id - 8));
		else if(id == 16)return tile.name;
		else return view8 ? 1 : 0;
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer dos = tile.getPacketTargetData();
		if (id < 8) {dos.writeByte(AutomatedTile.CmdOffset).writeByte(lineIndex(id)); dos.writeString((String)obj);}
		else if (id < 16) {dos.writeByte(AutomatedTile.CmdOffset + 1).writeByte(lineIndex(id - 8)); dos.writeString((String)obj);}
		else if (id == 16) {dos.writeByte(AutomatedTile.CmdOffset + 2); dos.writeString((String)obj);}
		else if (id == 17) dos.writeByte(AutomatedTile.CmdOffset + 3);
		else if (id == 18) dos.writeByte(AutomatedTile.CmdOffset + 4);
		else {
			this.setFocus(-1);
			if (id == 19) selByte -= (Integer)obj == 0 ? 1 : 8;
			else if (id == 20) selByte += (Integer)obj == 0 ? 1 : 8;
			else if (id == 24) view8 = !view8;
			selByte &= 0x1f;
			return;
		}
		if (send) BlockGuiHandler.sendPacketToServer(dos);
	}

	private int lineIndex(int l) {
		return (view8 ? l * 8 : l) + selByte * 8;
	}

	@Override
	protected void mouseClicked(int x, int y, int b) throws IOException {
		if (this.isPointInRegion(9, 26, 14, 62, x, y)) {
			int px = (x - guiLeft - 8) / 2, py = (y - guiTop - 25) / 2, i = py * 8 + px;
			if (focus >= 0 && focus < 8) this.addReqIndex((TextField)guiComps.get(focus), py, i);	
			else {
				this.setFocus(-1);
				if (view8) {
					if (py < selByte) selByte = py;
					else if (py - 7 > selByte) selByte = py - 7;
					this.setFocus(py - selByte);
				} else {
					selByte = py;
					this.setFocus(px);
				}
			}
		} else super.mouseClicked(x, y, b);
	}

	@Override
	protected void keyTyped(char c, int k) throws IOException {
		TextField t;
		if (k == Keyboard.KEY_UP && (focus == 0 || focus == 8)) {int f = focus; this.setDisplVar(19, view8 ? 1 : 0, false); this.setFocus(f + 7);}
		else if (k == Keyboard.KEY_DOWN && (focus == 7 || focus == 15)) {int f = focus; this.setDisplVar(20, view8 ? 1 : 0, false); this.setFocus(f - 7);}
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
		boolean num;
		switch(t.text.charAt(0)) {
		case '+': case '-': case '&': case '*': case '/': case '\\':
			num = false;
			break;
		case '<': case '>': case '=': case '~':
			num = true;
			break;
		case 'b': case 'B': case 's': case 'S': case 'm': case 'M': case 'i': case 'I':
			if (p0 == 1) p0 = 2;
			if (p1 < p0) return;
			num = false;
			int p = 1;
			for (int i = 0; i < 2; i++) {
				p = t.text.indexOf(',', p) + 1;
				if (p == 0 || p >= p1) {
					num = true;
					break;
				}
			}
			break;
		default: return;
		}
		String ins;
		if (num) {
			char c = t.text.charAt(p0);
			if (!Character.isAlphabetic(c)) {
				c = t.text.charAt(0);
				if (!Character.isAlphabetic(c)) c = 'B';
			}
			ins = c + Integer.toString(by);
		} else {
			ins = Integer.toHexString(bi);
		}
		t.text = t.text.substring(0, p0) + ins + t.text.substring(p1);
		if (t.text.length() > t.maxL) t.text = t.text.substring(0, t.maxL);
		t.cur = Math.min(t.maxL, p0 + ins.length());
	}

	private void showIO(TextField t) {
		int l = 1, id = lineIndex(t.id);
		char c;
		String[] s;
		if (t.text.isEmpty()) {
			c = ' ';
			s = null;
		} else {
			c = t.text.charAt(0);
			s = t.text.substring(1).split(",");
		}
		switch(c) {
		case '+': case '-': case '&': case '*': case '/': case '\\':
			for (String s1 : s) showBitParam(s1);
			break;
		case '<': case '>': case '=': case '~':
			for (String s1 : s) showNumParam(s1);
			break;
		case 'i': case 'I': l += 8;
		case 'm': case 'M': l += 8;
		case 's': case 'S': l += 8;
		case 'b': case 'B': l += 7;
			if(s[0].isEmpty()) break;
			s[0] = s[0].substring(1);
			for (int i = 0; i < s.length; i++)
				if (i < 2) showNumParam(s[i]);
				else showBitParam(s[i]);
		}
		this.drawTexturedModalRect(guiLeft + 8 + 2 * (id & 7), guiTop + 25 + 2 * (id >> 3), 240 + 2 * (id & 7), 0, l >= 8 ? 16 : 2 * l, (l + 7) / 8 * 2);
	}

	private void showBitParam(String code) {
		try {
			int i = Integer.parseInt(code.trim().toLowerCase(), 16);
			drawTexturedModalRect(guiLeft + 8 + 2 * (i % 8), guiTop + 25 + 2 * (i / 8), 240, 16, 2, 2);
		} catch(NumberFormatException e) {}
	}

	private void showNumParam(String code) {
		if (code.isEmpty()) return;
		int l;
		switch(code.charAt(0)) {
		case 'b': case 'B': l = 1; break;
		case 's': case 'S': l = 2; break;
		case 'm': case 'M': l = 3; break;
		case 'i': case 'I': l = 4; break;
		default: return;
		}
		try {
			int i = Integer.parseInt(code.substring(1)) & 0x1f;
			drawTexturedModalRect(guiLeft + 8, guiTop + 25 + 2 * i, 240, 16, 16, l * 2);
		} catch(NumberFormatException e) {}
	}

}
