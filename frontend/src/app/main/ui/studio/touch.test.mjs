// Run: node --test frontend/src/app/main/ui/studio/touch.test.mjs
// Zoom maths against Penpot's wheel formula (viewport/actions.cljs on-mouse-wheel) and the install guard.
// Gestures run in zakacoding-cloud perf/touch-e2e.mjs and tests/editor-touch-browser.mjs.
import test from 'node:test';
import assert from 'node:assert/strict';

const noop = () => {};
const doc = { readyState: 'loading', addEventListener: noop, head: { appendChild: noop }, createElement: () => ({ setAttribute: noop }) };
const win = { addEventListener: noop, location: { hash: '' } };
globalThis.window = win; globalThis.document = doc;
Object.defineProperty(globalThis, 'navigator', { value: { languages: ['en'], maxTouchPoints: 0 }, configurable: true });
globalThis.localStorage = { getItem: () => null, setItem: noop };
globalThis.location = win.location;
const { start } = await import('./touch.js');
start(); start(); // second call is a no-op
const touch = win.__dsTouch;
const penpotScale = delta => { const s = 1 + Math.abs(-0.0057 * delta); return delta > 0 ? 1 / s : s; };

test('pinch ratio maps to the wheel delta Penpot turns back into that zoom factor', () => {
  for (const r of [0.25, 0.5, 0.9, 0.99, 1, 1.01, 1.1, 2, 4]) assert.ok(Math.abs(penpotScale(touch.zoomDelta(r)) - r) < 1e-9, String(r));
  assert.ok(touch.zoomDelta(2) < 0 && touch.zoomDelta(0.5) > 0);
});
