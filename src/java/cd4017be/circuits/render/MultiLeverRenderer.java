package cd4017be.circuits.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cd4017be.circuits.tileEntity.MultiLever;
import cd4017be.lib.render.Util;

/**
 *
 * @author CD4017BE
 */
public class MultiLeverRenderer extends TileEntitySpecialRenderer<MultiLever> {

	private static final ResourceLocation MAIN_TEX = new ResourceLocation("circuits", "textures/blocks/levers.png");

	private final RenderManager manager = Minecraft.getMinecraft().getRenderManager();

	private void renderFace(BufferBuilder t, int idx, float x, float y, float w, float h) {
		float tw = 0.125F, tx = (float)idx * tw;
		t.pos(x, y + h, -0.25F).tex(tx, 1F).endVertex();
		t.pos(x + w, y + h, -0.25F).tex(tx + tw, 1F).endVertex();
		t.pos(x + w, y, -0.25F).tex(tx + tw, 0F).endVertex();
		t.pos(x, y, -0.25F).tex(tx, 0F).endVertex();
	}

	@Override
	public void render(MultiLever te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		GlStateManager.disableLighting();
		Util.luminate(te, te.getOrientation().front, 0);
		GlStateManager.pushMatrix();;
		Util.moveAndOrientToBlock(x, y, z, te.getOrientation());
		GlStateManager.scale(-0.0625F, -0.0625F, 0.0625F);
		GlStateManager.translate(-8F, -8F, -8F);
		GlStateManager.color(1, 1, 1, 1);
		manager.renderEngine.bindTexture(MAIN_TEX);
		BufferBuilder t = Tessellator.getInstance().getBuffer();
		t.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		for (int i = 0; i < 8; i++)
			this.renderFace(t, te.state >> i & 1, 4 * (i % 4), 8 * (i / 4), 4, 8);
		Tessellator.getInstance().draw();
		GlStateManager.popMatrix();
	}

}
