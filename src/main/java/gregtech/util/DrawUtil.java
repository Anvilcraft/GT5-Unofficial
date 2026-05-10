package gregtech.util;

import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

// Util class from IC2 experimental
public final class DrawUtil {
    public static void drawRepeated(
        final IIcon icon,
        final double x,
        final double y,
        final double width,
        final double height,
        final double z
    ) {
        final double iconWidthStep = (icon.getMaxU() - (double) icon.getMinU()) / 16.0;
        final double iconHeightStep = (icon.getMaxV() - (double) icon.getMinV()) / 16.0;
        final Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        for (double cy = y; cy < y + height; cy += 16.0) {
            final double quadHeight = Math.min(16.0, height + y - cy);
            final double maxY = cy + quadHeight;
            final double maxV = icon.getMinV() + iconHeightStep * quadHeight;
            for (double cx = x; cx < x + width; cx += 16.0) {
                final double quadWidth = Math.min(16.0, width + x - cx);
                final double maxX = cx + quadWidth;
                final double maxU = icon.getMinU() + iconWidthStep * quadWidth;
                tessellator.addVertexWithUV(cx, maxY, z, (double) icon.getMinU(), maxV);
                tessellator.addVertexWithUV(maxX, maxY, z, maxU, maxV);
                tessellator.addVertexWithUV(maxX, cy, z, maxU, (double) icon.getMinV());
                tessellator.addVertexWithUV(
                    cx, cy, z, (double) icon.getMinU(), (double) icon.getMinV()
                );
            }
        }
        tessellator.draw();
    }

    public static void renderIcon(
        final IIcon icon,
        final double size,
        final double z,
        final float nx,
        final float ny,
        final float nz
    ) {
        renderIcon(icon, 0.0, 0.0, size, size, z, nx, ny, nz);
    }

    public static void renderIcon(
        IIcon icon,
        final double xStart,
        final double yStart,
        final double xEnd,
        final double yEnd,
        final double z,
        final float nx,
        final float ny,
        final float nz
    ) {
        if (icon == null) {
            icon = RenderBlock.getMissingIcon(TextureMap.locationItemsTexture);
        }
        final Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setNormal(nx, ny, nz);
        if (nz > 0.0f) {
            tessellator.addVertexWithUV(
                xStart, yStart, z, (double) icon.getMinU(), (double) icon.getMinV()
            );
            tessellator.addVertexWithUV(
                xEnd, yStart, z, (double) icon.getMaxU(), (double) icon.getMinV()
            );
            tessellator.addVertexWithUV(
                xEnd, yEnd, z, (double) icon.getMaxU(), (double) icon.getMaxV()
            );
            tessellator.addVertexWithUV(
                xStart, yEnd, z, (double) icon.getMinU(), (double) icon.getMaxV()
            );
        } else {
            tessellator.addVertexWithUV(
                xStart, yEnd, z, (double) icon.getMinU(), (double) icon.getMaxV()
            );
            tessellator.addVertexWithUV(
                xEnd, yEnd, z, (double) icon.getMaxU(), (double) icon.getMaxV()
            );
            tessellator.addVertexWithUV(
                xEnd, yStart, z, (double) icon.getMaxU(), (double) icon.getMinV()
            );
            tessellator.addVertexWithUV(
                xStart, yStart, z, (double) icon.getMinU(), (double) icon.getMinV()
            );
        }
        tessellator.draw();
    }

    public static class RenderBlock {

        public static IIcon getMissingIcon(final ResourceLocation textureSheet) {
            return (IIcon) ((TextureMap) Minecraft.getMinecraft()
                .getTextureManager()
                .getTexture(textureSheet))
                .getAtlasSprite("missingno");
        }

    }

}
