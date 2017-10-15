package cd4017be.circuits.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cd4017be.circuits.tileEntity.Display8bit;
import cd4017be.circuits.tileEntity.MultiLever;
import cd4017be.lib.render.Util;
import cd4017be.lib.tileentity.BaseTileEntity;

/**
 *
 * @author CD4017BE
 */
public class RSInterfaceRenderer extends TileEntitySpecialRenderer<BaseTileEntity> {

	private final RenderManager manager = Minecraft.getMinecraft().getRenderManager();

	private void renderStateBinary(byte state, int tex, int h) {
		manager.renderEngine.bindTexture(new ResourceLocation("circuits", "textures/blocks/display_ovl.png"));
		VertexBuffer t = Tessellator.getInstance().getBuffer();
		t.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		for (int i = 0; i < 8; i++)
			this.renderFace(t, tex | (state >> i & 1), 4 * (i % 4), h * (i / 4) + 8 - h, 4, 8);
		Tessellator.getInstance().draw();
	}

	private void renderState(int state) {
		manager.renderEngine.bindTexture(new ResourceLocation("circuits", "textures/blocks/display_ovl.png"));
		VertexBuffer t = Tessellator.getInstance().getBuffer();
		t.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		int n = state >> 24;
		float dx = 20F / (float)(n + 1), x = 16F;
		for (int i = 0; i < n; i++) 
			this.renderFace(t, (state >> (i * 8) & 0x1f), x -= dx, 4, 4, 8);
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

	private void renderFace(VertexBuffer t, int idx, float x, float y, float w, float h) {
		float tw = 0.125F, th = 0.25F, tx = (float)(idx % 8) * tw, ty = (float)(idx / 8) * th;
		t.pos(x, y + h, -0.25F).tex(tx, ty + th).endVertex();
		t.pos(x + w, y + h, -0.25F).tex(tx + tw, ty + th).endVertex();
		t.pos(x + w, y, -0.25F).tex(tx + tw, ty).endVertex();
		t.pos(x, y, -0.25F).tex(tx, ty).endVertex();
	}

	@Override
	public void renderTileEntityAt(BaseTileEntity te, double x, double y, double z, float partialTicks, int destroyStage) {
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.disableLighting();
		Util.luminate(te, te.getOrientation().front, te instanceof MultiLever ? 0 : 15);
		GL11.glPushMatrix();
		Util.moveAndOrientToBlock(x, y, z, te.getOrientation());
		/*
		GL11.glTranslated(x + 0.5D, y + 0.5D, z + 0.5D);
		int s = te.getBlockMetadata();
		float a = s == 4 ? 0F : s == 5 ? 180F : s == 6 ? 90F : -90F;
		GL11.glRotatef(a, 0, 1, 0);
		*/
		GL11.glScalef(-0.0625F, -0.0625F, 0.0625F);
		GL11.glTranslatef(-8F, -8F, -8F);
		if (te instanceof MultiLever) {
			this.renderStateBinary(((MultiLever)te).state, 0, 8);
		} else if (te instanceof Display8bit) {
			Display8bit dsp = (Display8bit)te;
			if ((dsp.dspType & 3) == 0) this.renderStateBinary((byte)dsp.display, 2, 4);
			else this.renderState(dsp.display);
			if (!dsp.text0.isEmpty()) this.renderText(dsp.text0, 1);
			if (!dsp.text1.isEmpty()) this.renderText(dsp.text1, 12);
		}
		GL11.glPopMatrix();
	}

}
