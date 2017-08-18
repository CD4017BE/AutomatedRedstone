package cd4017be.circuits.render;

import org.lwjgl.opengl.GL11;

import cd4017be.circuits.tileEntity.BitShifter;
import cd4017be.lib.render.Util;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

public class BitShiftRenderer extends TileEntitySpecialRenderer<BitShifter> {

	private static final float ZLevel = 0.265625F;
	private static final ResourceLocation texture = new ResourceLocation("circuits", "textures/blocks/bitSlider.png");

	@Override
	public void renderTileEntityAt(BitShifter te, double x, double y, double z, float partialTicks, int destroyStage) {
		GlStateManager.pushMatrix();
		Util.moveAndOrientToBlock(x, y, z, te.getOrientation());
		Util.rotateTo(4);
		bindTexture(texture);
		float w = (float)te.size / 32F;
		int[] sliderIn = Util.texturedRect((float)te.ofsI / 32F - 0.5F, -0.125F, ZLevel, w, 0.25F, 0, 0, w, 1),
			sliderOut = Util.texturedRect((float)te.ofsO / 32F- 0.5F, -0.125F, ZLevel, w, 0.25F, 0, 0, w, 1);
		VertexBuffer buff = Tessellator.getInstance().getBuffer();
		buff.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		buff.addVertexData(sliderIn);
		Tessellator.getInstance().draw();
		Util.rotateTo(0);
		buff.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		buff.addVertexData(sliderOut);
		Tessellator.getInstance().draw();
		Util.rotateTo(0);
		buff.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		buff.addVertexData(sliderIn);
		Tessellator.getInstance().draw();
		Util.rotateTo(0);
		buff.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		buff.addVertexData(sliderOut);
		Tessellator.getInstance().draw();
		GlStateManager.popMatrix();
	}

}
