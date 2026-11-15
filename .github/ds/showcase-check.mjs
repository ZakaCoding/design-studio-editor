// Opens the DS showcase of a running frontend image and reports what rendered.
// usage: node showcase-check.mjs <base-url> <out-dir>
const [base, out] = process.argv.slice(2);
const { chromium } = await import(process.env.PLAYWRIGHT_MODULE);
const browser = await chromium.launch({ channel: "chrome", args: ["--no-sandbox"] });
const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
const errors = [];
page.on("pageerror", (e) => errors.push("pageerror: " + e.message));
page.on("console", (m) => { if (m.type() === "error") errors.push("console: " + m.text().slice(0, 300)); });
await page.route("**/api/**", (route) => {
  const url = route.request().url();
  const body = url.includes("get-profile")
    ? '["^ ","~:id","~u00000000-0000-0000-0000-000000000000"]'
    : '["^ "]';
  route.fulfill({ status: 200, contentType: "application/transit+json", body });
});
await page.goto(base + "/#/debug/playground", { waitUntil: "load" });
await page.waitForTimeout(6000);
const info = await page.evaluate(() => {
  const q = (s) => document.querySelectorAll(s).length;
  const cs = getComputedStyle(document.documentElement);
  const btn = document.querySelector('[class*="studio_ds_button__variant-primary"]');
  return {
    hash: location.hash,
    title: document.title,
    showcase: q('[class*="studio_ds_showcase__showcase"]'),
    buttons: q('[class*="studio_ds_button__button"]'),
    inputs: q('[class*="studio_ds_field__input"]'),
    cards: q('[class*="studio_ds_card__card"]'),
    panels: q('[class*="studio_ds_panel__panel"]'),
    toolbars: q('[role="toolbar"]'),
    badges: q('[class*="studio_ds_badge__badge"]'),
    tooltips: q('[role="tooltip"]'),
    logos: q('[class*="studio_ds_logo__logo"]'),
    toastRegion: q('[class*="studio_ds_toast__region"]'),
    tokenAccent: cs.getPropertyValue("--ds-accent-default").trim(),
    tokenBgPanel: cs.getPropertyValue("--ds-bg-panel").trim(),
    primaryBg: btn && getComputedStyle(btn).backgroundColor,
    primaryRadius: btn && getComputedStyle(btn).borderRadius,
    bodyText: document.body.innerText.slice(0, 200),
  };
});
await page.screenshot({ path: out + "/showcase-top.png" });
let interactions = {};
if (info.showcase) {
  await page.screenshot({ path: out + "/showcase-full.png", fullPage: false });
  const sc = page.locator('[class*="studio_ds_showcase__showcase"]');
  // Toast
  await page.getByRole("button", { name: "Success toast" }).click();
  await page.waitForTimeout(400);
  interactions.toasts = await page.locator('[class*="studio_ds_toast__region"] [class*="studio_ds_toast__toast"]').count();
  // Dialog: opens, takes focus, Esc closes, focus returns.
  const opener = page.getByRole("button", { name: "Open dialog" });
  await opener.click();
  await page.waitForTimeout(400);
  interactions.dialogOpen = await page.getByRole("dialog").count();
  interactions.focusInDialog = await page.evaluate(() => !!document.activeElement.closest('[role="dialog"]'));
  await page.screenshot({ path: out + "/showcase-dialog.png" });
  for (let i = 0; i < 8; i++) await page.keyboard.press("Tab");
  interactions.focusTrapped = await page.evaluate(() => !!document.activeElement.closest('[role="dialog"]'));
  await page.keyboard.press("Escape");
  await page.waitForTimeout(300);
  interactions.dialogClosed = (await page.getByRole("dialog").count()) === 0;
  interactions.focusReturned = await page.evaluate(() => document.activeElement.textContent.includes("Open dialog"));
  // Tooltip on keyboard focus
  await page.getByRole("button", { name: "Hover or focus me" }).focus();
  await page.waitForTimeout(700);
  interactions.tooltipVisible = await page.evaluate(() => [...document.querySelectorAll('[role="tooltip"]')].some((t) => getComputedStyle(t).visibility === "visible"));
  // Toolbar arrows
  await page.getByRole("button", { name: "Move" }).focus();
  await page.keyboard.press("ArrowRight");
  interactions.toolbarArrow = await page.evaluate(() => document.activeElement.getAttribute("aria-label"));
  // scroll captures
  for (const [i, y] of [[1, 700], [2, 1400], [3, 2100]]) {
    await sc.evaluate((el, y) => el.scrollTo(0, y), y);
    await page.waitForTimeout(200);
    await page.screenshot({ path: `${out}/showcase-${i}.png` });
  }
}
console.log(JSON.stringify({ info, interactions, errors: errors.slice(0, 12) }, null, 1));
await browser.close();
