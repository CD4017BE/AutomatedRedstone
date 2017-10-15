package cd4017be.circuits.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import cd4017be.circuits.tileEntity.EnergyValve;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.AdvancedGui;
import cd4017be.lib.util.TooltipUtil;

public class GuiEnergyValve extends AdvancedGui {

	private final EnergyValve tile;

	public GuiEnergyValve(IGuiData tile, EntityPlayer player) {
		super(new DataContainer(tile, player));
		this.tile = (EnergyValve) tile;
		this.MAIN_TEX = new ResourceLocation("circuits", "textures/gui/valve.png");
	}

	@Override
	public void initGui() {
		xSize = 158;
		ySize = 40;
		super.initGui();
		guiComps.add(new NumberSel(0, 7, 15, 36, 18, "", 1, 1200, 20, ()-> tile.tickInt, (i)-> {
			PacketBuffer dos = BlockGuiHandler.getPacketTargetData(tile.pos());
			dos.writeByte(0).writeInt(tile.tickInt = i);
			BlockGuiHandler.sendPacketToServer(dos);
		}).setup(8, 0xff404040, 2, true).around());
		guiComps.add(new Text<Float>(1, 8, 20, 34, 8, "valve.tick", ()-> (float)tile.tickInt / 20F).center().setTooltip("valve.timer"));
		guiComps.add(new Button(2, 52, 15, 18, 18, 0, ()-> tile.measure?1:0, (i)-> {
			PacketBuffer dos = BlockGuiHandler.getPacketTargetData(tile.pos());
			dos.writeByte(1);
			BlockGuiHandler.sendPacketToServer(dos);
		}).texture(158, 0).setTooltip("energyvalve.mode#"));
		guiComps.add(new Text<String>(3, 80, 16, 68, 16, "energyvalve.flow", ()-> TooltipUtil.formatNumber(tile.state, 4, 0)));
	}

}
