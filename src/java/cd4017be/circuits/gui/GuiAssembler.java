package cd4017be.circuits.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.AdvancedGui;
import cd4017be.lib.Gui.TileContainer;

/**
 *
 * @author CD4017BE
 */
public class GuiAssembler extends AdvancedGui {

	public GuiAssembler(IGuiData tileEntity, EntityPlayer player) {
		super(new TileContainer(tileEntity, player));
		this.MAIN_TEX = new ResourceLocation("circuits:textures/gui/assembler.png");
	}

	@Override
	public void initGui() {
		this.xSize = 176;
		this.ySize = 168;
		super.initGui();
		final DataContainer cont = (DataContainer)inventorySlots;
		for (int i = 0; i < 4; i++) {
			final int j = i;
			guiComps.add(new ProgressBar(2 * i, 62, 26 + i * 8, 52, 8, 176, 0, (byte)0, ()-> {
				int a = cont.refInts[j + 4], b = cont.refInts[j];
				return a == b ? 1F : a < b ? (float)a / (float)b : -1F / (float)(a - b + 1);
			}));
			guiComps.add(new Text<Object[]>(2 * i + 1, 62, 26 + i * 8, 52, 8, "\\%d / %d", ()-> new Object[]{cont.refInts[j + 4], cont.refInts[j]}).center().setTooltip("assembler.cmp" + i));
		}
		guiComps.add(new Text<Integer>(8, 62, 18, 52, 8, "assembler.err#", ()-> cont.refInts[8]).center().setTooltip("assembler.msg#"));
		guiComps.add(new InfoTab(9, 7, 6, 7, 8, "assembler.info"));
		guiComps.add(new Button(10, 134, 34, 16, 16, (b)-> sendCommand(0)).setTooltip("assembler.swap"));
		if (((TileContainer)inventorySlots).player.isCreative())
			guiComps.add(new Button(11, 152, 34, 16, 16, 0, ()-> 0, (b)-> sendCommand(1)).texture(176, 8).setTooltip("assembler.cheat"));
	}

}
