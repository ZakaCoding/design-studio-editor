/* Design Studio touch navigation for the native Penpot 2.18.3 editor canvas (#142).
   Moved as-is from studio/public/shell/ds-touch.js (F2 WP7). Bundled into the editor and
   started once from ui/workspace/viewport.cljs; its styles (formerly in ds-shell.css) are injected
   here, so no stylesheet seam is needed. Stock Penpot sets touch-action: none on the canvas and reads
   a finger as a left mouse button, so a finger drag selects or moves shapes and the canvas
   only pans with a mouse (middle button, Space+drag) or a trackpad (wheel). This layer
   handles pointerType "touch" on the canvas only; mouse, trackpad and Apple Pencil input
   stay stock, and it never edits files or selection itself.
   - two fingers: drag pans, pinch zooms (sent to Penpot as the wheel events a trackpad sends)
   - one finger: stock (select, move, draw), after a hold in which a second finger
     turns the touch into a pan instead, so a pan never starts a selection or a move
   - hand button at the top right of the canvas (touch devices only): one finger pans, taps do nothing;
     enabled by default until the user explicitly chooses editing */

const STYLE = `
.ds-hand, .ds-keyboard, .ds-hand-tip {
  --ds-ink: #182421; --ds-mint: #9CC9B7; --ds-lime: #D8F28A;
  font-family: Inter, "DM Sans", system-ui, -apple-system, "Segoe UI", sans-serif;
}
.ds-hand, .ds-keyboard { position: fixed; z-index: 850; inset-inline-start: 0; inset-block-start: 0; transform: translate(calc(var(--x, -999px) - 100%), var(--y, -999px));
  inline-size: 44px; block-size: 44px; display: grid; place-items: center; padding: 0; border: 1px solid rgba(255, 255, 255, .16); border-radius: 12px;
  background: #18191d; color: #fff; box-shadow: 0 6px 18px rgba(0, 0, 0, .35); cursor: pointer; touch-action: manipulation; -webkit-tap-highlight-color: transparent; }
.ds-hand svg, .ds-keyboard svg { inline-size: 22px; block-size: 22px; fill: none; stroke: currentColor; stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }
.ds-hand[aria-pressed="true"] { background: var(--ds-lime); border-color: var(--ds-lime); color: var(--ds-ink); }
.ds-hand:focus-visible, .ds-keyboard:focus-visible { outline: 3px solid var(--ds-mint); outline-offset: 2px; }
.ds-hand-tip { position: fixed; z-index: 850; inset-inline-start: 0; inset-block-start: 0; transform: translate(calc(var(--x, -999px) - 100%), var(--y, -999px));
  inline-size: min(280px, calc(100vw - 32px)); margin: 0; padding: 10px 12px; border-radius: 10px; background: #fff; color: var(--ds-ink);
  font-size: 13px; line-height: 1.45; box-shadow: 0 12px 32px rgba(0, 0, 0, .35); animation: ds-touch-fade .22s ease; }
@media (prefers-reduced-motion: reduce) { .ds-hand-tip { animation: none !important; transition: none !important; } }
@keyframes ds-touch-fade { from { opacity: 0; } to { opacity: 1; } }
/* Let Safari recover whole-page zoom before resuming canvas gestures. */
.ds-page-pinch { touch-action: pinch-zoom !important; }
`;

