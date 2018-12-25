package cd4017be.circuits.gui;

import org.lwjgl.input.Keyboard;

import cd4017be.api.circuits.Chip;
import cd4017be.api.circuits.IAdjustable;
import cd4017be.circuits.editor.op.ConstNode;
import cd4017be.lib.Gui.comp.Button;
import cd4017be.lib.Gui.comp.FormatText;
import cd4017be.lib.Gui.comp.FrameGrip;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.Spinner;
import cd4017be.lib.Gui.comp.TextField;
import net.minecraft.util.ResourceLocation;


/**
 * @author CD4017BE
 *
 */
public class GuiDebugger extends GuiFrame {

	private static final ResourceLocation TEX = new ResourceLocation("circuits", "textures/gui/debugger.png");
	public final Chip chip;
	private int interval = 1, timer = Integer.MIN_VALUE;
	private final int[] inputs, outputs;
	private int[] state;
	private final GuiStateAccess variables;

	/**
	 * @param parent
	 * @param chip
	 */
	public GuiDebugger(GuiFrame parent, Chip chip) {
		this(parent, chip, chip.inputs().length, chip.outputs().length);
	}

	private GuiDebugger(GuiFrame parent, Chip chip, int in, int out) {
		super(parent, 86, 40 + (in + out) * 18, 4 + in * 2 + out);
		this.chip = chip;
		texture(TEX, 256, 256);
		title("gui.circuits.debug.name", 0.5F);
		new FrameGrip(this, 8, 8, 0, 0);
		new Spinner(this, 36, 18, 7, 15, false, "\\%.2fs", ()-> (double)interval / 20.0, (v)-> interval = (int)Math.round(v * 20.0), 0.05, 60.0, 1.0, 0.05).tooltip("gui.circuits.interval");
		new Button(this, 18, 18, 43, 15, 2, ()-> timer < -interval ? 1 : 0, (s)-> timer = s == 0 ? -interval : Integer.MIN_VALUE).texture(230, 0).tooltip("gui.circuits.debug.run#");
		new Button(this, 18, 18, 61, 15, 0, ()-> chip.dirty ? 1 : 0, (i)-> {if (chip.dirty) chip.update();}).texture(230, 36).tooltip("gui.circuits.debug.step#");
		this.inputs = new int[in];
		this.outputs = new int[out];
		String[] labels = chip.inputs();
		for (int i = 0; i < in; i++) {
			final String label = labels[i];
			final int idx = i;
			new FormatText(this, 70, 9, 8, 34 + i * 18, "\\" + label, null).color(0xff00007f);
			chip.connectInput(i, ()-> inputs[idx]);
			new TextField(this, 70, 7, 8, 43 + i * 18, 16, ()-> "" + inputs[idx], (t)-> {try {
					inputs[idx] = ConstNode.parse(t);
					chip.dirty = true;
				} catch (NumberFormatException e) {}});
		}
		labels = chip.outputs();
		for (int i = 0; i < out; i++) {
			final String label = labels[i];
			final int idx = i;
			new FormatText(this, 70, 9, 8, 34 + (i + in) * 18, "\\" + label + "\n%d", ()-> new Object[] {outputs[idx]}).color(0xff007f00);
			chip.connectOutput(i, (v)-> outputs[idx] = v);
		}
		if (chip instanceof IAdjustable) {
			IAdjustable adj = (IAdjustable)chip;
			labels = adj.getLabels();
			this.variables = new GuiStateAccess(parent, labels.length < 6 ? labels.length : 6, labels, (i)-> state[i], (i, v) -> {adj.setParam(i, v); chip.dirty = true;});
			this.variables.position(gui.getGuiLeft() + w, gui.getGuiTop());
			this.variables.init(parent.screenWidth, parent.screenHeight, parent.zLevel, parent.fontRenderer);
			state = adj.getStates();
		} else variables = null;
	}

	public void update() {
		if (++timer < 0) return;
		timer -= interval;
		if (chip.dirty) {
			chip.update();
			if (chip instanceof IAdjustable)
				state = ((IAdjustable)chip).getStates();
		}
	}

	@Override
	public void drawBackground(int mx, int my, float t) {
		parent.drawNow();
		parent.bindTexture(mainTex);
		int in = 33 + inputs.length * 18,
			out = 7 + outputs.length * 18;
		gui.drawTexturedModalRect(x, y, 0, 0, 86, in);
		gui.drawTexturedModalRect(x, y + in, 0, 256 - out, 86, out);
		super.drawBackground(mx, my, t);
	}

	@Override
	public boolean keyIn(char c, int k, byte d) {
		if (k == Keyboard.KEY_ESCAPE) {
			parent.remove(this);
			parent.remove(variables);
			return true;
		}
		return super.keyIn(c, k, d);
	}

}
