package cd4017be.circuits.gui;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import cd4017be.circuits.tileEntity.Assembler;
import cd4017be.circuits.tileEntity.CircuitDesigner;
import cd4017be.circuits.tileEntity.CircuitDesigner.Con;
import cd4017be.circuits.tileEntity.CircuitDesigner.Module;
import cd4017be.circuits.tileEntity.CircuitDesigner.ModuleType;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.lib.Gui.AdvancedGui;
import cd4017be.lib.Gui.TileContainer;

public class GuiCircuitDesigner extends AdvancedGui {

	private static final float ScrollSize = 1F / (float)(CircuitDesigner.ModuleType.values().length - 8);
	private static final ResourceLocation BG_TEX = new ResourceLocation("circuits", "textures/gui/circuit_designer.png");
	private final CircuitDesigner tile;
	private int scroll;

	public GuiCircuitDesigner(IGuiData tile, EntityPlayer player) {
		super(new TileContainer(tile, player));
		this.MAIN_TEX = new ResourceLocation("circuits", "textures/gui/circuit_operators.png");
		this.drawBG = 6;
		this.tile = (CircuitDesigner) tile;
	}

	@Override
	public void initGui() {
		xSize = 244;
		ySize = 256;
		super.initGui();
		this.titleX = xSize / 4;
		guiComps.add(new TextField(0, 118, 4, 109, 8, 16).setTooltip("designer.Pname"));
		guiComps.add(new Button(1, 184, 232, 16, 16, -1).setTooltip("designer.save"));
		guiComps.add(new Button(2, 220, 232, 16, 16, -1).setTooltip("designer.load"));
		guiComps.add(new Button(3, 220, 214, 16, 16, 0).texture(36, 144).setTooltip("designer.wire#"));
		guiComps.add(new Button(4, 183, 173, 18, 9, 0).texture(0, 144).setTooltip("designer.in#"));
		guiComps.add(new Button(5, 201, 173, 18, 9, 0).texture(18, 144).setTooltip("designer.out#"));
		guiComps.add(new Button(6, 219, 173, 18, 9, -1).setTooltip("designer.del"));
		guiComps.add(new TextField(7, 184, 188, 52, 7, 10).color(0xffc0c000, 0xffff0000).setTooltip("designer.label"));
		guiComps.add(new Button(8, 153, 144, 47, 11, -1).setTooltip("designer.addOut"));
		guiComps.add(new Text<>(9, 153, 147, 84, 8, "designer.mods"));
		guiComps.add(new Slider(10, 228, 22, 116, 52, 144, 8, 12, false).scroll(ScrollSize).setTooltip("designer.scroll"));
		guiComps.add(new ModuleList(11, 202, 16).setTooltip("designer.mod"));
		guiComps.add(new WorkPane(12, 8, 16, 8));
		guiComps.add(new InfoTab(13, 7, 6, 7, 8, "designer.info"));
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		mc.renderEngine.bindTexture(BG_TEX);
		this.drawTexturedModalRect(guiLeft, guiTop, bgTexX, bgTexY, xSize, ySize);
		super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
	}

