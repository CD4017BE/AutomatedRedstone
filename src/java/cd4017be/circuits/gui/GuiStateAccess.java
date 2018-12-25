package cd4017be.circuits.gui;

import java.util.function.IntUnaryOperator;

import cd4017be.circuits.editor.op.ConstNode;
import cd4017be.lib.Gui.comp.FormatText;
import cd4017be.lib.Gui.comp.FrameGrip;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.Slider;
import cd4017be.lib.Gui.comp.TextField;
import cd4017be.lib.util.IntBiConsumer;
import net.minecraft.util.ResourceLocation;


/**
 * @author CD4017BE
 *
 */
public class GuiStateAccess extends GuiFrame {

	private static final ResourceLocation TEX = new ResourceLocation("circuits", "textures/gui/debugger.png");

	public final IntUnaryOperator get;
	public final IntBiConsumer set;
	int scroll;

	/**
	 * @param parent
	 * @param w
	 * @param h
	 * @param comps
	 */
	public GuiStateAccess(GuiFrame parent, int n, String[] labels, IntUnaryOperator get, IntBiConsumer set) {
		super(parent, 96, 22 + n * 18, 2 * n + 1);
		this.get = get;
		this.set = set;
		texture(TEX, 256, 256);
		title("gui.circuits.state.name", 0.5F);
		new FrameGrip(this, 8, 8, 0, 0);
		if (labels.length > n)
			new Slider(this, 8, 12, n * 18 - 2, 80, 16, 248, 0, false, ()-> scroll, (v)-> scroll = (int)Math.round(v), null, 0, labels.length - n);
		for (int i = 0; i < n; i++) {
			final int idx = i;
			new FormatText(this, 70, 9, 8, 16 + i * 18, "\\%s", ()-> new Object[] {labels[idx + scroll]});
			new TextField(this, 70, 7, 8, 25 + i * 18, 16, ()-> "" + get.applyAsInt(idx + scroll), (t)-> {try {
					set.accept(idx + scroll, ConstNode.parse(t));
				} catch (NumberFormatException e) {}});
		}
	}

	@Override
	public void drawBackground(int mx, int my, float t) {
		parent.drawNow();
		parent.bindTexture(mainTex);
		gui.drawTexturedModalRect(x, y, 86, 0, w, h - 8);
		gui.drawTexturedModalRect(x, y + h - 8, 86, 122, w, 8);
		super.drawBackground(mx, my, t);
	}

}
