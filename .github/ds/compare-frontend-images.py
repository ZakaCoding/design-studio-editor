#!/usr/bin/env python3
"""Compare a locally built frontend image with the official penpotapp/frontend image.

Usage: compare-frontend-images.py <built-image> <official-image> [--allow FILE]

--allow FILE lists glob patterns, one per line (# comments), for differences a
DS change makes on purpose: web-root relative paths (`js/extra.js`,
`index.html`) or absolute paths for files outside the web root
(`/entrypoint.sh`). A missing file means nothing is allowed.

Errors (exit 1):
  * image runtime config differs (user, entrypoint, cmd, ports, workdir, env);
  * the web root file list differs (after folding Vite content hashes such as
    assets/index-BpSX_BKS.css), unless the path matches a pattern in --allow;
  * index.html differs after folding the build version and timestamp;
  * any non web-root file the Dockerfile adds (/entrypoint.sh, /tmp templates,
    /etc/nginx) differs byte for byte.
Informational: web-root files whose bytes differ (bundles embed the version and
build time), size deltas. Writes a Markdown report to stdout and, when set, to
GITHUB_STEP_SUMMARY.
"""

import argparse
import difflib
import fnmatch
import hashlib
import json
import os
import re
import subprocess
import sys
import tarfile

WEBROOT = "/var/www/app"
RUNTIME_PATHS = ["/entrypoint.sh", "/tmp", "/etc/nginx"]
VITE_HASH = re.compile(r"^(?P<stem>.*/assets/.+?)-[A-Za-z0-9_-]{8}(?P<ext>\.[A-Za-z0-9]+)$")
CONFIG_KEYS = ["User", "Entrypoint", "Cmd", "ExposedPorts", "WorkingDir", "StopSignal"]


def sh(*args, **kw):
    return subprocess.run(list(args), check=True, capture_output=True, **kw).stdout


def inspect(image):
    return json.loads(sh("docker", "image", "inspect", image))[0]


def tree(container, path):
    """Return {relative path: sha256 | 'dir' | 'link:<target>'} for a path in a container."""
    proc = subprocess.Popen(["docker", "cp", f"{container}:{path}", "-"], stdout=subprocess.PIPE)
    out = {}
    with tarfile.open(fileobj=proc.stdout, mode="r|") as tar:
        for m in tar:
            name = m.name.split("/", 1)[1] if "/" in m.name else ""
            if m.isdir():
                out[name] = "dir"
            elif m.issym() or m.islnk():
                out[name] = "link:" + m.linkname
            elif m.isfile():
                h = hashlib.sha256()
                fh = tar.extractfile(m)
                for chunk in iter(lambda: fh.read(1 << 20), b""):
                    h.update(chunk)
                out[name] = (h.hexdigest(), m.size)
    if proc.wait() != 0:
        raise SystemExit(f"docker cp {container}:{path} failed")
    return out


def read_file(container, path):
    proc = subprocess.Popen(["docker", "cp", f"{container}:{path}", "-"], stdout=subprocess.PIPE)
    with tarfile.open(fileobj=proc.stdout, mode="r|") as tar:
        for m in tar:
            if m.isfile():
                data = tar.extractfile(m).read()
                proc.wait()
                return data.decode("utf-8", "replace")
    proc.wait()
    return ""


def fold(name):
    m = VITE_HASH.match(name)
    return f"{m.group('stem')}-<hash>{m.group('ext')}" if m else name


def normalize_index(html, version):
    version = version.strip()
    if version:
        html = re.sub(re.escape(version) + r"-\d{9,11}", "@VERSION_TAG@", html)
        html = html.replace(version, "@VERSION@")
    # The build date is written as an RFC 2822 string ("Mon, 09 Nov 2026
    # 09:30:48 +0000"), and the template's blank lines move with it, so fold
    # the value and collapse whitespace before comparing.
    html = re.sub(r'(penpotBuildDate\s*=\s*")[^"]*(")', r"\1@BUILD_DATE@\2", html)
    return re.sub(r"\s+", " ", html).strip()


