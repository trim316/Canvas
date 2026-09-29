#!/usr/bin/env python3
"""Deterministically generate small Canvas-owned OGG ambience assets.

Official CI runs this before Gradle so the distributable JAR always contains the
same assets. It intentionally uses only Python stdlib plus ffmpeg.
"""
from __future__ import annotations

import math
import pathlib
import struct
import subprocess
import tempfile
import wave

SR = 44100
ROOT = pathlib.Path("src/main/resources/assets/canvas/sounds")


def quantized(freq: float, duration: float) -> float:
    return round(freq * duration) / duration


def write_wave(path: pathlib.Path, samples: list[float]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(path), "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(SR)
        frames = bytearray()
        for sample in samples:
            value = max(-0.95, min(0.95, sample))
            frames.extend(struct.pack("<h", int(value * 32767)))
        out.writeframes(frames)


def loop_samples(duration: float, voices: list[tuple[float, float]], phase: float) -> list[float]:
    count = int(SR * duration)
    result: list[float] = []
    for i in range(count):
        t = i / SR
        value = 0.0
        for index, (freq, amp) in enumerate(voices):
            f = quantized(freq, duration)
            value += amp * math.sin(2.0 * math.pi * f * t + phase * index)
            value += amp * 0.20 * math.sin(2.0 * math.pi * f * 2.0 * t + 0.7 + phase * index)
        # Very slow periodic air movement; because the period is exactly the clip
        # duration it remains seamless at the loop boundary.
        value += 0.018 * math.sin(2.0 * math.pi * t / duration + phase)
        result.append(math.tanh(value * 1.3) * 0.34)
    return result


def coming_home_samples(duration: float = 6.0) -> list[float]:
    count = int(SR * duration)
    voices = [(130.81, 0.17), (164.81, 0.14), (196.00, 0.12), (261.63, 0.07), (329.63, 0.045)]
    result: list[float] = []
    for i in range(count):
        t = i / SR
        envelope = math.sin(math.pi * min(1.0, t / duration)) ** 1.4
        value = sum(amp * math.sin(2.0 * math.pi * freq * t) for freq, amp in voices)
        value += 0.06 * math.exp(-t / 2.2) * math.sin(2.0 * math.pi * 523.25 * t)
        result.append(math.tanh(value * envelope * 1.5) * 0.38)
    return result


def encode(target: pathlib.Path, samples: list[float]) -> None:
    with tempfile.TemporaryDirectory(prefix="canvas-audio-") as temp_dir:
        wav = pathlib.Path(temp_dir) / "source.wav"
        write_wave(wav, samples)
        target.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run(
            ["ffmpeg", "-loglevel", "error", "-y", "-i", str(wav), "-c:a", "libvorbis", "-q:a", "4", str(target)],
            check=True,
        )


def main() -> None:
    encode(ROOT / "cues/coming_home.ogg", coming_home_samples())
    encode(ROOT / "presence/hearth_air_v0.ogg",
           loop_samples(24.0, [(110.0, .13), (138.59, .10), (164.81, .08), (220.0, .035)], 0.55))
    encode(ROOT / "presence/harbor_air_v0.ogg",
           loop_samples(24.0, [(146.83, .07), (196.0, .055), (246.94, .045), (392.0, .016)], 0.85))
    encode(ROOT / "presence/void_stillness_v0.ogg",
           loop_samples(24.0, [(73.42, .08), (98.0, .04), (146.83, .025)], 1.25))
    for path in sorted(ROOT.rglob("*.ogg")):
        print(f"generated {path} {path.stat().st_size} bytes")


if __name__ == "__main__":
    main()
