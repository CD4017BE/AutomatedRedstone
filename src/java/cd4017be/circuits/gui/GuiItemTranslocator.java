package cd4017be.circuits.gui;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import cd4017be.circuits.tileEntity.ItemTranslocator;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.TileContainer;
import cd4017be.lib.templates.AutomatedTile;
import cd4017be.lib.templates.GuiMachine;

public class GuiItemTranslocator extends GuiMachine {

	private final ItemTranslocator tileEntity;
    
    public GuiItemTranslocator(ItemTranslocator tileEntity, EntityPlayer player)
    {
        super(new TileContainer(tileEntity, player));
        this.tileEntity = tileEntity;
    }
    
    @Override
    public void initGui() 
    {
        this.xSize = 212;
        this.ySize = 204;
        super.initGui();
    }
    
    @Override
	protected void drawGuiContainerForegroundLayer(int mx, int my) 
    {
		super.drawGuiContainerForegroundLayer(mx, my);
		this.drawInfo(188, 108, 16, 12, "\\i", "gui.rs.dir");
		this.drawInfo(8, 16, 7, 16, "\\i", "gui.rs.channel");
		this.drawInfo(17, 16, 7, 7, "\\i", "gui.trans.invIn");
		this.drawInfo(17, 25, 7, 7, "\\i", "gui.invRead.invS");
		this.drawInfo(98, 16, 7, 7, "\\i", "gui.trans.invOut");
		this.drawInfo(98, 25, 7, 7, "\\i", "gui.invRead.invS");
		this.drawInfo(44, 16, 16, 7, "\\i", "gui.filter.neg");
		this.drawInfo(44, 25, 16, 7, "\\i", "gui.filter.ore");
		this.drawInfo(62, 25, 16, 7, "\\i", "gui.filter.nbt");
		this.drawInfo(62, 16, 16, 7, "\\i", "gui.filter.meta");
		this.drawInfo(80, 20, 16, 8, "\\i", "gui.trans.am");
		this.drawInfo(188, 88, 16, 16, "\\i", "gui.trans.redir");
	}
    
