#!/usr/bin/env python3
"""Fail when the tree differs from the upstream base tag outside the allowed paths.

Usage: seams-guard.py --base <commit-ish> [--head HEAD] [--seams STUDIO-SEAMS.md]

Rules (see STUDIO-SEAMS.md):
  * a path that exists in the base tag may only differ when it is listed,
    exactly, in the Seams table;
  * a new path must match a DS-owned pattern or be listed in the Seams table;
  * DS-owned patterns must not match any path of the base tag;
  * Seams rows are literal paths (no wildcards);
  * protected paths never differ, even when listed.
Writes GitHub annotations and, when GITHUB_STEP_SUMMARY is set, a summary.
"""

import argparse
import os
import re
import subprocess
import sys

PROTECTED = {"LICENSE"}
OWNED_BLOCK = ("<!-- ds-owned:begin -->", "<!-- ds-owned:end -->")
SEAMS_BLOCK = ("<!-- seams:begin -->", "<!-- seams:end -->")
WILDCARD = re.compile(r"[*?\[]")


def git(*args):
    out = subprocess.run(["git", *args], check=True, capture_output=True)
    return out.stdout


def block(text, markers, name):
    begin, end = markers
    if text.count(begin) != 1 or text.count(end) != 1:
        raise SystemExit(f"STUDIO-SEAMS.md: expected exactly one {name} block ({begin} ... {end})")
    start = text.index(begin) + len(begin)
    stop = text.index(end)
    if stop < start:
        raise SystemExit(f"STUDIO-SEAMS.md: {name} block markers are out of order")
    return text[start:stop]


def parse_owned(body):
    patterns = []
    for line in body.splitlines():
        line = line.strip()
        if not line:
            continue
        m = re.fullmatch(r"[-*]\s+`([^`]+)`.*", line)
        if not m:
            raise SystemExit(f"STUDIO-SEAMS.md: bad DS-owned line: {line!r}")
        patterns.append(m.group(1).strip())
    return patterns


def parse_seams(body):
    rows = []
    for line in body.splitlines():
        line = line.strip()
        if not line:
            continue
        if not line.startswith("|"):
            raise SystemExit(f"STUDIO-SEAMS.md: bad Seams line (not a table row): {line!r}")
        cells = [c.strip() for c in line.strip("|").split("|")]
        if cells and (cells[0] == "Path" or set(cells[0]) <= set("-: ")):
            continue  # header or separator
        m = re.fullmatch(r"`([^`]+)`", cells[0])
        if not m:
            raise SystemExit(f"STUDIO-SEAMS.md: Seams path must be in backticks: {line!r}")
        if len(cells) < 4 or not all(cells[1:4]):
            raise SystemExit(f"STUDIO-SEAMS.md: Seams row needs Path, Why, Upstream, Since: {line!r}")
        rows.append(m.group(1).strip())
    return rows


def glob_to_regex(pattern):
    out, i = [], 0
    while i < len(pattern):
        if pattern.startswith("**/", i):
            out.append("(?:.*/)?")
            i += 3
        elif pattern.startswith("**", i):
            out.append(".*")
            i += 2
        elif pattern[i] == "*":
            out.append("[^/]*")
            i += 1
        elif pattern[i] == "?":
            out.append("[^/]")
            i += 1
        else:
            out.append(re.escape(pattern[i]))
            i += 1
    return re.compile("".join(out) + r"\Z")


def annotate(level, msg, path=None):
    where = f" file={path}" if path else ""
    print(f"::{level}{where}::{msg}")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", required=True)
    ap.add_argument("--head", default="HEAD")
    ap.add_argument("--seams", default="STUDIO-SEAMS.md")
    a = ap.parse_args()

    text = open(a.seams, encoding="utf-8").read()
    owned = parse_owned(block(text, OWNED_BLOCK, "ds-owned"))
    seams = parse_seams(block(text, SEAMS_BLOCK, "seams"))
    owned_re = [(p, glob_to_regex(p)) for p in owned]

    base_paths = set(
        p.decode() for p in git("ls-tree", "-r", "--name-only", "-z", a.base).split(b"\0") if p
    )
    raw = git("diff", "--name-status", "--no-renames", "-z", a.base, a.head).split(b"\0")
    changes = []
    for i in range(0, len(raw) - 1, 2):
        changes.append((raw[i].decode(), raw[i + 1].decode()))

    errors, warnings, ok = [], [], []

    for row in seams:
        if WILDCARD.search(row):
            errors.append((row, "Seams rows must be exact paths, not patterns"))
    dupes = sorted({r for r in seams if seams.count(r) > 1})
    for d in dupes:
        errors.append((d, "listed more than once in Seams"))
    for pattern, rx in owned_re:
        shadowed = sorted(p for p in base_paths if rx.match(p))
        if shadowed:
            sample = ", ".join(shadowed[:5])
            errors.append(
                (None, f"DS-owned pattern `{pattern}` matches {len(shadowed)} upstream file(s), e.g. {sample}")
            )

    seam_set = set(seams)
    changed = set()
    for status, path in changes:
        changed.add(path)
        in_base = path in base_paths
        owned_hit = next((p for p, rx in owned_re if rx.match(path)), None)
        if path in PROTECTED:
            errors.append((path, f"protected upstream file changed ({status}); restore it from the base tag"))
        elif in_base:
            if path in seam_set:
                ok.append((status, path, "seam"))
            else:
                errors.append((path, f"upstream file changed ({status}) but not listed in STUDIO-SEAMS.md"))
        elif owned_hit:
            ok.append((status, path, f"owned `{owned_hit}`"))
        elif path in seam_set:
            ok.append((status, path, "seam (new file)"))
        else:
            errors.append((path, f"new file ({status}) outside DS-owned paths and not listed in STUDIO-SEAMS.md"))

    for row in seams:
        if row not in changed and not WILDCARD.search(row):
            warnings.append((row, "listed in STUDIO-SEAMS.md but identical to the base tag (stale row)"))

    for path, msg in errors:
        annotate("error", msg, path)
    for path, msg in warnings:
        annotate("warning", msg, path)

    lines = [
        "### Studio seams guard",
        "",
        f"- Base: `{a.base}` → head `{a.head}`",
        f"- Changed paths: {len(changes)} (allowed {len(ok)}, errors {len(errors)}, stale rows {len(warnings)})",
        f"- Seams listed: {len(seams)}; DS-owned patterns: {len(owned)}",
        "",
    ]
    if errors:
        lines += ["| Problem | Path |", "| --- | --- |"]
        lines += [f"| {m} | `{p}` |" if p else f"| {m} | |" for p, m in errors[:200]]
        lines.append("")
    if ok:
        lines += ["<details><summary>Allowed changes</summary>", "", "| Status | Path | Allowed by |", "| --- | --- | --- |"]
        lines += [f"| {s} | `{p}` | {why} |" for s, p, why in ok[:500]]
        lines += ["", "</details>"]
    report = "\n".join(lines) + "\n"
    print(report)
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as fh:
            fh.write(report)
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
