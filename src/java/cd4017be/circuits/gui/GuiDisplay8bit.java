package cd4017be.circuits.gui;

import java.io.IOException;

import org.lwjgl.opengl.GL11;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import cd4017be.circuits.tileEntity.Display8bit;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.TileContainer;
import cd4017be.lib.templates.GuiMachine;

public class GuiDisplay8bit extends GuiMachine {

	private final Display8bit tileEntity;
    private int sel = -1;
    private TextField text;
    
    public GuiDisplay8bit(Display8bit tileEntity, EntityPlayer player)
    {
        super(new TileContainer(tileEntity, player));
        this.tileEntity = tileEntity;
    }
    
    @Override
    public void initGui() 
    {
        this.xSize = 64;
        this.ySize = 53;
        super.initGui();
    }
    
    @Override
	protected void drawGuiContainerForegroundLayer(int mx, int my) 
    {
		super.drawGuiContainerForegroundLayer(mx, my);
		this.drawInfo(8, 27, 18, 7, "\\i", "display8bit.format");
		this.drawInfo(8, 16, 18, 7, "\\i", "display8bit.text");
		this.drawInfo(8, 38, 18, 7, "\\i", "display8bit.text");
		this.drawInfo(38, 27, 18, 7, "\\i", "display8bit.mode");
	}
    
    @Override
    protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) 
    {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.renderEngine.bindTexture(new ResourceLocation("circuits", "textures/gui/circuit.png"));
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 128, this.xSize, this.ySize);
        this.drawTexturedModalRect(this.guiLeft + 37, this.guiTop + 26, 64, 128 + tileEntity.dspType * 9, 20, 9);
        int x, y;
        x = guiLeft + 8;
        for (int i = 0; i < 3; i++) {
        	y = guiTop + 16 + i * 11;
        	if (i == sel) text.draw(x, y, 0x80ffff, 0xffff8080);
        	else this.fontRendererObj.drawString(i==0?tileEntity.text0:i==1?tileEntity.format:tileEntity.text1, x, y, 0x80ffff);
        }
        this.drawStringCentered(tileEntity.getName(), this.guiLeft + this.xSize / 2, this.guiTop + 4, 0x404040);
    }
    
    @Override
    protected void keyTyped(char c, int k) throws IOException {
    	if (sel >= 0) {
    		byte r = text.keyTyped(c, k);
    		if (r == 1) this.setTextField(-1);
    		else if (r >= 0) this.setTextField((sel + r + 5) % 3);
    	} else super.keyTyped(c, k);
    }
    
    private void setTextField(int k) throws IOException {
		if (k == sel) return;
		if (sel >= 0) {
			PacketBuffer dos = tileEntity.getPacketTargetData();
			dos.writeByte(sel);
			dos.writeString(text.text);
			BlockGuiHandler.sendPacketToServer(dos);
		}
		if (k == 0) text = new TextField(tileEntity.text0, 16);
		else if (k == 1) text = new TextField(tileEntity.format, 3);
		else if (k == 2) text = new TextField(tileEntity.text1, 16);
		else text = null;
		if (text != null) text.cur = text.text.length();
		sel = k;
	}
    
    @Override
    protected void mouseClicked(int x, int y, int b) throws IOException
    {
    	int s = -1;
        if (this.isPointInRegion(38, 27, 18, 7, x, y)) {
            tileEntity.dspType = (byte)((tileEntity.dspType + 1) % 3);
            PacketBuffer dos = tileEntity.getPacketTargetData();
            dos.writeByte(3);
            dos.writeByte(tileEntity.dspType);
            BlockGuiHandler.sendPacketToServer(dos);
        } else if (this.isPointInRegion(8, 16, 48, 7, x, y)) s = 0;
        else if (this.isPointInRegion(8, 27, 18, 7, x, y)) s = 1;
        else if (this.isPointInRegion(8, 38, 48, 7, x, y)) s = 2;
        this.setTextField(s);
        super.mouseClicked(x, y, b);
    }

}
