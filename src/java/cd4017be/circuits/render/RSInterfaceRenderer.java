/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cd4017be.circuits.tileEntity.Display8bit;
import cd4017be.circuits.tileEntity.Lever8bit;
import cd4017be.lib.ModTileEntity;

/**
 *
 * @author CD4017BE
 */
public class RSInterfaceRenderer extends TileEntitySpecialRenderer<ModTileEntity>
{
    private final RenderManager manager = Minecraft.getMinecraft().getRenderManager();
    
    private void renderStateBinary(byte state, int tex, int h)
    {
        manager.renderEngine.bindTexture(new ResourceLocation("circuits", "textures/blocks/displayOvl.png"));
        WorldRenderer t = Tessellator.getInstance().getWorldRenderer();
        t.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        for (int i = 0; i < 8; i++)
        	this.renderFace(t, tex | (state >> i & 1), 4 * (i % 4), h * (i / 4) + 8 - h, 4, 8);
        Tessellator.getInstance().draw();
    }
    
    private void renderState(char[] format, int state, boolean hex)
    {
    	byte[] digits;
    	if (hex) digits = new byte[]{(byte)(state & 0xf), (byte)(state >> 4), 0};
    	else digits = new byte[]{(byte)(state % 10), (byte)((state / 10) % 10), (byte)(state / 100)};
    	manager.renderEngine.bindTexture(new ResourceLocation("circuits", "textures/blocks/displayOvl.png"));
    	WorldRenderer t = Tessellator.getInstance().getWorldRenderer();
        t.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        int n = 0;
        float dx = 20F / (float)(format.length + 1), x = 16F;
        for (int i = format.length - 1; i >= 0; i--) {
        	int d;
        	char c = format[i];
        	if (c == '#') d = digits[n++] + 8;
        	else if (c >= '0' && c <= '9') d = c - '0' + 8;
        	else if (c >= 'a' && c <= 'f') d = c - 'a' + 18;
        	else if (c >= 'A' && c <= 'F') d = c - 'A' + 18;
        	else if (c == ':') d = 24;
        	else if (c == '.') d = 25;
        	else if (c == '-') d = 26;
        	else if (c == '%') d = 27;
        	else d = 28;
        	this.renderFace(t, d, x -= dx, 4, 4, 8);
        }
        Tessellator.getInstance().draw();
    }
    
    private void renderText(String s, float y) {
    	FontRenderer fr = manager.getFontRenderer();
        float w = fr.getStringWidth(s);
        float scale = Math.min(0.375F, 14F / w);
        GL11.glPushMatrix();
        GL11.glTranslatef(8F - w * 0.5F * scale, y + 1.5F - 4F * scale, -0.25F);
    	GL11.glScalef(scale, scale, 0F);
        fr.drawString(s, 0, 0, 0xffff00);
        GL11.glPopMatrix();
    }
    
    private void renderFace(WorldRenderer t, int idx, float x, float y, float w, float h) {
    	float tw = 0.125F, th = 0.25F, tx = (float)(idx % 8) * tw, ty = (float)(idx / 8) * th;
    	t.pos(x, y + h, -0.25F).tex(tx, ty + th).endVertex();
        t.pos(x + w, y + h, -0.25F).tex(tx + tw, ty + th).endVertex();
        t.pos(x + w, y, -0.25F).tex(tx + tw, ty).endVertex();
        t.pos(x, y, -0.25F).tex(tx, ty).endVertex();
    }

	@Override
	public void renderTileEntityAt(ModTileEntity te, double x, double y, double z, float partialTicks, int destroyStage) {
		GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5D, y + 0.5D, z + 0.5D);
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.disableLighting();
        int s = te.getBlockMetadata();
        float a = s == 4 ? 0F : s == 5 ? 180F : s == 6 ? 90F : -90F;
        GL11.glRotatef(a, 0, 1, 0);
        GL11.glScalef(-0.0625F, -0.0625F, 0.0625F);
        GL11.glTranslatef(-8F, -8F, -8F);
        if (te instanceof Lever8bit) {
            this.renderStateBinary(((Lever8bit)te).state, 0, 8);
        } else if (te instanceof Display8bit) {
            Display8bit dsp = (Display8bit)te;
            if (dsp.dspType == 0) this.renderStateBinary(dsp.state, 2, 4);
            else if (dsp.dspType == 1) this.renderState(dsp.format.toCharArray(), dsp.state & 0xff, true);
            else this.renderState(dsp.format.toCharArray(), dsp.state & 0xff, false);
            if (!dsp.text0.isEmpty()) this.renderText(dsp.text0, 1);
            if (!dsp.text1.isEmpty()) this.renderText(dsp.text1, 12);
        }
        GL11.glPopMatrix();
	}
    
}
