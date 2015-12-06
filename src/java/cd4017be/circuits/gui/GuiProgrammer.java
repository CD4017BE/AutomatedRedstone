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
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import cd4017be.circuits.tileEntity.Programmer;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.TileContainer;
import cd4017be.lib.templates.AutomatedTile;
import cd4017be.lib.templates.GuiMachine;

/**
 *
 * @author CD4017BE
 */
public class GuiProgrammer extends GuiMachine
{
    private int Scroll = 0;
    private int curW = 0;
    private int curX = 0;
    private int curY = 0;
    private String numS = "";
    private final Programmer tileEntity;
    
    public GuiProgrammer(Programmer tileEntity, EntityPlayer player)
    {
        super(new TileContainer(tileEntity, player));
        this.tileEntity = tileEntity;
    }

    @Override
    public void initGui() 
    {
        this.xSize = 176;
        this.ySize = 189;
        super.initGui();
    }

    @Override
	protected void drawGuiContainerForegroundLayer(int mx, int my) 
    {
		super.drawGuiContainerForegroundLayer(mx, my);
		this.drawInfo(152, 37, 16, 16, "\\i", "gui.program.save");
		this.drawInfo(152, 73, 16, 16, "\\i", "gui.program.load");
		this.drawInfo(89, 16, 12, 8, "\\i", "gui.program.cs");
		this.drawInfo(102, 16, 12, 8, "\\i", "gui.program.cr");
		this.drawInfo(116, 16, 12, 8, "\\i", "gui.program.o1");
		this.drawInfo(129, 16, 12, 8, "\\i", "gui.program.o2");
		this.drawInfo(95, 4, 74, 8, "\\i", "gui.program.name");
		this.drawInfo(8, 16, 70, 8, "\\i", "gui.program.info");
	}

	@Override
    protected void drawGuiContainerBackgroundLayer(float f, int i, int j) 
    {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.renderEngine.bindTexture(new ResourceLocation("circuits", "textures/gui/programmer.png"));
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
        int n = tileEntity.gates.length <= 8 ? 0 : Scroll * 52 / (tileEntity.gates.length - 8);
        this.drawTexturedModalRect(this.guiLeft + 8, this.guiTop + 25 + n, 176, 0, 8, 12);
        this.drawText();
        this.drawConfig();
        fontRendererObj.drawString("Circuit Programm:", this.guiLeft + 8, this.guiTop + 4, 0x404040);
        fontRendererObj.drawString(curW == 2 && curY == 32 ? numS : tileEntity.name, this.guiLeft + 96, this.guiTop + 4, 0x404040);
        this.drawStringCentered(tileEntity.message, this.guiLeft + this.xSize / 2, this.guiTop + 91, 0x804040);
    }
    
    private void drawText()
    {
        for (int i = Scroll; i < Scroll + 8 && i < tileEntity.gates.length; i++) {
            this.fontRendererObj.drawString(tileEntity.gates[i], this.guiLeft + 31, this.guiTop + 25 + (i - Scroll)*8, 0xffffff);
            this.fontRendererObj.drawString("" + i, this.guiLeft + 18, this.guiTop + 25 + (i - Scroll)*8, 0xffff80);
        }
        if (curW == 1 && curY >= Scroll && curY < Scroll + 8 && curY < tileEntity.gates.length && curX <= tileEntity.gates[curY].length()) {
            int i = this.guiTop + (curY - Scroll) * 8 + 25;
            int j = this.fontRendererObj.getStringWidth(tileEntity.gates[curY].substring(0, curX));
            this.drawVerticalLine(this.guiLeft + 30 + j, i, i + 7, 0xffff8080);
        }
    }
    
