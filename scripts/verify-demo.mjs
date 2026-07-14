// Executes the :advanced demo bundle (public/js/main.js) inside jsdom and asserts
// the four wrapping patterns behave: codegen'd components (kebab props, sections,
// styles/classNames, polymorphic component=), the controlled-input shim, a hook
// (use-disclosure), and the imperative notifications API.
//
//   npx shadow-cljs release demo && node scripts/verify-demo.mjs
import { JSDOM } from 'jsdom';
import fs from 'node:fs';

const dom = new JSDOM('<!doctype html><html><body><div id="app"></div></body></html>', {
  url: 'http://localhost/',
  pretendToBeVisual: true,
  runScripts: 'outside-only',
});

const bundle = fs.readFileSync('public/js/main.js', 'utf8');
dom.window.eval(bundle);

const doc = dom.window.document;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function poll(desc, fn, timeout = 3000) {
  const start = Date.now();
  for (;;) {
    const v = fn();
    if (v) return v;
    if (Date.now() - start > timeout) throw new Error(`TIMEOUT waiting for: ${desc}`);
    await sleep(50);
  }
}

function assert(cond, desc) {
  if (!cond) throw new Error(`FAIL: ${desc}`);
  console.log(`ok — ${desc}`);
}

function setInputValue(input, value) {
  // React tracks the value setter; go through the native prototype setter
  const setter = Object.getOwnPropertyDescriptor(
    dom.window.HTMLInputElement.prototype, 'value').set;
  setter.call(input, value);
  input.dispatchEvent(new dom.window.Event('input', { bubbles: true }));
}

try {
  // -------------------------------------------------- 1. codegen'd components render
  const btn = await poll('#btn-notify rendered', () => doc.getElementById('btn-notify'));
  assert(btn.className.includes('mantine-Button-root'), 'Button has Mantine classes');
  assert(btn.className.includes('poc-btn') && btn.className.includes('poc-btn-root'),
    'classNames {:root [...]} space-joined onto root');
  assert(btn.style.fontWeight === '900', 'styles {:root {:font-weight 900}} -> inline style (kebab->camel)');
  assert(btn.style.getPropertyValue('--poc-var') === 'on', 'styles --* key passed verbatim');
  assert(btn.textContent.includes('🔔'), 'left-section content rendered (kebab -> leftSection)');

  const anchor = doc.getElementById('btn-anchor');
  assert(anchor.tagName === 'A' && anchor.getAttribute('href') === 'https://mantine.dev',
    'polymorphic :component "a" renders an anchor with href');

  const badges = doc.querySelectorAll('[id^="badge-"]');
  assert(badges.length === 3 && badges[0].tagName === 'SPAN',
    'children seq flattened; Badge polymorphic :component "span"');

  // -------------------------------------------------- 2. controlled input (shim)
  const input = doc.getElementById('input-name');
  assert(input.value === 'hello', 'controlled TextInput renders external :value');
  setInputValue(input, 'world');
  await poll('echo updates', () => doc.getElementById('input-echo').textContent === 'Echo: world');
  assert(input.value === 'world', 'typing flows shim -> onChange -> state -> back into input');

  // -------------------------------------------------- 3. hook: use-disclosure
  const toggle = doc.getElementById('btn-toggle');
  assert(toggle.textContent.includes('Show details'), 'disclosure starts closed');
  toggle.click();
  await poll('toggle label flips', () => toggle.textContent.includes('Hide details'));
  assert(true, 'use-disclosure tuple destructured; .toggle handler drives re-render');

  // -------------------------------------------------- 4. imperative notifications
  btn.click();
  const note = await poll('notification appears', () =>
    [...doc.querySelectorAll('[class*="mantine-Notification-root"], [role="alert"]')]
      .find((el) => el.textContent.includes('Sent from mantine.notifications/show')));
  assert(note.textContent.includes('It works'), 'notifications/show data converted (title rendered)');

  const styleTags = doc.querySelectorAll('style[data-mantine-styles]');
  assert(styleTags.length > 0, 'MantineProvider injected its style/CSS-variable tags');

  console.log('\nALL FOUR PATTERNS VERIFIED ✅');
  process.exit(0);
} catch (e) {
  console.error('\n' + e.message);
  process.exit(1);
}
