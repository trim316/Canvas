package net.canvasmod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.level.storage.LevelResource;

final class CanvasPlaceRuntime {
    private static final int SAMPLE_INTERVAL = 100;
    private static final int MAX_PLACES_PER_PLAYER = 96;

    private final Map<UUID, State> states = new HashMap<>();
    private Path file;
    private Path evidenceFile;
    private long tick;

    void onServerStarting(MinecraftServer server) {
        states.clear();
        tick = 0;
        Path root = server.getWorldPath(LevelResource.ROOT);
        file = root.resolve("data").resolve("canvas-places-v1.properties");
        evidenceFile = root.resolve("canvas-runtime-evidence").resolve("place-familiarity.log");
        load();
        evidence("session_start", "-", "place_schema=v1");
    }

    void onServerTick(MinecraftServer server) {
        tick++;
        if (tick % SAMPLE_INTERVAL != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) observe(player);
    }

    void onServerStopped() {
        save();
        evidence("session_stop", "-", "players=" + states.size());
        states.clear();
        tick = 0;
    }

    private void observe(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos center = player.blockPosition();
        if (level.getChunkSource().getChunkNow(center.getX() >> 4, center.getZ() >> 4) == null) return;

        State state = states.computeIfAbsent(player.getUUID(), ignored -> new State());
        PlaceFamiliarityPolicy.Evidence placeEvidence = PlaceEvidenceDetector.scan(level, center);
        PlaceFamiliarityPolicy.Kind kind = placeEvidence.classify();
        String dimension = level.dimension().identifier().toString();

        RouteFamiliarityTracker.Observation routeObservation = state.routes.observe(
                kind,
                dimension,
                player.getX(),
                player.getZ());
        if (routeObservation.becameFamiliar()) {
            evidence("route_familiar", player.getUUID().toString(),
                    "tick=" + tick
                            + ",segment=" + routeObservation.segmentKey()
                            + ",traversals=" + routeObservation.traversals());
            save();
        }

        PlaceMemory knownPlace = findKnown(
                state.places,
                kind,
                dimension,
                player.getX(),
                player.getY(),
                player.getZ());
        boolean familiarNow = knownPlace != null;
        String activeLandmarkKey = "";
        boolean landmarkNow = false;
        if (knownPlace == null) {
            state.landmarks.observe("", kind, tick);
        } else {
            String landmarkKey = LandmarkRecognitionPolicy.landmarkKey(
                    knownPlace.kind(),
                    knownPlace.dimension(),
                    knownPlace.x(),
                    knownPlace.y(),
                    knownPlace.z());
            activeLandmarkKey = landmarkKey;
            LandmarkFamiliarityTracker.Observation landmarkObservation =
                    state.landmarks.observe(landmarkKey, knownPlace.kind(), tick);
            landmarkNow = state.landmarks.isLandmark(landmarkKey);
            if (landmarkObservation.becameLandmark()) {
                evidence("landmark_recognized", player.getUUID().toString(),
                        "tick=" + tick
                                + ",kind=" + knownPlace.kind()
                                + ",key=" + landmarkObservation.key()
                                + ",visits=" + landmarkObservation.visits());
                save();
            }
        }
        sendPlaceState(player, state, kind, familiarNow);
        sendLandmarkState(player, state, kind, activeLandmarkKey, landmarkNow);

        boolean recognized = state.accumulator.observe(
                kind,
                dimension,
                player.getX(),
                player.getY(),
                player.getZ(),
                PlaceFamiliarityPolicy.REQUIRED_GOOD_SAMPLES);

        if (kind != PlaceFamiliarityPolicy.Kind.NONE && state.accumulator.goodSamples() == 1) {
            evidence("place_candidate", player.getUUID().toString(),
                    "tick=" + tick + "," + placeEvidence.summary());
        }

        if (!recognized) return;

        double x = state.accumulator.x();
        double y = state.accumulator.y();
        double z = state.accumulator.z();

        if (alreadyKnown(state.places, kind, dimension, x, y, z)) {
            state.accumulator.reset();
            return;
        }
        if (state.places.size() >= MAX_PLACES_PER_PLAYER) {
            evidence("place_memory_full", player.getUUID().toString(),
                    "tick=" + tick + ",limit=" + MAX_PLACES_PER_PLAYER);
            state.accumulator.reset();
            return;
        }

        PlaceMemory newPlace = new PlaceMemory(kind, dimension, x, y, z);
        state.places.add(newPlace);
        String landmarkKey = LandmarkRecognitionPolicy.landmarkKey(
                newPlace.kind(),
                newPlace.dimension(),
                newPlace.x(),
                newPlace.y(),
                newPlace.z());
        state.landmarks.observe(landmarkKey, newPlace.kind(), tick);
        sendPlaceState(player, state, kind, true);
        sendLandmarkState(
                player,
                state,
                kind,
                landmarkKey,
                state.landmarks.isLandmark(landmarkKey));
        evidence("place_recognized", player.getUUID().toString(),
                "tick=" + tick + "," + placeEvidence.summary()
                        + ",x=" + Math.round(x)
                        + ",y=" + Math.round(y)
                        + ",z=" + Math.round(z));
        state.accumulator.reset();
        save();
    }

