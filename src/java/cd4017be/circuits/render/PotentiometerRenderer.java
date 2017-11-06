package cd4017be.circuits.render;

import org.lwjgl.opengl.GL11;

import cd4017be.circuits.tileEntity.Potentiometer;
import cd4017be.lib.render.Util;
import cd4017be.lib.util.Orientation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

public class PotentiometerRenderer extends TileEntitySpecialRenderer<Potentiometer> {

	private static final ResourceLocation texture = new ResourceLocation("circuits", "textures/blocks/levers.png");
	private static final int[] handleModel = Util.texturedRect(0.0625F-0.5F, 0.0625F-0.5F, 0.515625F, 10F/32F, 3F/32F, 22F/32F, 0, 10F/32F, 3F/8F);

	@Override
	public void render(Potentiometer te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.disableLighting();
		Util.luminate(te, te.getOrientation().front, 0);
		GlStateManager.pushMatrix();
		Util.moveAndOrientToBlock(x, y, z, te.getOrientation());
		Util.moveAndOrientToBlock(-0.5, ((float)te.cur - (float)te.min) / ((float)te.max - (float)te.min) * 25F / 32F - 0.5, -0.5, Orientation.S);
		bindTexture(texture);
		BufferBuilder buff = Tessellator.getInstance().getBuffer();
		buff.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		buff.addVertexData(handleModel);
		Tessellator.getInstance().draw();
		GlStateManager.popMatrix();
	}

}
