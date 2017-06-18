package cd4017be.circuits.gui;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import cd4017be.circuits.tileEntity.CircuitDesigner;
import cd4017be.circuits.tileEntity.CircuitDesigner.Con;
import cd4017be.circuits.tileEntity.CircuitDesigner.Module;
import cd4017be.circuits.tileEntity.CircuitDesigner.ModuleType;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.TooltipInfo;
import cd4017be.lib.Gui.GuiMachine;
import cd4017be.lib.Gui.TileContainer;

public class GuiCircuitDesigner extends GuiMachine {

	private static final float ScrollSize = 24F;
	private static final ResourceLocation COMP_TEX =  new ResourceLocation("circuits", "textures/gui/circuitOperators.png");
	private final CircuitDesigner tile;
	private int scroll;

	public GuiCircuitDesigner(CircuitDesigner tile, EntityPlayer player) {
		super(new TileContainer(tile, player));
		this.MAIN_TEX = new ResourceLocation("circuits", "textures/gui/circuitDesigner.png");
		this.tile = tile;
	}

	@Override
	public void initGui() {
		xSize = 236;
		ySize = 244;
		super.initGui();
		this.titleX = xSize / 4;
		for (int i = 0; i < 10; i++)
			guiComps.add(new Button(i, 7, 15 + i * 9, 18, 9, -1).setTooltip("designer.m" + (char)(i + '0')));
		guiComps.add(new Button(10, 25, 144, 18, 9, -1).setTooltip("designer.mA"));
		guiComps.add(new Button(11, 43, 144, 18, 9, -1).setTooltip("designer.mB"));
		for (int i = 0; i < 5; i++)
			guiComps.add(new Button(12 + i, 7, 108 + i * 9, 18, 9, -1).setTooltip("designer.m" + (char)(i + 'C')));
		guiComps.add(new Button(17, 151, 144, 18, 9, 0).texture(238, 158).setTooltip("designer.io#"));
		guiComps.add(new Button(18, 191, 144, 18, 9, -1).setTooltip("designer.del"));
		guiComps.add(new Slider(19, 219, 22, 116, 248, 0, 8, 12, false).scroll(1F/ScrollSize).setTooltip("designer.scroll"));
		guiComps.add(new Button(20, 174, 161, 18, 9, 0).texture(238, 81).setTooltip("designer.out#"));
		guiComps.add(new Button(21, 210, 161, 18, 9, 0).texture(238, 27).setTooltip("designer.in#"));
		guiComps.add(new TextField(22, 175, 176, 52, 7, 10).color(0xff0000ff, 0xffff0000).setTooltip("designer.const"));
		guiComps.add(new TextField(23, 175, 189, 52, 7, 10).color(0xffc0c000, 0xffff0000).setTooltip("designer.label"));
		guiComps.add(new Button(24, 201, 161, 9, 9, 0).texture(238, 0).setTooltip("designer.sgn#"));
		guiComps.add(new Button(25, 212, 202, 16, 16, 0).texture(240, 126).setTooltip("designer.wire#"));
		guiComps.add(new TextField(26, 118, 4, 109, 8, 16).setTooltip("designer.Pname"));
		guiComps.add(new Button(27, 176, 220, 16, 16, -1).setTooltip("designer.save"));
		guiComps.add(new Button(28, 212, 220, 16, 16, -1).setTooltip("designer.load"));
		guiComps.add(new Button(29, 194, 202, 16, 16, 0).texture(240, 176).setTooltip("designer.mode#"));
		guiComps.add(new WorkPane(30, 25, 16, 8));
		guiComps.add(new GuiComp(31, 7, 6, 7, 8).setTooltip("designer.info"));
	}

