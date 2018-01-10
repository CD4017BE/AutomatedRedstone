package cd4017be.circuits.gui;

import java.util.ArrayList;
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
import cd4017be.lib.Gui.AdvancedGui;

/**
 *
 * @author CD4017BE
 */
public class GuiCircuit extends AdvancedGui {

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
		guiComps.add(new NumberSel(2, 65, 15, 36, 18, "", tile.getMinInterval(), 1200, 20,
				()-> tile.tickInt,
				(i)-> {
					tile.tickInt = i;
					setDisplVar(2, null, true);
				}
			).setup(8, 0xff404040, 2, true).around());
		guiComps.add(new Text<Float>(3, 66, 20, 34, 8, "circuit.tick", ()-> (float)tile.tickInt / 20F).center().setTooltip("circuit.timer"));
		guiComps.add(new Memory(4, 7, 24, 32, 64));
		for (int i = 0; i < tile.iocfg.length; i++) {
			int j = 5 + i * 5;
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
			int s = cfg.size();
			int l = 32 - s;
			guiComps.add(new Slider(j + 3, 68 + s / 2, 37 + i * 9, l, 108, 74, s, 5, true,
					()-> (float)cfg.ofs / (float)l,
					(obj)-> cfg.ofs = (byte)(obj * (float)l),
					(obj)-> {
						cfg.ofs = (byte)(obj * (float)l);
						setDisplVar(j, cfg, true);
					}
				).scroll(1F/l));
			guiComps.add(new Tooltip<Object[]>(j + 4, 67, 37 + i * 9, 32, 5, cfg.dir ? "circuit.ofsO" : "circuit.ofsI", ()-> new Object[]{cfg.ofs, 1 << cfg.ofs}));
		}
		guiComps.add(new InfoTab(guiComps.size(), 7, 6, 7, 8, "circuit.info"));
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer dos = BlockGuiHandler.getPacketTargetData(tile.pos());
		if (id == 0) dos.writeByte(1).writeByte(tile.mode);
		else if (id == 1) dos.writeByte(3);
		else if (id == 2) dos.writeByte(2).writeInt(tile.tickInt);
		else {
			int p = (id - 5) / 5;
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
			int s = cfg.addr & 0x3f, i = 0;
			for (int j = s; j >= 0 && i < 64; i++)
				if ((tile.usedAddr >> i & 1) != 0) j--;
			i--;
			int l = Math.min(s + (cfg.addr >> 6 & 3) + 1, tile.ram.length);
			for (; s < l; i++, s++) {
				int val = tile.ram[s] & 0xff;
				drawTexturedModalRect(guiLeft + 7 + (i & 7) * 4, guiTop + 24 + (i >> 3) * 8, 192 + (val & 15) * 4, 128 + (val >> 4) * 8, 4, 8);
			}
		}
		@Override
		public void draw() {
			drawTexturedModalRect(px, py, tx, ty + (get.get().dir ? h : 0), w, h);
		}
	}

	class Memory extends GuiComp<Object> {

		public Memory(int id, int px, int py, int w, int h) {
			super(id, px, py, w, h);
		}

		@Override
		public void draw() {
			mc.renderEngine.bindTexture(MAIN_TEX);
			for (int i = 0, j = 0; j < tile.startIdx && i < 64; i++)
				if ((tile.usedAddr >> i & 1) != 0) {
					int val = tile.ram[j++] & 0xff;
					drawTexturedModalRect(px + (i & 7) * 4, py + (i >> 3) * 8, 192 + (val & 15) * 4, (val >> 4) * 8, 4, 8);
				}
		}

		@Override
		public void drawOverlay(int mx, int my) {
			int i = (mx - px) / 4 + (my - py) / 8 * 8;
			if (i >= 0) {
				int j = Long.bitCount(tile.usedAddr << (63 - i)) - 1;
				int r = (int) (tile.usedAddr >> i);
				ArrayList<String> list = new ArrayList<String>(4);
				if ((r & 1) != 0) {
					list.add(String.format("8: %02x %d", tile.ram[j] & 0xff, tile.getNum(j)));
					if ((r & 2) != 0) {
						list.add(String.format("16: %d", tile.getNum(j | 0x40)));
						if ((r & 4) != 0) {
							list.add(String.format("24: %d", tile.getNum(j | 0x80)));
							if ((r & 8) != 0)
								list.add(String.format("32: %d", tile.getNum(j | 0xc0)));
						}
					}
				}
				drawHoveringText(list, mx, my);
			}
		}

		@Override
		public boolean mouseIn(int x, int y, int b, int d) {
			int i = (x - px) / 4 + (y - py) / 8 * 8;
			if (i < 0 || (tile.usedAddr >> i & 1) == 0) return false;
			int j = Long.bitCount(tile.usedAddr << (63 - i)) - 1;
			byte val;
			if (d == 0) {
				if (b == 0) val = -1;
				else if (b == 1) val = 0;
				else return false;
			} else if (d == 3) {
				val = (byte)(tile.ram[j] + b * (isShiftKeyDown() ? 16 : 1));
			} else return false;
			PacketBuffer data = BlockGuiHandler.getPacketTargetData(tile.pos());
			data.writeByte(4).writeByte(j).writeByte(val);
			BlockGuiHandler.sendPacketToServer(data);
			return true;
		}

	}

}
