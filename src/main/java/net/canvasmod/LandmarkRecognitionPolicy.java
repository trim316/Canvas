package net.canvasmod;

public final class LandmarkRecognitionPolicy {
    public static final int GRID_SIZE = 16;
    public static final int REQUIRED_VISITS = 4;
    public static final long MIN_REVISIT_GAP_TICKS = 20L * 60L;
    public static final int MAX_LANDMARKS = 256;

    private LandmarkRecognitionPolicy() { }

    public static boolean eligibleKind(PlaceFamiliarityPolicy.Kind kind) {
        if (kind == null) return false;
        return switch (kind) {
            case DOCK, FARM, VIEWPOINT, GATHERING_SPOT -> true;
            case NONE, PATH -> false;
        };
    }

    public static String landmarkKey(
            PlaceFamiliarityPolicy.Kind kind,
            String dimension,
            double x,
            double y,
            double z) {
        int cellX = Math.floorDiv((int)Math.floor(x), GRID_SIZE);
        int cellY = Math.floorDiv((int)Math.floor(y), 8);
        int cellZ = Math.floorDiv((int)Math.floor(z), GRID_SIZE);
        return kind.name() + "|" + dimension + "|" + cellX + "," + cellY + "," + cellZ;
    }
}
