package net.canvasmod;

public final class WorldMemoryScopePolicy {
    private WorldMemoryScopePolicy() { }

    public static String scope(String worldId, String localKey) {
        String world = worldId == null ? "" : worldId.trim();
        String local = localKey == null ? "" : localKey.trim();
        if (world.isBlank() || local.isBlank()) return "";
        return "world|" + world + "|" + local;
    }

    public static String mobPrefix(String worldId) {
        String world = worldId == null ? "" : worldId.trim();
        return world.isBlank() ? "" : "world." + world + ".mob.";
    }
}
