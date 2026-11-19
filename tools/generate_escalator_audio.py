"""Generate an original, periodic motor/tread loop; no sampled recordings.

Requires NumPy and ffmpeg. The periodic noise and integer cycle frequencies
avoid a built-in start/end fade that would pulse every time Minecraft loops it.
"""
from pathlib import Path
import json
import subprocess
import wave

import numpy as np

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/createtransit"
RATE = 44100
SECONDS = 8


def generate():
    count = RATE * SECONDS
    time = np.arange(count) / RATE
    frequencies = np.fft.rfftfreq(count, 1 / RATE)
    rng = np.random.default_rng(1504)

    def noise(low, high):
        spectrum = rng.normal(size=len(frequencies)) + 1j * rng.normal(size=len(frequencies))
        window = (1 - np.exp(-(frequencies / low) ** 4)) * np.exp(-(frequencies / high) ** 4)
        spectrum *= window
        spectrum[0] = spectrum[-1] = 0
        values = np.fft.irfft(spectrum, n=count)
        return values / np.std(values)

    # Gentle low motor body, broadband rolling tread, and subdued chain rhythm.
    motor = (.31 * np.sin(2 * np.pi * 70 * time + .12 * np.sin(2 * np.pi * .5 * time))
             + .08 * np.sin(2 * np.pi * 140 * time)
             + .025 * np.sin(2 * np.pi * 280 * time))
    rumble = .11 * noise(45, 450) * (1 + .06 * np.sin(2 * np.pi * .25 * time))
    tread = .045 * noise(350, 2400) * (.7 + .3 * np.sin(2 * np.pi * 2.75 * time))
    chain = .016 * noise(750, 3400) * (.7 + .3 * np.sin(2 * np.pi * 14 * time))
    signal = motor + rumble + tread + chain
    signal -= signal.mean()
    signal *= .58 / np.max(np.abs(signal))
    pcm = np.round(signal * 32767).astype("<i2")

    output = ASSETS / "sounds"
    output.mkdir(exist_ok=True)
    temporary = output / "escalator_running.wav"
    with wave.open(str(temporary), "wb") as sound:
        sound.setparams((1, 2, RATE, 0, "NONE", "not compressed"))
        sound.writeframes(pcm.tobytes())
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", str(temporary),
                    "-c:a", "libvorbis", "-q:a", "4", str(output / "escalator_running.ogg")], check=True)
    temporary.unlink()

    definitions = ASSETS / "sounds.json"
    sounds = json.loads(definitions.read_text())
    sounds["escalator_running"] = {
        "subtitle": "subtitles.createtransit.escalator_running",
        "sounds": [{"name": "createtransit:escalator_running", "attenuation_distance": 12}]
    }
    definitions.write_text(json.dumps(sounds, indent=2) + "\n")
    translations = ASSETS / "lang/en_us.json"
    language = json.loads(translations.read_text())
    language["subtitles.createtransit.escalator_running"] = "Escalator motor and moving steps"
    translations.write_text(json.dumps(language, indent=2) + "\n")
    print("Generated 8-second mono Vorbis escalator loop with a 12-block hearing range.")


if __name__ == "__main__":
    generate()
