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
import net.minecraft.util.text.translation.I18n;

import org.lwjgl.opengl.GL11;

import cd4017be.circuits.tileEntity.Assembler;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.TileContainer;
import cd4017be.lib.templates.AutomatedTile;
import cd4017be.lib.templates.GuiMachine;

/**
 *
 * @author CD4017BE
 */
public class GuiAssembler extends GuiMachine
{
    private final Assembler tileEntity;
    
    public GuiAssembler(Assembler tileEntity, EntityPlayer player)
    {
        super(new TileContainer(tileEntity, player));
        this.tileEntity = tileEntity;
    }
    
    @Override
    public void initGui() 
    {
        this.xSize = 176;
        this.ySize = 168;
        super.initGui();
    }
    
    @Override
	protected void drawGuiContainerForegroundLayer(int mx, int my) 
    {
		super.drawGuiContainerForegroundLayer(mx, my);
		this.drawInfo(81, 30, 50, 24, "\\i", "assembler.do");
		this.drawInfo(8, 34, 16, 16, "\\i", "assembler.undo");
	}

	@Override
    protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) 
    {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.renderEngine.bindTexture(new ResourceLocation("circuits", "textures/gui/assembler.png"));
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
        this.drawItemConfig(tileEntity, -36, 7);
        this.drawStringCentered(tileEntity.getName(), this.guiLeft + this.xSize / 2, this.guiTop + 4, 0x404040);
        this.drawStringCentered(I18n.translateToLocal("container.inventory"), this.guiLeft + this.xSize / 2, this.guiTop + 76, 0x404040);
        this.drawStringCentered("InOut= " + tileEntity.netData.ints[0], this.guiLeft + 106, this.guiTop + 38, 0x408040);
        this.drawStringCentered("Gates= " + tileEntity.netData.ints[1], this.guiLeft + 106, this.guiTop + 30, 0x408040);
        this.drawStringCentered("Count= " + tileEntity.netData.ints[2], this.guiLeft + 106, this.guiTop + 46, 0x408040);
    }
    
    @Override
    protected void mouseClicked(int x, int y, int b) throws IOException
    {
        this.clickItemConfig(tileEntity, x - this.guiLeft + 36, y - this.guiTop - 7);
        byte a = -1;
        if (this.isPointInRegion(80, 29, 52, 26, x, y)) {
            a = 0;
        }
        if (a >= 0) {
            PacketBuffer dos = tileEntity.getPacketTargetData();
	        dos.writeByte(AutomatedTile.CmdOffset + a);
	        BlockGuiHandler.sendPacketToServer(dos);
        }
        super.mouseClicked(x, y, b);
    }
    
}
