package cd4017be.circuits.gui;

import java.util.function.Supplier;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import cd4017be.circuits.tileEntity.Circuit;
import cd4017be.circuits.tileEntity.Circuit.IOcfg;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.GuiMachine;

/**
 *
 * @author CD4017BE
 */
public class GuiCircuit extends GuiMachine {

	private final Circuit tile;

	public GuiCircuit(IGuiData tileEntity, EntityPlayer player) {
		super(new DataContainer(tileEntity, player));
		this.tile = (Circuit) tileEntity;
		this.MAIN_TEX = new ResourceLocation("circuits", "textures/gui/circuit.png");
	}

	@Override
	public void initGui() {
		this.xSize = 108;
		this.ySize = 96;
		super.initGui();
		tabsY = -56;
		guiComps.add(new Button(0, 45, 15, 18, 18, 0,
				()-> (int)tile.mode,
				(obj)-> {
					tile.mode = (byte)((tile.mode + ((Integer)obj == 0 ? 1 : 3)) % 4);
					setDisplVar(0, null, true);
				}
			).texture(166, 0).setTooltip("circuit.on#"));//on/off
		guiComps.add(new Button(1, 7, 15, 33, 9, -1).setTooltip("circuit.reset"));//reset
		guiComps.add(new Button(2, 65, 15, 18, 5, -1));//+1s
		guiComps.add(new Button(3, 65, 28, 18, 5, -1));//-1s
		guiComps.add(new Button(4, 83, 15, 18, 5, -1));//+1t
		guiComps.add(new Button(5, 83, 28, 18, 5, -1));//-1t
		guiComps.add(new GuiComp<>(6, 66, 20, 34, 8).setTooltip("circuit.timer"));//timer
		for (int i = 0; i < tile.iocfg.length; i++) {
			int j = 7 + i * 5;
			final IOcfg cfg = tile.iocfg[i];
			guiComps.add(new IOPort(j, 43, 35 + i * 9, 58, 9, 108, 54, ()-> cfg));
			guiComps.add(new Tooltip<String>(j + 1, 45, 35 + i * 9, 20, 9, cfg.dir ? "circuit.dirO" : "circuit.dirI", ()-> cfg.label));
			guiComps.add(new Button(j + 2, 56, 35 + i * 9, 9, 9, cfg.dir ? 2 : 1,
					()-> EnumFacing.VALUES[cfg.side],
					(obj)-> {
						cfg.side = (byte)((cfg.side + ((Integer)obj == 0 ? 1 : 5)) % 6);
						setDisplVar(j, cfg, true);
					}
				).texture(108, 0));
			int l = 32 - cfg.size;
			guiComps.add(new Slider(j + 3, 68 + cfg.size / 2, 37 + i * 9, l, 108, 74, cfg.size, 5, true,
					()-> (float)cfg.ofs / (float)l,
					(obj)-> cfg.ofs = (byte)(obj * (float)l),
					(obj)-> {
						cfg.ofs = (byte)(obj * (float)l);
						setDisplVar(j, cfg, true);
					}
				).scroll(1F/l));
			guiComps.add(new Tooltip<Object[]>(j + 4, 67, 37 + i * 9, 32, 5, cfg.dir ? "circuit.ofsO" : "circuit.ofsI", ()-> new Object[]{cfg.ofs, 1 << cfg.ofs}));
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float t, int mx, int my) {
		super.drawGuiContainerBackgroundLayer(t, mx, my);
		mc.renderEngine.bindTexture(MAIN_TEX);
		for (int i = 0; i < tile.memoryOfs; i++)
			this.drawTexturedModalRect(guiLeft + 7, guiTop + 25 + 2 * i, 223, tile.ram[i], 33, 1);
		this.drawStringCentered(String.format("%.2f", (float)tile.tickInt / 20F).concat("s"), this.guiLeft + 83, this.guiTop + 20, 0x404040);
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer dos = BlockGuiHandler.getPacketTargetData(tile.pos());
		if (id == 0) dos.writeByte(1).writeByte(tile.mode);
		else if (id == 1) dos.writeByte(3);
		else if (id == 2 || id == 4) dos.writeByte(2).writeInt(tile.tickInt = Math.min(1200, tile.tickInt + (id == 2 ? 20 : 1)));
		else if (id == 3 || id == 5) dos.writeByte(2).writeInt(tile.tickInt = Math.max(Circuit.ClockSpeed[tile.getBlockMetadata() % Circuit.ClockSpeed.length], tile.tickInt - (id == 3 ? 20 : 1)));
		else {
			int p = (id - 7) / 5;
			IOcfg cfg = (IOcfg)obj;
			dos.writeByte(0).writeByte(p).writeByte(cfg.side).writeByte(cfg.ofs);
		}
		if (send) BlockGuiHandler.sendPacketToServer(dos);
	}

	class IOPort extends GuiComp<IOcfg> {
		final int tx, ty;
		public IOPort(int id, int px, int py, int w, int h, int tx, int ty, Supplier<IOcfg> get) {
			super(id, px, py, w, h, get, null, null);
			this.tx = tx;
			this.ty = ty;
		}
		@Override
		public void drawOverlay(int mx, int my) {
			mc.renderEngine.bindTexture(MAIN_TEX);
			IOcfg cfg = get.get();
			int l = Math.min((cfg.addr & 0xff) + cfg.size, tile.ram.length * 8), j, k;
			for (int i = cfg.addr & 0xff; i < l; i++) 
				drawTexturedModalRect(guiLeft + 7 + (j = i & 7) * 4, guiTop + 25 + (k = i >> 3) * 2, 219, tile.ram[k] >> j & 1, 5, 1);
		}
		@Override
		public void draw() {
			drawTexturedModalRect(px, py, tx, ty + (get.get().dir ? h : 0), w, h);
		}
	}

}