def snapshot(image):
    cid = sh("docker", "create", image).decode().strip()
    try:
        web = tree(cid, WEBROOT)
        runtime = {}
        for p in RUNTIME_PATHS:
            for k, v in tree(cid, p).items():
                runtime[(p + "/" + k).rstrip("/") if k else p] = v
        version = read_file(cid, WEBROOT + "/version.txt")
        index = read_file(cid, WEBROOT + "/index.html")
    finally:
        subprocess.run(["docker", "rm", "-f", cid], capture_output=True)
    return {"web": web, "runtime": runtime, "version": version.strip(), "index": index}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("built")
    ap.add_argument("official")
    ap.add_argument("--allow", help="file with glob patterns (web-root relative) allowed to differ in the file list")
    a = ap.parse_args()

    allow = []
    if a.allow and os.path.exists(a.allow):
        allow = [l.strip() for l in open(a.allow, encoding="utf-8") if l.strip() and not l.startswith("#")]

    errors, warnings, info = [], [], []
    bi, oi = inspect(a.built), inspect(a.official)

    for key in CONFIG_KEYS:
        if bi["Config"].get(key) != oi["Config"].get(key):
            errors.append(f"image config `{key}` differs: built={bi['Config'].get(key)!r} official={oi['Config'].get(key)!r}")
    benv, oenv = set(bi["Config"].get("Env") or []), set(oi["Config"].get("Env") or [])
    if benv != oenv:
        errors.append(f"image Env differs: only built={sorted(benv - oenv)} only official={sorted(oenv - benv)}")

    b, o = snapshot(a.built), snapshot(a.official)

    # 1. Web-root file list, Vite hashes folded.
    def listing(web):
        out = {}
        for name, v in web.items():
            if v != "dir":
                out.setdefault(fold(name), []).append(name)
        return out

    bl, ol = listing(b["web"]), listing(o["web"])
    only_b = sorted(set(bl) - set(ol))
    only_o = sorted(set(ol) - set(bl))
    allowed = lambda p: any(fnmatch.fnmatch(p, pat) for pat in allow)
    for p in only_b:
        (info if allowed(p) else errors).append(f"web root: `{p}` only in built image")
    for p in only_o:
        (info if allowed(p) else errors).append(f"web root: `{p}` only in official image")
    folded_names = sum(1 for k in bl if "<hash>" in k)

    # 2. index.html after folding version/timestamp.
    bidx = normalize_index(b["index"], b["version"])
    oidx = normalize_index(o["index"], o["version"])
    if not b["index"]:
        errors.append("built image has no index.html")
    elif bidx != oidx and allowed("index.html"):
        info.append("index.html differs (allowed)")
    elif bidx != oidx:
        diff = list(
            difflib.unified_diff(
                re.split(r"(?<=>)", oidx), re.split(r"(?<=>)", bidx), "official/index.html", "built/index.html", n=1, lineterm=""
            )
        )
        errors.append("index.html differs after folding version and build time:\n```diff\n"
                      + "\n".join(l[:300] for l in diff[:80]) + "\n```")

    # 3. Files the Dockerfile adds outside the web root: byte identical.
    for path in sorted(set(b["runtime"]) | set(o["runtime"])):
        bv, ov = b["runtime"].get(path), o["runtime"].get(path)
        if bv != ov and allowed(path):
            info.append(f"runtime file `{path}` differs (allowed)")
        elif bv != ov:
            errors.append(f"runtime file `{path}` differs (built={'missing' if bv is None else 'present'}, official={'missing' if ov is None else 'present'})")

    # 4. Content of common web-root files (informational).
    same, differ = 0, []
    for k in sorted(set(bl) & set(ol)):
        if len(bl[k]) == 1 and len(ol[k]) == 1:
            if b["web"][bl[k][0]] == o["web"][ol[k][0]]:
                same += 1
            else:
                differ.append(k)
    static_diff = [p for p in differ if p.startswith(("fonts/", "images/"))]
    if static_diff:
        warnings.append(f"{len(static_diff)} static asset(s) under fonts/ or images/ differ, e.g. " + ", ".join(f"`{p}`" for p in static_diff[:5]))

    bsize = sum(v[1] for v in b["web"].values() if isinstance(v, tuple))
    osize = sum(v[1] for v in o["web"].values() if isinstance(v, tuple))
    delta = (bsize - osize) / osize * 100 if osize else 0.0
    if abs(delta) > 5:
        warnings.append(f"web root size differs by {delta:+.1f}% ({bsize} vs {osize} bytes)")
    base_ver = o["version"]
    if base_ver and not b["version"].startswith(base_ver):
        warnings.append(f"built version.txt `{b['version']}` does not start with the official `{base_ver}`")

    lines = [
        "### Frontend image vs official",
        "",
        f"- Built: `{a.built}` ({bi['Id'][:19]}, {bi['Size'] / 1e6:.0f} MB), version.txt `{b['version']}`",
        f"- Official: `{a.official}` ({oi['Id'][:19]}, {oi['Size'] / 1e6:.0f} MB, digest {', '.join(oi.get('RepoDigests') or ['-'])}), version.txt `{o['version']}`",
        f"- Web root files: built {sum(len(v) for v in bl.values())}, official {sum(len(v) for v in ol.values())}; "
        f"names with folded Vite hashes: {folded_names}",
        f"- Web root bytes: built {bsize}, official {osize} ({delta:+.2f}%)",
        f"- Common files byte-identical: {same}; differing: {len(differ)} (bundles embed version/build time)",
        f"- Result: {'FAIL' if errors else 'PASS'} ({len(errors)} error(s), {len(warnings)} warning(s))",
        "",
    ]
    for e in errors:
        print(f"::error::{e.splitlines()[0]}")
        lines.append(f"- ERROR: {e}")
    for w in warnings:
        print(f"::warning::{w}")
        lines.append(f"- WARNING: {w}")
    for i in info:
        lines.append(f"- allowed: {i}")
    if differ:
        lines += ["", "<details><summary>Web-root files with different bytes</summary>", ""]
        lines += [f"- `{p}`" for p in differ[:200]]
        lines += ["", "</details>"]
    report = "\n".join(lines) + "\n"
    print(report)
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as fh:
            fh.write(report)
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
