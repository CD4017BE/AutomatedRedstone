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
			guiComps.add(new TextField(i, 51, (t?15:16) + i * 9, 63, 8, 11).color(0xff40c0c0, 0xffff4040).setTooltip(t ? "oszi.add" : "oszi.div"));
		}
		for (int i = 0; i < 4; i++) guiComps.add(new Button(i + 8, 8, 15 + i * 18, 16, 9, 1).texture(176, 0).setTooltip("oszi.rsIn"));
		for (int i = 0; i < 4; i++) guiComps.add(new Button(i + 12, 8, 24 + i * 18, 16, 9, 0).texture(192, i * 18).setTooltip("oszi.on"));
		for (int i = 0; i < 4; i++) guiComps.add(new TextField(i + 16, 116, 16 + i * 18, 52, 7, 12).color(OszillographRenderer.textColors[i], 0xffffffff).setTooltip("oszi.info"));
		guiComps.add(new Button(20, 7, 87, 18, 5, -1));//+1s
		guiComps.add(new Button(21, 7, 100, 18, 5, -1));//-1s
		guiComps.add(new Button(22, 25, 87, 18, 5, -1));//+1t
		guiComps.add(new Button(23, 25, 100, 18, 5, -1));//-1t
		guiComps.add(new Text(24, 7, 92, 36, 8, "oszi.int").center().setTooltip("oszi.time"));
		guiComps.add(new Button(25, 44, 87, 16, 9, 0).texture(208, 0).setTooltip("oszi.trigger#"));
		guiComps.add(new Button(26, 62, 87, 16, 9, 1).texture(176, 0).setTooltip("oszi.src"));
		guiComps.add(new Button(27, 80, 87, 16, 9, 0).texture(192, 72).setTooltip("oszi.comp#"));
		guiComps.add(new TextField(28, 44, 97, 52, 7, 8).color(0xff40c0c0, 0xffff4040).setTooltip("oszi.level"));
	}

	@Override
	protected Object getDisplVar(int id) {
		if (id < 8) return Float.toString(tile.transf[id]);
		else if (id < 12) return EnumFacing.VALUES[(tile.mode >> (id - 8) * 4 & 7) % 6];
		else if (id < 16) return tile.mode >> ((id - 12) * 4 + 3) & 1;
		else if (id < 20) return tile.info[id - 16];
		else switch(id) {
		case 24: return (float)tile.tickInt / 2F;
		case 25: return tile.mode >> 16 & 3;
		case 26: {
			int m = tile.mode >> 16 & 3;
			return m == 1 ? EnumFacing.VALUES[tile.mode >> 18 & 7] : m == 2 ? (tile.mode >> 18 & 7) + 6 : 10;
		} case 27: return tile.mode >> 21 & 1;
		case 28: return Float.toString(tile.triggerLevel);
		default: return null;
		}
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer dos = tile.getPacketTargetData();
		if (id < 8) try {
			dos.writeByte(AutomatedTile.CmdOffset + id);
			dos.writeFloat(Float.parseFloat((String)obj));
		} catch(NumberFormatException e){return;}
		else if (id < 12) {
			int i = (id - 8) * 4;
			dos.writeByte(AutomatedTile.CmdOffset + 9).writeInt(tile.mode = tile.mode & ~(7 << i) | ((tile.mode >> i & 7) + ((Integer)obj == 0 ? 1 : 5)) % 6 << i);
		} else if (id < 16)
			dos.writeByte(AutomatedTile.CmdOffset + 9).writeInt(tile.mode ^= 8 << (id - 12) * 4);
		else if (id < 20) {
			dos.writeByte(AutomatedTile.CmdOffset + id - 6);
			dos.writeString((String)obj);
		} else switch(id) {
		case 20: case 22:
			dos.writeByte(AutomatedTile.CmdOffset + 8).writeInt(tile.tickInt = Math.min(1200, tile.tickInt + (id == 20 ? 20 : 1)));
			break;
		case 21: case 23:
			dos.writeByte(AutomatedTile.CmdOffset + 8).writeInt(tile.tickInt = Math.max(1, tile.tickInt - (id == 21 ? 20 : 1)));
			break;
		case 25:
			dos.writeByte(AutomatedTile.CmdOffset + 9).writeInt(tile.mode = tile.mode & 0xfcffff | ((tile.mode >> 16 & 3) + ((Integer)obj == 0 ? 1 : 3)) % 4 << 16);
			break;
		case 26: 
			int m = tile.mode >> 16 & 3;
			m = m == 1 ? 6 : m == 2 ? 4 : 0;
			if (m == 0) return;
			dos.writeByte(AutomatedTile.CmdOffset + 9).writeInt(tile.mode = tile.mode & 0xe3ffff | ((tile.mode >> 18 & 3) + ((Integer)obj == 0 ? 1 : m - 1)) % m << 18);
			break;
		case 27:
			dos.writeByte(AutomatedTile.CmdOffset + 9).writeInt(tile.mode ^= 0x200000);
			break;
		case 28: try {
			dos.writeByte(AutomatedTile.CmdOffset + 14);
			dos.writeFloat(Float.parseFloat((String)obj));
			break;
		} catch(NumberFormatException e){return;}
		default: return;
		}
		if (send) BlockGuiHandler.sendPacketToServer(dos);
	}

}
