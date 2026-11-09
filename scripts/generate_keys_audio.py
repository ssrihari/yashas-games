#!/usr/bin/env python3
"""Generate one MP3 per spoken line in keys.html with Microsoft Edge TTS.

The lines live in the <script id="voice-lines"> JSON block in keys.html
(id -> text). Each becomes audio/keys/<id>.mp3.

Usage:
    python3 scripts/generate_keys_audio.py
    python3 scripts/generate_keys_audio.py --voice en-US-AnaNeural --force

Install the dependency with:
    pip install --upgrade edge-tts   (old versions get a 403 from Microsoft)
ffmpeg, if present, trims the silence Edge TTS leaves at the end of each clip.
"""

import argparse
import asyncio
import json
import re
import shutil
import subprocess
from pathlib import Path

import edge_tts


ROOT = Path(__file__).resolve().parent.parent
DEFAULT_VOICE = "en-US-AnaNeural"
# Edge TTS adds ~1.4s of silence after each clip, which makes chained clips
# ("Yes!" + "B!") feel sluggish. Trim both ends, keep a short natural tail.
TRIM_FILTER = (
    "silenceremove=start_periods=1:start_threshold=-50dB,"
    "areverse,silenceremove=start_periods=1:start_threshold=-50dB,areverse,"
    "apad=pad_dur=0.15"
)


def trim_silence(path):
    if not shutil.which("ffmpeg"):
        return
    tmp = path.with_suffix(".tmp.mp3")
    subprocess.run(
        ["ffmpeg", "-y", "-loglevel", "error", "-i", str(path), "-af", TRIM_FILTER, str(tmp)],
        check=True,
    )
    tmp.replace(path)


def load_lines():
    html = (ROOT / "keys.html").read_text(encoding="utf-8")
    match = re.search(
        r'<script type="application/json" id="voice-lines">\s*(.*?)\s*</script>',
        html,
        re.DOTALL,
    )
    if not match:
        raise RuntimeError("Could not find the voice-lines block in keys.html")
    return json.loads(match.group(1))


async def generate_one(line_id, text, output_dir, voice, rate, semaphore, force):
    output = output_dir / f"{line_id}.mp3"
    if output.exists() and output.stat().st_size > 1000 and not force:
        return "skipped"

    async with semaphore:
        for attempt in range(3):
            try:
                await edge_tts.Communicate(text, voice, rate=rate).save(str(output))
                await asyncio.to_thread(trim_silence, output)
                return "generated"
            except Exception:
                if attempt == 2:
                    raise
                await asyncio.sleep(2**attempt)


async def generate(args):
    lines = load_lines()
    output_dir = ROOT / args.output
    output_dir.mkdir(parents=True, exist_ok=True)
    semaphore = asyncio.Semaphore(args.concurrency)
    results = await asyncio.gather(
        *(
            generate_one(line_id, text, output_dir, args.voice, args.rate, semaphore, args.force)
            for line_id, text in lines.items()
        )
    )
    print(
        f"{len(lines)} lines: "
        f"{results.count('generated')} generated, "
        f"{results.count('skipped')} skipped"
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--voice", default=DEFAULT_VOICE)
    parser.add_argument("--rate", default="-10%")
    parser.add_argument("--output", default="audio/keys")
    parser.add_argument("--concurrency", type=int, default=8)
    parser.add_argument("--force", action="store_true", help="Regenerate existing files")
    args = parser.parse_args()
    asyncio.run(generate(args))


if __name__ == "__main__":
    main()
