package net.canvasmod;

public final class HomeRecognitionAccumulator {
    public static final int DEFAULT_REQUIRED_GOOD_SAMPLES = 6;
    public static final double DEFAULT_CANDIDATE_RADIUS_SQ = 14.0 * 14.0;

    private String dimension;
    private double x;
    private double y;
    private double z;
    private int goodSamples;
    private int bestScore;

    public boolean observe(
            String newDimension,
            double newX,
            double newY,
            double newZ,
            HomeEvidencePolicy.Evidence evidence,
            int requiredGoodSamples) {
        if (!evidence.qualifies()) {
            reset();
            return false;
        }

        if (dimension == null
                || !dimension.equals(newDimension)
                || goodSamples == 0
                || distanceSq(newX, newY, newZ, x, y, z) > DEFAULT_CANDIDATE_RADIUS_SQ) {
            dimension = newDimension;
            x = newX;
            y = newY;
            z = newZ;
            goodSamples = 1;
            bestScore = evidence.score();
            return requiredGoodSamples <= 1;
        }

        goodSamples++;
        bestScore = Math.max(bestScore, evidence.score());
        return goodSamples >= requiredGoodSamples;
    }

    public void reset() {
        dimension = null;
        goodSamples = 0;
        bestScore = 0;
    }

    public String dimension() { return dimension; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public int goodSamples() { return goodSamples; }
    public int bestScore() { return bestScore; }

    private static double distanceSq(double ax,double ay,double az,double bx,double by,double bz) {
        double dx=ax-bx, dy=ay-by, dz=az-bz;
        return dx*dx + dy*dy + dz*dz;
    }
}
