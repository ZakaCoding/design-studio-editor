# Upstream pull request: MCP save confirmation

This file is the description of a pull request for
[penpot/penpot](https://github.com/penpot/penpot). It is kept in the Design
Studio fork so the text travels with the code. The owner sends it from their
own account after the F2 release is live.

Before sending:

1. Rebase the change onto upstream `develop`. It was written and tested on
   tag `2.18.7`.
2. Rename the three error codes (see "Open points for the maintainers", point 1) if the
   maintainers prefer a neutral prefix.
3. Remove this file, `STUDIO-SEAMS.md` and the `ds` wording from the branch
   that goes upstream. Squash to one commit with the title below and a
   `Signed-off-by` line (`git commit -s`), as `CONTRIBUTING.md` asks.
4. Do not add a `CHANGES.md` entry; upstream writes those.

Everything below the line is the pull request.

---

**Title:** `:bug: Make MCP tasks wait for the save and stop writing after a failed save`

### Related Ticket

<!-- Add the issue number once one is open. There is no upstream issue for this yet. -->

### Summary

With the integrated MCP server, an `execute_code` task reports success as soon
as its script returns. The edits exist only in the browser at that moment; the
persistence queue sends them afterwards. Two things follow:

- If that save fails or never completes, the agent has already been told the
  edit succeeded. It carries on, and the work is lost when the tab is closed.
- While saving is failing (`:retrying`, `:error`) or has stalled, further MCP
  tasks keep adding edits to a queue that is not being stored.

A person sees the "changes could not be saved" notification and stops. An agent
does not see it.

This change makes the MCP plugin wait for the existing persistence queue before
it reports success, and refuses new MCP writes while saving cannot be trusted.
It adds no save, no retry and no new request: it only reads the persistence
state and uses the existing `::force-persist` event and
`wait-persisted-or-error`.

Behavior, for the integrated MCP plugin only:

| Situation | Before | After |
| --- | --- | --- |
| Task writes, save succeeds | success when the script returns | success after the save is acknowledged |
| Task only reads | success | success, no wait, also after a failed save |
| Task writes, save fails or does not settle in 90 s | success | error `DS_SAVE_UNCONFIRMED`; the user gets the save warning |
| Script throws after writing | script error | waits for the save first, then the script error (or `DS_SAVE_UNCONFIRMED`) |
| Write while status is `:error` or `:retrying`, while `:failing-since` is set, after a stall report, or after 5 min pending/saving without progress | write accepted | write refused with `DS_SAVE_BLOCKED`; reads still work |
| Write after an unconfirmed save | write accepted | refused until the editor is reopened |
| File changes during the task | success | `DS_SAVE_UNCONFIRMED` |

Other plugins are not affected: the guard only acts for `mcp-plugin-id`, and
only for `content:write`, `library:write` and `comment:write`. It is not an
authorization check; `check-permission` still decides what a plugin may do.

What it does not do: it does not fix the transport, reconcile an unknown server
outcome, keep changes offline, or make reloading an unsaved editor safe. The
native retry and notification behavior is unchanged.

#### Changes

- `frontend/src/app/plugins/persistence_guard.cljs` (new): the guard.
  `write-blocked?` is a pure function of the persistence state. `note-write!`
  refuses MCP writes. `checkpoint` and `settle!` bracket one task. The
  namespace requires only `app.main.store`, because `app.plugins.register`
  requires it and `app.main.data.persistence` reaches `app.plugins.register`
  through `app.main.errors`; the persistence operations are passed in by the
  caller.
- `frontend/src/app/plugins/register.cljs`: `check-permission` calls
  `note-write!` first.
- `frontend/src/app/plugins/api.cljs`: the context exposes a non-enumerable
  `$checkPersistence`, next to `$plugin`.
- `plugins/libs/plugins-runtime/src/lib/api/index.ts`: `checkPermission`
  reports each granted permission to `context.$checkPersistence` when the host
  provides it. This is needed because calls such as `createRectangle` are only
  permission-checked in the runtime, not in ClojureScript.
- `frontend/src/app/main/data/workspace/mcp.cljs`: the `mcp` extension object
  gets `persistenceCheckpoint` and `settlePersistence`, wired to
  `::dps/force-persist`, `dps/wait-persisted-or-error` (90 s, below the 120 s
  task timeout of the MCP server) and `errors/flash-persistence`.
- `mcp/packages/plugin/src/task-handlers/ExecuteCodeTaskHandler.ts` and
  `index.d.ts`: the handler takes a checkpoint before running the script and
  awaits `settlePersistence` afterwards. A standalone plugin (no `mcp` global)
  behaves as before.

`app.main.data.persistence` is not modified.

#### Tests

- `frontend/test/frontend_tests/plugins/persistence_guard_test.cljs`: one case
  per persistence state for `write-blocked?`; the task flow (acknowledged
  save, failed save, wait ending without an answer, synchronous answer, file
  change before the flush, during the save and before the task ends, missing
  checkpoint, failing flush, failing warning); the latch; reads after a
  failure; other plugins untouched; the `check-permission` seam.
- `frontend/test/frontend_tests/data/workspace_mcp_test.cljs`
  (`test-persistence-ops`): the wiring to the persistence API.
- `plugins/libs/plugins-runtime/src/lib/api/persistence-hook.spec.ts`: the
  runtime hook.

### Steps to reproduce

1. Enable the MCP server, open a file and connect an MCP client.
2. Make saving fail: block `POST /api/main/methods/update-file` in the browser
   dev tools (request blocking), or stop the backend.
3. From the MCP client run `execute_code` with
   `const r = penpot.createRectangle(); r.name = "lost"; return r.id;`.

Before: the tool call returns the id as a success. Reload the file: the
rectangle is not there. Repeating step 3 keeps "succeeding".

After: the first call returns `DS_SAVE_UNCONFIRMED` once the save has failed or
90 s have passed, and the save warning is shown. Further write calls return
`DS_SAVE_BLOCKED`. A read-only call such as `return penpot.currentPage.name;`
still succeeds. With saving working, the same script returns success only after
`update-file` has answered.

### Open points for the maintainers

1. **Error text.** The three messages start with `DS_SAVE_BLOCKED`,
   `DS_SAVE_UNCONFIRMED` and `DS_SAVE_UNAVAILABLE` so that an agent or a client
   can match on a stable code. `DS` is our fork's prefix; `PENPOT_SAVE_*` or
   any other naming is fine. They are plain English strings in
   `persistence_guard.cljs`, not translated, because the reader is the MCP
   client.
2. **Latch.** After an unconfirmed save, MCP writes stay refused until the
   workspace is loaded again. That is deliberately strict: the outcome on the
   server is unknown, and an agent that retries would duplicate the edit. If
   you prefer, the latch can be cleared when the queue next reaches `:saved`.
3. **Duplicated constants.** `persistence-guard/mcp-plugin-id` repeats
   `register/mcp-plugin-id` (a test asserts they are equal) and
   `stall-timeout-ms` repeats the private `saving-stall-timeout-ms`. Both could
   move to a small shared namespace.
4. **`runtime-ready`.** The runtime reports `runtime-ready` once per API it
   creates, and `checkpoint` refuses to start a task until it has seen it. This
   protects against a cached older `plugins-runtime` that would skip the hook.
   If the runtime and the app are always served from one build, it can go.
5. **Scope.** Only `execute_code` is bracketed, because it is the only MCP task
   that writes. Regular plugins are left alone on purpose; the same hook could
   serve them later.

### Checklist

- [ ] Choose the correct target branch; use `develop` by default.
- [x] Provide a brief summary of the changes introduced.
- [x] Add a detailed explanation of how to reproduce the issue and/or verify the fix, if applicable.
- [ ] Include screenshots or videos, if applicable.
- [x] Add or modify existing integration tests in case of bugs or new features, if applicable.
- [ ] Refactor any modified SCSS files following the refactor guide. (No SCSS changed.)
- [ ] Check CI passes successfully.
