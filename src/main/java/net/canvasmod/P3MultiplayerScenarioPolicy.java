package net.canvasmod;

import java.util.List;

public final class P3MultiplayerScenarioPolicy {
    private P3MultiplayerScenarioPolicy() { }

    public record Result(
            boolean sharedSettlement,
            boolean worldMemoryIsolated,
            boolean communityGathering,
            int settlementMembers,
            int gatheringParticipants) {
        public boolean passed() {
            return sharedSettlement
                    && worldMemoryIsolated
                    && communityGathering
                    && settlementMembers >= 2
                    && gatheringParticipants >= 2;
        }
    }

    public static Result evaluate(
            List<SharedSettlementPolicy.HomeAnchor> homes,
            String worldA,
            String worldB,
            String localMemoryKey,
            List<SharedGatheringPolicy.PlayerPresence> online) {
        var settlements = SharedSettlementPolicy.detect(homes);
        if (settlements.size() != 1) {
            return new Result(false, memoryIsolated(worldA, worldB, localMemoryKey), false, 0, 0);
        }

        var settlement = settlements.get(0);
        var gathering = SharedGatheringPolicy.detect(settlement, online);
        return new Result(
                true,
                memoryIsolated(worldA, worldB, localMemoryKey),
                gathering != null,
                settlement.members().size(),
                gathering == null ? 0 : gathering.participants().size());
    }

    private static boolean memoryIsolated(String worldA, String worldB, String localMemoryKey) {
        String a = WorldMemoryScopePolicy.scope(worldA, localMemoryKey);
        String b = WorldMemoryScopePolicy.scope(worldB, localMemoryKey);
        return !a.isBlank() && !b.isBlank() && !a.equals(b);
    }
}