	@Override
	protected Object getDisplVar(int id) {
		switch(id) {
		case 17: return tile.mode ? 1 : 0;
		case 19: return (float)scroll / ScrollSize;
		case 20: return tile.selMod != null ? tile.selMod.size / 8 : 0;
		case 21: if (tile.selMod != null && tile.selMod.type != ModuleType.IN) {
				Con c = tile.selMod.cons[tile.selMod.selCon];
				return c != null ? c.type == 1 ? 5 : c.size / 8 : 0;
			} else return 0;
		case 22: if (tile.selMod == null) return "";
			if (tile.selMod.type == ModuleType.IN) return "" + tile.selMod.size;	
			else {
				Con c = tile.selMod.cons[tile.selMod.selCon];
				return c != null && c.type == 1 ? "" + c.size : "";
			}
		case 23: return tile.selMod != null ? tile.selMod.label : "";
		case 24: if (tile.selMod != null && tile.selMod.type != ModuleType.IN) {
				Con c = tile.selMod.cons[tile.selMod.selCon];
				return c != null && c.type >= 2 ? c.type - 1 : 0;
			} else return 0;
		case 25: return tile.renderAll ? 1 : 0;
		case 26: return tile.name;
		case 29: return tile.mode ? 1 : 0;
		default: return null;
		}
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		if (id < 18) tile.add(ModuleType.values()[id]);
		else switch(id) {
		case 18: if (tile.selMod != null) {
				tile.remove(tile.selMod);
				tile.selMod = null;
				tile.modified++;
			} break;
		case 19: scroll = (int)((Float)obj * ScrollSize); break;
		case 20: if (tile.selMod != null && tile.selMod.size > 1) {
				tile.selMod.resize(8 + (tile.selMod.size / 8 + ((Integer)obj == 0 ? 0 : 2)) % 4 * 8);
				tile.modified++;
			} break;
		case 21: if (tile.selMod !=null && tile.selMod.type.conType(tile.selMod.selCon)) {
				Con c = tile.selMod.cons[tile.selMod.selCon];
				int i = c.type == 1 ? 0 : c.size / 8;
				i = (i + ((Integer)obj == 0 ? 1 : 4)) % 5;
				if (i == 0) {
					c.mod = null;
					c.addr = -1;
					c.size = 0;
					c.type = 1;
				} else {
					if (c.type == 1) {
						c.mod = tile.selMod;
						c.addr = 0;
						c.type = 2;
					}
					c.size = i * 8;
				} 
				tile.modified++;
			} break;
		case 22: if (tile.selMod == null) break;
			if (tile.selMod.type == ModuleType.IN) try {
				int n = Integer.parseInt((String)obj);
				if (n <= 0) n = 1;
				else if (n > 32) n = 32;
				else if (n > 8) n &= 0xf8;
				tile.selMod.resize(n);
				tile.modified++;
			} catch (NumberFormatException e) {}
			else {
				Con c = tile.selMod.cons[tile.selMod.selCon];
				if (c != null && c.type == 1) try {
					c.size = Integer.parseInt((String)obj);
					tile.modified++;
				} catch (NumberFormatException e) {};
			} break;
		case 23: if (tile.selMod != null) {
				tile.selMod.label = (String)obj;
				tile.modified++;
			} break;
		case 24: if (tile.selMod != null) {
				Con c = tile.selMod.cons[tile.selMod.selCon];
				if (c != null && c.type >= 2) {
					c.type = (byte)(5 - c.type);
					tile.modified++;
				}
			} break;
		case 25: {
				PacketBuffer dos = tile.getPacketTargetData();
				dos.writeByte((tile.renderAll = !tile.renderAll) ? 7 : 6);
				BlockGuiHandler.sendPacketToServer(dos);
			} break;
		case 26: {
				PacketBuffer dos = tile.getPacketTargetData();
				dos.writeByte(3);
				dos.writeString(tile.name = (String)obj);
				BlockGuiHandler.sendPacketToServer(dos);
			} break;
		case 27: if (isShiftKeyDown()) saveAsFile();
			else {
				PacketBuffer dos = tile.getPacketTargetData();
				dos.writeByte(1);
				BlockGuiHandler.sendPacketToServer(dos);
			} break;
		case 28: if (isShiftKeyDown()) readAsFile();
			else {
				PacketBuffer dos = tile.getPacketTargetData();
				dos.writeByte(2);
				BlockGuiHandler.sendPacketToServer(dos);
			} break;
		case 29: {
				PacketBuffer dos = tile.getPacketTargetData();
				dos.writeByte((tile.mode = !tile.mode) ? 5 : 4);
				dos.writeString(tile.name = (String)obj);
				BlockGuiHandler.sendPacketToServer(dos);
				tile.selMod = null;
			} break;
		}
	}

	@Override
	protected void keyTyped(char c, int k) throws IOException {
		if (k == Keyboard.KEY_PRIOR && scroll > 0) scroll--;
		else if (k == Keyboard.KEY_NEXT && scroll < ScrollSize) scroll++;
		else if (k == Keyboard.KEY_DELETE) setDisplVar(18, null, false);
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
		PacketBuffer dos = tile.getPacketTargetData();
		dos.writeByte(0);
		dos.writeBytes(tile.serialize());
		BlockGuiHandler.sendPacketToServer(dos);
	}

	private void saveAsFile() {
		if (tile.name.isEmpty()) sendChat(TooltipInfo.format("gui.cd4017be.designer.noname"));
		File file = new File(Minecraft.getMinecraft().mcDataDir, "circuitSchematics/" + tile.name + ".dat");
		try {
			ByteBuf data = tile.serialize();
			file.getParentFile().mkdirs();
			file.createNewFile();
			FileOutputStream fos = new FileOutputStream(file);
			data.readBytes(fos, data.writerIndex());
			fos.close();
			sendChat(TooltipInfo.format("gui.cd4017be.designer.saved", file));
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
			sendChat(TooltipInfo.format("gui.cd4017be.designer.loaded", file));
		} catch(FileNotFoundException e) {
			sendChat(TooltipInfo.format("gui.cd4017be.designer.notFound", file));
		} catch(Exception e) {
			sendChat(e.toString());
		}
	}

