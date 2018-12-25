package cd4017be.circuits.gui;

import cd4017be.lib.Gui.DataContainer.IGuiData;

import static cd4017be.circuits.tileEntity.Editor.*;

import cd4017be.api.circuits.Chip;
import cd4017be.api.circuits.IChipItem;
import cd4017be.circuits.editor.BoundingBox2D;
import cd4017be.circuits.editor.op.IConfigurable;
import cd4017be.circuits.editor.op.OpNode;
import cd4017be.circuits.tileEntity.Editor;
import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.Gui.comp.Button;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.InfoTab;
import cd4017be.lib.Gui.comp.Progressbar;
import cd4017be.lib.Gui.comp.TextField;
import cd4017be.lib.Gui.comp.Tooltip;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;


/**
 * @author CD4017BE
 *
 */
public class GuiEditor extends ModularGui {

	private static final ResourceLocation BG_TEX = new ResourceLocation("circuits", "textures/gui/circuit_editor.png");
	private static final ResourceLocation MAIN_TEX = new ResourceLocation("circuits", "textures/gui/circuit_parts.png");

	public final Editor tile;
	public final CircuitBoard board;
	public final TextField editLabel, editCfg;
	public final GuiErrorMarker error;

	public GuiEditor(IGuiData tile, EntityPlayer player) {
		super(new TileContainer(tile, player));
		this.tile = (Editor)tile;
		GuiFrame comps = new GuiFrame(this, 256, 256, 17).background(BG_TEX, 0, 0).title(tile.getName(), 0.1F);
		comps.texture(MAIN_TEX, 256, 256);
		new InfoTab(comps, 7, 8, 7, 6, "gui.circuits.editor.info");
		new TextField(comps, 120, 8, 128, 4, 64, ()-> this.tile.name, (name)-> sendPkt(A_NAME, name)).tooltip("gui.circuits.editor.name");
		this.board = new CircuitBoard(comps, 240, 120, 8, 16, this::changeSelPart);
		new PartHotbar(comps, 240, 14, 8, 142, board::place);
		(this.editLabel = new TextField(comps, 74, 7, 174, 174, 20, this::getLabel, (s)-> send(A_SET_LABEL, s))).tooltip("gui.circuits.opLabel");
		this.editCfg = new TextField(comps, 74, 7, 174, 185, 20, this::getConfig, (s)-> send(A_SET_VALUE, s));
		new Button(comps, 18, 9, 231, 195, 0, null, board::del).tooltip("gui.circuits.editor.del");
		new Button(comps, 16, 16, 232, 210, 0, null, (i)-> sendCommand(A_NEW)).tooltip("gui.circuits.editor.new");
		new Button(comps, 16, 16, 214, 210, 0, null, (i)-> sendCommand(A_LOAD)).tooltip("gui.circuits.editor.load");
		new Button(comps, 16, 16, 196, 210, 0, null, (i)-> sendCommand(A_SAVE)).tooltip("gui.circuits.editor.save");
		new Button(comps, 16, 16, 174, 210, 0, null, this::compile).tooltip("gui.circuits.editor.compile");
		
		new Progressbar(comps, 56, 4, 192, 232, 0, 226, Progressbar.H_SLIDE, ()-> Math.min(this.tile.ingreds[0], 112), 0, 112);
		new Progressbar(comps, 56, 4, 192, 238, 0, 232, Progressbar.H_SLIDE, ()-> Math.min(this.tile.ingreds[1], 112), 0, 112);
		new Progressbar(comps, 56, 4, 192, 244, 0, 238, Progressbar.H_SLIDE, ()-> Math.min(this.tile.ingreds[2], 112), 0, 112);
		new Progressbar(comps, 56, 2, 192, 233, 0, 230, Progressbar.PIXELS, ()-> this.tile.ingreds[3], 0, 112);
		new Progressbar(comps, 56, 2, 192, 239, 0, 236, Progressbar.PIXELS, ()-> this.tile.ingreds[4], 0, 112);
		new Progressbar(comps, 56, 2, 192, 245, 0, 242, Progressbar.PIXELS, ()-> this.tile.ingreds[5], 0, 112);
		new Tooltip(comps, 56, 16, 192, 232, "gui.circuits.editor.ingreds", ()-> new Object[] {
			this.tile.ingreds[0], this.tile.ingreds[3], this.tile.ingreds[1], this.tile.ingreds[4], this.tile.ingreds[2], this.tile.ingreds[5]
		});
		this.compGroup = comps;
		this.error = new GuiErrorMarker(this);
		changeSelPart();
	}

	private String getLabel() {
		BoundingBox2D<OpNode> part = board.selPart;
		return part != null ? part.owner.label : "";
	}

	private String getConfig() {
		BoundingBox2D<OpNode> part = board.selPart;
		if (part != null) {
			OpNode node = part.owner;
			if (node instanceof IConfigurable)
				return ((IConfigurable)node).getCfg();
		}
		return "";
	}

	private void send(byte tag, String s) {
		BoundingBox2D<OpNode> part = board.selPart;
		if (part != null)
			sendPkt(tag, (byte)part.owner.index, s);
	}

	void changeSelPart() {
		BoundingBox2D<OpNode> part = board.selPart;
		if (part == null) {
			editLabel.setEnabled(false);
			editCfg.setEnabled(false);
		} else if (part.owner instanceof IConfigurable) {
			editLabel.setEnabled(true);
			editCfg.tooltip(((IConfigurable)part.owner).cfgTooltip());
			editCfg.setEnabled(true);
		} else {
			editLabel.setEnabled(true);
			editCfg.setEnabled(false);
		}
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		if (tile.modified) {
			CircuitBoard board = this.board;
			board.parts.clear();
			for (OpNode op : tile.operators)
				if (op != null)
					board.parts.add(op.getBounds());
			if (board.selPart != null) {
				OpNode op = tile.operators.get(board.selPart.owner.index & 0xff);
				board.selPart = op == null ? null : op.getBounds();
				changeSelPart();
			}
			tile.modified = false;
		}
		error.update();
	}

	private void compile(int b) {
		sendCommand(A_COMPILE);
	}

}
