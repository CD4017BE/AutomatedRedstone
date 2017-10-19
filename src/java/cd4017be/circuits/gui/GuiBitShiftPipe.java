package cd4017be.circuits.gui;

import cd4017be.circuits.tileEntity.BitShiftPipe;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.AdvancedGui;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

public class GuiBitShiftPipe extends AdvancedGui {

	private final BitShiftPipe tile;

	public GuiBitShiftPipe(IGuiData tileEntity, EntityPlayer player) {
		super(new DataContainer(tileEntity, player));
		this.tile = (BitShiftPipe) tileEntity;
		this.MAIN_TEX = new ResourceLocation("circuits", "textures/gui/bit_shift.png");
	}

	@Override
	public void initGui() {
		xSize = 134;
		ySize = 76;
		super.initGui();
		tabsY = -56;
		for (int i = 0; i < 6; i++)
			setSlider(i);
		for (int i = 0; i < 6; i++)
			guiComps.add(new TextField(i + 6, 80, 16 + i * 9, 12, 7, 2).setTooltip("bit.size"));
		for (int i = 12; i < 18; i++)
			setSlider(i);
		for (int i = 0; i < 6; i++)
			guiComps.add(new TextField(i + 18, 42, 16 + i * 9, 12, 7, 2).setTooltip("bit.size"));
		for (int i = 0; i < 6; i++) {
			final EnumFacing f = EnumFacing.VALUES[i];
			guiComps.add(new Button(i + 24, 70, 15 + i * 9, 9, 9, 2, ()-> f, (o)-> {}).texture(143, 5).setTooltip("bit.out"));
		}
		for (int i = 0; i < 6; i++) {
			final EnumFacing f = EnumFacing.VALUES[i];
			guiComps.add(new Button(i + 30, 55, 15 + i * 9, 9, 9, 1, ()-> f, (o)-> {}).texture(134, 5).setTooltip("bit.in"));
		}
		guiComps.add(new InfoTab(36, 7, 6, 7, 8, "bit.info"));
	}

	private void setSlider(int i) {
		int w = tile.shifts[i + 6], l = 32 - w, j = guiComps.size();
		Slider comp;
		if (j > i) {
			comp = (Slider)guiComps.get(i);
			if (comp.l == l) return;
		}
		comp = new Slider(i, (i < 12 ? 94 : 8) + w / 2, 17 + i % 12 * 9, l, 134, 0, w, 5, true).scroll(1F / (float)l);
		comp.setTooltip("x*" + l + "+0;bit.ofs");
		if (j > i) guiComps.set(i, comp);
		else guiComps.add(comp);
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		for (int i = 0; i < 6; i++) setSlider(i);
		for (int i = 12; i < 18; i++) setSlider(i);
	}

	@Override
	protected Object getDisplVar(int id) {
		if (id < 24) return id % 12 < 6 ? (float)tile.shifts[id] / (float)(32 - tile.shifts[id + 6]) : Byte.toString(tile.shifts[id]);
		return null;
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		if (id >= 24) return;
		PacketBuffer data = BlockGuiHandler.getPacketTargetData(tile.pos());
		data.writeByte(id);
		if (id % 12 < 6) {
			data.writeByte(tile.shifts[id] = (byte) ((Float)obj * (float)(32 - tile.shifts[id + 6])));
		} else try {
			byte b = Byte.parseByte((String)obj);
			if (b < 0) b = 0;
			else if (b > 32) b = 32;
			data.writeByte(b);
		} catch (NumberFormatException e) {return;}
		if (send) BlockGuiHandler.sendPacketToServer(data);
	}

}
