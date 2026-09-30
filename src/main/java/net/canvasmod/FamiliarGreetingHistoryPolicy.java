package net.canvasmod;

/**
 * Remember which relationship milestones a player has already been told
 * about in THIS world. Observation remains the source of actual familiarity;
 * these tiers only silence repetitive UI text, never confer extra progress.
 */
public final class FamiliarGreetingHistoryPolicy {
    private FamiliarGreetingHistoryPolicy() { }

    public static int tier(FamiliarBondPolicy.Bond bond) {
        if (bond == null) return 0;
        return switch (bond) {
            case UNKNOWN -> 0;
            case RECOGNIZED -> 1;
            case OLD_FRIEND -> 2;
        };
    }

    public static boolean shouldAnnounce(
            FamiliarBondPolicy.Bond bond, int previouslyAnnouncedTier) {
        return tier(bond) > Math.max(0, previouslyAnnouncedTier);
    }

    public static int acknowledge(FamiliarBondPolicy.Bond bond, int prior) {
        return Math.max(Math.max(0, prior), tier(bond));
    }
}
