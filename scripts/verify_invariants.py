#!/usr/bin/env python3
from pathlib import Path

roots = [Path("src/main/java"), Path("src/client/java")]
forbidden_mutations = [
    ".setBlock(",
    ".setBlockAndUpdate(",
    ".destroyBlock(",
    ".removeBlock(",
    ".setBlockState(",
]
forced_chunk_patterns = [
    ".getChunk(",
    ".getChunkAt(",
    ".loadChunk(",
]

violations = []
for root in roots:
    for path in root.rglob("*.java"):
        text = path.read_text(encoding="utf-8", errors="replace")
        for pattern in forbidden_mutations:
            if pattern in text:
                violations.append(f"{path}: forbidden world mutation pattern {pattern}")
        for pattern in forced_chunk_patterns:
            if pattern in text:
                violations.append(f"{path}: possible forced chunk-load pattern {pattern}")

detectors = [
    ("HomeEvidenceDetector", Path("src/main/java/net/canvasmod/HomeEvidenceDetector.java")),
    ("PlaceEvidenceDetector", Path("src/main/java/net/canvasmod/PlaceEvidenceDetector.java")),
]
for name, path in detectors:
    detector = path.read_text(encoding="utf-8", errors="replace")
    if ".getChunkNow(" not in detector:
        violations.append(f"{name} must remain loaded-only via getChunkNow")

if violations:
    raise SystemExit("Canvas invariant verification FAILED:\n" + "\n".join(violations))

print("Canvas invariant verification: PASS")
print("- no production block/world mutation calls found")
print("- no production forced chunk-load calls found")
print("- HOME and place detectors remain getChunkNow loaded-only")
