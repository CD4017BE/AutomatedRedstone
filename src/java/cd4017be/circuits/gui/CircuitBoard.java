package cd4017be.circuits.gui;

import static cd4017be.circuits.tileEntity.Editor.*;

import java.util.ArrayList;
import org.lwjgl.opengl.GL11;

import cd4017be.circuits.editor.BoundingBox2D;
import cd4017be.circuits.editor.PinRef;
import cd4017be.circuits.editor.op.ConstNode;
import cd4017be.circuits.editor.op.IONode;
import cd4017be.circuits.editor.op.OpNode;
import cd4017be.circuits.editor.op.OpType;
import cd4017be.circuits.editor.op.Pin;
import cd4017be.lib.Gui.comp.GuiCompBase;
import cd4017be.lib.Gui.comp.GuiFrame;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

/**
 * @author CD4017BE
 *
 */
public class CircuitBoard extends GuiCompBase<GuiFrame> {

	static final ResourceLocation TEX = new ResourceLocation("circuits", "textures/gui/circuit_parts.png");
	private final Runnable update;
	public final ArrayList<BoundingBox2D<OpNode>> parts = new ArrayList<>();
	public BoundingBox2D<OpNode> selPart;
	int originX, originY, moveX, moveY;
	PinRef selPin;
	private OpNode placing;

	public CircuitBoard(GuiFrame parent, int w, int h, int x, int y, Runnable update) {
		super(parent, w, h, x, y);
		this.update = update;
	}

	@Override
	public void drawBackground(int mx, int my, float t) {
		parent.bindTexture(parent.mainTex);
		for (BoundingBox2D<OpNode> part : parts)
			drawPart(part);
		if (placing != null) {
			placing.rasterX = (mx - x) / 4;
			placing.rasterY = (my - y) / 4;
			BoundingBox2D<OpNode> part = placing.getBounds();
			drawPart(part);
		}
		drawWires(mx, my);
		if (placing != null) {
			parent.drawNow();
			BoundingBox2D<OpNode> part = placing.getBounds();
			for (BoundingBox2D<OpNode> p : parts)
				if (part.overlapsWith(p))
					drawSelection(p, 0x80ff0000);
		} else if (selPart != null) {
			parent.drawNow();
			BoundingBox2D<OpNode> part = selPart;
			if (moveX != 0 || moveY != 0) {
				part = part.offset(moveX*2, moveY*2);
				for (BoundingBox2D<OpNode> p : parts)
					if (part.overlapsWith(p))
						drawSelection(p, 0x80ff0000);
			}
			drawSelection(part, part.enclosedBy(BOARD_AREA) ? 0x80c08000 : 0xffff0000);
		}
	}

	private void drawWires(int mx, int my) {
		GlStateManager.disableTexture2D();
		BufferBuilder vb = Tessellator.getInstance().getBuffer();
		vb.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
		int r = 0x7f, g = 0, b = 0, a = 0xff;
		int i0 = -1, i1 = -1;
		if (selPin != null) {
			i0 = selPin.source.index;
			i1 = selPin.index;
		}
		float z = parent.zLevel;
		for (BoundingBox2D<OpNode> part : parts) {
			OpNode op = part.owner;
			int[] pos = op.getInputPositions();
			Pin p;
			for (int i = 0, l = pos.length; i < l; i++) {
				if (op.index == i0 && i == i1) {
					int t = pos[i];
					vb.pos(x + 2.5F + (t >> 16) * 4, y + 2.5F + (t & 0xffff) * 4, z).color(r,g,b,a).endVertex();
					vb.pos(mx + 0.5F, my + 0.5F, z).color(r,g,b,a).endVertex();
				} else if ((p = op.inputs[i]) != null) {
					int t = pos[i];
					vb.pos(x + 2.5F + (t >> 16) * 4, y + 2.5F + (t & 0xffff) * 4, z).color(r,g,b,a).endVertex();
					t = p.src.getOutputPos(p.index);
					vb.pos(x + 2.5F + (t >> 16) * 4, y + 2.5F + (t & 0xffff) * 4, z).color(r,g,b,a).endVertex();
				}
			}
		}
		Tessellator.getInstance().draw();
		GlStateManager.enableTexture2D();
	}

	private void drawPart(BoundingBox2D<OpNode> part) {
		OpNode node = part.owner;
		OpType op = node.op;
		int t = op.texture, w = t >> 24 & 0xff, h = t >> 16 & 0xff;
		int px = x + 2 + part.x0 + part.x1 - w/2, py = y + 2 + part.y0 + part.y1 - h/2;
		if (node instanceof IONode || node instanceof ConstNode) {
			parent.gui.drawTexturedModalRect(px, py, t >> 8 & 0xff, t & 0xff, w, h);
			t += 68;
			String s = node.label;
			int dx, n = 6;
			switch(op) {
			case in: dx = 1; break;
			case out: dx = 7; break;
			case read: dx = 4; n = 5; break;
			case write: dx = 5; n = 5; break;
			case cst: dx = 4; n = 5; s = ((ConstNode)node).value; break;
			default: return;
			}
			drawTinyText(s, px + dx, py + 4, n);
		}
		parent.drawRect(px, py, t >> 8 & 0xff, t & 0xff, w, h);
	}

