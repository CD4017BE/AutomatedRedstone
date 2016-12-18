package cd4017be.circuits.render;

import org.lwjgl.opengl.GL11;

import cd4017be.circuits.tileEntity.Oszillograph;
import cd4017be.lib.render.Util;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

public class OszillographRenderer extends TileEntitySpecialRenderer<Oszillograph> {

	private static final float LineThick = 0.01F, ZLevel = 0.515625F, TexX = 0, TexY0 = 0, TexY1 = 1, textH = 0.0625F, textW = 0.375F;
	private static final int[] defaultQuad = {//X, Y, Z, U, V, C
		0, 0, Float.floatToIntBits(ZLevel), Float.floatToIntBits(TexX), Float.floatToIntBits(TexY0), 0,
		0, 0, Float.floatToIntBits(ZLevel), Float.floatToIntBits(TexX), Float.floatToIntBits(TexY1), 0};
	private static final int QuadSize = defaultQuad.length;
	public static final int[] colors = {0x0000ff, 0x00ff00, 0xff0000, 0xb400b4},
			textColors = {0xff0000, 0x00ff00, 0x0000ff, 0xb400b4};
	private static final ResourceLocation texture = new ResourceLocation("circuits", "textures/blocks/osziLine.png");

	@Override
	public void renderTileEntityAt(Oszillograph te, double x, double y, double z, float partialTicks, int destroyStage) {
		GlStateManager.pushMatrix();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.depthMask(false);
		GlStateManager.disableLighting();
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
		Util.moveAndOrientToBlock(x, y, z, te.getOrientation()^1);
		GL11.glScalef(0.9375F, 0.9375F, 1F);
		bindTexture(texture);
		VertexBuffer buff = Tessellator.getInstance().getBuffer();
		buff.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION_TEX_COLOR);
		for (int i = 0; i < te.vertexData.length; i++) {
			int[] vb = te.vertexData[i];
			if (vb != null) {
				buff.addVertexData(vb);
			}
		}
		Tessellator.getInstance().draw();
		renderInfoTexts(te);
		GlStateManager.popMatrix();
		GlStateManager.disableBlend();
		GlStateManager.depthMask(true);
	}

	private void renderInfoTexts(Oszillograph te) {
		FontRenderer fr = getFontRenderer();
		for (int i = 0; i < 4; i++)
			if (te.vertexData[i] != null && !te.info[i].isEmpty()) {
				boolean right = (i & 1) != 0, top = i < 2;
				String t = te.info[i];
				int l = fr.getStringWidth(t), h = fr.FONT_HEIGHT;
				float scale = Math.min(textH / (float)h, textW / (float)l);
				GlStateManager.pushMatrix();
				GL11.glTranslatef(right ? 0.5F : -0.5F, top ? 0.5F : -0.5F, ZLevel);
				GL11.glScalef(scale, -scale, 0);
				fr.drawString(t, right ? -l : 0, top ? 0 : -h, textColors[i]);
				GlStateManager.popMatrix();
			}
	}

	public static void recolor(int[] vb, int p, int color) {
		int n = vb.length / QuadSize - 1;
		for (int i = 5; i < vb.length; i += QuadSize) {
			int alpha = (i / QuadSize - p + n) % n;
			alpha = alpha < 15 ? alpha * 0x11000000 : 0xff000000;
			vb[i + 6] = vb[i] = color | alpha;
		}
	}

	public static void setValue(int[] vb, int p, float y) {
		int i = p * QuadSize + 1;
		vb[i] = Float.floatToIntBits(y + LineThick);
		vb[i + 6] = Float.floatToIntBits(y - LineThick);
		if (p == 0) setValue(vb, vb.length / QuadSize - 1, y);
	}

	public static int[] newVertexData(int n) {
		int[] vb = new int[(n + 1) * QuadSize];
		for (int i = 0, k = 0; i < vb.length; i += QuadSize, k++) {
			System.arraycopy(defaultQuad, 0, vb, i, QuadSize);
			vb[i + 6] = vb[i] = Float.floatToIntBits((float)k / (float)n - 0.5F);
		}
		return vb;
	}

}
