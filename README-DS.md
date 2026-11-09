# Design Studio editor

This repository is a fork of [penpot/penpot](https://github.com/penpot/penpot)
maintained by ZakaCoding for Design Studio (`design.zakacoding.dev`). Only the
**frontend** is changed and built here. Backend, exporter and MCP keep running
the official Penpot images of the same release. Upstream's own documentation in
`README.md` and the rest of the tree is unchanged and still describes Penpot.

Design Studio is not affiliated with or endorsed by Kaleidos or the Penpot
project. "Penpot" and the Penpot logo belong to their owners. Our product does
not use the Penpot logo; we name Penpot only to say where this code comes from.

## Branches, tags and images

| Ref | Meaning |
| --- | --- |
| `upstream/<tag>` | Exactly the official Penpot tag, e.g. `upstream/2.18.7` |
| `ds/main` | `DS_BASE_TAG` plus our commits (default branch) |
| `<tag>-ds.N` | Our release tags, e.g. `2.18.7-ds.1` |

Image: `zakacoding/design-studio-editor:<tag>` (branch builds:
`<branch>-<sha12>`), labelled `org.opencontainers.image.revision=<commit>`. It
is the same static nginx image as the official `penpotapp/frontend`, so memory
use in production does not change. CI does not push it to a registry yet; it
uploads `docker save` output as a workflow artifact.

## License (MPL-2.0)

- `LICENSE` stays exactly as upstream ships it, and every upstream file keeps
  its MPL-2.0 header. New files we add carry the MPL-2.0 header too.
- Upstream has no `NOTICE` file at 2.18.7; if one appears, it is kept as is.
- The source of every file we change is public in this repository, which
  satisfies MPL-2.0 for the code served to browsers. The built bundle keeps
  the `LICENSE` file that `manage.sh` writes into it.
- Every file we touch outside our namespace is listed in
  [`STUDIO-SEAMS.md`](STUDIO-SEAMS.md); CI enforces the list.

## Build

Who: GitHub Actions only, on GitHub-hosted `ubuntu-24.04` runners (4 vCPU,
16 GB). Never on the Design Studio VPS (4 GB). Workflows:

- `DS: frontend image` (`.github/workflows/ds-frontend-image.yml`): on push to
  `ds/**`, on `*-ds.*` tags, or by hand (Actions → Run workflow). It runs the
  seams guard, then `./manage.sh build-frontend-bundle` inside the pinned
  `penpotapp/devenv` image, builds `.github/ds/Dockerfile.frontend-on-official`
  (the bundle and `docker/images/files` on top of the official
  `penpotapp/frontend` image of the base tag, pinned by digest), smoke
  tests the result and the official image with the same checks, compares them
  (file list, `index.html`, nginx files) and uploads the image tarball.
- `DS: seams guard` (`.github/workflows/ds-seams-guard.yml`): on pull requests
  into `ds/**`, and called by the image workflow.

Needs no secrets. Upstream's `docker/images/Dockerfile.frontend` starts `FROM dhi.io/...` (Docker Hardened
Images), which refuses anonymous pulls; the official image of the base tag is that Dockerfile's output, so we
redo only the source-dependent steps on top of it. Optional secrets `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN`
(read-only) only raise Docker Hub pull limits.

Use an artifact: `gunzip -c design-studio-editor-<tag>.tar.gz | docker load`
after checking the `.sha256` file.

Rebuild by hand on any Linux machine with Docker and about 16 GB RAM:

```bash
docker pull penpotapp/devenv@sha256:<digest from ds-frontend-image.yml>
docker tag  penpotapp/devenv@sha256:<digest> penpotapp/devenv:ds-pinned
git tag -l '*-ds.*' | xargs -r git tag -d # keep the app version parseable
DEVENV_TAG=ds-pinned BUILD_WASM=yes ./manage.sh build-frontend-bundle
rsync -a --delete bundles/frontend/ docker/images/bundle-frontend/
docker build --build-arg BUNDLE_PATH=./bundle-frontend/ \
  --build-arg OFFICIAL_IMAGE="penpotapp/frontend:<base tag>@sha256:<digest from ds-frontend-image.yml>" \
  -f .github/ds/Dockerfile.frontend-on-official \
  --label org.opencontainers.image.revision="$(git rev-parse HEAD)" \
  -t zakacoding/design-studio-editor:local docker/images
```

When a DS change adds, removes or rewrites files in the image on purpose,
list them in `.github/ds/expected-image-diff.txt` (glob patterns relative to
`/var/www/app`, `index.html`, or absolute paths such as `/entrypoint.sh`).

## Upstream workflows

Upstream's workflows need Penpot's self-hosted runners and secrets. They stay
unmodified in the tree and are **disabled** in this repository's Actions
settings. After a rebase that adds a new upstream workflow, disable it too.
Open pull requests with `--repo ZakaCoding/design-studio-editor --base ds/main`
so they never target penpot/penpot by accident.
