package net.canvasmod;

/** A deliberately small overlap budget for Canvas-owned atmosphere loops.
 *  Changing scenes may crossfade the current loop and exactly one previous
 *  loop; rapid scene changes retire older loops instead of layering sounds.
 */
public final class AmbienceHandoffPolicy {
    public static final int MAX_FADING_PREVIOUS_LOOPS = 1;

    private AmbienceHandoffPolicy() { }

    public static int oldestLoopsToRetire(int fadingCount) {
        return Math.max(0, fadingCount - MAX_FADING_PREVIOUS_LOOPS);
    }
}