    private void drawConfig()
    {
        for (int i = 0; i < 8; i++) {
            this.fontRendererObj.drawString(this.getConfig(i), this.guiLeft + 90, this.guiTop + 25 + i * 8, 0xffffff);
            this.fontRendererObj.drawString(this.getConfig(i + 8), this.guiLeft + 103, this.guiTop + 25 + i * 8, 0xffffff);
            this.fontRendererObj.drawString(this.getConfig(i + 16), this.guiLeft + 117, this.guiTop + 25 + i * 8, 0xffffff);
            this.fontRendererObj.drawString(this.getConfig(i + 24), this.guiLeft + 130, this.guiTop + 25 + i * 8, 0xffffff);
        }
        if (curW == 2 && curX <= numS.length()) {
            int i = this.guiTop + (curY == 32 ? 4 :(curY % 8) * 8 + 25);
            int j = this.guiLeft + this.fontRendererObj.getStringWidth(numS.substring(0, curX)) + (curY == 32 ? 95 : 89 + (curY / 8) * 13 + curY / 16);
            this.drawVerticalLine(j, i, i + 7, 0xffff8080);
        }
    }
    
    private String getConfig(int i)
    {
        if (curW == 2 && i == curY) return numS;
        else if (i < 16) return "" + tileEntity.counter[i];
        else return "" + tileEntity.outputs[i - 16];
    }
    
    @Override
    public void handleMouseInput() 
    {
        int d = Mouse.getEventDWheel() / 120;
        if (d != 0) {
            Scroll -= d;
            if (Scroll > tileEntity.gates.length - 8) Scroll = tileEntity.gates.length - 8;
            if (Scroll < 0) Scroll = 0;
        }
        super.handleMouseInput();
    }
    
    @Override
    protected void mouseClicked(int x, int y, int b) 
    {
        super.mouseClicked(x, y, b);
        if (this.func_146978_c(89, 25, 52, 64, x, y)) {
            this.sendCurrentChange();
            curW = 0;
            curY = (x - this.guiLeft - 89) / 13 * 8 + (y - this.guiTop - 25) / 8;
            if (curY < 0 || curY >= 32) return;
            numS = this.getConfig(curY);
            curW = 2;
            curX = numS.length();
        } else if (this.func_146978_c(18, 25, 60, 64, x, y)) {
            this.sendCurrentChange();
            curW = 1;
            curY = (y - this.guiTop - 25) / 8 + Scroll;
            if (curY >= tileEntity.gates.length) curY = tileEntity.gates.length - 1;
            curX = tileEntity.gates[curY].length();
        } else if (this.func_146978_c(88, 4, 80, 8, x, y)){
            this.sendCurrentChange();
            curW = 2;
            curY = 32;
            numS = tileEntity.name;
            curX = tileEntity.name.length();
        } else {
            this.sendCurrentChange();
            curW = 0;
        }
        if (this.func_146978_c(152, 37, 16, 16, x, y)) {
            this.sendCMD((byte)1);
        } else if (this.func_146978_c(152, 73, 16, 16, x, y)) {
            this.sendCMD((byte)0);
        } else if (this.func_146978_c(151, 15, 18, 18, x, y)) {
            //Help Screen
        }
    }

