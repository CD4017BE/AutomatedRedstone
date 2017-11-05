package cd4017be.circuits.render;

import org.lwjgl.opengl.GL11;

import cd4017be.circuits.tileEntity.Display8bit;
import cd4017be.lib.render.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

public class DisplayRenderer extends TileEntitySpecialRenderer<Display8bit> {

	private static final int[] colors = {
			0x17_09_3f, 0x00_00_aa, 0x00_aa_00, 0x00_aa_aa,
			0xaa_00_00, 0xaa_00_aa, 0xaa_55_00, 0xaa_aa_aa,
			0x55_55_55, 0x55_55_ff, 0x55_ff_55, 0x00_ff_ff,
			0xff_55_55, 0xff_00_ff, 0xff_ff_00, 0xff_ff_ff
		};

	private static final ResourceLocation TEX_7SEGMENT = new ResourceLocation("circuits", "textures/blocks/digits7segment.png");

	private final RenderManager manager = Minecraft.getMinecraft().getRenderManager();

	private void renderState(int state) {
		manager.renderEngine.bindTexture(TEX_7SEGMENT);
		VertexBuffer t = Tessellator.getInstance().getBuffer();
		t.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		int n = state >> 24 & 15, bits, mask;
		if (n > 3) {bits = 4; mask = 0xf;}
		else {bits = 8; mask = 0xff;}
		float dx = 20F / (float)(n + 1), x = 16F;
		for (int i = 0; i < n; i++) 
			this.renderFace(t, (state >> (i * bits) & mask), x -= dx, 4, 4, 8);
		Tessellator.getInstance().draw();
	}

	private void renderText(String s, float y, int c) {
		FontRenderer fr = manager.getFontRenderer();
		float w = fr.getStringWidth(s);
		float scale = Math.min(0.375F, 14F / w);
		GL11.glPushMatrix();
		GL11.glTranslatef(8F - w * 0.5F * scale, y + 1.5F - 4F * scale, -0.25F);
		GL11.glScalef(scale, scale, 0F);
		fr.drawString(s, 0, 0, c);
		GL11.glPopMatrix();
	}

	private void renderFace(VertexBuffer t, int idx, float x, float y, float w, float h) {
		float tw = 0.0625F, th = 0.25F, tx = (float)(idx % 16) * tw, ty = (float)(idx / 16) * th;
		t.pos(x, y + h, -0.25F).tex(tx, ty + th).endVertex();
		t.pos(x + w, y + h, -0.25F).tex(tx + tw, ty + th).endVertex();
		t.pos(x + w, y, -0.25F).tex(tx + tw, ty).endVertex();
		t.pos(x, y, -0.25F).tex(tx, ty).endVertex();
	}

	@Override
	public void renderTileEntityAt(Display8bit te, double x, double y, double z, float partialTicks, int destroyStage) {
		GlStateManager.pushMatrix();
		Util.moveAndOrientToBlock(x, y, z, te.getOrientation());
		GlStateManager.scale(-0.0625F, -0.0625F, 0.0625F);
		GlStateManager.translate(-8F, -8F, -8F);
		GlStateManager.depthMask(false);
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
		GlStateManager.disableLighting();
		Util.luminate(te, te.getOrientation().front, 15);
		if (te.display == -1) te.formatState();
		int c = colors[te.display >> 28 & 15];
		GlStateManager.color((float)(c >> 16 & 0xff) / 255F, (float)(c >> 8 & 0xff) / 255F, (float)(c & 0xff) / 255F);
		this.renderState(te.display);
		GlStateManager.color(1, 1, 1, 1);
		if (!te.text0.isEmpty()) this.renderText(te.text0, 1, c);
		if (!te.text1.isEmpty()) this.renderText(te.text1, 12, c);
		GlStateManager.popMatrix();
		GlStateManager.disableBlend();
		GlStateManager.depthMask(true);
	}

}
