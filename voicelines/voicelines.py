"""Speech synthesis and FFmpeg broadcast processing shared by previews and assets."""

import subprocess
import array
import sys
import tempfile
import time
import wave
from pathlib import Path


def synthesize_edge(text, voice, rate, pitch, raw_path):
    import edge_tts

    raw_path = Path(raw_path)
    raw_path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=raw_path.parent) as temporary:
        source_mp3 = Path(temporary) / "speech.mp3"
        source_wav = Path(temporary) / "speech.wav"
        attempts = 8
        for attempt in range(attempts):
            try:
                edge_tts.Communicate(text, voice=voice, rate=rate, pitch=pitch).save_sync(
                    str(source_mp3))
                break
            except edge_tts.exceptions.NoAudioReceived as error:
                if attempt == attempts - 1:
                    raise RuntimeError(
                        f"Edge TTS returned no audio for {voice} at {pitch} "
                        f"after {attempts} attempts."
                    ) from error
            except Exception as error:
                if attempt == attempts - 1:
                    raise RuntimeError(f"Edge TTS could not generate {voice}: {error}") from error
            time.sleep(min(2 ** attempt, 20))
        result = subprocess.run(
            ["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-i", str(source_mp3),
             "-c:a", "pcm_s16le", str(source_wav)], capture_output=True, text=True)
        if result.returncode:
            raise RuntimeError(f"FFmpeg could not decode Edge TTS audio: {result.stderr.strip()}")
        source_wav.replace(raw_path)


def join_speech_segments(parts, output_path):
    """Join PCM speech with explicit millisecond pauses, trimming only edge silence."""
    clips = {}
    audio_format = None
    for part in parts:
        if isinstance(part, int):
            continue
        with wave.open(str(part), "rb") as source:
            current = (source.getnchannels(), source.getsampwidth(), source.getframerate())
            if current[0:2] != (1, 2):
                raise ValueError("Speech segments must be mono PCM16")
            if audio_format is not None and current != audio_format:
                raise ValueError("Speech segments must have the same sample rate")
            audio_format = current
            samples = array.array("h", source.readframes(source.getnframes()))
            if sys.byteorder != "little":
                samples.byteswap()
            active = [i for i, value in enumerate(samples) if abs(value) > 128]
            if not active:
                raise ValueError(f"Silent speech segment: {part}")
            padding = current[2] // 50
            samples = samples[max(0, active[0] - padding):active[-1] + padding + 1]
            if sys.byteorder != "little":
                samples.byteswap()
            clips[part] = samples.tobytes()
    if audio_format is None:
        raise ValueError("At least one speech segment is required")
    output_path = Path(output_path)
    with tempfile.TemporaryDirectory(dir=output_path.parent) as temporary:
        combined = Path(temporary) / "combined.wav"
        with wave.open(str(combined), "wb") as output:
            output.setnchannels(audio_format[0])
            output.setsampwidth(audio_format[1])
            output.setframerate(audio_format[2])
            for part in parts:
                if isinstance(part, int):
                    output.writeframes(b"\0\0" * (audio_format[2] * part // 1000))
                else:
                    output.writeframes(clips[part])
        combined.replace(output_path)


def apply_effects(raw_path, output_path, effects, pitch_ratio=1.0):
    room_size = effects["reverb_room_size"]
    damping = effects["reverb_damping"]
    width = effects["reverb_width"]
    feedback = effects["delay_feedback"]
    wet_mix = effects.get("wet_mix", 0.65)
    delays = [round(base * (0.7 + room_size)) for base in (47, 71, 113, 181, 271, 419)]
    decays = [amount * (1 - damping * 0.5) * width
              for amount in (0.67, 0.58, 0.49, 0.39, 0.29, 0.19)]
    echo_step_ms = max(1, round(effects["delay_interval"] * 1000))
    delay_taps = [(echo_step_ms * repeat, 0.75 * feedback**repeat)
                  for repeat in range(1, 9) if 0.75 * feedback**repeat >= 0.0001]
    input_filters = []
    if pitch_ratio != 1.0:
        input_filters.append(f"rubberband=pitch={pitch_ratio:.5f}:formant=preserved")
    input_filters.extend((f"adelay={effects['pre_delay_ms']}:all=1",
                          f"apad=pad_dur={effects['after_delay_ms'] / 1000}",
                          f"volume={effects.get('input_gain_db', 0)}dB", "asplit=2"))
    wet_filters = ["highpass=f=180", "lowpass=f=4800"]
    for taps in (zip(delays, decays), delay_taps):
        taps = [(delay, decay) for delay, decay in taps if decay >= 0.0001]
        if taps:
            delay_list = "|".join(str(delay) for delay, _ in taps)
            decay_list = "|".join(f"{decay:.4f}" for _, decay in taps)
            wet_filters.append(f"aecho=0.8:0.8:{delay_list}:{decay_list}")
    wet_filters.append(f"volume={wet_mix}")
    filters = (
        f"[0:a]{','.join(input_filters)}[dry][wet];"
        "[dry]anull[d];"
        f"[wet]{','.join(wet_filters)}[w];"
        f"[d][w]amix=inputs=2:duration=longest:normalize=0,"
        f"volume={effects.get('gain_db', 0)}dB,"
        "acompressor=threshold=-16dB:ratio=2:attack=10:release=160,"
        "alimiter=limit=0.95:level=0[out]"
    )
    output_path = Path(output_path)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=output_path.parent) as temporary:
        temporary_output = Path(temporary) / output_path.name
        result = subprocess.run(
            ["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-i", str(raw_path),
             "-filter_complex", filters, "-map", "[out]", "-c:a", "libvorbis",
             str(temporary_output)], capture_output=True, text=True)
        if result.returncode:
            raise RuntimeError(f"FFmpeg failed for {raw_path}: {result.stderr.strip()}")
        temporary_output.replace(output_path)
