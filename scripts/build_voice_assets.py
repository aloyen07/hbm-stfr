"""Build STFR voice assets from voicelines/voice_config.json and voicelines.json.

Run with voicelines/.venv/bin/python. Existing recordings are reused unless
--force is passed; --catalog-only refreshes resource manifests without TTS.
"""

import argparse
import contextlib
import hashlib
import json
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
VOICES = ROOT / "voicelines"
ASSETS = ROOT / "src/main/resources/assets/hbmstfr"
PREFIX = "message.hbmstfr."
GROUPS = ("reactor.", "scene.", "failure.", "autopilot.")
CODE = re.compile(r"[a-z][a-z0-9_]*\Z")
LINE = re.compile(r"[a-z0-9][a-z0-9._-]*\Z")
PITCH = re.compile(r"([+-]\d+)Hz\Z")
STATE_PATH = VOICES / "build_state.json"
LOCK_PATH = VOICES / ".build_state.lock"


def fingerprint(name, language, text, voice, effects, segments=None):
    payload = {
        "name": name,
        "language": language,
        "text": text,
        "voice": voice["voice"],
        "rate": voice["rate"],
        "pitch": voice["pitch"],
        "edge_pitch": voice["edge_pitch"],
        "effects": effects,
        "format": 2,
    }
    if segments is not None:
        payload["speech_segments"] = segments
    return hashlib.sha256(json.dumps(payload, sort_keys=True, ensure_ascii=False)
                          .encode("utf-8")).hexdigest()


def raw_path(language, text, voice):
    cache_key = hashlib.sha256(
        "\0".join((text, voice["voice"], voice["rate"], voice["edge_pitch"]))
        .encode("utf-8")).hexdigest()[:16]
    return VOICES / "raw" / language / f"voice_{cache_key}.wav"


def save_state(state, replace=False, updates=None):
    with state_lock():
        if not replace and STATE_PATH.exists():
            stored = json.loads(STATE_PATH.read_text(encoding="utf-8"))
            stored.update(state if updates is None else updates)
            state = stored
        temporary = STATE_PATH.with_suffix(".json.tmp")
        temporary.write_text(json.dumps(state, ensure_ascii=False, indent=2) + "\n",
                             encoding="utf-8")
        temporary.replace(STATE_PATH)


@contextlib.contextmanager
def state_lock():
    LOCK_PATH.touch(exist_ok=True)
    with LOCK_PATH.open("r+") as lock:
        try:
            import fcntl
            fcntl.flock(lock, fcntl.LOCK_EX)
        except ImportError:
            pass
        yield


def read_inputs():
    config = json.loads((VOICES / "voice_config.json").read_text(encoding="utf-8"))
    lines = json.loads((VOICES / "voicelines.json").read_text(encoding="utf-8"))["lines"]
    languages = config["languages"]
    if config["default_language"] not in languages:
        raise ValueError("default_language must name a configured language")
    for code, voice in languages.items():
        if not CODE.fullmatch(code):
            raise ValueError(f"Invalid language code: {code}")
        if not voice.get("minecraft_locales") or not voice.get("voice"):
            raise ValueError(f"Missing voice or Minecraft locales for {code}")
        if not PITCH.fullmatch(voice["pitch"]) or not PITCH.fullmatch(voice["edge_pitch"]):
            raise ValueError(f"Invalid pitch for {code}; expected e.g. -20Hz")
    for name, translations in lines.items():
        if not LINE.fullmatch(name):
            raise ValueError(f"Invalid line name: {name}")
        if not isinstance(translations, dict):
            raise ValueError(f"Invalid translations for {name}")
    return config, lines


