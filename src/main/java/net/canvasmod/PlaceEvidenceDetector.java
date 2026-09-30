package net.canvasmod;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public final class PlaceEvidenceDetector {
    private static final int RADIUS = 4;

    private PlaceEvidenceDetector() { }

    public static PlaceFamiliarityPolicy.Evidence scan(ServerLevel level, BlockPos center) {
        int path = 0;
        int wood = 0;
        int water = 0;
        int farm = 0;
        int comfort = 0;
        int social = 0;

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) continue;

                    BlockState state = level.getBlockState(pos);
                    String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().toLowerCase(Locale.ROOT);

                    if (isPath(id)) path++;
                    if (isWoodSurface(id)) wood++;
                    if (!state.getFluidState().isEmpty()) water++;
                    if (isFarm(id)) farm++;
                    if (isComfort(id)) comfort++;
                    if (isSocialAnchor(id)) social++;
                }
            }
        }

        return new PlaceFamiliarityPolicy.Evidence(
                path,
                wood,
                water,
                farm,
                comfort,
                social,
                level.canSeeSky(center.above()),
                countEdgeDrops(level, center));
    }

    private static int countEdgeDrops(ServerLevel level, BlockPos center) {
        int drops = 0;
        int[][] directions = {{RADIUS, 0}, {-RADIUS, 0}, {0, RADIUS}, {0, -RADIUS}};
        for (int[] direction : directions) {
            BlockPos edge = center.offset(direction[0], 0, direction[1]);
            if (level.getChunkSource().getChunkNow(edge.getX() >> 4, edge.getZ() >> 4) == null) continue;
            boolean clear = true;
            for (int dy = -1; dy >= -3; dy--) {
                BlockPos probe = edge.offset(0, dy, 0);
                if (level.getChunkSource().getChunkNow(probe.getX() >> 4, probe.getZ() >> 4) == null
                        || !level.getBlockState(probe).isAir()) {
                    clear = false;
                    break;
                }
            }
            if (clear) drops++;
        }
        return drops;
    }

    private static boolean isPath(String id) {
        return id.endsWith("dirt_path")
                || id.endsWith("gravel")
                || id.endsWith("packed_mud")
                || id.endsWith("mud_bricks")
                || id.endsWith("cobblestone")
                || id.endsWith("stone_bricks");
    }

    private static boolean isWoodSurface(String id) {
        return id.contains("planks")
                || id.endsWith("_slab")
                || id.endsWith("_stairs")
                || id.endsWith("_fence");
    }

    private static boolean isFarm(String id) {
        return id.endsWith("farmland")
                || id.endsWith("wheat")
                || id.endsWith("carrots")
                || id.endsWith("potatoes")
                || id.endsWith("beetroots")
                || id.endsWith("melon_stem")
                || id.endsWith("pumpkin_stem");
    }

    private static boolean isComfort(String id) {
        return id.contains("lantern")
                || id.contains("campfire")
                || id.contains("flower_pot")
                || id.contains("carpet")
                || id.contains("candle")
                || id.endsWith("_torch");
    }

    private static boolean isSocialAnchor(String id) {
        return id.endsWith("bell") || id.contains("campfire");
    }
}
