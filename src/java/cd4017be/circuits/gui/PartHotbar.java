package cd4017be.circuits.gui;

import java.util.ArrayList;
import java.util.function.Consumer;

import cd4017be.circuits.editor.op.OpType;
import static cd4017be.circuits.editor.op.OpType.*;
import cd4017be.lib.Gui.comp.GuiCompBase;
import cd4017be.lib.Gui.comp.GuiCompGroup;
import cd4017be.lib.util.TooltipUtil;

/**
 * @author CD4017BE
 *
 */
public class PartHotbar extends GuiCompBase<GuiCompGroup> {

	static final OpType[] LIST_ORDER = {
			in, out, read, write, bsplt, bcomb,
			cst, not, or, nor, and, nand, xor, xnor,
			not0, is0, nsgn, psgn,
			eq, neq, ls, geq,
			inc, dec, neg, abs,
			add, sub, mul, div, mod, bsl, bsr, usr,
			max, min, sort, swt, frg
		};

	final Consumer<OpType> pick;
	final int hotbarSize;
	int scroll = 0;

	public PartHotbar(GuiCompGroup parent, int w, int h, int x, int y, Consumer<OpType> pick) {
		super(parent, w, h, x, y);
		this.pick = pick;
		this.hotbarSize = w / 16;
	}

	@Override
	public void drawOverlay(int mx, int my) {
		int i = (mx - x) / 16 + scroll;
		if (i < 0 || i >= LIST_ORDER.length) return;
		OpType t = LIST_ORDER[i];
		ArrayList<String> list = new ArrayList<String>();
		for (String s : TooltipUtil.translate("gui.circuits.op." + t.name()).split("\n")) list.add(s);
		//int j = Assembler.logicCost(mt, 1), k = Assembler.logicCost(mt, 4);
		//String l = j == k ? Integer.toString(j) : Integer.toString(j) + "-" + Integer.toString(k);
		//j = Assembler.calcCost(mt, 1); k = Assembler.calcCost(mt, 4);
		//String c = j == k ? Integer.toString(j) : Integer.toString(j) + "-" + Integer.toString(k);
		//list.add(TooltipUtil.format("gui.cd4017be.designer.cost", l, c));
		parent.drawTooltip(list, mx, my);
	}

	@Override
	public void drawBackground(int mx, int my, float t) {
		for (int i = 0, l = Math.min(hotbarSize, LIST_ORDER.length - scroll); i < l; i++) {
			OpType op = LIST_ORDER[i + scroll];
			int tex = op.texture, tw = tex >> 24 & 0xff, th = tex >> 16 & 0xff, tx = tex >> 8 & 0xff, ty = tex & 0xff;
			if (th > h) th = h;
			if (tw > 16) {
				if (op.ins == 0) tx += tw - 16;
				tw = 16;
			}
			parent.drawRect(x + i * 16 + (16-tw) / 2, y + (h-th) / 2, tx, ty, tw, th);
		}
	}

	@Override
	public boolean mouseIn(int mx, int my, int b, byte d) {
		if (d == 3) {
			if (b < 0 && scroll < LIST_ORDER.length - hotbarSize) scroll++;
			else if (b > 0 && scroll > 0) scroll--;
			else return true;
			return true;
		}
		if (d != 0) return false;
		int i = (mx - x) / 16 + scroll;
		if (i >= 0 && i < LIST_ORDER.length) {
			pick.accept(LIST_ORDER[i]);
			return true;
		} else return false;
	}

}