package net.aquiferic.diversitycore.api.registry;

/** Width-to-height ratio of a stage sprite; geometry keeps a one-block width and its bottom anchor. */
public record CropTextureRatio(int width, int height) {
    public static final CropTextureRatio SQUARE = new CropTextureRatio(1, 1);

    public CropTextureRatio {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Crop texture ratio dimensions must be positive");
        }
    }

    public float modelHeight() {
        return 16.0F * height / width;
    }
}
