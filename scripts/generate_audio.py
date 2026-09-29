#!/usr/bin/env python3
"""Deterministically generate Canvas-owned OGG ambience and transition assets."""
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


def loop_samples(duration: float, voices: list[tuple[float, float]], phase: float, air: float) -> list[float]:
    count = int(SR * duration)
    result: list[float] = []
    for i in range(count):
        t = i / SR
        value = 0.0
        for index, (freq, amp) in enumerate(voices):
            f = quantized(freq, duration)
            value += amp * math.sin(2.0 * math.pi * f * t + phase * index)
            value += amp * 0.18 * math.sin(2.0 * math.pi * f * 2.0 * t + 0.7 + phase * index)
        value += air * math.sin(2.0 * math.pi * t / duration + phase)
        result.append(math.tanh(value * 1.25) * 0.34)
    return result


def one_shot(duration: float, voices: list[tuple[float, float]], decay: float) -> list[float]:
    count = int(SR * duration)
    result: list[float] = []
    for i in range(count):
        t = i / SR
        envelope = math.sin(math.pi * min(1.0, t / duration)) ** 1.25
        envelope *= math.exp(-decay * t)
        value = sum(amp * math.sin(2.0 * math.pi * freq * t) for freq, amp in voices)
        result.append(math.tanh(value * envelope * 1.6) * 0.38)
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
    encode(ROOT / "cues/coming_home.ogg",
           one_shot(6.0, [(130.81,.17),(164.81,.14),(196.0,.12),(261.63,.07),(329.63,.045)], .10))
    encode(ROOT / "cues/familiar_face.ogg",
           one_shot(2.2, [(392.0,.10),(493.88,.08),(587.33,.06)], .55))
    encode(ROOT / "cues/home_shift.ogg",
           one_shot(3.6, [(220.0,.10),(277.18,.08),(329.63,.07),(440.0,.04)], .24))

    encode(ROOT / "presence/home_morning.ogg",
           loop_samples(28.0, [(164.81,.09),(220.0,.07),(277.18,.055),(329.63,.035)], .35, .014))
    encode(ROOT / "presence/home_day.ogg",
           loop_samples(28.0, [(130.81,.08),(164.81,.065),(196.0,.05),(261.63,.028)], .55, .012))
    encode(ROOT / "presence/home_evening.ogg",
           loop_samples(28.0, [(110.0,.10),(146.83,.08),(174.61,.06),(220.0,.035)], .78, .017))
    encode(ROOT / "presence/home_night.ogg",
           loop_samples(28.0, [(82.41,.105),(110.0,.07),(130.81,.045),(164.81,.024)], 1.0, .020))
    encode(ROOT / "presence/home_storm.ogg",
           loop_samples(28.0, [(73.42,.11),(98.0,.07),(123.47,.045),(146.83,.025)], 1.20, .026))
    encode(ROOT / "presence/harbor_air_v0.ogg",
           loop_samples(24.0, [(146.83,.07),(196.0,.055),(246.94,.045),(392.0,.016)], .85, .018))
    encode(ROOT / "presence/void_stillness_v0.ogg",
           loop_samples(24.0, [(73.42,.08),(98.0,.04),(146.83,.025)], 1.25, .018))

    for path in sorted(ROOT.rglob("*.ogg")):
        print(f"generated {path} {path.stat().st_size} bytes")


if __name__ == "__main__":
    main()
