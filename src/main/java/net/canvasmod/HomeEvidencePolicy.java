package net.canvasmod;

public final class HomeEvidencePolicy {
    private HomeEvidencePolicy() { }

    public record Evidence(boolean sheltered, int beds, int storage, int work, int comfort) {
        public int score() {
            int score = sheltered ? 2 : 0;
            if (beds > 0) score += 4;
            if (storage > 0) score += 2;
            if (work > 0) score += 2;
            if (comfort >= 2) score += 1;
            return score;
        }

        public boolean qualifies() {
            boolean domesticAnchor = storage > 0 || work > 0 || comfort >= 3;
            return sheltered && beds > 0 && domesticAnchor && score() >= 7;
        }

        public String summary() {
            return "sheltered=" + sheltered
                    + ",beds=" + beds
                    + ",storage=" + storage
                    + ",work=" + work
                    + ",comfort=" + comfort
                    + ",score=" + score();
        }
    }

    public static boolean isBed(String id) {
        return id.endsWith("_bed");
    }

    public static boolean isStorage(String id) {
        return containsAny(id, "chest", "barrel", "shulker_box");
    }

    public static boolean isWork(String id) {
        return containsAny(id,
                "crafting_table", "furnace", "smoker", "blast_furnace",
                "stonecutter", "anvil", "loom", "cartography_table",
                "smithing_table", "grindstone", "brewing_stand", "enchanting_table");
    }

    public static boolean isComfort(String id) {
        return containsAny(id,
                "bookshelf", "lantern", "campfire", "flower_pot", "carpet",
                "candle", "torch", "painting");
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) return true;
        }
        return false;
    }
}
