"""Checks explicit speech pauses without contacting the TTS service."""
import array
import importlib.util
from pathlib import Path
import tempfile
import unittest
import wave

spec = importlib.util.spec_from_file_location(
    "stfr_voice_audio", Path(__file__).resolve().parents[1] / "voicelines/voicelines.py")
voice = importlib.util.module_from_spec(spec)
spec.loader.exec_module(voice)


class SpeechSegmentsTest(unittest.TestCase):
    def test_pause_is_explicit_and_only_edge_silence_is_trimmed(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for name in ("first", "last"):
                with wave.open(str(root / (name + ".wav")), "wb") as clip:
                    clip.setnchannels(1)
                    clip.setsampwidth(2)
                    clip.setframerate(1000)
                    # Keep the internal 50ms gap, trim the 200ms edges to 20ms.
                    clip.writeframes(array.array("h", [0] * 200 + [1000] * 50
                                     + [0] * 50 + [1000] * 50 + [0] * 200).tobytes())
            output = root / "complete.wav"
            voice.join_speech_segments([root / "first.wav", 900, root / "last.wav"], output)
            with wave.open(str(output), "rb") as result:
                samples = array.array("h", result.readframes(result.getnframes()))
                self.assertEqual(1280, len(samples))
                self.assertTrue(all(value == 0 for value in samples[190:1090]))
                self.assertEqual([1000] * 50, list(samples[20:70]))
                self.assertEqual([0] * 50, list(samples[70:120]))
                self.assertEqual([1000] * 50, list(samples[1110:1160]))

    def test_no_speech_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(ValueError):
                voice.join_speech_segments([900], Path(directory) / "invalid.wav")


if __name__ == "__main__":
    unittest.main()
