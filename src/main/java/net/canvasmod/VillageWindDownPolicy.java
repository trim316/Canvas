package net.canvasmod;

public final class VillageWindDownPolicy {
    public static final int REQUIRED_SAMPLES = 3;
    private int consecutive;
    private boolean presented;
    public void reset() { consecutive = 0; presented = false; }
    public boolean observe(VillageLifePolicy.Rhythm rhythm) {
        if (rhythm != VillageLifePolicy.Rhythm.WIND_DOWN) { reset(); return false; }
        if (consecutive < REQUIRED_SAMPLES) consecutive++;
        if (!presented && consecutive >= REQUIRED_SAMPLES) { presented = true; return true; }
        return false;
    }
    public int consecutiveSamples() { return consecutive; }
}