	@Override
	protected Object getDisplVar(int id) {
		Module m;
		switch(id) {
		case 0: return tile.name;
		case 3: return tile.renderAll ? 1 : 0;
		case 4: {
			if (tile.selMod < 0 || (m = tile.modules[tile.selMod]) == null || m.cons.length == 0) return 0;
			Con c = m.cons[m.selCon];
			if (c == null) return 0;
			return (int)c.type + 1;
		}
		case 5:
			if (tile.selMod < 0 || (m = tile.modules[tile.selMod]) == null) return 0;
			if (m.type.isNum) return m.size;
			if (m.type.varInAm) return m.cons() + 4;
			else return 0;
		case 7:
			if (tile.selMod < 0 || (m = tile.modules[tile.selMod]) == null) return "";
			return m.label;
		case 10: return (float)scroll * ScrollSize;
		default: return null;
		}
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		Module m;
		switch(id) {
		case 0: {
			PacketBuffer dos = BlockGuiHandler.getPacketTargetData(tile.pos());
			dos.writeByte(3);
			dos.writeString(tile.name = (String)obj);
			BlockGuiHandler.sendPacketToServer(dos);
		} break;
		case 1:
			if (isShiftKeyDown()) saveAsFile();
			else {
				PacketBuffer dos = BlockGuiHandler.getPacketTargetData(tile.pos());
				dos.writeByte(1);
				BlockGuiHandler.sendPacketToServer(dos);
			} break;
		case 2:
			if (isShiftKeyDown()) readAsFile();
			else {
				PacketBuffer dos = BlockGuiHandler.getPacketTargetData(tile.pos());
				dos.writeByte(2);
				BlockGuiHandler.sendPacketToServer(dos);
			} break;
		case 3: {
			PacketBuffer dos = BlockGuiHandler.getPacketTargetData(tile.pos());
			dos.writeByte((tile.renderAll = !tile.renderAll) ? 7 : 6);
			BlockGuiHandler.sendPacketToServer(dos);
		} break;
		case 4:
			if (tile.selMod < 0 || (m = tile.modules[tile.selMod]) == null || m.cons.length == 0) break;
			Con c = m.cons[m.selCon];
			if (c == null) break;
			if (c.type < 4) {
				c.type = (byte)((c.type + ((Integer)obj == 0 ? 1 : 3)) % 4);
				tile.modified++;
			} else if (m.type.can8bit) {
				c.type ^= 1;
				tile.modified++;
			} break;
		case 5:
			if (tile.selMod < 0 || (m = tile.modules[tile.selMod]) == null) break;
			if (m.type.isNum) {
				m.resize(1 + (m.size + ((Integer)obj == 0 ? 0 : 2)) % 4);
				tile.modified++;
			} else if (m.type.varInAm) {
				if ((Integer)obj == 0) m.addCon();
				else m.removeCon();
				tile.modified++;
			} break;
		case 6:
			if (tile.selMod >= 0) {
				tile.modules[tile.selMod] = null;
				tile.selMod = -1;
				tile.modified++;
			} break;
		case 7:
			if (tile.selMod < 0 || (m = tile.modules[tile.selMod]) == null) break;
			m.label = (String)obj;
			if (m.type == ModuleType.CST) try {
				int s = 32 - m.size * 8;
				m.label = Integer.toString((Integer.parseInt(m.label) << s) >> s);
			} catch(NumberFormatException e) {m.label = "0";}
			tile.modified++;
			break;
		case 10: scroll = (int)((Float)obj / ScrollSize); break;
		case 8: obj = ModuleType.OUT;
		case 11: tile.add((ModuleType)obj); break;
		}
	}

	@Override
	protected void keyTyped(char c, int k) throws IOException {
		if (k == Keyboard.KEY_PRIOR && scroll > 0) scroll--;
		else if (k == Keyboard.KEY_NEXT && scroll < 1F / ScrollSize) scroll++;
		else if (k == Keyboard.KEY_DELETE) setDisplVar(6, null, false);
		super.keyTyped(c, k);
	}

	@Override
	public void onGuiClosed() {
		super.onGuiClosed();
		if (tile.modified != 0) save();
	}

	@Override
	public void updateScreen() {
		if (tile.modified != 0) save();
		super.updateScreen();
	}

	private void save() {
		tile.modified = 0;
		PacketBuffer dos = BlockGuiHandler.getPacketTargetData(tile.pos());
		dos.writeByte(0);
		dos.writeBytes(tile.serialize());
		BlockGuiHandler.sendPacketToServer(dos);
	}

	private void saveAsFile() {
		if (tile.name.isEmpty()) sendChat(TooltipUtil.format("gui.cd4017be.designer.noname"));
		File file = new File(Minecraft.getMinecraft().mcDataDir, "circuitSchematics/" + tile.name + ".dat");
		try {
			ByteBuf data = tile.serialize();
			file.getParentFile().mkdirs();
			file.createNewFile();
			FileOutputStream fos = new FileOutputStream(file);
			data.readBytes(fos, data.writerIndex());
			fos.close();
			sendChat(TooltipUtil.format("gui.cd4017be.designer.saved", file));
		} catch (Exception e) {
			sendChat(e.toString());
		}
	}

	private void readAsFile() {
		File file = new File(Minecraft.getMinecraft().mcDataDir, "circuitSchematics/" + tile.name + ".dat");
		try {
			FileInputStream fis = new FileInputStream(file);
			ByteBuf data = Unpooled.buffer();
			int i;
			while ((i = fis.available()) > 0)
				data.writeBytes(fis, i);
			fis.close();
			tile.deserialize(data);
			tile.modified++;
			sendChat(TooltipUtil.format("gui.cd4017be.designer.loaded", file));
		} catch(FileNotFoundException e) {
			sendChat(TooltipUtil.format("gui.cd4017be.designer.notFound", file));
		} catch(Exception e) {
			sendChat(e.toString());
		}
	}

