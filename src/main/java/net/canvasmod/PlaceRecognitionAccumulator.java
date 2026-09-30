package net.canvasmod;

public final class PlaceRecognitionAccumulator {
    private PlaceFamiliarityPolicy.Kind kind = PlaceFamiliarityPolicy.Kind.NONE;
    private String dimension;
    private double x;
    private double y;
    private double z;
    private int goodSamples;

    public boolean observe(
            PlaceFamiliarityPolicy.Kind newKind,
            String newDimension,
            double newX,
            double newY,
            double newZ,
            int requiredGoodSamples) {
        if (newKind == null || newKind == PlaceFamiliarityPolicy.Kind.NONE || newDimension == null) {
            reset();
            return false;
        }

        if (goodSamples == 0
                || !PlaceFamiliarityPolicy.samePlace(
                        kind, dimension, x, y, z,
                        newKind, newDimension, newX, newY, newZ,
                        PlaceFamiliarityPolicy.CANDIDATE_RADIUS_SQ)) {
            kind = newKind;
            dimension = newDimension;
            x = newX;
            y = newY;
            z = newZ;
            goodSamples = 1;
            return requiredGoodSamples <= 1;
        }

        goodSamples++;
        double n = goodSamples;
        x += (newX - x) / n;
        y += (newY - y) / n;
        z += (newZ - z) / n;
        return goodSamples >= requiredGoodSamples;
    }

    public void reset() {
        kind = PlaceFamiliarityPolicy.Kind.NONE;
        dimension = null;
        goodSamples = 0;
    }

    public PlaceFamiliarityPolicy.Kind kind() { return kind; }
    public String dimension() { return dimension; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public int goodSamples() { return goodSamples; }
}
