package cd4017be.circuits.gui;

import java.util.ArrayList;
import java.util.Arrays;

import cd4017be.circuits.editor.InvalidSchematicException;
import cd4017be.circuits.editor.InvalidSchematicException.ErrorType;
import cd4017be.circuits.editor.op.OpNode;
import cd4017be.lib.Gui.comp.GuiCompBase;
import cd4017be.lib.Gui.comp.GuiCompGroup;
import cd4017be.lib.util.TooltipUtil;


/**
 * @author CD4017BE
 *
 */
public class GuiErrorMarker extends GuiCompBase<GuiCompGroup> {

	private final GuiEditor gui;
	public InvalidSchematicException lastErr;
	

	/**
	 * @param parent
	 */
	public GuiErrorMarker(GuiEditor gui) {
		super(gui.compGroup, gui.compGroup.w, gui.compGroup.h, 0, 0);
		this.gui = gui;
	}

	public void update() {
		int e = gui.tile.ingreds[6];
		ErrorType t = ErrorType.get(e);
		if (t == null) {
			lastErr = null;
			setEnabled(false);
		} else if (lastErr == null || lastErr.type != t) {
			int n = e >> 8 & 0xffff;
			ArrayList<OpNode> ops = gui.tile.operators;
			OpNode node = n < ops.size() ? ops.get(n) : null;
			if (node != null) {
				gui.board.selPart = node.getBounds();
				gui.changeSelPart();
			}
			lastErr = new InvalidSchematicException(t, node, e >> 24 & 0xff);
			setEnabled(true);
		}
	}

	@Override
	public void drawOverlay(int mx, int my) {
		InvalidSchematicException lastErr = this.lastErr;
		if (lastErr == null) return;
		OpNode node = lastErr.node;
		int px, py;
		switch(lastErr.type) {
		case noCircuitBoard:
			px = 182;
			py = 240;
			break;
		case missingMaterial:
			px = 220;
			py = 234 + lastErr.pin * 6;
			break;
		case duplicateLabel:
		case invalidLabel:
			px = 211;
			py = 177;
			break;
		case invalidCfg:
			px = 211;
			py = 188;
			break;
		case readConflict:
		case writeConflict:
			if (node == null) return;
			px = (node.rasterX << 2) + 14;
			py = (node.rasterY << 2) + 19;
			break;
		case causalLoop:
		case missingInput:
			if (node == null) return;
			int[] pins = node.getInputPositions();
			if (lastErr.pin < pins.length) {
				int p = pins[lastErr.pin];
				px = ((p >> 16 & 0xffff) << 2) + 10;
				py = ((p & 0xffff) << 2) + 18;
				break;
			}
		default: return;
		}
		gui.mc.renderEngine.bindTexture(parent.mainTex);
		gui.drawTexturedModalRect(x + px - 4, y + py - 8, 248, 236, 8, 8);
		parent.drawTooltip(Arrays.asList(TooltipUtil.getConfigFormat("gui.circuits." + lastErr.type.name()).split("\n")), x + px, y + py);
	}

}