	private void drawSmallNum(int x, int y, int i, int w) {
		char[] cs = Integer.toString(i).toCharArray();
		if (cs.length > w) {
			int scale = (cs.length + w - 1) / w;
			GlStateManager.pushMatrix();
			GlStateManager.scale(1F/(float)scale, 1F/(float)scale, 1);
			x *= scale; y *= scale;
		}
		for (char c : cs) {
			drawTexturedModalRect(x, y, c == '-' ? 232 : 192 + 4 * (c - '0'), 250, 4, 6);
			x += 4;
		}
		if (cs.length > w) GlStateManager.popMatrix();
	}

	class WorkPane extends GuiComp<Object> {

		int targetPos = -1;

		public WorkPane(int id, int px, int py, int h) {
			super(id, px, py, 192, h * 16);
		}

		@Override
		public void draw() {
			mc.renderEngine.bindTexture(COMP_TEX);
			GlStateManager.enableDepth();
			GlStateManager.depthMask(true);
			GlStateManager.depthFunc(GL11.GL_ALWAYS);
			zLevel = 0.5F;
			drawTexturedModalRect(px, py, 0, 0, w, h/2);
			drawTexturedModalRect(px, py + h/2, 0, 0, w, h/2);
			GlStateManager.depthFunc(GL11.GL_EQUAL);
			GlStateManager.enableBlend();
			for (Module mod = tile.module0; mod != null; mod = mod.next)
				if (mod.nextPos > scroll*8 && mod.pos < scroll*8 + h / 2) {
					int x1 = px + (mod.pos & 7) * 24;
					int y1 = py + ((mod.pos >> 3) - scroll) * 16;
					int t = mod.type.ordinal();
					if (mod.type.idxLoc > 0) {
						drawTexturedModalRect(x1, y1, 0, 128 + (t - 10) * 16, 192, 16);
						for (int i = 8; i < mod.size; i+=8)
							drawTexturedModalRect(x1, y1 + i * 2, 0, 240, 192, 16);
						float dx = 192F / (float)(mod.cons.length + 1);
						float x = (float)(x1 - 8) + dx;
						for (int i = 0; i < mod.cons.length; i++, x += dx) {
							Con c = mod.cons[i];
							if (c != null && c.type == 1) drawSmallNum((int)x + 13, y1 + 9, c.size, 8);
						}
					} else if (mod.type == ModuleType.IN) {
						if (mod.size < 8) {
							drawTexturedModalRect(x1, y1, 192, 160, 24, 16);
							for (int i = 1; i < mod.size; i++)
								drawTexturedModalRect(x1 + i * 24, y1, 192, 176, 24, 16);
						} else {
							drawTexturedModalRect(x1, y1, 0, 96, 192, 16);
							for (int i = 8; i < mod.size; i+=8)
								drawTexturedModalRect(x1, y1 + i * 2, 0, 112, 192, 16);
						}
					} else {
						drawTexturedModalRect(x1, y1, 192, t * 16, 24, 16);
						if (mod.type.ordinal() >= 6 || mod.type.ordinal() < 10)
							for (Con c : mod.cons) {
								if (c != null && c.type == 1) drawSmallNum(x1 + 2, y1 + 2, c.size, 2);
								x1 += 13;
							}
					}
				}
			if (tile.renderAll)
				for (Module mod = tile.module0; mod != null; mod = mod.next)
					drawModuleCons(mod);
			if (tile.selMod != null && (tile.mode || !tile.renderAll)) drawModuleCons(tile.selMod);
			if (tile.mode) {
				for (Module mod : tile.outputs) {
					int x1 = px + (mod.pos & 7) * 24;
					int y1 = py + ((mod.pos >> 3) - scroll) * 16;
					if (mod.size < 8) {
						drawTexturedModalRect(x1, y1, 216, 160, 24, 16);
						for (int i = 1; i < mod.size; i++)
							drawTexturedModalRect(x1 + i * 24, y1, 216, 176, 24, 16);
					} else {
						drawTexturedModalRect(x1, y1, 0, 64, 192, 16);
						for (int i = 8; i < mod.size; i+=8)
							drawTexturedModalRect(x1, y1 + i * 2, 0, 80, 192, 16);
					}
				}
			}
			if (targetPos >= 0) {
				int x1 = px + (targetPos & 7) * 24;
				int y1 = py + ((targetPos >> 3) - scroll) * 16;
				drawTexturedModalRect(x1, y1, 192, 224, 24, 16);
			}
			GlStateManager.depthFunc(GL11.GL_LEQUAL);
			GlStateManager.depthMask(false);
			GlStateManager.disableDepth();
			zLevel = 0;
		}