    @Override
    protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) 
    {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.renderEngine.bindTexture(new ResourceLocation("circuits", "textures/gui/itemTranslocator.png"));
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
        int x, y, c;
        for (int i = 0; i < 8; i++) {
        	x = this.guiLeft + 7 + i / 4 * 99;
        	y = this.guiTop + 15 + (i % 4) * 18;
        	this.drawTexturedModalRect(x + 9, y, 230, 9 * tileEntity.getDir(i), 9, 9);
        	this.drawTexturedModalRect(x + 9, y + 9, 230, 9 * tileEntity.getDir(i | 8), 9, 9);
        	this.drawTexturedModalRect(x + 90, y, 230, 9 * tileEntity.getDir(i | 16), 9, 9);
        	this.drawTexturedModalRect(x + 90, y + 9, 230, 9 * tileEntity.getDir(i | 24), 9, 9);
        	c = tileEntity.getMode(i);
        	if ((c & 1) != 0) this.drawTexturedModalRect(x + 36, y, 212, 55, 18, 9);
        	if ((c & 2) != 0) this.drawTexturedModalRect(x + 54, y, 212, 46, 18, 9);
        	if ((c & 4) != 0) this.drawTexturedModalRect(x + 54, y + 9, 212, 37, 18, 9);
        	if ((c & 8) != 0) this.drawTexturedModalRect(x + 36, y + 9, 212, 28, 18, 9);
        }
        x = this.guiLeft + 187;
        for (int i = 0; i < 6; i++) {
        	y = this.guiTop + 107 + 15 * i;
        	c = tileEntity.getDirection(i);
        	if (c != 0) this.drawTexturedModalRect(x, y, 212, c == -1 ? 0 : 14, 18, 14);
        }
        if ((tileEntity.netData.longs[3] >> 47L & 1L) != 0) this.drawTexturedModalRect(this.guiLeft + 115, this.guiTop + 87, 212, 64, 18, 18);
        this.drawTexturedModalRect(this.guiLeft + 187, this.guiTop + 87, 212, 82 + 18 * (int)(tileEntity.netData.longs[3] >> 44L & 0x7L), 18, 18);
        for (int i = 0; i < 8; i++)
        	this.drawStringCentered("" + tileEntity.netData.ints[i], this.guiLeft + 88 + i / 4 * 99, this.guiTop + 20 + (i % 4) * 18, 0x404040);
        this.drawStringCentered(String.format("%.2f", (float)tileEntity.netData.ints[8] / 20F).concat("s"), this.guiLeft + 79, this.guiTop + 92, 0x404040);
        this.drawStringCentered(tileEntity.getInventoryName(), this.guiLeft + this.xSize / 2, this.guiTop + 4, 0x404040);
        this.drawStringCentered("Inventory", this.guiLeft + this.xSize / 2, this.guiTop + 110, 0x404040);
    }

	@Override
    protected void mouseClicked(int x, int y, int b) 
    {
    	byte a = -1, d = -1;
    	if (this.func_146978_c(115, 87, 18, 18, x, y)) {
            a = 18;
        } else if (this.func_146978_c(187, 87, 18, 18, x, y)) {
            a = 19;
        } else if (this.func_146978_c(44, 88, 10, 16, x, y)) {
            tileEntity.netData.ints[8] -= 20;
            if (tileEntity.netData.ints[8] < 1) tileEntity.netData.ints[8] = 1;
            a = 17;
        } else if (this.func_146978_c(54, 88, 10, 16, x, y)) {
            tileEntity.netData.ints[8] -= 1;
            if (tileEntity.netData.ints[8] < 1) tileEntity.netData.ints[8] = 1;
            a = 17;
        } else if (this.func_146978_c(94, 88, 10, 16, x, y)) {
            tileEntity.netData.ints[8] += 1;
            if (tileEntity.netData.ints[8] > 1200) tileEntity.netData.ints[8] = 1200;
            a = 17;
        } else if (this.func_146978_c(104, 88, 10, 16, x, y)) {
            tileEntity.netData.ints[8] += 20;
            if (tileEntity.netData.ints[8] > 1200) tileEntity.netData.ints[8] = 1200;
            a = 17;
        }
    	int k, l;
        for (int i = 0; i < 6 && a < 0; i++) {
            k = 187; l = 107 + i * 15;
            if (this.func_146978_c(k, l, 18, 14, x, y)) {
            	a = 16;
            	d = (byte)i;
            	break;
            }
        }
        for (int i = 0; i < 8 && a < 0; i++) {
        	k = 7 + i / 4 * 99; l = 15 + (i % 4) * 18;
        	if (this.func_146978_c(k + 9, l, 9, 9, x, y)) {
        		a = (byte)(8 + i);
        		d = 0;
        	} else if (this.func_146978_c(k + 9, l + 9, 9, 9, x, y)) {
        		a = (byte)(8 + i);
        		d = 1;
        	} else if (this.func_146978_c(k + 90, l, 9, 9, x, y)) {
        		a = (byte)(8 + i);
        		d = 2;
        	} else if (this.func_146978_c(k + 90, l + 9, 9, 9, x, y)) {
        		a = (byte)(8 + i);
        		d = 3;
        	} else if (this.func_146978_c(k + 36, l, 18, 9, x, y)) {
        		a = (byte)(8 + i);
        		d = 4;
        	} else if (this.func_146978_c(k + 54, l, 18, 9, x, y)) {
        		a = (byte)(8 + i);
        		d = 5;
        	} else if (this.func_146978_c(k + 54, l + 9, 18, 9, x, y)) {
        		a = (byte)(8 + i);
        		d = 6;
        	} else if (this.func_146978_c(k + 36, l + 9, 18, 9, x, y)) {
        		a = (byte)(8 + i);
        		d = 7;
        	} else if (this.func_146978_c(k + 72, l, 18, 5, x, y)) {
        		tileEntity.netData.ints[i] += b == 0 ? 1 : 8;
        		if (tileEntity.netData.ints[i] > 64) tileEntity.netData.ints[i] = 64;
        		a = (byte)i;
        	} else if (this.func_146978_c(k + 72, l + 13, 18, 5, x, y)) {
        		tileEntity.netData.ints[i] -= b == 0 ? 1 : 8;
        		if (tileEntity.netData.ints[i] < 0) tileEntity.netData.ints[i] = 0;
        		a = (byte)i;
        	}
        }
        if (a >= 0)
        {
            try {
	            ByteArrayOutputStream bos = tileEntity.getPacketTargetData();
	            DataOutputStream dos = new DataOutputStream(bos);
	            dos.writeByte(AutomatedTile.CmdOffset + a);
	            if (a < 8) dos.writeInt(tileEntity.netData.ints[a]);
	            else if (a == 17) dos.writeInt(tileEntity.netData.ints[8]);
	            else if (a < 17)dos.writeByte(d);
	            BlockGuiHandler.sendPacketToServer(bos);
            } catch (IOException e){}
        }
        super.mouseClicked(x, y, b);
    }

}
