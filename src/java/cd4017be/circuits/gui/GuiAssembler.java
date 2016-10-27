package cd4017be.circuits.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import cd4017be.circuits.tileEntity.Assembler;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.TooltipInfo;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.GuiMachine;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.templates.AutomatedTile;

/**
 *
 * @author CD4017BE
 */
public class GuiAssembler extends GuiMachine {

	private final Assembler tileEntity;

	public GuiAssembler(Assembler tileEntity, EntityPlayer player) {
		super(new TileContainer(tileEntity, player));
		this.tileEntity = tileEntity;
		this.MAIN_TEX = new ResourceLocation("circuits:textures/gui/assembler.png");
	}

	@Override
	public void initGui() {
		this.xSize = 176;
		this.ySize = 168;
		super.initGui();
		guiComps.add(new Button(0, 88, 24, 9, 9, -1).setTooltip("assembler.add0"));
		guiComps.add(new Button(1, 88, 33, 9, 9, -1).setTooltip("assembler.add1"));
		guiComps.add(new Button(2, 88, 42, 9, 9, -1).setTooltip("assembler.add2"));
		guiComps.add(new Button(3, 88, 51, 9, 9, -1).setTooltip("assembler.add3"));
		guiComps.add(new GuiComp(4, 7, 33, 18, 18).setTooltip("assembler.destr"));
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float t, int mx, int my) {
		super.drawGuiContainerBackgroundLayer(t, mx, my);
		DataContainer cont = (DataContainer)inventorySlots;
		this.drawStringCentered(TooltipInfo.format("gui.cd4017be.assembler.comp0", cont.refInts[0]), this.guiLeft + 124, this.guiTop + 25, 0x408040);
		this.drawStringCentered(TooltipInfo.format("gui.cd4017be.assembler.comp1", cont.refInts[1]), this.guiLeft + 124, this.guiTop + 33, 0x408040);
		this.drawStringCentered(TooltipInfo.format("gui.cd4017be.assembler.comp2", cont.refInts[2]), this.guiLeft + 124, this.guiTop + 43, 0x408040);
		this.drawStringCentered(TooltipInfo.format("gui.cd4017be.assembler.comp3", cont.refInts[3]), this.guiLeft + 124, this.guiTop + 51, 0x408040);
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer dos = tileEntity.getPacketTargetData();
		dos.writeByte(AutomatedTile.CmdOffset + id).writeByte(isShiftKeyDown() ? 64 : ((Integer)obj == 0 ? 1 : 8));
		BlockGuiHandler.sendPacketToServer(dos);
	}

}
