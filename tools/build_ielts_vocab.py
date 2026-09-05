#!/usr/bin/env python3
"""Build the deterministic IELTS vocabulary asset from a pinned ECDICT CSV snapshot."""

from __future__ import annotations

import argparse
import csv
import gzip
import hashlib
import json
import re
from pathlib import Path


WORD_PATTERN = re.compile(r"[A-Za-z][A-Za-z'-]{1,39}")


def integer(value: str) -> int:
    try:
        return int(value or 0)
    except ValueError:
        return 0


def normalized_word(value: str) -> str:
    return value.strip().lower().replace("’", "'")


def build(input_path: Path, output_path: Path, manifest_path: Path, revision: str) -> None:
    digest = hashlib.sha256()
    with input_path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)

    words: dict[str, dict[str, object]] = {}
    with input_path.open("r", encoding="utf-8", newline="") as source:
        for row in csv.DictReader(source):
            tags = set((row.get("tag") or "").split())
            word = (row.get("word") or "").strip()
            key = normalized_word(word)
            if "ielts" not in tags or not WORD_PATTERN.fullmatch(word):
                continue
            phonetic = (row.get("phonetic") or "").strip()
            translation = (row.get("translation") or "").strip()
            definition = (row.get("definition") or "").strip()
            if not phonetic or not translation:
                continue
            candidate = {
                "k": key,
                "w": word,
                "p": phonetic,
                "d": definition,
                "t": translation,
                "o": (row.get("pos") or "").strip(),
                "e": (row.get("exchange") or "").strip(),
                "c": integer(row.get("collins") or "0"),
                "b": integer(row.get("bnc") or "0"),
                "f": integer(row.get("frq") or "0"),
            }
            current = words.get(key)
            if current is None or (candidate["c"], -candidate["f"]) > (current["c"], -current["f"]):
                words[key] = candidate

    ordered = sorted(
        words.values(),
        key=lambda item: (
            -int(item["c"]),
            int(item["f"]) if int(item["f"]) > 0 else 1_000_000,
            int(item["b"]) if int(item["b"]) > 0 else 1_000_000,
            str(item["k"]),
        ),
    )
    output_path.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps(ordered, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    with output_path.open("wb") as raw:
        with gzip.GzipFile(filename="", mode="wb", fileobj=raw, mtime=0, compresslevel=9) as target:
            target.write(payload)

    manifest = {
        "source": "https://github.com/skywind3000/ECDICT",
        "revision": revision,
        "sourceSha256": digest.hexdigest(),
        "filter": "tag contains exact token 'ielts'; alphabetic and hyphenated headwords; phonetic and Chinese translation required",
        "wordCount": len(ordered),
        "license": "MIT",
    }
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {len(ordered)} IELTS words to {output_path}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("input", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("manifest", type=Path)
    parser.add_argument("--revision", required=True)
    args = parser.parse_args()
    build(args.input, args.output, args.manifest, args.revision)


if __name__ == "__main__":
    main()