    private static PlaceMemory findKnown(
            List<PlaceMemory> places,
            PlaceFamiliarityPolicy.Kind kind,
            String dimension,
            double x,
            double y,
            double z) {
        if (kind == null || kind == PlaceFamiliarityPolicy.Kind.NONE) return null;
        for (PlaceMemory place : places) {
            if (PlaceFamiliarityPolicy.samePlace(
                    place.kind(), place.dimension(), place.x(), place.y(), place.z(),
                    kind, dimension, x, y, z,
                    PlaceFamiliarityPolicy.DUPLICATE_RADIUS_SQ)) {
                return place;
            }
        }
        return null;
    }

    private static boolean alreadyKnown(
            List<PlaceMemory> places,
            PlaceFamiliarityPolicy.Kind kind,
            String dimension,
            double x,
            double y,
            double z) {
        return findKnown(places, kind, dimension, x, y, z) != null;
    }

    private static void sendPlaceState(
            ServerPlayer player,
            State state,
            PlaceFamiliarityPolicy.Kind kind,
            boolean familiar) {
        String key = (kind == null ? PlaceFamiliarityPolicy.Kind.NONE : kind).name()
                + "|" + (familiar ? "1" : "0");
        if (key.equals(state.lastPayloadKey)) return;
        state.lastPayloadKey = key;
        ServerPlayNetworking.send(player, new PlaceStatePayload(
                kind == null ? PlaceFamiliarityPolicy.Kind.NONE.name() : kind.name(),
                familiar ? 1 : 0));
    }

    private static void sendLandmarkState(
            ServerPlayer player,
            State state,
            PlaceFamiliarityPolicy.Kind kind,
            String contextKey,
            boolean landmark) {
        PlaceFamiliarityPolicy.Kind safeKind =
                kind == null ? PlaceFamiliarityPolicy.Kind.NONE : kind;
        String safeKey = contextKey == null ? "" : contextKey;
        String payloadKey = safeKind.name()
                + "|" + safeKey
                + "|" + (landmark ? "1" : "0");
        if (payloadKey.equals(state.lastLandmarkPayloadKey)) return;
        state.lastLandmarkPayloadKey = payloadKey;
        ServerPlayNetworking.send(player, new LandmarkStatePayload(
                safeKind.name(),
                safeKey,
                landmark ? 1 : 0));
    }