	private void drawSmallNum(int x, int y, String i, int w) {
		char[] cs = i.toCharArray();
		if (cs.length > w) {
			int scale = (cs.length + w - 1) / w;
			GlStateManager.pushMatrix();
			GlStateManager.scale(1F/(float)scale, 1F/(float)scale, 1);
			x *= scale; y *= scale;
		}
		for (char c : cs) {
			drawTexturedModalRect(x, y, c == '-' ? 40 : 4 * (c - '0'), 250, 4, 6);
			x += 4;
		}
		if (cs.length > w) GlStateManager.popMatrix();
	}

	private void drawScaledString(int x, int y, int width, int height, String s, int color) {
		int w = fontRenderer.getStringWidth(s);
		boolean doScale = w > width || fontRenderer.FONT_HEIGHT - 1 > height;
		if (doScale) {
			int scale = Math.max((w + width - 1) / width, (fontRenderer.FONT_HEIGHT + height - 2) / height);
			GlStateManager.pushMatrix();
			GlStateManager.scale(1F/(float)scale, 1F/(float)scale, 1);
			x *= scale; y *= scale;
		}
		fontRenderer.drawString(s, x, y, color);
		if (doScale) GlStateManager.popMatrix();
	}

	class ModuleList extends GuiComp<ModuleType> {

		public ModuleList(int id, int px, int py) {
			super(id, px, py, 24, 128);
		}

		@Override
		public void drawOverlay(int mx, int my) {
			int i = (my - py) / 16 + scroll;
			if (i < 0 || i >= ModuleType.values().length) return;
			ArrayList<String> list = new ArrayList<String>();
			for (String s : TooltipUtil.translate("gui.cd4017be."+ tooltip + i).split("\n")) list.add(s);
			ModuleType mt = ModuleType.values()[i];
			int j = Assembler.logicCost(mt, 1), k = Assembler.logicCost(mt, 4);
			String l = j == k ? Integer.toString(j) : Integer.toString(j) + "-" + Integer.toString(k);
			j = Assembler.calcCost(mt, 1); k = Assembler.calcCost(mt, 4);
			String c = j == k ? Integer.toString(j) : Integer.toString(j) + "-" + Integer.toString(k);
			list.add(TooltipUtil.format("gui.cd4017be.designer.cost", l, c));
			drawHoveringText(list, mx, my, fontRenderer);
		}

		@Override
		public void draw() {
			mc.renderEngine.bindTexture(MAIN_TEX);
			for (int i = 0; i < 8; i++) {
				int j = scroll + i;
				drawTexturedModalRect(px, py + i * 16, (j >> 3) * 24, (j & 7) * 16, 24, 16);
			}
		}

		@Override
		public boolean mouseIn(int x, int y, int b, int d) {
			if (d != 0) return false;
			int i = (y - py) / 16 + scroll;
			if (i >= 0 && i < ModuleType.values().length) {
				set.accept(ModuleType.values()[i]);
				return true;
			} else return false;
		}

	}

	class WorkPane extends GuiComp<Object> {

		int targetPos = -1;

		public WorkPane(int id, int px, int py, int h) {
			super(id, px, py, 192, h * 16 + 12);
		}

