/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits.render;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cd4017be.circuits.tileEntity.Display8bit;
import cd4017be.circuits.tileEntity.Lever8bit;

/**
 *
 * @author CD4017BE
 */
public class RSInterfaceRenderer extends TileEntitySpecialRenderer
{
    private final RenderManager manager = RenderManager.instance;
    
    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float t) 
    {
        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5D, y, z + 0.5D);
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glDisable(GL11.GL_LIGHTING);
        int s = te.getBlockMetadata();
        float a = 0F;
        if (s == 3) a = 180F;
        else if (s == 4) a = 90F;
        else if (s == 5) a = -90F;
        GL11.glRotatef(a, 0, 1, 0);
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
    
    private void renderStateBinary(byte state, int tex)
    {
        manager.renderEngine.bindTexture(new ResourceLocation("circuits", "textures/blocks/icons.png"));
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        for (int i = 0; i < 8; i++) {
            float tx = (tex | (state >> i & 1)) * 0.25F;
            float ty = 0;
            t.addVertexWithUV((i % 4) * -0.25F + 0.25F, (i / 4) * -0.5F + 1F, -0.53125F, tx, ty);
            t.addVertexWithUV((i % 4) * -0.25F + 0.5F, (i / 4) * -0.5F + 1F, -0.53125F, tx + 0.25F, ty);
            t.addVertexWithUV((i % 4) * -0.25F + 0.5F, (i / 4) * -0.5F + 0.5F, -0.53125F, tx + 0.25F, ty + 0.5F);
            t.addVertexWithUV((i % 4) * -0.25F + 0.25F, (i / 4) * -0.5F + 0.5F, -0.53125F, tx, ty + 0.5F);
        }
        t.draw();
    }
    
    private void renderText(byte state, boolean hex)
    {
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
    }
    
}
