[English](README.md) | [Русский](README.ru.md)

# STFR Voice Assets

Voice settings are configured in [voice_config.json](voice_config.json). Two
languages are currently enabled: `ru` and `en`. Each language specifies an Edge
TTS voice, speech rate, target pitch, and a list of Minecraft locales. The shared
effects use the `broadcast` preset.

## Generate all in-game announcements

Run from the project root:

```bash
bash scripts/generate_voices.sh
```

The generator checks the complete dataset for both languages: text, pronunciation
in `speech_segments`, voice settings, and effects. Only missing or changed
recordings are generated; up-to-date OGG files are reused. There is no need to
maintain lists of announcements manually. The script forwards arguments to the
shared `build_voice_assets.py`, such as `--lang ru` or `--force`.

## Quick preview

Run from the `voicelines` directory:

```text
python preview_voice.py --name broadcast
python preview_voice.py --lang ru --name ru_test
python preview_voice.py --lang en --rate=-10% --pitch=-16Hz --name en_test
python preview_voice.py --show-settings
```

The result is saved to `output/<language>/test.reactor_ready.<name>.ogg` without
changing the mod's resources. Raw speech is cached in `raw/`: changing the pitch
or effects does not require another Edge TTS request. Use `--refresh` to force a
cache refresh.

The state of in-game OGG files is stored in `build_state.json`. Each entry records
a hash of the text, language, voice, rate, pitch, and effects. After an error, a
normal rerun generates only missing or outdated recordings and saves the manifest
after every successful file. Use `--force` only for an intentional full rebuild
regardless of the manifest.

## Language settings

Each key in `languages` is a short language code used for the sound directory:

```json
"ru": {
  "minecraft_locales": ["ru_ru"],
  "voice": "ru-RU-DmitryNeural",
  "rate": "-5%",
  "pitch": "-20Hz",
  "edge_pitch": "-8Hz"
}
```

- `minecraft_locales` selects this language entry for matching client locales.
- `voice` is the Edge TTS voice name.
- `rate` controls speech speed: a negative value makes speech slower.
- `pitch` is the final voice pitch. Additional pitch reduction is applied locally
  with FFmpeg while preserving the tempo.
- `edge_pitch` is the safe pitch value sent to Edge TTS. Dmitry uses `-8Hz`
  because the service sometimes returns no audio at `-12Hz` or lower.

If the client's locale is not listed, `default_language` is used. If an
announcement has no recording in the selected language, the game also falls back
to an available recording in the default language.

## Effects

- `input_gain_db`: input voice level before splitting into dry and processed
  layers.
- `gain_db`: final level after mixing the layers.
- `reverb_room_size`, `reverb_width`, `reverb_damping`: reflection characteristics.
- `delay_interval`, `delay_feedback`: interval and decay of the short echo.
- `wet_mix`: level of the processed layer; `0` produces an almost dry voice,
  while `1` makes the effect most noticeable.
- `pre_delay_ms`, `after_delay_ms`: silence before speech and space for the
  effects tail.

Any setting can be temporarily overridden for a preview, for example:

```text
python preview_voice.py --lang ru --input-gain-db=5 --wet-mix=0.5 --after-delay-ms=1800 --name softer
```

## Add a language

For example, to add Chinese:

1. Add a `zh` object to `languages` in `voice_config.json` and list the desired
   Minecraft locales, such as `zh_cn`.
2. Add a `zh` field to the relevant announcements in `voicelines.json`.
3. Preview an announcement: `python preview_voice.py --lang zh --name zh_test`.
4. Build the in-game resources from the project root.

```text
voicelines/.venv/bin/python scripts/build_voice_assets.py --force
```

The generator creates the `sounds/voice/zh` directory and the entries in
`sounds.json`, `voice_lines.json`, and `voice_languages.json`. Announcements
without a `zh` translation are skipped; the game uses the default language for
those announcements.

## Build commands

```text
# Generate missing or outdated recordings
voicelines/.venv/bin/python scripts/build_voice_assets.py

# Rebuild all recordings after changing the voice, rate, or effects
voicelines/.venv/bin/python scripts/build_voice_assets.py --force

# Rebuild one announcement
voicelines/.venv/bin/python scripts/build_voice_assets.py --force --line reactor.start.abort_manual

# Rebuild one language without also writing the shared catalogs
voicelines/.venv/bin/python scripts/build_voice_assets.py --force --lang ru --recordings-only

# Update JSON catalogs from existing OGG files without contacting Edge TTS
voicelines/.venv/bin/python scripts/build_voice_assets.py --catalog-only

# Remove OGG files for languages and announcements no longer present in the configuration
voicelines/.venv/bin/python scripts/build_voice_assets.py --catalog-only --prune
```

Requires Python 3, FFmpeg with the `rubberband` filter, internet access, and the
packages listed in `requirements.txt`. A new in-game voice announcement interrupts
the previous one; the standard notification sound plays separately.

## Pauses within an announcement

For precise pauses, give an announcement a `speech_segments` field with a separate
list for each language. Strings are synthesized as speech; integers specify
pauses in milliseconds. Startup completion example:

```json
"speech_segments": {
  "en": ["Stabilizers Thermonuclear Fusion Reactor is now", 900, "online."]
}
```

Segments are cached separately, leading and trailing silence is trimmed before
concatenation, and the shared effects are applied to the assembled announcement.
Segment settings are included in the build hash. The regular `ru`/`en` fields
retain the full text for the catalog. Repeat `--line` to rebuild several selected
announcements.

Edge TTS is currently pinned to 7.3.1 in `requirements.txt`; version 7.2.8 does not
support the service's updated protocol and returns HTTP 403.

Individual announcements can specify per-language `voice_overrides` with `rate`,
`pitch`, and `edge_pitch` fields. These are used for Russian startup announcements
where Dmitry sometimes returns no audio with negative Edge settings. Neutral
values of `rate: +0%` and `edge_pitch: +0Hz` keep the selected voice unchanged;
local FFmpeg processing still lowers it to the configured `pitch: -20Hz`.

## Russian stress in «ядра́»

Russian announcements containing the form «ядра» use a separate pronunciation in
`speech_segments.ru`: `ядраа`. This variant was selected after listening to test
recordings; an accent mark and emphasis on the vowel did not produce the desired
result. The regular `ru` fields retain the standard spelling for the on-screen
catalog. Existing pauses in `speech_segments` are preserved when pronunciation is
adjusted. Changing the segments updates the fingerprint, so the next generator
run rebuilds the affected recordings without requiring `--force`.
