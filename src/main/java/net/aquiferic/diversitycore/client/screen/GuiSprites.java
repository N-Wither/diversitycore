package net.aquiferic.diversitycore.client.screen;

import net.aquiferic.diversitycore.DiversityCore;
import net.aquiferic.diversitycore.util.QS;
import net.minecraft.resources.Identifier;

public final class GuiSprites {
    public static final Identifier TEXTURE = QS.idt(DiversityCore.MODID, "textures/gui/widgets.png");

    public static final int TEXTURE_WIDTH = 256;
    public static final int TEXTURE_HEIGHT = 256;

    public static final NineSlice PANEL = new NineSlice(new Region(0, 0, 12, 12), 4, 4, 4, 4);
    public static final Region SLOT = new Region(0, 16, 18, 18);
    public static final Region BIG_SLOT = new Region(18, 16, 26, 26);


    public record Region(int x, int y, int width, int height) {}
    public record NineSlice(Region region, int left, int top, int right, int bottom) {}
}