def recording(name, language, text, voice, effects, segments=None):
    from voicelines import apply_effects, synthesize_edge, join_speech_segments

    destination = ASSETS / "sounds/voice" / language / f"{name}.ogg"
    rate = voice["rate"]
    edge_pitch = voice["edge_pitch"]
    source = json.dumps(segments, ensure_ascii=False) if segments is not None else text
    raw = raw_path(language, source, voice)
    raw.parent.mkdir(parents=True, exist_ok=True)
    destination.parent.mkdir(parents=True, exist_ok=True)
    if not raw.exists() or raw.stat().st_size == 0:
        if segments is None:
            synthesize_edge(text, voice["voice"], rate, edge_pitch, raw)
        else:
            parts = []
            for segment in segments:
                if isinstance(segment, int):
                    parts.append(segment)
                    continue
                part = raw_path(language, segment, voice)
                if not part.is_file() or part.stat().st_size == 0:
                    synthesize_edge(segment, voice["voice"], rate, edge_pitch, part)
                parts.append(part)
            join_speech_segments(parts, raw)
    desired = int(PITCH.fullmatch(voice["pitch"]).group(1))
    base = int(PITCH.fullmatch(edge_pitch).group(1))
    pitch_ratio = 1 + (desired - base) / 140
    if not 0.5 <= pitch_ratio <= 2:
        raise ValueError(f"Pitch out of range for {language}: {voice['pitch']}")
    apply_effects(raw, destination, effects, pitch_ratio)
    return destination


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--force", action="store_true", help="Replace existing OGG assets")
    parser.add_argument("--catalog-only", action="store_true", help="Update JSON from existing OGG assets")
    parser.add_argument("--line", action="append",
                        help="Generate one announcement; repeat to select several")
    parser.add_argument("--lang", help="Generate only one configured language")
    parser.add_argument("--recordings-only", action="store_true",
                        help="Generate OGG files without writing shared catalogs")
    parser.add_argument("--prune", action="store_true",
                        help="Remove OGG assets not described by the current config/texts")
    parser.add_argument("--adopt-cache", action="store_true",
                        help="Record existing OGGs backed by matching raw cache as up-to-date")
    args = parser.parse_args()
    if args.force and args.catalog_only:
        parser.error("--force and --catalog-only cannot be combined")
    if args.catalog_only and args.recordings_only:
        parser.error("--catalog-only and --recordings-only cannot be combined")
    config, lines = read_inputs()
    state = json.loads(STATE_PATH.read_text(encoding="utf-8")) if STATE_PATH.exists() else {}
    if args.lang and args.lang not in config["languages"]:
        parser.error(f"Unknown language {args.lang!r}; available: "
                     f"{', '.join(config['languages'])}")
    sys.path.insert(0, str(VOICES))
    names = sorted(name for name in lines if name.startswith(GROUPS))
    if args.line:
        unknown = set(args.line) - set(names)
        if unknown:
            parser.error(f"Unknown announcements: {', '.join(sorted(unknown))}")

    generated = 0
    catalog = {}
    sounds_path = ASSETS / "sounds.json"
    sounds = json.loads(sounds_path.read_text(encoding="utf-8"))
    sounds = {key: value for key, value in sounds.items() if not key.startswith("voice.")}
    for name in names:
        available = {}
        for language, voice in config["languages"].items():
            overrides = lines[name].get("voice_overrides", {}).get(language, {})
            if set(overrides) - {"rate", "pitch", "edge_pitch"}:
                raise ValueError(f"Unsupported voice overrides: {language}/{name}")
            voice = dict(voice, **overrides)
            text = lines[name].get(language)
            if not isinstance(text, str) or not text.strip():
                continue
            destination = ASSETS / "sounds/voice" / language / f"{name}.ogg"
            state_key = f"{language}/{name}"
            segments = lines[name].get("speech_segments", {}).get(language)
            if segments is not None:
                if (not isinstance(segments, list) or not segments
                        or not any(isinstance(part, str) and part.strip() for part in segments)
                        or any(not (isinstance(part, str) and part.strip()
                                    or type(part) is int and 0 <= part <= 10_000)
                               for part in segments)):
                    raise ValueError(f"Invalid speech segments: {language}/{name}")
            current_fingerprint = fingerprint(name, language, text, voice, config["effects"], segments)
            source = json.dumps(segments, ensure_ascii=False) if segments is not None else text
            cached_raw = raw_path(language, source, voice)
            if (args.adopt_cache and destination.is_file() and cached_raw.is_file()
                    and destination.stat().st_mtime >= cached_raw.stat().st_mtime):
                state[state_key] = current_fingerprint
                save_state(state, updates={state_key: current_fingerprint})
            selected_language = args.lang is None or args.lang == language
            if (not args.catalog_only and selected_language
                    and (args.line is None or name in args.line)):
                if (args.force or not destination.exists()
                        or state.get(state_key) != current_fingerprint):
                    recording(name, language, text, voice, config["effects"], segments)
                    state[state_key] = current_fingerprint
                    save_state(state, updates={state_key: current_fingerprint})
                    generated += 1
                    print(f"{language}: {name}", flush=True)
            if not destination.is_file():
                continue
            available[language] = text
            sounds[f"voice.{language}.{name}"] = {
                "sounds": [{"name": f"hbmstfr:voice/{language}/{name}", "stream": True}]
            }
        if available:
            catalog[PREFIX + name] = available

    if args.recordings_only:
        print(f"Generated {generated} recordings")
        return
    sounds_path.write_text(json.dumps(sounds, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (ASSETS / "voice_lines.json").write_text(
        json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (ASSETS / "voice_languages.json").write_text(
        json.dumps({"default_language": config["default_language"],
                    "languages": {code: voice["minecraft_locales"]
                                  for code, voice in config["languages"].items()}},
                   ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    removed = 0
    if args.prune:
        expected = {
            (ASSETS / "sounds/voice" / language / f"{name}.ogg").resolve()
            for name in names for language in config["languages"]
            if isinstance(lines[name].get(language), str) and lines[name][language].strip()
        }
        for path in (ASSETS / "sounds/voice").rglob("*.ogg"):
            if path.resolve() not in expected:
                path.unlink()
                removed += 1
        valid_state = {
            f"{language}/{name}" for name in names for language in config["languages"]
            if isinstance(lines[name].get(language), str) and lines[name][language].strip()
        }
        state = {key: value for key, value in state.items() if key in valid_state}
        save_state(state, replace=True)
    print(f"Generated {generated} recordings; removed {removed}; "
          f"catalog has {len(catalog)} announcements")


if __name__ == "__main__":
    main()