/** Install the touch layer. Safe to call more than once. */
export function start() {
  'use strict';
  if (window.__dsTouch) return;
  const style = document.createElement('style');
  style.setAttribute('data-ds-touch', '');
  style.textContent = STYLE;
  document.head.appendChild(style);

  const CANVAS = 'svg.viewport-controls';
  const TEXT_TOOL = 'button[data-tool="text"]';
  const TEXT_EDITOR = '.text-editor [contenteditable="true"]';
  const pageZoomed = () => {
    const v = window.visualViewport;
    return !!v && v.width > 0 && v.width < document.documentElement.clientWidth - 2;
  };
  // SLOP: movement that commits one finger to editing when Hand is off.
  // Never commit a stationary finger on a timer: a slow second finger must still pan safely.
  // PINCH: relative change in finger distance before a two-finger touch starts zooming.
  const SLOP = 10, PINCH = 0.01, CLICK_GUARD = 450;
  // Penpot 2.18.3 viewport/actions.cljs: scale = 1 + |-0.0057 * delta|, inverted when delta > 0.
  const SCALE_PER_PIXEL = 0.0057;
  const zoomDelta = ratio => ratio >= 1 ? -(ratio - 1) / SCALE_PER_PIXEL : (1 / ratio - 1) / SCALE_PER_PIXEL;

  const lang = () => {
    try {
      const m = /"~:lang","(\w+)/.exec(localStorage.getItem('penpot-user:/profile') || '');
      if (m) return m[1].toLowerCase().startsWith('id') ? 'id' : 'en';
    } catch { /* storage blocked */ }
    const prefs = navigator.languages && navigator.languages.length ? navigator.languages : [navigator.language || 'en'];
    return String(prefs[0] || 'en').toLowerCase().startsWith('id') ? 'id' : 'en';
  };
  const T = {
    en: { hand: 'Pan with one finger', hint: 'Hand on: drag with one finger to pan. Pinch with two fingers to zoom. Turn Hand off to select, move or draw.', editHint: 'Hand off: one finger edits. Use two fingers together to pan or pinch to zoom. Turn Hand on for safe one-finger navigation.' },
    id: { hand: 'Geser kanvas dengan satu jari', hint: 'Mode tangan aktif: geser dengan satu jari. Cubit dengan dua jari untuk zoom. Matikan mode tangan untuk memilih, memindah, atau menggambar.', editHint: 'Mode tangan mati: satu jari mengedit. Gunakan dua jari bersamaan untuk menggeser atau mencubit untuk zoom. Aktifkan mode tangan untuk navigasi aman.' },
  };
  const store = {
    get(k) { try { return localStorage.getItem('ds-touch:' + k); } catch { return null; } },
    set(k, v) { try { localStorage.setItem('ds-touch:' + k, v); } catch { /* blocked: resets per load */ } },
  };

  // mode: idle | held | stock | gesture | browser (recover a clipped page viewport)
  let mode = 'idle', held = null, stockId = null, last = null, guardUntil = 0;
  let hand = store.get('hand') !== '0';
  const touches = new Map(), ignored = new Set();
  const canvasOf = e => e.target instanceof Element ? e.target.closest(CANVAS) : null;
  const swallow = (e, prevent) => { e.stopImmediatePropagation(); if (prevent) e.preventDefault(); };

  function wheel(x, y, deltaX, deltaY, ctrlKey) {
    const canvas = document.querySelector(CANVAS);
    if (canvas) canvas.dispatchEvent(new WheelEvent('wheel', { bubbles: true, cancelable: true, composed: true, clientX: x, clientY: y, deltaX, deltaY, deltaMode: 0, ctrlKey }));
  }
  function pan() {
    const pts = [...touches.values()].slice(0, 2);
    if (!pts.length) { last = null; return; }
    const two = pts.length === 2;
    const x = two ? (pts[0].x + pts[1].x) / 2 : pts[0].x, y = two ? (pts[0].y + pts[1].y) / 2 : pts[0].y;
    const d = two ? Math.hypot(pts[0].x - pts[1].x, pts[0].y - pts[1].y) : 0;
    let base = d;
    if (last && last.n === pts.length) {
      // Fingers drag the content, so the viewport moves the opposite way.
      if (x !== last.x || y !== last.y) wheel(x, y, last.x - x, last.y - y, false);
      // A two-finger pan wobbles the finger distance by a pixel or so: zoom only past PINCH,
      // measured from the last applied distance so slow pinches still add up.
      base = last.d;
      if (two && base > 0 && d > 0 && Math.abs(d / base - 1) >= PINCH) { wheel(x, y, 0, zoomDelta(d / base), true); base = d; }
    }
    last = { x, y, d: base, n: pts.length };
  }
  // Each finger moves in its own event: sample all of them once per frame, or the first finger's
  // move alone reads as a pinch. A press or a release flushes the pending frame first.
  let raf = 0;
  const frame = () => { if (!raf) raf = requestAnimationFrame(() => { raf = 0; pan(); }); };
  const flush = () => { cancelAnimationFrame(raf); raf = 0; pan(); };
  function startGesture() { mode = 'gesture'; last = null; pan(); }

  const pointer = e => ({ bubbles: true, cancelable: true, composed: true, pointerId: e.pointerId, pointerType: 'touch', isPrimary: e.isPrimary,
    clientX: e.clientX, clientY: e.clientY, screenX: e.screenX, screenY: e.screenY, width: e.width, height: e.height,
    shiftKey: e.shiftKey, ctrlKey: e.ctrlKey, altKey: e.altKey, metaKey: e.metaKey });
  // A finger has no hover, and Penpot picks the shape under the pointer from its hover state,
  // which settles a little after a move. So the pointer moves to the touch point as the finger
  // lands, and the hold gives the hover time to settle before the press.
  const hover = e => e.target.dispatchEvent(new PointerEvent('pointermove', { ...pointer(e), button: -1, buttons: 0 }));
  // Hand the held finger to Penpot as if it had just touched down. Untrusted, so it passes our
  // listeners. Penpot captures the pointer first thing; a finger that is already lifting can no
  // longer be captured, so capture errors are ignored for this one synchronous dispatch.
  function release() {
    const e = held; held = null; mode = 'stock'; stockId = e.pointerId;
    if (!e.target.isConnected) return;
    const init = pointer(e);
    const capture = Element.prototype.setPointerCapture;
    Element.prototype.setPointerCapture = function (id) { try { capture.call(this, id); } catch { /* finger already up */ } };
    try {
      e.target.dispatchEvent(new PointerEvent('pointerdown', { ...init, button: 0, buttons: 1, pressure: e.pressure || 0.5 }));
    } finally { Element.prototype.setPointerCapture = capture; }
  }
  function reset() { cancelAnimationFrame(raf); raf = 0; held = null; mode = 'idle'; stockId = null; last = null; touches.clear(); ignored.clear(); }

  function onDown(e) {
    // A real mouse/pen press ends the compatibility-click guard on hybrid iPads.
    if (e.isTrusted && e.pointerType !== 'touch') { guardUntil = 0; return; }
    if (e.pointerType !== 'touch' || !e.isTrusted) return;
    const id = e.pointerId, p = { x: e.clientX, y: e.clientY };
    guardUntil = 0; // a new touch owns the next click
    // Native editing surfaces own their caret, selection and keyboard gestures.
    if (e.target instanceof Element && e.target.closest('input, textarea, [contenteditable="true"]')) return;
    if (mode === 'idle') {
      if (!canvasOf(e)) return;
      touches.set(id, p);
      // When Safari zooms the page, allow a native pinch to recover the whole
      // interface. Stop editor presses, but do not cancel browser defaults.
      if (pageZoomed()) { mode = 'browser'; swallow(e, false); return; }
      hint(true);
      if (hand) { startGesture(); swallow(e, true); return; }
      mode = 'held'; held = e; hover(e);
      swallow(e, false); // not preventDefault: if this becomes a stock touch, its tap must stay stock
      return;
    }
    if (mode === 'browser') { touches.set(id, p); swallow(e, false); return; }
    if (mode === 'held') { held = null; touches.set(id, p); startGesture(); swallow(e, true); return; }
    if (mode === 'gesture') { flush(); touches.set(id, p); last = null; pan(); swallow(e, true); return; }
    // stock: Penpot is mid-drag with one finger; extra fingers are ignored rather than read as a second mouse.
    ignored.add(id); swallow(e, true);
  }
  function onMove(e) {
    if (e.pointerType !== 'touch' || !e.isTrusted) return;
    const id = e.pointerId;
    if (mode === 'browser' && touches.has(id)) { swallow(e, false); return; }
    if (mode === 'held' && held.pointerId === id) {
      if (Math.hypot(e.clientX - held.clientX, e.clientY - held.clientY) < SLOP) { swallow(e, false); return; }
      release(); return; // this move reaches Penpot right after the replayed press
    }
    if (mode === 'gesture' && touches.has(id)) { touches.set(id, { x: e.clientX, y: e.clientY }); frame(); swallow(e, true); return; }
    if (ignored.has(id)) swallow(e, true);
  }
  function onUp(e) {
    if (e.pointerType !== 'touch' || !e.isTrusted) return;
    const id = e.pointerId;
    if (mode === 'browser' && touches.has(id)) {
      touches.delete(id); swallow(e, false);
      if (!touches.size) { mode = 'idle'; guardUntil = Date.now() + CLICK_GUARD; }
      return;
    }
    if (mode === 'held' && held.pointerId === id) {
      if (e.type === 'pointercancel') { reset(); swallow(e, false); return; }
      const text = document.querySelector(TEXT_TOOL + '[aria-pressed="true"]');
      const target = held.target, init = pointer(e);
      release(); mode = 'idle'; stockId = null; touches.clear();
      if (text) {
        // Penpot starts the text-drawing subscription after the press. Give it
        // a frame before release, while keeping the two-finger decision intact.
        swallow(e, true);
        setTimeout(() => {
          if (!target.isConnected) return;
          // The box-drawing stream initializes on a position sample after down.
          // A changed sample initializes the drawing stream. Move one CSS
          // pixel within Penpot's click-draw threshold, then release here.
          target.dispatchEvent(new PointerEvent('pointermove', { ...init, clientX: init.clientX + 1, button: -1, buttons: 1, pressure: 0.5 }));
          requestAnimationFrame(() => {
            if (!target.isConnected) return;
            const up = new PointerEvent('pointerup', { ...init, button: 0, buttons: 0 });
            // Penpot checks legacy `which`, which a constructed release leaves 0.
            Object.defineProperty(up, 'which', { value: 1 });
            const releaseCapture = Element.prototype.releasePointerCapture;
            Element.prototype.releasePointerCapture = function (id) { try { releaseCapture.call(this, id); } catch { /* finger already up */ } };
            try { target.dispatchEvent(up); }
            finally { Element.prototype.releasePointerCapture = releaseCapture; }
          });
        }, 0);
      }
      return; // other quick taps keep their original release
    }
    if (mode === 'gesture' && touches.has(id)) {
      // The release carries the last position, which may not have come as a move.
      // Cancellation may have no position: flush only the last known real move.
      if (e.type !== 'pointercancel') touches.set(id, { x: e.clientX, y: e.clientY });
      flush();
      touches.delete(id); last = null; pan(); swallow(e, true);
      if (!touches.size) { mode = 'idle'; guardUntil = Date.now() + CLICK_GUARD; }
      return;
    }
    if (ignored.delete(id)) { swallow(e, true); return; }
    if (mode === 'stock' && id === stockId) { mode = 'idle'; stockId = null; touches.clear(); }
  }
  // A tap in hand mode, or the end of a pan, must not reach Penpot as a click on a shape.
  function onClick(e) {
    if (e.target instanceof Element && e.target.closest(TEXT_TOOL) && touchDevice()) setHand(false);
    // Keyboard activation and explicitly identified mouse/pen clicks stay native.
    if (e.detail === 0 || (e.pointerType && e.pointerType !== 'touch')) return;
    if (Date.now() < guardUntil && canvasOf(e)) swallow(e, true);
  }

  // ---- Hand button and the one-time gesture hint, on touch devices only.
  const touchDevice = () => (navigator.maxTouchPoints || 0) > 0;
  let button = null, keyboard = null, recovery = null, tip = null, tipTimer = 0;
  const pinchTargets = new Set();
  function allowPagePinch(canvas) {
    const targets = new Set();
    // Browsers intersect touch-action along the hit target's ancestor path.
    // Penpot also disables gestures on a workspace section above the canvas.
    for (let e = canvas; e; e = e.parentElement) targets.add(e);
    for (const e of pinchTargets) if (!targets.has(e)) { e.classList.remove('ds-page-pinch'); pinchTargets.delete(e); }
    for (const e of targets) { e.classList.add('ds-page-pinch'); pinchTargets.add(e); }
  }
  function setHand(value) {
    hand = value; store.set('hand', hand ? '1' : '0'); reset();
    if (button) button.setAttribute('aria-pressed', String(hand));
    hint(false);
  }
  function toggle() { setHand(!hand); }
  // iPad Safari may suppress the software keyboard when Penpot mounts/focuses
  // its editor asynchronously. Focus the real editor in a fresh user gesture.
  // No proxy input or synthetic text updates: native beforeinput/IME owns typing.
  function focusText(reopen) {
    const editor = document.querySelector(TEXT_EDITOR);
    if (!editor) return;
    if (reopen && document.activeElement === editor) editor.blur();
    editor.focus({ preventScroll: true });
  }
  const ICON = '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M18 11V6a2 2 0 0 0-4 0v0M14 10V4a2 2 0 0 0-4 0v2M10 10.5V6a2 2 0 0 0-4 0v8"/><path d="M18 8a2 2 0 1 1 4 0v6a8 8 0 0 1-8 8h-2c-2.8 0-4.5-.86-5.99-2.34l-3.6-3.6a2 2 0 0 1 2.83-2.82L7 15"/></svg>';
  function place() {
    const canvas = touchDevice() && /^#\/workspace/.test(location.hash) ? document.querySelector(CANVAS) : null;
    if (!canvas) {
      if (button) { button.remove(); button = null; }
      if (keyboard) { keyboard.remove(); keyboard = null; }
      if (recovery) { recovery.remove(); recovery = null; }
      allowPagePinch(null);
      document.documentElement.classList.remove('ds-page-zoomed');
      hint(false); return;
    }
    if (!button) {
      const t = T[lang()];
      button = document.createElement('button');
      button.type = 'button'; button.className = 'ds-hand';
      button.setAttribute('aria-label', t.hand); button.title = t.hand; button.setAttribute('aria-pressed', String(hand));
      button.innerHTML = ICON;
      button.addEventListener('click', toggle);
      document.body.appendChild(button);
    }
    const r = canvas.getBoundingClientRect();
    const v = window.visualViewport;
    const left = v?.offsetLeft || 0, top = v?.offsetTop || 0;
    const width = v?.width || innerWidth, height = v?.height || innerHeight;
    const x = Math.max(left + 112, Math.min(r.right - 16, left + width - 16));
    const y = Math.max(top + 16, Math.min(r.top + 36, top + height - 60));
    const zoomed = pageZoomed();
    document.documentElement.classList.toggle('ds-page-zoomed', zoomed);
    allowPagePinch(zoomed ? canvas : null);
    if (zoomed) {
      hint(false);
      if (!recovery) {
        recovery = document.createElement('p'); recovery.className = 'ds-hand-tip ds-page-recovery'; recovery.setAttribute('role', 'status');
        recovery.textContent = lang() === 'id' ? 'Cubit ke dalam untuk menampilkan seluruh editor. Setelah itu, cubit untuk zoom kanvas.' : 'Pinch inward to show the whole editor. Then pinch to zoom the canvas.';
        document.body.appendChild(recovery);
      }
      recovery.style.inlineSize = Math.min(280, width - 32) + 'px';
      recovery.style.setProperty('--x', (left + width - 16) + 'px'); recovery.style.setProperty('--y', (top + 72) + 'px');
    } else if (recovery) { recovery.remove(); recovery = null; }
    // Top right of the canvas, below the ruler: Penpot keeps its tools top centre and bottom left.
    button.style.setProperty('--x', x + 'px'); button.style.setProperty('--y', y + 'px');
    if (document.querySelector(TEXT_EDITOR)) {
      if (!keyboard) {
        keyboard = document.createElement('button'); keyboard.type = 'button'; keyboard.className = 'ds-keyboard';
        const label = lang() === 'id' ? 'Tampilkan keyboard' : 'Show keyboard';
        keyboard.setAttribute('aria-label', label); keyboard.title = label;
        keyboard.innerHTML = '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="2" y="5" width="20" height="14" rx="2"/><path d="M6 9h1m4 0h1m4 0h1M6 12h1m4 0h1m4 0h1M7 16h10"/></svg>';
        let touchPress = false;
        keyboard.addEventListener('pointerdown', e => { touchPress = e.pointerType === 'touch'; e.preventDefault(); });
        // WebKit may omit click after a cancelled touch press. Pointerup keeps
        // focus inside the trusted touch gesture; click handles mouse/keyboard.
        keyboard.addEventListener('pointerup', e => {
          if (e.isTrusted && e.pointerType === 'touch') { e.preventDefault(); focusText(true); }
        });
        keyboard.addEventListener('keydown', e => { if (e.key === 'Enter' || e.key === ' ') touchPress = false; });
        keyboard.addEventListener('click', e => {
          // Some WebKit versions label the compatibility click as mouse/empty.
          // Do not blur/refocus twice for one tap; keydown enables keyboard activation.
          if (!touchPress) focusText(true);
          touchPress = false;
        });
        document.body.appendChild(keyboard);
      }
      keyboard.style.setProperty('--x', (x - 52) + 'px'); keyboard.style.setProperty('--y', y + 'px');
    } else if (keyboard) { keyboard.remove(); keyboard = null; }
    if (tip) { tip.style.setProperty('--x', x + 'px'); tip.style.setProperty('--y', (y + 52) + 'px'); }
  }
  // Shown once per device, on the first touch on the canvas.
  function hint(show) {
    if (!show) { clearTimeout(tipTimer); if (tip) { tip.remove(); tip = null; } return; }
    if (tip || !button || store.get('hint') === '1') return;
    store.set('hint', '1');
    tip = document.createElement('p');
    tip.className = 'ds-hand-tip'; tip.setAttribute('role', 'status'); tip.textContent = T[lang()][hand ? 'hint' : 'editHint'];
    document.body.appendChild(tip); place();
    tipTimer = setTimeout(() => hint(false), 6000);
  }

  // Capture on window runs before Penpot's own window and React root listeners.
  const opts = { capture: true, passive: false };
  window.addEventListener('pointerdown', onDown, opts);
  window.addEventListener('pointermove', onMove, opts);
  window.addEventListener('pointerup', onUp, opts);
  window.addEventListener('pointercancel', onUp, opts);
  window.addEventListener('click', onClick, opts);
  window.addEventListener('dblclick', onClick, opts);
  window.addEventListener('blur', reset);
  document.addEventListener('visibilitychange', () => { if (document.hidden) reset(); });

  let queued = false;
  const queue = () => { if (!queued) { queued = true; requestAnimationFrame(() => { queued = false; try { place(); } catch { /* never break the editor */ } }); } };
  const start = () => {
    queue(); new MutationObserver(queue).observe(document.body, { childList: true, subtree: true });
    window.addEventListener('hashchange', queue); window.addEventListener('resize', queue);
    window.visualViewport?.addEventListener('resize', queue); window.visualViewport?.addEventListener('scroll', queue);
  };
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start); else start();

  Object.defineProperty(window, '__dsTouch', { value: Object.freeze({ zoomDelta, SLOP, version: 1 }) });
}