    @Override
    protected void keyTyped(char c, int k) 
    {
        if (curW == 1) {
            try {
            int l = tileEntity.gates.length;
            if (k == Keyboard.KEY_RIGHT){
                curX++;
                if (curX > tileEntity.gates[curY].length() && curY < tileEntity.gates.length - 1){
                    this.sendCurrentChange();
                    curY ++;
                    curX = 0;
                } else if (curX > tileEntity.gates[curY].length()) curX = tileEntity.gates[curY].length();
            }
            else if (k == Keyboard.KEY_LEFT){
                curX--;
                if (curX < 0 && curY > 0){
                    this.sendCurrentChange();
                    curY --;
                    curX = tileEntity.gates[curY].length();
                } else if (curX < 0) curX = 0;
            }
            else if (k == Keyboard.KEY_DOWN && curY < l - 1) {
                this.sendCurrentChange();
                curY ++;
                curX = tileEntity.gates[curY].length();
            } else if (k == Keyboard.KEY_UP && curY > 0) {
                this.sendCurrentChange();
                curY --;
                curX = tileEntity.gates[curY].length();
            } else if (k == Keyboard.KEY_DELETE && curX < tileEntity.gates[curY].length()) {
                tileEntity.gates[curY] = tileEntity.gates[curY].substring(0, curX).concat(tileEntity.gates[curY].substring(curX + 1));
            } else if (k == Keyboard.KEY_BACK) {
                if (curX <= 0) {
                    this.sendAddRemove(4, curY);
                    if (curY > 0) curX = tileEntity.gates[--curY].length();
                } else {
                    curX --;
                    tileEntity.gates[curY] = tileEntity.gates[curY].substring(0, curX).concat(tileEntity.gates[curY].substring(curX + 1));
                }
            } else if (k == Keyboard.KEY_RETURN) {
                this.sendCurrentChange();
                if (curX <= 0) this.sendAddRemove(3, curY++);
                else this.sendAddRemove(3, ++curY);
            } else if (ChatAllowedCharacters.isAllowedCharacter(c)){
                tileEntity.gates[curY] = tileEntity.gates[curY].substring(0, curX).concat("" +c).concat(tileEntity.gates[curY].substring(curX));
                curX++;
            }
            } catch (IndexOutOfBoundsException e) {
                if (curY < 0) curY = 0;
                if (curY >= tileEntity.gates.length) curY = tileEntity.gates.length - 1;
                if (curX < 0) curX = 0;
                if (curX > tileEntity.gates[curY].length()) curX = tileEntity.gates[curY].length();
            }
            if (curY < Scroll) Scroll = curY;
            if (curY >= Scroll + 8) Scroll = curY - 7;
        } else if (curW == 2) {
            try {
            if (k == Keyboard.KEY_LEFT && curX > 0) curX--;
            else if (k == Keyboard.KEY_RIGHT && curX < numS.length()) curX++;
            else if (k == Keyboard.KEY_DELETE && curX < numS.length()){
                numS = numS.substring(0, curX).concat(numS.substring(curX + 1));
            } else if (k == Keyboard.KEY_BACK && curX > 0) {
                curX --;
                numS = numS.substring(0, curX).concat(numS.substring(curX + 1));
            } else if (k == Keyboard.KEY_RETURN) {
                this.sendCurrentChange();
                curW = 0;
            } else if (ChatAllowedCharacters.isAllowedCharacter(c)){
                numS = numS.substring(0, curX).concat("" +c).concat(numS.substring(curX));
                curX++;
            }
            } catch (IndexOutOfBoundsException e) {
                if (curX < 0) curX = 0;
                if (curX > numS.length()) curX = numS.length();
            }
        } else super.keyTyped(c, k);
    }
    
    private void sendCurrentChange()
    {
        try{
        if (curW == 1) {
            ByteArrayOutputStream bos = tileEntity.getPacketTargetData();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeByte(AutomatedTile.CmdOffset + 2);
            dos.writeByte(curY);
            dos.writeUTF(tileEntity.gates[curY]);
            BlockGuiHandler.sendPacketToServer(bos);
        } else if (curW == 2) {
            ByteArrayOutputStream bos = tileEntity.getPacketTargetData();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeByte(AutomatedTile.CmdOffset + (curY < 16 ? 6 : curY < 32 ? 5 : 7));
            if (curY < 32) {
                dos.writeByte(curY % 16);
                dos.writeByte(Short.parseShort(numS));
            } else dos.writeUTF(numS);
            BlockGuiHandler.sendPacketToServer(bos);
        }
        } catch(IOException e) {
        } catch(NumberFormatException e) {
        } catch(ArrayIndexOutOfBoundsException e) {}
    }
    
    private void sendCMD(byte cmd)
    {
        try {
            ByteArrayOutputStream bos = tileEntity.getPacketTargetData();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeByte(AutomatedTile.CmdOffset + cmd);
            BlockGuiHandler.sendPacketToServer(bos);
        } catch(IOException e) {}
    }
    
    private void sendAddRemove(int cmd, int l)
    {
        try {
            ByteArrayOutputStream bos = tileEntity.getPacketTargetData();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeByte(AutomatedTile.CmdOffset + cmd);
            dos.writeByte(l);
            BlockGuiHandler.sendPacketToServer(bos);
        } catch(IOException e) {}
    }
    
}
