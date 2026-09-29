package net.canvasmod;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;

public final class HomeEvidenceDetector {
    private static final int RADIUS = 8;

    private HomeEvidenceDetector() { }

    private static boolean isSheltered(ServerLevel level, BlockPos center) {
        if (!level.canSeeSky(center.above())) return true;
        for (int dy = 1; dy <= 8; dy++) {
            BlockPos above = center.above(dy);
            if (level.getChunkSource().getChunkNow(above.getX() >> 4, above.getZ() >> 4) == null) break;
            if (!level.getBlockState(above).isAir()) return true;
        }
        return false;
    }

    public static HomeEvidencePolicy.Evidence scan(ServerLevel level, BlockPos center) {
        int beds = 0;
        int storage = 0;
        int work = 0;
        int comfort = 0;
        boolean sheltered = isSheltered(level, center);

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dy = -3; dy <= 4; dy++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    BlockPos pos = new BlockPos(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) continue;

                    String id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock())
                            .toString().toLowerCase(Locale.ROOT);
                    if (HomeEvidencePolicy.isBed(id)) beds++;
                    if (HomeEvidencePolicy.isStorage(id)) storage++;
                    if (HomeEvidencePolicy.isWork(id)) work++;
                    if (HomeEvidencePolicy.isComfort(id)) comfort++;
                }
            }
        }

        return new HomeEvidencePolicy.Evidence(sheltered, beds, storage, work, comfort);
    }
}