    void save() {
        if (file == null) return;
        Properties properties = new Properties();
        for (var entry : states.entrySet()) {
            String prefix = entry.getKey().toString();
            List<PlaceMemory> places = entry.getValue().places;
            properties.setProperty(prefix + ".count", Integer.toString(places.size()));
            for (int i = 0; i < places.size(); i++) {
                PlaceMemory place = places.get(i);
                String key = prefix + ".place." + i + ".";
                properties.setProperty(key + "kind", place.kind().name());
                properties.setProperty(key + "dimension", place.dimension());
                properties.setProperty(key + "x", Double.toString(place.x()));
                properties.setProperty(key + "y", Double.toString(place.y()));
                properties.setProperty(key + "z", Double.toString(place.z()));
            }

            var routes = entry.getValue().routes.entries();
            properties.setProperty(prefix + ".route.count", Integer.toString(routes.size()));
            int routeIndex = 0;
            for (var route : routes.entrySet()) {
                String routeKey = prefix + ".route." + routeIndex + ".";
                properties.setProperty(routeKey + "key", route.getKey());
                properties.setProperty(routeKey + "traversals", Integer.toString(route.getValue()));
                routeIndex++;
            }

            var landmarks = entry.getValue().landmarks.visits();
            properties.setProperty(prefix + ".landmark.count", Integer.toString(landmarks.size()));
            int landmarkIndex = 0;
            for (var landmark : landmarks.entrySet()) {
                String landmarkKey = prefix + ".landmark." + landmarkIndex + ".";
                properties.setProperty(landmarkKey + "key", landmark.getKey());
                properties.setProperty(landmarkKey + "visits", Integer.toString(landmark.getValue()));
                landmarkIndex++;
            }
        }

        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (var out = Files.newOutputStream(tmp)) {
                properties.store(out, "Canvas familiar places v1");
            }
            try {
                Files.move(tmp, file,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) { }
    }

    private void load() {
        if (file == null || !Files.exists(file)) return;
        Properties properties = new Properties();
        try (var in = Files.newInputStream(file)) {
            properties.load(in);
        } catch (IOException ignored) {
            return;
        }

        for (String key : properties.stringPropertyNames()) {
            if (!key.endsWith(".count")) continue;
            String prefix = key.substring(0, key.length() - ".count".length());
            try {
                UUID id = UUID.fromString(prefix);
                int count = Math.min(
                        MAX_PLACES_PER_PLAYER,
                        Math.max(0, Integer.parseInt(properties.getProperty(key, "0"))));
                State state = states.computeIfAbsent(id, ignored -> new State());
                for (int i = 0; i < count; i++) {
                    String placeKey = prefix + ".place." + i + ".";
                    PlaceFamiliarityPolicy.Kind kind = PlaceFamiliarityPolicy.Kind.valueOf(
                            properties.getProperty(placeKey + "kind", "NONE"));
                    String dimension = properties.getProperty(placeKey + "dimension", "");
                    double x = Double.parseDouble(properties.getProperty(placeKey + "x", "0"));
                    double y = Double.parseDouble(properties.getProperty(placeKey + "y", "64"));
                    double z = Double.parseDouble(properties.getProperty(placeKey + "z", "0"));
                    if (kind != PlaceFamiliarityPolicy.Kind.NONE && !dimension.isBlank()) {
                        state.places.add(new PlaceMemory(kind, dimension, x, y, z));
                    }
                }

                int routeCount = Math.min(
                        RouteFamiliarityPolicy.MAX_SEGMENTS,
                        Math.max(0, Integer.parseInt(
                                properties.getProperty(prefix + ".route.count", "0"))));
                for (int i = 0; i < routeCount; i++) {
                    String routeKey = prefix + ".route." + i + ".";
                    String segment = properties.getProperty(routeKey + "key", "");
                    int traversals = Integer.parseInt(
                            properties.getProperty(routeKey + "traversals", "0"));
                    state.routes.restore(segment, traversals);
                }

                int landmarkCount = Math.min(
                        LandmarkRecognitionPolicy.MAX_LANDMARKS,
                        Math.max(0, Integer.parseInt(
                                properties.getProperty(prefix + ".landmark.count", "0"))));
                for (int i = 0; i < landmarkCount; i++) {
                    String landmarkKey = prefix + ".landmark." + i + ".";
                    String memoryKey = properties.getProperty(landmarkKey + "key", "");
                    int visits = Integer.parseInt(
                            properties.getProperty(landmarkKey + "visits", "0"));
                    state.landmarks.restore(memoryKey, visits);
                }
            } catch (IllegalArgumentException ignored) { }
        }
    }

    private void evidence(String type, String player, String detail) {
        if (evidenceFile == null) return;
        String line = Instant.now() + " type=" + type + " player=" + player + " " + detail
                + System.lineSeparator();
        try {
            Files.createDirectories(evidenceFile.getParent());
            Files.writeString(evidenceFile, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) { }
    }

    private record PlaceMemory(
            PlaceFamiliarityPolicy.Kind kind,
            String dimension,
            double x,
            double y,
            double z) { }

    private static final class State {
        final PlaceRecognitionAccumulator accumulator = new PlaceRecognitionAccumulator();
        final RouteFamiliarityTracker routes = new RouteFamiliarityTracker();
        final LandmarkFamiliarityTracker landmarks = new LandmarkFamiliarityTracker();
        final List<PlaceMemory> places = new ArrayList<>();
        String lastPayloadKey = "";
        String lastLandmarkPayloadKey = "";
    }
}
