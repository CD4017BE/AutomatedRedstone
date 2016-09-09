/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

import cd4017be.circuits.tileEntity.Circuit;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.TooltipInfo;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.GuiMachine;

/**
 *
 * @author CD4017BE
 */
public class GuiCircuit extends GuiMachine {

	private final Circuit tile;

	public GuiCircuit(Circuit tileEntity, EntityPlayer player) {
		super(new DataContainer(tileEntity, player));
		this.tile = tileEntity;
		this.MAIN_TEX = new ResourceLocation("circuits", "textures/gui/circuit.png");
	}

	@Override
	public void initGui() {
		this.xSize = 108;
		this.ySize = 96;
		super.initGui();
		for (int i = 0; i < 6; i++) {
			guiComps.add(new Button(i * 4, 59, 35 + i * 9, 9, 9, 0).texture(108, 36).setTooltip("circuit.dir"));//dir
			guiComps.add(new TextField(i * 4 + 1, 46, 36 + i * 9, 11, 7, 2).setTooltip("circuit.ram"));//ramIdx
			guiComps.add(new TextField(i * 4 + 2, 68, 36 + i * 9, 11, 7, 2).setTooltip("circuit.size"));//extIdx
			guiComps.add(new TextField(i * 4 + 3, 82, 36 + i * 9, 11, 7, 2).setTooltip("circuit.ext"));//size
		}
		guiComps.add(new Button(24, 45, 15, 18, 18, 0).texture(108, 0).setTooltip("circuit.on"));//on/off
		guiComps.add(new Button(25, 7, 15, 33, 9, -1).setTooltip("circuit.reset"));//reset
		guiComps.add(new Button(26, 65, 15, 18, 5, -1));//+1s
		guiComps.add(new Button(27, 65, 28, 18, 5, -1));//-1s
		guiComps.add(new Button(28, 83, 15, 18, 5, -1));//+1t
		guiComps.add(new Button(29, 83, 28, 18, 5, -1));//-1t
		guiComps.add(new GuiComp(30, 66, 20, 34, 8).setTooltip("circuit.timer"));//timer
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float t, int mx, int my) {
		super.drawGuiContainerBackgroundLayer(t, mx, my);
		mc.renderEngine.bindTexture(MAIN_TEX);
		for (int i = 0; i < tile.ram.length; i++)
			this.drawTexturedModalRect(guiLeft + 7, guiTop + 25 + 2 * i, 223, tile.ram[i], 33, 1);
		if (tile.ram.length > 0 && this.isPointInRegion(46, 36, 54, 52, mx, my)) {
			int s = (my - guiTop - 35) / 9, dir = tile.getDir(s);
			if (dir > 0) {
				int p = tile.getRamIdx(s), l = tile.getSize(s), j, k;
				for (int i = p; i < p + l; i++) 
					this.drawTexturedModalRect(guiLeft + 7 + (j = i & 7) * 4, guiTop + 25 + (k = i >> 3) % tile.ram.length * 2, 219, tile.ram[k % tile.ram.length] >> j & 1, 5, 1);
			}
			this.drawSideCube(guiLeft - 64, guiTop + 7, s, (byte)dir);
		}
		this.drawStringCentered(tile.getName(), this.guiLeft + this.xSize / 2, this.guiTop + 4, 0x404040);
		this.drawStringCentered(String.format("%.2f", (float)tile.tickInt / 20F).concat("s"), this.guiLeft + 83, this.guiTop + 20, 0x404040);
		this.drawStringCentered(TooltipInfo.format("gui.cd4017be.circuit.io", tile.usedIO(), tile.var & 0xff), guiLeft + xSize / 2, guiTop + ySize + 4, 0xff4040);
	}

	@Override
	protected Object getDisplVar(int id) {
		if (id == 24) return (int)(tile.cfgI >>> 60) & 1;
		if (id > 24) return null;
		int s = id / 4; id %= 4;
		if (id == 0) return tile.getDir(s);
		return id == 1 ? Integer.toHexString(tile.getRamIdx(s)) : Integer.toString(id == 2 ? tile.getSize(s) : tile.getExtIdx(s));
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		PacketBuffer dos = tile.getPacketTargetData();
		if (id < 24) {
			int s = id / 4; id %= 4;
			if (id == 0) tile.setDir(s, (tile.getDir(s) + ((Integer)obj == 0 ? 1 : 2)) % 3);
			else if (id == 1) try {tile.setRamIdx(s, Integer.parseInt((String)obj, 16));} catch(NumberFormatException e) {return;}
			else if (id == 2) try {tile.setSize(s, Integer.parseInt((String)obj));} catch(NumberFormatException e) {return;}
			else try {tile.setExtIdx(s, Integer.parseInt((String)obj));} catch(NumberFormatException e) {return;}
			if (tile.usedIO() > tile.var) tile.setDir(s, 0);
			dos.writeByte(0).writeLong(tile.cfgI).writeLong(tile.cfgE);
		} else if (id == 24) dos.writeByte(1);
		else if (id == 25) dos.writeByte(3);
		else if (id == 26 || id == 28) dos.writeByte(2).writeInt(tile.tickInt = Math.min(1200, tile.tickInt + (id == 26 ? 20 : 1)));
		else if (id == 27 || id == 29) dos.writeByte(2).writeInt(tile.tickInt = Math.max(1, tile.tickInt - (id == 27 ? 20 : 1)));
		else return;
		if (send) BlockGuiHandler.sendPacketToServer(dos);
	}

}
