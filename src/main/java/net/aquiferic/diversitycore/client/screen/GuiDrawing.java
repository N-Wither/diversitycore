package net.aquiferic.diversitycore.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/** Shared drawing helpers for the widgets texture atlas. */
public final class GuiDrawing {
    private GuiDrawing() {
    }

    public static void drawRegion(GuiGraphicsExtractor graphics, GuiSprites.Region region, int x, int y) {
        blit(graphics, x, y, region.x(), region.y(), region.width(), region.height());
    }

    /** Keeps corners at their original size and tiles the edges and center without stretching. */
    public static void drawNineSlice(GuiGraphicsExtractor graphics, GuiSprites.NineSlice slice,
            int x, int y, int width, int height) {
        var region = slice.region();
        int centerWidth = region.width() - slice.left() - slice.right();
        int centerHeight = region.height() - slice.top() - slice.bottom();
        if (slice.left() < 0 || slice.top() < 0 || slice.right() < 0 || slice.bottom() < 0
                || centerWidth <= 0 || centerHeight <= 0) {
            throw new IllegalArgumentException("Nine-slice borders must leave a positive center region");
        }
        if (width < slice.left() + slice.right() || height < slice.top() + slice.bottom()) {
            throw new IllegalArgumentException("Panel size must fit its nine-slice borders");
        }

        int[] sourceWidths = {slice.left(), centerWidth, slice.right()};
        int[] sourceHeights = {slice.top(), centerHeight, slice.bottom()};
        int[] targetWidths = {slice.left(), width - slice.left() - slice.right(), slice.right()};
        int[] targetHeights = {slice.top(), height - slice.top() - slice.bottom(), slice.bottom()};
        int sourceY = region.y();
        int targetY = y;
        for (int row = 0; row < 3; row++) {
            int sourceX = region.x();
            int targetX = x;
            for (int column = 0; column < 3; column++) {
                tile(graphics, targetX, targetY, targetWidths[column], targetHeights[row],
                    sourceX, sourceY, sourceWidths[column], sourceHeights[row]);
                sourceX += sourceWidths[column];
                targetX += targetWidths[column];
            }
            sourceY += sourceHeights[row];
            targetY += targetHeights[row];
        }
    }

    private static void tile(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
            int sourceX, int sourceY, int tileWidth, int tileHeight) {
        if (tileWidth <= 0 || tileHeight <= 0) {
            return;
        }
        for (int dy = 0; dy < height; dy += tileHeight) {
            for (int dx = 0; dx < width; dx += tileWidth) {
                // Crop the final tile to the remaining space instead of extending into adjacent sprites.
                blit(graphics, x + dx, y + dy, sourceX, sourceY,
                    Math.min(tileWidth, width - dx), Math.min(tileHeight, height - dy));
            }
        }
    }

    private static void blit(GuiGraphicsExtractor graphics, int x, int y, int sourceX, int sourceY,
            int width, int height) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, GuiSprites.TEXTURE, x, y, sourceX, sourceY,
            width, height, GuiSprites.TEXTURE_WIDTH, GuiSprites.TEXTURE_HEIGHT);
    }
}
