#!/usr/bin/env python3
"""Generate one question-only MP3 per quiz question with Microsoft Edge TTS.

Usage:
    python3 scripts/generate_audio.py
    python3 scripts/generate_audio.py --voice en-US-AnaNeural --concurrency 8

Install the dependency with:
    pip install edge-tts
"""

import argparse
import asyncio
import json
import re
from pathlib import Path

import edge_tts


ROOT = Path(__file__).resolve().parent.parent
DEFAULT_VOICE = "en-US-AnaNeural"


def load_questions():
    quiz_html = (ROOT / "quiz.html").read_text(encoding="utf-8")
    match = re.search(
        r'<script type="application/json" id="question-bank">\s*(.*?)\s*</script>',
        quiz_html,
        re.DOTALL,
    )
    if not match:
        raise RuntimeError("Could not find the question bank in quiz.html")
    return json.loads(match.group(1))


async def generate_one(question, output_dir, voice, rate, semaphore, force):
    output = output_dir / f"{question['id']}.mp3"
    if output.exists() and output.stat().st_size > 1000 and not force:
        return "skipped"

    async with semaphore:
        for attempt in range(3):
            try:
                # Underscores are visible blanks in the quiz, but Edge TTS
                # reads them as "underscore". Say "dash" instead.
                speech_text = re.sub(r"_+", " dash ", question["question"])
                communicate = edge_tts.Communicate(
                    speech_text, voice, rate=rate
                )
                await communicate.save(str(output))
                return "generated"
            except Exception:
                if attempt == 2:
                    raise
                await asyncio.sleep(2**attempt)


async def generate(args):
    questions = load_questions()
    output_dir = ROOT / args.output
    output_dir.mkdir(parents=True, exist_ok=True)
    semaphore = asyncio.Semaphore(args.concurrency)
    results = await asyncio.gather(
        *(
            generate_one(
                question,
                output_dir,
                args.voice,
                args.rate,
                semaphore,
                args.force,
            )
            for question in questions
        )
    )
    print(
        f"{len(questions)} questions: "
        f"{results.count('generated')} generated, "
        f"{results.count('skipped')} skipped"
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--voice", default=DEFAULT_VOICE)
    parser.add_argument("--rate", default="-5%")
    parser.add_argument("--output", default="audio")
    parser.add_argument("--concurrency", type=int, default=8)
    parser.add_argument("--force", action="store_true", help="Regenerate existing files")
    args = parser.parse_args()
    asyncio.run(generate(args))


if __name__ == "__main__":
    main()
