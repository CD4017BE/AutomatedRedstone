package cd4017be.circuits.gui;

import java.io.IOException;

import org.lwjgl.opengl.GL11;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import cd4017be.circuits.tileEntity.ArithmeticConverter;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.TileContainer;
import cd4017be.lib.templates.AutomatedTile;
import cd4017be.lib.templates.GuiMachine;

/**
*
* @author CD4017BE
*/
public class GuiArithmeticConverter extends GuiMachine
{

	private final ArithmeticConverter tileEntity;
	private int sel = -1;
	private TextField text = new TextField("", 4);

	public GuiArithmeticConverter(ArithmeticConverter tileEntity, EntityPlayer player)
	{
		super(new TileContainer(tileEntity, player));
		this.tileEntity = tileEntity;
	}

	@Override
	public void initGui() 
	{
		this.xSize = 56;
		this.ySize = 85;
		super.initGui();
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mx, int my) 
	{
		super.drawGuiContainerForegroundLayer(mx, my);
		this.drawInfo(21, 15, 28, 9, "\\i", "rs.const");
		this.drawInfo(11, 24, 9, 54, "\\i", "rs.dir");
		this.drawInfo(21, 24, 20, 54, "\\i", "rs.filter");
		if (this.isPointInRegion(12, 25, 7, 52, mx, my)) {
			int s = (my - guiTop - 25) / 9;
			this.drawSideCube(-64, 15, s, (byte)(tileEntity.getConfig(s | 8) + 2));
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) 
	{
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.renderEngine.bindTexture(new ResourceLocation("circuits", "textures/gui/circuit.png"));
		this.drawTexturedModalRect(this.guiLeft, this.guiTop, 192, 80, this.xSize, this.ySize);
		int x = guiLeft + 11, y = guiTop + 15, c;
		for (int i = 0; i < 6; i++) {
			c = tileEntity.getConfig(i | 8);
			this.drawTexturedModalRect(x, y + 9 + i * 9, 242, c * 9, 9, 9);
		}
		String s;
		x = guiLeft + 22;
		for (int i = 0; i < 7; i++) {
			y = guiTop + 25 + i * 9;
			if (i == 6) y -= 63;
			if (i == sel) text.draw(x, y, 0x80ffff, 0xffff8080);
			else {
				s = Integer.toString(tileEntity.getConfig(i));
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
			else if (r >= 0) this.setTextField((sel + r + 6) % 7);
		} else super.keyTyped(c, k);
	}

	@Override
	protected void mouseClicked(int x, int y, int b) throws IOException {
		boolean a = false;
		int k = (y - this.guiTop - 15) / 9 - 1;
		if (k == -1) k = 6;
		else if (k < 0 || k > 6) k = -1;
		if (this.isPointInRegion(11, 15, 9, 63, x, y)) {
			if (b == 0 && k < 6) {
				tileEntity.netData.longs[0] ^= 1L << (58 + k);
				a = true;
			}
		}
		if (!this.isPointInRegion(21, 15, 20, 63, x, y)) k = -1;
		if (k != sel) this.setTextField(k);
		else if (a) this.sendCurrentChange();
		else super.mouseClicked(x, y, b);
	}
	
	private void setTextField(int k) throws IOException {
		if (k == sel) return;
		if (sel >= 0) try {
				tileEntity.setConfig(sel, Integer.parseInt(text.text));
				this.sendCurrentChange();
			} catch(NumberFormatException e) {}
		if (k >= 0) {
			text.text = Integer.toString(tileEntity.getConfig(k));
			text.cur = sel < 0 ? text.text.length() : Math.min(text.cur, text.text.length());
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
