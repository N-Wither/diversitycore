package net.aquiferic.diversitycore.compat.jade;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import snownee.jade.api.ui.IDisplayHelper;
import snownee.jade.api.ui.ResizeableElement;

/** A resizable, narrated bar drawn without textures or Jade implementation classes. */
final class EnergyBarElement extends ResizeableElement {
    private final Component label;
    private final float fill;

    EnergyBarElement(Component label, float fill) {
        this.label = label;
        this.fill = Mth.clamp(fill, 0.0F, 1.0F);
        width = Math.max(100, Minecraft.getInstance().font.width(label) + 12);
        height = 14;
        flexGrow(1);
    }

    @Override
    public void setFreeSpace(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public Component getNarration() {
        return label;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        int x = getX();
        int y = getY();
        int right = x + width;
        int bottom = y + height;
        rect(graphics, x, y, right, bottom, 0xFF183D48);
        rect(graphics, x + 1, y + 1, right - 1, bottom - 1, 0xFF091D25);
        int filledRight = x + 2 + Math.round(Math.max(0, width - 4) * fill);
        int middle = y + height / 2;
        rect(graphics, x + 2, y + 2, filledRight, middle, 0xFF4DDACF);
        rect(graphics, x + 2, middle, filledRight, bottom - 2, 0xFF219FAD);
        graphics.text(Minecraft.getInstance().font, label,
                x + (width - Minecraft.getInstance().font.width(label)) / 2,
                y + (height - Minecraft.getInstance().font.lineHeight) / 2,
                ARGB.color(IDisplayHelper.get().opacity(), 0xFFFFFFFF), true);
    }

    private static void rect(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom, int color) {
        if (right > left && bottom > top) {
            graphics.fill(left, top, right, bottom, ARGB.color(IDisplayHelper.get().opacity(), color));
        }
    }
}