		@Override
		public void draw() {
			mc.renderEngine.bindTexture(MAIN_TEX);
			GlStateManager.enableBlend();
			Module mod;
			for (int i = 0; i < tile.modules.length; i++)
				if ((mod = tile.modules[i]) != null) {
					int x1 = px + (i & 7) * 24;
					int y1 = py + (i >> 3) * 16;
					int t = mod.type.ordinal();
					drawTexturedModalRect(x1, y1, (t >> 3) * 24, (t & 7) * 16, 24, 16);
					for (int j = 1; j < mod.size; j++)
						drawTexturedModalRect(px + (i + j & 7) * 24, py + ((i + j) >> 3) * 16, j * 24, 128, 24, 16);
					if (mod.type.varInAm) {
						for (int j = 0; j < mod.cons.length; j++)
							if (mod.cons[j] == null)
								drawTexturedModalRect(x1 + 1, y1 + mod.type.conRenderPos(j), 0, 128, 4, 4);
					}
					if (mod.type == ModuleType.CST)
						drawSmallNum(x1 + 3, y1 + 9, mod.label, 4);
					else if (mod.type == ModuleType.IN) {
						drawScaledString(x1 + 2, y1 + 10, 20, 7, mod.label, 0xffffffff);
						mc.renderEngine.bindTexture(MAIN_TEX);
					} else if (mod.type == ModuleType.OUT) {
						drawScaledString(x1 + 2, y1 + 7, 20, 4, mod.label, 0xffffffff);
						mc.renderEngine.bindTexture(MAIN_TEX);
					}
					if (i == tile.selMod) {
						drawTexturedModalRect(x1, y1, 96, 128, 24, 16);
						if (mod.cons.length > 0) {
							Con c = mod.cons[mod.selCon];
							if (c != null)
								drawTexturedModalRect(x1 + 1, y1 + mod.type.conRenderPos(mod.selCon), 0, c.type < 4 ? 136 : 132, 4, 4);
						}
					}
				}
			for (Module m : tile.modules)
				if (m != null)
					drawModuleCons(m);
			if (targetPos >= 0) {
				int x1 = px + (targetPos & 7) * 24;
				int y1 = py + (targetPos >> 3) * 16;
				drawTexturedModalRect(x1, y1, 120, 128, 24, 16);
			}
		}

		private void drawModuleCons(Module m) {
			int x1 = px + (m.pos & 7) * 24;
			int y1 = py + (m.pos >> 3) * 16;
			GlStateManager.disableTexture2D();
			BufferBuilder vb = Tessellator.getInstance().getBuffer();
			vb.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
			int r, g, b, a = tile.renderAll || m.pos == tile.selMod ? 0xff : 0x60;
			for (int i = 0; i < m.cons.length; i++) {
				Con c = m.cons[i];
				if (c == null || c.addr < 0) continue;
				g = m.pos == tile.selMod && i == m.selCon ? 0xc0 : 0;
				if (c.type < 4) {r = 0; b = 0xff;}
				else {r = 0xff; b = 0;}
				int addr = c.getAddr(), y2 = y1 + m.type.conRenderPos(i) + 2;
				for (int n = c.type < 4 ? c.type : 0; n >= 0; n--, addr++) {
					vb.pos(x1 + 3, y2, zLevel).color(r,g,b,a).endVertex();
					vb.pos(px + (addr & 7) * 24 + 22, (double)(py + (addr >> 3) * 16) + 8.5, zLevel).color(r,g,b,a).endVertex();
				}
			}
			Tessellator.getInstance().draw();
			GlStateManager.enableTexture2D();
		}

		@Override
		public void drawOverlay(int x, int y) {
			int mp = (x - px) / 24 + (y - py) / 16 * 8;
			Module m = tile.find(mp);
			if (m != null && !m.label.isEmpty())
				drawHoveringText(Arrays.asList(m.label), x, y);
		}

		@Override
		public boolean mouseIn(int x, int y, int b, int d) {
			Module selMod = tile.selMod >= 0 ? tile.modules[tile.selMod] : null;
			if (d == 3 && selMod != null) {
				if (selMod.cons.length == 0) return true;
				selMod.selCon = Math.floorMod(selMod.selCon + b, selMod.cons());
				return true;
			}
			int mp = (x - px) / 24 + (y - py) / 16 * 8;
			if (d == 1 && b == 0 && selMod != null) {
				targetPos = targetPos >= 0 || mp == tile.selMod ? mp : -1;
				return true;
			} else if (d == 2 && b == 0) setFocus(-1);
			targetPos = -1;
			if (d != 0) return true;
			Module m = tile.find(mp);
			if (b == 0) tile.selMod = m == null ? -1 : mp;
			else if (b == 1 && selMod != null && selMod.cons.length > 0 && mp < 64) {
				Con c = selMod.cons[selMod.selCon];
				if (c == null) return true;
				c.setAddr(m, mp);
				tile.modified = 1;
			}
			return true;
		}

		@Override
		public void unfocus() {
			if (targetPos >= 0 && tile.selMod >= 0 && targetPos != tile.selMod && targetPos < tile.modules.length) {
				tile.move(tile.selMod, targetPos);
				tile.selMod = targetPos;
			}
		}

		@Override
		public boolean focus() {return true;}

	}

}