		private void drawModuleCons(Module m) {
			int x1 = px + (m.pos & 7) * 24;
			int y1 = py + ((m.pos >> 3) - scroll) * 16;
			if (m == tile.selMod) drawTexturedModalRect(x1, y1, 192, 208, 24, 16);
			GlStateManager.disableTexture2D();
			VertexBuffer vb = Tessellator.getInstance().getBuffer();
			vb.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
			double dx = (m.type.idxLoc > 0 ? 192.0 : 24.0) / (double)(m.cons.length + 1), x2 = (double)x1 + dx;
			y1 += 12;
			if (m == tile.selMod && m.type != ModuleType.IN) {
				vb.pos(x2 + dx * (double)m.selCon, y1 - 1, zLevel).color(0xff,0,0,0xff).endVertex();
				vb.pos(x2 + dx * (double)m.selCon, y1 + 1, zLevel).color(0xff,0,0,0xff).endVertex();
			}
			int r, a = 0xff;
			for (int i = 0; i < m.cons.length; i++, x2 += dx) {
				Con c = m.cons[i];
				if (c == null || c.addr < 0) continue;
				r = m == tile.selMod && i == m.selCon ? 0xff: 0;
				if (c.type == 0) {
					vb.pos(x2, y1, zLevel).color(r,0,0xff,a).endVertex();
					int addr = c.getAddr();
					vb.pos(px + (addr & 7) * 24 + 22, py + ((addr >> 3) - scroll) * 16 + 8, zLevel).color(r,0,0xff,a).endVertex();
				} else {
					int addr = c.getAddr();
					for (int n = c.size / 8; n > 0; n--, addr+=8) {
						vb.pos(x2, y1, zLevel).color(r,0xff,0x80,a).endVertex();
						vb.pos(px + (addr & 7) * 24 + 188, py + ((addr >> 3) - scroll) * 16 + 8, zLevel).color(r,0xff,0x80,a).endVertex();
					}
				}
			}
			Tessellator.getInstance().draw();
			GlStateManager.enableTexture2D();
		}

		@Override
		public void drawOverlay(int x, int y) {
			int mp = (x - px) / 24 + ((y - py) / 16 + scroll) * 8;
			ArrayList<String> list = new ArrayList<String>();
			for (Module m : tile.outputs)
				if (m.pos <= mp && m.nextPos > mp && !m.label.isEmpty()) list.add(m.label);
			Module m = tile.find(tile.module0, mp, false);
			if (m != null && !m.label.isEmpty()) list.add(m.label);
			list.add(String.format("%d_%02X", mp/8, mp));
			drawHoveringText(list, x, y);
		}

		@Override
		public boolean mouseIn(int x, int y, int b, int d) {
			if (d == 3 && tile.selMod != null) {
				if (tile.selMod.type == ModuleType.IN) return true;
				tile.selMod.selCon += b + tile.selMod.cons.length;
				tile.selMod.selCon %= tile.selMod.cons.length;
				return true;
			}
			int mp = (x - px) / 24 + ((y - py) / 16 + scroll) * 8;
			if (d == 1 && b == 0 && tile.selMod != null) {
				targetPos = targetPos >= 0 || mp == tile.selMod.pos ? mp : -1;
				return true;
			} else if (d == 2 && b == 0) setFocus(-1);
			targetPos = -1;
			if (d != 0) return true;
			if (tile.mode) {
				for (Module m : tile.outputs)
					if (m.pos <= mp && m.nextPos > mp) {
						tile.selMod = m; return true;
					}
				tile.selMod = null;
			} else {
				Module m = tile.find(tile.module0, mp, false);
				if (b == 0) tile.selMod = m;
				else if (b == 1 && tile.selMod != null && tile.selMod.type != ModuleType.IN) {
					boolean t = tile.selMod.type.conType(tile.selMod.selCon);
					if (t) mp &= 0xf8;
					if (m != null) mp -= m.pos;
					Con c = tile.selMod.cons[tile.selMod.selCon];
					if (c == null) c = new Con(m, mp, 1, (byte)0);
					else if (c.getAddr() == mp && c.type == 0) c = null;
					else {
						c.mod = m;
						c.addr = mp;
						if (c.type == 1) {
							c.type = 2;
							c.size = 8;
						}
					}
					tile.selMod.cons[tile.selMod.selCon] = c;
					tile.modified = 1;
				}
			}
			return true;
		}

		@Override
		public void unfocus() {
			if (targetPos >= 0 && tile.selMod != null && targetPos != tile.selMod.pos) {
				tile.move(tile.selMod, targetPos);
			}
		}

		@Override
		public boolean focus() {return true;}

	}

}
