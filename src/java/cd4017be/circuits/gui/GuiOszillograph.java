package cd4017be.circuits.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import cd4017be.circuits.render.OszillographRenderer;
import cd4017be.circuits.tileEntity.Oszillograph;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.AdvancedGui;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.util.Utils;

public class GuiOszillograph extends AdvancedGui {

	private final Oszillograph tile;

	public GuiOszillograph(IGuiData tile, EntityPlayer player) {
		super(new TileContainer(tile, player));
		this.tile = (Oszillograph) tile;
		this.MAIN_TEX = new ResourceLocation("circuits", "textures/gui/oszillograph.png");
	}

	@Override
	public void initGui() {
		this.xSize = 176;
		this.ySize = 204;
		this.tabsY = 15 - 63;
		super.initGui();
		for (int i = 0; i < 8; i++) {
			boolean t = (i & 1) != 0;
			guiComps.add(new TextField(i, 105, (t?15:16) + i * 9, 63, 8, 11).color(0xff40c0c0, 0xffff4040).setTooltip(t ? "oszi.add" : "oszi.div"));
		}
		for (int i = 0; i < 4; i++) guiComps.add(new Button(i + 8, 27, 24 + i * 18, 16, 9, 1).texture(176, 0).setTooltip("oszi.rsIn"));
		for (int i = 0; i < 4; i++) guiComps.add(new Button(i + 12, 8, 24 + i * 18, 16, 9, 0).texture(192, i * 18).setTooltip("oszi.on"));
		for (int i = 0; i < 4; i++) guiComps.add(new TextField(i + 16, 8, 16 + i * 18, 70, 7, 16).color(OszillographRenderer.textColors[i], 0xffffffff).setTooltip("oszi.label"));
		for (int i = 0; i < 4; i++) guiComps.add(new TextField(i + 20, 45, 25 + i * 18, 11, 7, 2).setTooltip("circuit.ext"));
		for (int i = 0; i < 4; i++) guiComps.add(new TextField(i + 24, 58, 25 + i * 18, 11, 7, 2).setTooltip("circuit.size"));
		for (int i = 0; i < 4; i++) guiComps.add(new Button(i + 28, 71, 24 + i * 18, 7, 9, 0).texture(208, 36).setTooltip("oszi.signed#"));
		guiComps.add(new NumberSel(32, 7, 87, 36, 18, "", 1, 1200, 20).setup(8, 0xff404040, 2, true).around());
		guiComps.add(new Text<>(33, 7, 92, 36, 8, "oszi.int").center().setTooltip("oszi.time"));
		guiComps.add(new Button(34, 44, 91, 16, 9, 0).texture(208, 0).setTooltip("oszi.trigger#"));
		guiComps.add(new Button(35, 62, 91, 16, 9, 1).texture(176, 0).setTooltip("oszi.src"));
		guiComps.add(new Button(36, 80, 91, 16, 9, 0).texture(192, 72).setTooltip("oszi.comp#"));
		guiComps.add(new TextField(37, 98, 92, 63, 7, 11).color(0xff40c0c0, 0xffff4040).setTooltip("oszi.level"));
		guiComps.add(new InfoTab(38, 7, 6, 7, 8, "oszi.info"));
	}

	@Override
	protected Object getDisplVar(int id) {
		if (id < 8) return Float.toString(tile.transf[id]);
		else if (id < 12) return EnumFacing.VALUES[(int)(tile.cfg >> (id - 8) * 16 + 1 & 7) % 6];
		else if (id < 16) return (int)(tile.cfg >> (id - 12) * 16) & 1;
		else if (id < 20) return tile.info[id - 16];
		else if (id < 24) return "" + (int)(tile.cfg >> (id - 20) * 16 + 4 & 31);
		else if (id < 28) return "" + ((int)(tile.cfg >> (id - 24) * 16 + 9 & 31) + 1);
		else if (id < 32) return (int)(tile.cfg >> (id - 28) * 16 + 14) & 1;
		else switch(id) {
		case 32: return tile.tickInt;
		case 33: return (float)tile.tickInt / 20F;
		case 34: return tile.mode & 3;
		case 35: {
			int m = tile.mode & 3;
			return m == 1 ? EnumFacing.VALUES[tile.mode >> 2 & 7] : m == 2 ? (tile.mode >> 2 & 7) + 6 : 10;
		} case 36: return tile.mode >> 5 & 1;
		case 37: return Float.toString(tile.triggerLevel);
		default: return null;
		}
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer dos = BlockGuiHandler.getPacketTargetData(tile.pos());
		if (id < 8) try {
			dos.writeByte(id);
			dos.writeFloat(Float.parseFloat((String)obj));
		} catch(NumberFormatException e){return;}
		else if (id < 12) dos.writeByte(15).writeLong(tile.cfg = Utils.cycleState(tile.cfg, (id - 8) * 16 + 1, 7, 6, (Integer)obj == 0));
		else if (id < 16) dos.writeByte(15).writeLong(tile.cfg ^= 1L << (id - 12) * 16);
		else if (id < 20) {dos.writeByte(id - 6); dos.writeString((String)obj);}
		else if (id < 24) try {
			dos.writeByte(15).writeLong(tile.cfg = Utils.setState(tile.cfg, (id - 20) * 16 + 4, 31, Integer.parseInt((String)obj)));
		} catch(NumberFormatException e){return;}
		else if (id < 28) try {
			dos.writeByte(15).writeLong(tile.cfg = Utils.setState(tile.cfg, (id - 24) * 16 + 9, 31, Integer.parseInt((String)obj) - 1));
		} catch(NumberFormatException e){return;}
		else if (id < 32) 
			dos.writeByte(15).writeLong(tile.cfg ^= 0x4000L << (id - 28) * 16);
		else switch(id) {
		case 32: dos.writeByte(8).writeInt(tile.tickInt = (Integer)obj); break;
		case 34: dos.writeByte(9).writeInt(tile.mode = Utils.cycleState(tile.mode, 0, 3, 4, (Integer)obj == 0)); break;
		case 35: 
			int m = tile.mode & 3;
			m = m == 1 ? 6 : m == 2 ? 4 : 0;
			if (m == 0) return;
			dos.writeByte(9).writeInt(tile.mode = Utils.cycleState(tile.mode, 2, 7, m, (Integer)obj == 0));
			break;
		case 36: dos.writeByte(9).writeInt(tile.mode ^= 0x20); break;
		case 37: try {
			dos.writeByte(14);
			dos.writeFloat(Float.parseFloat((String)obj));
			break;
		} catch(NumberFormatException e){return;}
		default: return;
		}
		if (send) BlockGuiHandler.sendPacketToServer(dos);
	}

}