	private void drawTinyText(String s, int x, int y, int w) {
		char[] cs = s.toCharArray();
		boolean scaled = cs.length > w;
		if (scaled) {
			int scale = (cs.length + w - 1) / w;
			GlStateManager.pushMatrix();
			GlStateManager.scale(1F/(float)scale, 1F/(float)scale, 1);
			x *= scale; y *= scale; y += (scale - 1) * 5 / 2;
			w *= scale;
		}
		x += (w - cs.length) * 2;
		for (char c : cs) {
			parent.gui.drawTexturedModalRect(x, y, c << 2 & 0xfc, 244 + (c >> 6) * 6, 4, 6);
			x += 4;
		}
		if (scaled) GlStateManager.popMatrix();
	}

	private void drawSelection(BoundingBox2D<OpNode> part, int c) {
		int x0 = x + 2 + part.x0 * 2,
			x1 = x + 2 + part.x1 * 2,
			y0 = y + 2 + part.y0 * 2,
			y1 = y + 2 + part.y1 * 2;
		Gui.drawRect(x0, y0, x1, y0 + 1, c);
		Gui.drawRect(x0 + 1, y1, x1 + 1, y1 + 1, c);
		Gui.drawRect(x0, y0 + 1, x0 + 1, y1 + 1, c);
		Gui.drawRect(x1, y0, x1 + 1, y1, c);
	}

	@Override
	public boolean mouseIn(int mx, int my, int b, byte d) {
		mx = (mx - x) / 2;
		my = (my - y) / 2;
		if (b == 0) {
			if (placing != null && d != 1) {
				parent.gui.sendPkt(A_ADD, (byte)placing.op.ordinal(), (byte)(mx/2), (byte)(my/2));
				placing = null;
				return true;
			}
			if (d == 0) {
				selPart = findPart(mx-1, my-1);
				update.run();
				originX = mx/2;
				originY = my/2;
			} else if (d == 2 && selPart != null) unfocus();
			moveX = mx/2 - originX;
			moveY = my/2 - originY;
		} else if (b == 1 && d == 0) {
			PinRef pin = findPin(mx/2, my/2);
			if (pin != null && pin.index < pin.source.inputs.length) selPin = pin;
			else if (selPin != null) {
				parent.gui.sendPkt(A_CONNECT, (byte)selPin.source.index,
					pin == null ? (byte)-1 : (byte)pin.source.index,
					(byte)(selPin.index | (pin == null ? 0xf0 : (pin.index - pin.source.inputs.length) << 4)));
				selPin = null;
			}
			return true;
		} else if (b == 2 && d != 0) {
			BoundingBox2D<OpNode> part = findPart(mx-1, my-1);
			if (part != null) placing = part.owner.op.node(part.owner.index);
			selPart = null;
			update.run();
		}
		return true;
	}

	@Override
	public void unfocus() {
		if (selPart != null && (moveX != 0 || moveY != 0)) {
			if (selPart.offset(moveX*2, moveY*2).enclosedBy(BOARD_AREA))
				parent.gui.sendPkt(A_MOVE, (byte)selPart.owner.index,
					(byte)(selPart.owner.rasterX + moveX),
					(byte)(selPart.owner.rasterY + moveY));
			else parent.gui.sendPkt(A_REM, (byte)selPart.owner.index);
			originX += moveX;
			originY += moveY;
		}
	}

	@Override
	public boolean focus() {return true;}

	private BoundingBox2D<OpNode> findPart(int x, int y) {
		for (BoundingBox2D<OpNode> part : parts)
			if (part.isPointInside(x, y)) {
				return part;
			}
		return null;
	}

	private PinRef findPin(int x, int y) {
		BoundingBox2D<OpNode> point = new BoundingBox2D<OpNode>(null, x*2-1, y*2-1, 2, 2);
		for (BoundingBox2D<OpNode> part : parts)
			if (part.overlapsWith(point)) {
				PinRef pin = part.owner.findPin(x, y);
				if (pin != null) return pin;
			}
		return null;
	}

	public void place(OpType op) {
		this.placing = op.node(0);
	}

	public void del(int b) {
		if (selPart != null)
			parent.gui.sendPkt(A_REM, (byte)selPart.owner.index);
	}

}