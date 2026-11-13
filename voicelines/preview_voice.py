"""Render named preview clips using the shared multilingual voice configuration."""

import argparse
import hashlib
import json
import re
from pathlib import Path

from voicelines import apply_effects, synthesize_edge


BASE = Path(__file__).resolve().parent
NAME = re.compile(r"[a-z0-9][a-z0-9._-]*\Z")
PITCH = re.compile(r"([+-]\d+)Hz\Z")
PITCH_REFERENCE_HZ = 140


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--name", default="custom", help="Unique preview filename suffix")
    parser.add_argument("--lang", default="all", help="Language code from voice_config.json, or all")
    parser.add_argument("--voice", help="Override the voice for one selected language")
    parser.add_argument("--rate", help="Speech rate, such as -5%")
    parser.add_argument("--pitch", help="Desired pitch, such as -20Hz")
    parser.add_argument("--input-gain-db", type=float, help="Level before effects, in dB")
    parser.add_argument("--gain-db", type=float, help="Final level after mixing, in dB")
    parser.add_argument("--reverb-room-size", type=float)
    parser.add_argument("--reverb-width", type=float)
    parser.add_argument("--reverb-damping", type=float)
    parser.add_argument("--delay-interval", type=float)
    parser.add_argument("--delay-feedback", type=float)
    parser.add_argument("--wet-mix", type=float)
    parser.add_argument("--pre-delay-ms", type=int)
    parser.add_argument("--after-delay-ms", type=int)
    parser.add_argument("--show-settings", action="store_true")
    parser.add_argument("--refresh", action="store_true", help="Regenerate cached speech")
    args = parser.parse_args()
    if not NAME.fullmatch(args.name):
        parser.error("--name must contain only lowercase letters, digits, dots, dashes or underscores")

    config = json.loads((BASE / "voice_config.json").read_text(encoding="utf-8"))
    translations = json.loads((BASE / "voicelines.json").read_text(encoding="utf-8"))["lines"]
    languages = config["languages"]
    if args.lang != "all" and args.lang not in languages:
        parser.error(f"Unknown language {args.lang!r}; available: {', '.join(languages)}")
    selected = list(languages) if args.lang == "all" else [args.lang]
    if args.voice and len(selected) != 1:
        parser.error("--voice requires one language selected with --lang")
    effects = dict(config["effects"])
    for key in ("input_gain_db", "gain_db", "reverb_room_size", "reverb_width",
                "reverb_damping", "delay_interval", "delay_feedback", "wet_mix",
                "pre_delay_ms", "after_delay_ms"):
        value = getattr(args, key)
        if value is not None:
            effects[key] = value
    if args.show_settings:
        print(json.dumps({"languages": {code: languages[code] for code in selected},
                          "effects": effects}, ensure_ascii=False, indent=2))
        return

    for language in selected:
        voice = languages[language]
        text = translations["test.reactor_ready"].get(language)
        if not text:
            parser.error(f"Missing test.reactor_ready text for {language} in voicelines.json")
        edge_pitch = voice.get("edge_pitch", "+0Hz")
        desired_pitch = args.pitch or voice["pitch"]
        if not PITCH.fullmatch(edge_pitch) or not PITCH.fullmatch(desired_pitch):
            parser.error(f"Invalid pitch for {language}; use a signed value such as -20Hz")
        ratio = 1 + (int(PITCH.fullmatch(desired_pitch).group(1))
                     - int(PITCH.fullmatch(edge_pitch).group(1))) / PITCH_REFERENCE_HZ
        if not 0.5 <= ratio <= 2.0:
            parser.error(f"Pitch for {language} is outside the useful preview range")
        voice_name = args.voice or voice["voice"]
        rate = args.rate or voice["rate"]
        cache_key = hashlib.sha256(
            "\0".join((text, voice_name, rate, edge_pitch)).encode("utf-8")
        ).hexdigest()[:16]
        raw = BASE / "raw" / language / f"preview_{cache_key}.wav"
        output = BASE / "output" / language / f"test.reactor_ready.{args.name}.ogg"
        raw.parent.mkdir(parents=True, exist_ok=True)
        output.parent.mkdir(parents=True, exist_ok=True)
        if args.refresh or not raw.is_file() or raw.stat().st_size == 0:
            synthesize_edge(text, voice_name, rate, edge_pitch, raw)
        apply_effects(raw, output, effects, ratio)
        print(output)


if __name__ == "__main__":
    main()
