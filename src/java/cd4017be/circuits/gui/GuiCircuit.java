/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits.gui;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cd4017be.circuits.tileEntity.Circuit;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.TileContainer;
import cd4017be.lib.templates.AutomatedTile;
import cd4017be.lib.templates.GuiMachine;

/**
 *
 * @author CD4017BE
 */
public class GuiCircuit extends GuiMachine
{
    
    private final Circuit tileEntity;
    
    public GuiCircuit(Circuit tileEntity, EntityPlayer player)
    {
        super(new TileContainer(tileEntity, player));
        this.tileEntity = tileEntity;
    }
    
    @Override
    public void initGui() 
    {
        this.xSize = 176;
        this.ySize = 76;
        super.initGui();
    }
    
    @Override
	protected void drawGuiContainerForegroundLayer(int mx, int my) 
    {
		super.drawGuiContainerForegroundLayer(mx, my);
		this.drawInfo(26, 34, 16, 16, "\\i", "circuit.io");
		this.drawInfo(44, 34, 16, 16, "\\i", "rs.channel");
	}
    
    @Override
    protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) 
    {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.renderEngine.bindTexture(new ResourceLocation("circuits", "textures/gui/circuit.png"));
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
        if (tileEntity.getConfig(6) != 0) this.drawTexturedModalRect(this.guiLeft + 115, this.guiTop + 15, 72, 76, 18, 18);
        int x, y, c;
        for (int i = 0; i < 6; i++) {
            x = this.guiLeft + 25 + (i / 2) * 54;
            y = this.guiTop + 33 + (i % 2) * 18;
            c = tileEntity.getConfig(i);
            this.drawTexturedModalRect(x, y, (c >> 4) * 18, 76, 18, 18);
            this.drawTexturedModalRect(x + 18, y, (c & 0xf) * 18, 152, 18, 18);
        }
        this.drawStringCentered(tileEntity.getInventoryName(), this.guiLeft + this.xSize / 2, this.guiTop + 4, 0x404040);
        this.drawStringCentered(String.format("%.2f", (float)tileEntity.netData.ints[0] / 20F).concat("s"), this.guiLeft + 79, this.guiTop + 20, 0x404040);
        
    }
    
    @Override
    protected void mouseClicked(int x, int y, int b) 
    {
        byte a = -1;
        if (this.func_146978_c(133, 15, 36, 18, x, y)) {
            a = 0;
        } else if (this.func_146978_c(115, 15, 18, 18, x, y)) {
            tileEntity.setConfig(6, tileEntity.getConfig(6) ^ 1);
            a = 2;
        } else if (this.func_146978_c(44, 16, 10, 16, x, y)) {
            tileEntity.netData.ints[0] -= 20;
            a = 1;
        } else if (this.func_146978_c(54, 16, 10, 16, x, y)) {
            tileEntity.netData.ints[0] -= 1;
            a = 1;
        } else if (this.func_146978_c(94, 16, 10, 16, x, y)) {
            tileEntity.netData.ints[0] += 1;
            a = 1;
        } else if (this.func_146978_c(104, 16, 10, 16, x, y)) {
            tileEntity.netData.ints[0] += 20;
            a = 1;
        }
        int k, l, c;
        for (int i = 0; i < 6; i++) {
            k = 25 + (i / 2) * 54;
            l = 33 + (i % 2) * 18;
            c = tileEntity.getConfig(i);
            if (this.func_146978_c(k, l, 18, 18, x, y)) {
                tileEntity.setConfig(i, c + 0x10);
                a = 2;
                break;
            } else if (this.func_146978_c(k + 18, l, 18, 18, x, y)) {
                tileEntity.setConfig(i, (c&0x30) | ((c&0xf) + (b==0?1:9)) % 10);
                a = 2;
                break;
            }
        }
        if (a >= 0)
        {
            try {
            ByteArrayOutputStream bos = tileEntity.getPacketTargetData();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeByte(AutomatedTile.CmdOffset + a);
            if (a == 1) {
                if (tileEntity.netData.ints[0] < 1) tileEntity.netData.ints[0] = 1;
                if (tileEntity.netData.ints[0] > 1200) tileEntity.netData.ints[0] = 1200;
                dos.writeInt(tileEntity.netData.ints[0]);
            }
            else if (a == 2) dos.writeLong(tileEntity.netData.longs[0]);
            BlockGuiHandler.sendPacketToServer(bos);
            } catch (IOException e){}
        }
        super.mouseClicked(x, y, b);
    }
    
}
