package cd4017be.circuits.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import cd4017be.circuits.render.OszillographRenderer;
import cd4017be.circuits.tileEntity.Oszillograph;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.GuiMachine;
import cd4017be.lib.Gui.TileContainer;
import cd4017be.lib.templates.AutomatedTile;
import cd4017be.lib.util.Utils;

public class GuiOszillograph extends GuiMachine {

	private final Oszillograph tile;

	public GuiOszillograph(Oszillograph tile, EntityPlayer player) {
		super(new TileContainer(tile, player));
		this.tile = tile;
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
		for (int i = 0; i < 4; i++) guiComps.add(new TextField(i + 16, 8, 16 + i * 18, 70, 7, 16).color(OszillographRenderer.textColors[i], 0xffffffff).setTooltip("oszi.info"));
		for (int i = 0; i < 4; i++) guiComps.add(new TextField(i + 20, 45, 25 + i * 18, 11, 7, 2).setTooltip("circuit.ext"));
		for (int i = 0; i < 4; i++) guiComps.add(new TextField(i + 24, 58, 25 + i * 18, 11, 7, 2).setTooltip("circuit.size"));
		for (int i = 0; i < 4; i++) guiComps.add(new Button(i + 28, 71, 24 + i * 18, 7, 9, 0).texture(208, 36).setTooltip("oszi.signed#"));
		guiComps.add(new Button(32, 7, 87, 18, 5, -1));//+1s
		guiComps.add(new Button(33, 7, 100, 18, 5, -1));//-1s
		guiComps.add(new Button(34, 25, 87, 18, 5, -1));//+1t
		guiComps.add(new Button(35, 25, 100, 18, 5, -1));//-1t
		guiComps.add(new Text(36, 7, 92, 36, 8, "oszi.int").center().setTooltip("oszi.time"));
		guiComps.add(new Button(37, 44, 91, 16, 9, 0).texture(208, 0).setTooltip("oszi.trigger#"));
		guiComps.add(new Button(38, 62, 91, 16, 9, 1).texture(176, 0).setTooltip("oszi.src"));
		guiComps.add(new Button(39, 80, 91, 16, 9, 0).texture(192, 72).setTooltip("oszi.comp#"));
		guiComps.add(new TextField(40, 98, 92, 63, 7, 11).color(0xff40c0c0, 0xffff4040).setTooltip("oszi.level"));
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
		case 36: return (float)tile.tickInt / 2F;
		case 37: return tile.mode & 3;
		case 38: {
			int m = tile.mode & 3;
			return m == 1 ? EnumFacing.VALUES[tile.mode >> 2 & 7] : m == 2 ? (tile.mode >> 2 & 7) + 6 : 10;
		} case 39: return tile.mode >> 5 & 1;
		case 40: return Float.toString(tile.triggerLevel);
		default: return null;
		}
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer dos = BlockGuiHandler.getPacketTargetData(tile.pos());
		if (id < 8) try {
			dos.writeByte(AutomatedTile.CmdOffset + id);
			dos.writeFloat(Float.parseFloat((String)obj));
		} catch(NumberFormatException e){return;}
		else if (id < 12) dos.writeByte(AutomatedTile.CmdOffset + 15).writeLong(tile.cfg = Utils.cycleState(tile.cfg, (id - 8) * 16 + 1, 7, 6, (Integer)obj == 0));
		else if (id < 16) dos.writeByte(AutomatedTile.CmdOffset + 15).writeLong(tile.cfg ^= 1L << (id - 12) * 16);
		else if (id < 20) {dos.writeByte(AutomatedTile.CmdOffset + id - 6); dos.writeString((String)obj);}
		else if (id < 24) try {
			dos.writeByte(AutomatedTile.CmdOffset + 15).writeLong(tile.cfg = Utils.setState(tile.cfg, (id - 20) * 16 + 4, 31, Integer.parseInt((String)obj)));
		} catch(NumberFormatException e){return;}
		else if (id < 28) try {
			dos.writeByte(AutomatedTile.CmdOffset + 15).writeLong(tile.cfg = Utils.setState(tile.cfg, (id - 24) * 16 + 9, 31, Integer.parseInt((String)obj) - 1));
		} catch(NumberFormatException e){return;}
		else if (id < 32) 
			dos.writeByte(AutomatedTile.CmdOffset + 15).writeLong(tile.cfg ^= 0x4000L << (id - 28) * 16);
		else switch(id) {
		case 32: case 34: dos.writeByte(AutomatedTile.CmdOffset + 8).writeInt(tile.tickInt = Math.min(1200, tile.tickInt + (id == 32 ? 20 : 1))); break;
		case 33: case 35: dos.writeByte(AutomatedTile.CmdOffset + 8).writeInt(tile.tickInt = Math.max(1, tile.tickInt - (id == 33 ? 20 : 1))); break;
		case 37: dos.writeByte(AutomatedTile.CmdOffset + 9).writeInt(tile.mode = Utils.cycleState(tile.mode, 0, 3, 4, (Integer)obj == 0)); break;
		case 38: 
			int m = tile.mode & 3;
			m = m == 1 ? 6 : m == 2 ? 4 : 0;
			if (m == 0) return;
			dos.writeByte(AutomatedTile.CmdOffset + 9).writeInt(tile.mode = Utils.cycleState(tile.mode, 2, 7, m, (Integer)obj == 0));
			break;
		case 39: dos.writeByte(AutomatedTile.CmdOffset + 9).writeInt(tile.mode ^= 0x20); break;
		case 40: try {
			dos.writeByte(AutomatedTile.CmdOffset + 14);
			dos.writeFloat(Float.parseFloat((String)obj));
			break;
		} catch(NumberFormatException e){return;}
		default: return;
		}
		if (send) BlockGuiHandler.sendPacketToServer(dos);
	}

}
