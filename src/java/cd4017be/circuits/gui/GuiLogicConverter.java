/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits.gui;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cd4017be.circuits.tileEntity.LogicConverter;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.TileContainer;
import cd4017be.lib.templates.AutomatedTile;
import cd4017be.lib.templates.GuiMachine;

/**
 *
 * @author CD4017BE
 */
public class GuiLogicConverter extends GuiMachine
{

    private final LogicConverter tileEntity;
    private int sel = -1;
    private TextField text = new TextField("", 2);
    
    public GuiLogicConverter(LogicConverter tileEntity, EntityPlayer player)
    {
        super(new TileContainer(tileEntity, player));
        this.tileEntity = tileEntity;
    }
    
    @Override
    public void initGui() 
    {
        this.xSize = 50;
        this.ySize = 76;
        super.initGui();
    }
    
    @Override
	protected void drawGuiContainerForegroundLayer(int mx, int my) 
    {
		super.drawGuiContainerForegroundLayer(mx, my);
		this.drawInfo(11, 15, 9, 54, "\\i", "rs.dir");
		this.drawInfo(21, 15, 14, 54, "\\i", "rs.filter");
		if (this.isPointInRegion(12, 16, 7, 52, mx, my)) {
			int s = (my - guiTop - 16) / 9;
			this.drawSideCube(-64, 5, s, (byte)((tileEntity.getConfig(6) >> s & 1) + 2));
		}
	}
    
    @Override
    protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) 
    {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.renderEngine.bindTexture(new ResourceLocation("circuits", "textures/gui/circuit.png"));
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 192, 0, this.xSize, this.ySize);
        int x, y, c = tileEntity.getConfig(6);
        for (int i = 0; i < 6; i++)
            this.drawTexturedModalRect(this.guiLeft + 11, this.guiTop + 15 + i * 9, 242, (c >> i & 1) * 9, 9, 9);
        String s;
        x = guiLeft + 22;
        for (int i = 0; i < 6; i++) {
        	y = guiTop + 16 + i * 9;
        	if (i == sel) text.draw(x, y, 0x80ffff, 0xffff8080);
        	else {
        		s = String.format("%02X", tileEntity.getConfig(i));
        		this.fontRendererObj.drawString(s, x, y, 0x80ffff);
        	}
        }
        this.drawStringCentered(tileEntity.getName(), this.guiLeft + this.xSize / 2, this.guiTop + 4, 0x404040);
    }
    
    @Override
    protected void keyTyped(char c, int k) throws IOException {
    	if (sel >= 0) {
    		byte r = text.keyTyped(c, k);
    		if (r == 1) this.setTextField(-1);
    		else if (r >= 0) this.setTextField((sel + r + 5) % 6);
    	} else super.keyTyped(c, k);
    }
    
    @Override
    protected void mouseClicked(int x, int y, int b) throws IOException {
        boolean a = false;
        int k = (y - this.guiTop - 15) / 9;
        if (k < 0 || k >= 6) k = -1;
        if (this.isPointInRegion(11, 15, 9, 54, x, y)) {
        	tileEntity.netData.longs[0] ^= 1L << (48 + k);
        	a = true;
        }
        if (!this.isPointInRegion(21, 15, 14, 54, x, y)) k = -1;
        if (k != sel) this.setTextField(k);
        else if (a) this.sendCurrentChange();
        else super.mouseClicked(x, y, b);
    }
    
    private void setTextField(int k) throws IOException {
		if (k == sel) return;
		if (sel >= 0) try {
			tileEntity.setConfig(sel, Integer.parseInt(text.text, 16));
				this.sendCurrentChange();
			} catch(NumberFormatException e) {}
		if (k >= 0) {
			text.text = String.format("%02X", tileEntity.getConfig(k));
			if (sel < 0) text.cur = text.text.length();
		}
		sel = k;
	}

	private void sendCurrentChange() throws IOException {
		PacketBuffer dos = tileEntity.getPacketTargetData();
		dos.writeByte(AutomatedTile.CmdOffset);
		dos.writeLong(tileEntity.netData.longs[0]);
		BlockGuiHandler.sendPacketToServer(dos);
	}
    
}
