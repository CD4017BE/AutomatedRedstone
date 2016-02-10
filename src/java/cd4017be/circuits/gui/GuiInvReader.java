package cd4017be.circuits.gui;

import java.io.IOException;

import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cd4017be.circuits.tileEntity.InvReader;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.TileContainer;
import cd4017be.lib.templates.AutomatedTile;
import cd4017be.lib.templates.GuiMachine;

public class GuiInvReader extends GuiMachine
{

	private final InvReader tileEntity;
	private final GuiTextField[] refVal = new GuiTextField[8];
    
    public GuiInvReader(InvReader tileEntity, EntityPlayer player)
    {
        super(new TileContainer(tileEntity, player));
        this.tileEntity = tileEntity;
    }
    
    @Override
    public void initGui() 
    {
        this.xSize = 212;
        this.ySize = 186;
        super.initGui();
        for (int i = 0; i < 8; i++) {
        	refVal[i] = new GuiTextField(0, fontRendererObj, this.guiLeft + 53 + i / 4 * 99, this.guiTop + 24 + (i % 4) * 18, 52, 8);
        	refVal[i].setEnableBackgroundDrawing(false);
        }
    }
    
    @Override
	protected void drawGuiContainerForegroundLayer(int mx, int my) 
    {
		super.drawGuiContainerForegroundLayer(mx, my);
		this.drawInfo(188, 90, 16, 12, "\\i", "rs.dir");
		this.drawInfo(8, 16, 7, 16, "\\i", "rs.channel");
		this.drawInfo(17, 16, 7, 7, "\\i", "invRead.inv");
		this.drawInfo(17, 25, 7, 7, "\\i", "invRead.invS");
		this.drawInfo(44, 16, 7, 7, "\\i", "filter.neg");
		this.drawInfo(44, 25, 7, 7, "\\i", "invRead.comp");
		this.drawInfo(53, 16, 16, 7, "\\i", "filter.ore");
		this.drawInfo(71, 16, 16, 7, "\\i", "filter.nbt");
		this.drawInfo(89, 16, 16, 7, "\\i", "filter.meta");
		this.drawInfo(53, 24, 52, 8, "\\i", "invRead.am");
	}

	@Override
    protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) 
    {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.renderEngine.bindTexture(new ResourceLocation("circuits", "textures/gui/invReader.png"));
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
        int x, y, c;
        for (int i = 0; i < 8; i++) {
        	x = this.guiLeft + 7 + i / 4 * 99;
        	y = this.guiTop + 15 + (i % 4) * 18;
        	this.drawTexturedModalRect(x + 9, y, 212, 55 + 9 * tileEntity.getDir(i), 9, 9);
        	this.drawTexturedModalRect(x + 9, y + 9, 212, 55 + 9 * tileEntity.getDir(i | 8), 9, 9);
        	c = tileEntity.getMode(i);
        	this.drawTexturedModalRect(x + 36, y + 9, 221, 64 + 9 * (c & 0x3), 9, 9);
        	if ((c & 0x4) != 0) this.drawTexturedModalRect(x + 36, y, 221, 55, 9, 9);
        	if ((c & 0x8) != 0) this.drawTexturedModalRect(x + 81, y, 212, 46, 18, 9);
        	if ((c & 0x10) != 0) this.drawTexturedModalRect(x + 63, y, 212, 37, 18, 9);
        	if ((c & 0x20) != 0) this.drawTexturedModalRect(x + 45, y, 212, 28, 18, 9);
        }
        x = this.guiLeft + 187;
        for (int i = 0; i < 6; i++) {
        	y = this.guiTop + 89 + 15 * i;
        	c = tileEntity.getDirection(i);
        	if (c != 0) this.drawTexturedModalRect(x, y, 212, c == -1 ? 0 : 14, 18, 14);
        }
        for (int i = 0; i < refVal.length; i++) refVal[i].drawTextBox(); 
        this.drawStringCentered(tileEntity.getName(), this.guiLeft + this.xSize / 2, this.guiTop + 4, 0x404040);
        this.drawStringCentered(StatCollector.translateToLocal("container.inventory"), this.guiLeft + this.xSize / 2, this.guiTop + 92, 0x404040);
    }
    
    @Override
	protected void keyTyped(char c, int k) throws IOException
    {
		for (int i = 0; i < refVal.length; i++)
			if (refVal[i].isFocused()) {
				refVal[i].textboxKeyTyped(c, k);
				if (k == Keyboard.KEY_RETURN) {
					refVal[i].setFocused(false);
					try {
			            PacketBuffer dos = tileEntity.getPacketTargetData();
			            dos.writeByte(AutomatedTile.CmdOffset + i);
			            dos.writeInt(Integer.parseInt(refVal[i].getText()));
			            BlockGuiHandler.sendPacketToServer(dos);
					} catch (NumberFormatException e) {}
				}
				return;
			}
		super.keyTyped(c, k);
	}

	@Override
	public void updateScreen() 
	{
		for (int i = 0; i < refVal.length; i++)
			if (!refVal[i].isFocused())
				refVal[i].setText("" + tileEntity.netData.ints[i]);
		super.updateScreen();
	}

	@Override
    protected void mouseClicked(int x, int y, int b) throws IOException
    {
        for (int i = 0; i < 8; i++) refVal[i].mouseClicked(x, y, b);
    	byte a = -1, d = -1;
        int k, l;
        for (int i = 0; i < 6; i++) {
            k = 187; l = 89 + i * 15;
            if (this.isPointInRegion(k, l, 18, 14, x, y)) {
            	a = 16;
            	d = (byte)i;
            	break;
            }
        }
        for (int i = 0; i < 8 && a < 0; i++) {
        	k = 7 + i / 4 * 99; l = 15 + (i % 4) * 18;
        	if (this.isPointInRegion(k + 9, l, 9, 9, x, y)) {
        		a = (byte)(8 + i);
        		d = 0;
        	} else if (this.isPointInRegion(k + 9, l + 9, 9, 9, x, y)) {
        		a = (byte)(8 + i);
        		d = 1;
        	} else if (this.isPointInRegion(k + 36, l + 9, 9, 9, x, y)) {
        		a = (byte)(8 + i);
        		d = 2;
        	} else if (this.isPointInRegion(k + 36, l, 9, 9, x, y)) {
        		a = (byte)(8 + i);
        		d = 3;
        	} else if (this.isPointInRegion(k + 81, l, 18, 9, x, y)) {
        		a = (byte)(8 + i);
        		d = 4;
        	} else if (this.isPointInRegion(k + 63, l, 18, 9, x, y)) {
        		a = (byte)(8 + i);
        		d = 5;
        	} else if (this.isPointInRegion(k + 45, l, 18, 9, x, y)) {
        		a = (byte)(8 + i);
        		d = 6;
        	}
        }
        if (a >= 0) {
            PacketBuffer dos = tileEntity.getPacketTargetData();
	        dos.writeByte(AutomatedTile.CmdOffset + a);
	        dos.writeByte(d);
	        BlockGuiHandler.sendPacketToServer(dos);
        }
        super.mouseClicked(x, y, b);
    }

}
