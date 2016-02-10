/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits.render;

import net.minecraft.client.Minecraft;
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
    
    private void renderStateBinary(byte state, int tex)
    {
        manager.renderEngine.bindTexture(new ResourceLocation("circuits", "textures/blocks/displayOvl.png"));
        WorldRenderer t = Tessellator.getInstance().getWorldRenderer();
        t.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        for (int i = 0; i < 8; i++) {
        	this.renderFace(t, tex | (state >> i & 1), 4 * (i % 4), 8 * (i / 4), 4, 8);
        	/*
        	float tsx = 0.125F, tsy = 0.25F;
        	float tx = (tex | (state >> i & 1)) * tsx;
            float ty = 0;
            t.pos((i % 4) * -0.25F + 0.25F, (i / 4) * -0.5F + 1F, -0.53125F).tex(tx, ty).endVertex();
            t.pos((i % 4) * -0.25F + 0.5F, (i / 4) * -0.5F + 1F, -0.53125F).tex(tx + tsx, ty).endVertex();
            t.pos((i % 4) * -0.25F + 0.5F, (i / 4) * -0.5F + 0.5F, -0.53125F).tex(tx + tsx, ty + tsy).endVertex();
            t.pos((i % 4) * -0.25F + 0.25F, (i / 4) * -0.5F + 0.5F, -0.53125F).tex(tx, ty + tsy).endVertex();
            */
        }
        Tessellator.getInstance().draw();
    }
    
    private void renderText(byte state, boolean hex)
    {
    	manager.renderEngine.bindTexture(new ResourceLocation("circuits", "textures/blocks/displayOvl.png"));
    	WorldRenderer t = Tessellator.getInstance().getWorldRenderer();
        t.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        if (hex) {
        	this.renderFace(t, (state >> 4 & 0xf) | 16, 2, 4, 4, 8);
        	this.renderFace(t, (state & 0xf) | 16, 10, 4, 4, 8);
        } else {
        	int n = state & 0xff;
        	this.renderFace(t, n / 100 | 16, 1, 4, 4, 8);
        	this.renderFace(t, (n / 10) % 10 | 16, 6, 4, 4, 8);
        	this.renderFace(t, n % 10 | 16, 11, 4, 4, 8);
        }
        Tessellator.getInstance().draw();
    	
        /*
    	String s = hex ? Integer.toHexString(state & 0xff) : Integer.toString(state & 0xff);
        FontRenderer fr = manager.getFontRenderer();
        GL11.glTranslatef(0.5F, 1F, -0.53125F);
        //GL11.glCullFace(GL11.GL_FRONT);
        if (hex) {
            GL11.glScalef(-0.0625F, -0.0625F, 0F);
            s = s.toUpperCase();
            if (s.length() < 2) s = "0".concat(s);
            fr.drawString(s.substring(0, 1), 1, 4, 0xffff00);
            fr.drawString(s.substring(1), 9, 4, 0xffff00);
        } else {
            GL11.glScalef(-0.05F, -0.0625F, 0F);
            fr.drawString(s, 19 - fr.getStringWidth(s), 4, 0xffff00);
        }
        //GL11.glCullFace(GL11.GL_BACK);
         */
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
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glDisable(GL11.GL_LIGHTING);
        int s = te.getBlockMetadata();
        float a = s == 4 ? 0F : s == 5 ? 180F : s == 6 ? 90F : -90F;
        GL11.glRotatef(a, 0, 1, 0);
        GL11.glScalef(-0.0625F, -0.0625F, 0.0625F);
        GL11.glTranslatef(-8F, -8F, -8F);
        if (te instanceof Lever8bit) {
            this.renderStateBinary(((Lever8bit)te).state, 0);
        } else if (te instanceof Display8bit) {
            Display8bit dsp = (Display8bit)te;
            if (dsp.dspType == 0) this.renderStateBinary(dsp.state, 2);
            else if (dsp.dspType == 1) this.renderText(dsp.state, true);
            else this.renderText(dsp.state, false);
        }
        GL11.glPopMatrix();
	}
    
}
