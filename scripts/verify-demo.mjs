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

// jsdom has no ResizeObserver / Element.scrollIntoView; Spotlight (ScrollArea +
// keyboard action selection) needs both to render and to toggle.
dom.window.ResizeObserver = class {
  observe() {}
  unobserve() {}
  disconnect() {}
};
dom.window.Element.prototype.scrollIntoView = function () {};

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

  const badges = await poll('badges rendered', () => {
    const b = doc.querySelectorAll('[id^="badge-"]');
    return b.length === 3 ? b : null;
  });
  assert(badges[0].tagName === 'SPAN',
    'children seq flattened; Badge polymorphic :component "span"');

  // -------------------------------------------------- 2. controlled input (shim)
  const input = doc.getElementById('input-name');
  assert(input.value === 'hello', 'controlled TextInput renders external :value');
  setInputValue(input, 'world');
  await poll('echo updates', () => doc.getElementById('input-echo').textContent === 'Echo: world');
  assert(input.value === 'world', 'typing flows shim -> onChange -> state -> back into input');

  // -------------------------------------------------- 2b. widened core surface
  const alert = doc.getElementById('alert');
  assert(alert.className.includes('mantine-Alert-root') && alert.textContent.includes('Widened core coverage'),
    'newly-generated mc/alert renders with Mantine classes');
  const anchorEl = doc.getElementById('anchor');
  assert(anchorEl.tagName === 'A' && anchorEl.className.includes('mantine-Anchor-root'),
    'newly-generated mc/anchor renders an <a> with Mantine classes');
  const kbd = doc.getElementById('kbd');
  assert(kbd.tagName === 'KBD' && kbd.textContent.includes('Ctrl'),
    'newly-generated mc/kbd renders a <kbd>');

  // -------------------------------------------------- 2c. newly-curated controlled input
  const select = doc.getElementById('fruit-select');
  assert(select.tagName === 'SELECT' && select.value === 'apple',
    'controlled NativeSelect renders external :value');
  const selectSetter = Object.getOwnPropertyDescriptor(
    dom.window.HTMLSelectElement.prototype, 'value').set;
  selectSetter.call(select, 'cherry');
  select.dispatchEvent(new dom.window.Event('change', { bubbles: true }));
  await poll('fruit echo updates', () => doc.getElementById('fruit-echo').textContent === 'Fruit: cherry');
  assert(select.value === 'cherry', 'NativeSelect change flows shim -> onChange -> state -> back (shape-agnostic value read)');

  // -------------------------------------------------- 3. hook: use-disclosure
  const toggle = doc.getElementById('btn-toggle');
  const collapse = doc.getElementById('collapse');
  assert(toggle.textContent.includes('Show details'), 'disclosure starts closed');
  assert(collapse.getAttribute('aria-hidden') === 'true',
    'Collapse starts hidden (:expanded false actually reaches the component)');
  toggle.click();
  await poll('toggle label flips', () => toggle.textContent.includes('Hide details'));
  await poll('collapse becomes visible', () => collapse.getAttribute('aria-hidden') === 'false');
  assert(true, 'use-disclosure tuple destructured; .toggle drives Collapse :expanded');

  // -------------------------------------------------- 3b. hooks return-shape split
  // tuple: use-counter -> [count, handlers]; count destructured positionally, handler
  // object method (.increment) drives a re-render.
  const countBtn = doc.getElementById('btn-count');
  assert(countBtn.textContent === 'Count: 5', 'use-counter tuple: count destructured positionally (initial 5)');
  countBtn.click();
  await poll('count increments', () => countBtn.textContent === 'Count: 6');
  assert(countBtn.textContent === 'Count: 6', 'use-counter handlers object .increment re-renders (raw JS return)');

  // object: use-viewport-size -> {width, height} read via ^js interop.
  const viewport = doc.getElementById('viewport-size');
  const vpMatch = viewport.textContent.match(/^Viewport: (\d+)x(\d+)$/);
  assert(vpMatch && Number(vpMatch[1]) > 0 && Number(vpMatch[2]) > 0,
    'use-viewport-size object read via ^js interop (width/height > 0)');

  // scalar: use-id -> non-empty string.
  const genId = doc.getElementById('generated-id');
  assert(/^ID: .+/.test(genId.textContent) && genId.textContent !== 'ID: ',
    'use-id scalar return rendered (non-empty string id)');

  // -------------------------------------------------- 4. imperative notifications
  btn.click();
  const note = await poll('notification appears', () =>
    [...doc.querySelectorAll('[class*="mantine-Notification-root"], [role="alert"]')]
      .find((el) => el.textContent.includes('Sent from mantine.notifications/show')));
  assert(note.textContent.includes('It works'), 'notifications/show data converted (title rendered)');

  const styleTags = doc.querySelectorAll('style[data-mantine-styles]');
  assert(styleTags.length > 0, 'MantineProvider injected its style/CSS-variable tags');

  // -------------------------------------------------- 5. @mantine/dates + @mantine/charts
  // Selector sets for the stylesheets public/index.html links (core first, packages after).
  const pkgSelectors = (pkg) =>
    new Set((fs.readFileSync(`node_modules/@mantine/${pkg}/styles.css`, 'utf8')
      .match(/\.m_[0-9a-f]+/g) || []).map((s) => s.slice(1)));
  const linked = ['core', 'notifications', 'spotlight', 'dates', 'charts'];
  const perPkg = Object.fromEntries(linked.map((p) => [p, pkgSelectors(p)]));
  const linkedUnion = new Set(linked.flatMap((p) => [...perPkg[p]]));
  const hashedClasses = (root) => {
    const out = new Set();
    (function walk(n) {
      if (n.classList) n.classList.forEach((c) => { if (/^m_[0-9a-f]+$/.test(c)) out.add(c); });
      for (const c of n.children) walk(c);
    })(root);
    return [...out];
  };

  const datePicker = doc.getElementById('date-picker');
  assert(datePicker && datePicker.querySelector('[class*="mantine-DatePicker-"]'),
    'mantine.dates/date-picker renders with Mantine classes');
  const dateClasses = hashedClasses(datePicker);
  assert(dateClasses.every((c) => linkedUnion.has(c)),
    'every hashed class in the DatePicker subtree pairs with a linked stylesheet selector');
  assert(dateClasses.some((c) => perPkg.dates.has(c)),
    'DatePicker subtree uses selectors defined in @mantine/dates/styles.css (dates CSS paired)');

  // controlled DatePicker end-to-end: external :value selects the day, clicking another
  // day flows through the shim -> onChange -> state -> back into :value.
  assert(doc.getElementById('date-echo').textContent === 'Date: 2026-07-14',
    'controlled DatePicker renders external :value (day 14 selected)');
  const days = [...datePicker.querySelectorAll('.mantine-DatePicker-day')];
  const target = days.find((d) => d.getAttribute('data-selected') !== 'true'
    && d.getAttribute('data-outside') !== 'true' && !d.disabled);
  const targetDay = target.textContent;
  const echoBefore = doc.getElementById('date-echo').textContent;
  target.click();
  await poll('date echo updates', () => doc.getElementById('date-echo').textContent !== echoBefore);
  assert([...datePicker.querySelectorAll('.mantine-DatePicker-day')]
    .find((d) => d.getAttribute('data-selected') === 'true').textContent === targetDay,
    'clicking a day flows shim -> onChange -> state -> back into DatePicker :value');

  const lineChart = doc.getElementById('line-chart');
  assert(lineChart && lineChart.className.includes('mantine-LineChart-'),
    'mantine.charts/line-chart renders with Mantine classes');
  const chartClasses = hashedClasses(lineChart);
  assert(chartClasses.every((c) => linkedUnion.has(c)),
    'every hashed class in the LineChart subtree pairs with a linked stylesheet selector');
  assert(chartClasses.some((c) => perPkg.charts.has(c)),
    'LineChart subtree uses selectors defined in @mantine/charts/styles.css (charts CSS paired)');

  // -------------------------------------------------- 6. imperative modals + spotlight
  // modals: open drives the ModalsProvider (converted :title + :children), close by id.
  doc.getElementById('btn-open-modal').click();
  const modalBody = await poll('modal opens', () => {
    const el = doc.getElementById('modal-body');
    return el && el.textContent.includes('Imperative modal body') ? el : null;
  });
  assert(!!modalBody, 'mantine.modals/open renders the modal (converted :title + :children)');
  assert([...doc.querySelectorAll('[class*="mantine-Modal-title"]')]
    .some((el) => el.textContent.includes('Demo modal')),
    'modal title from converted :title prop rendered');
  doc.getElementById('btn-close-modal').click();
  await poll('modal closes', () => !doc.getElementById('modal-body'));
  assert(!doc.getElementById('modal-body'), 'mantine.modals/close (by raw id) removes the modal');

  // spotlight: its component is the UI; toggle opens it (actions rendered), toggle again closes.
  doc.getElementById('btn-toggle-spotlight').click();
  const spotlightAction = await poll('spotlight opens', () =>
    [...doc.querySelectorAll('[class*="mantine-Spotlight-"]')]
      .find((el) => el.textContent.includes('First action')));
  assert(!!spotlightAction, 'mantine.spotlight/toggle opens the Spotlight (actions prop rendered)');
  const spotUsed = hashedClasses(doc.body).filter((c) => perPkg.spotlight.has(c));
  assert(spotUsed.length > 0,
    'opened Spotlight paints selectors from @mantine/spotlight/styles.css (stylesheet linked after core)');
  doc.getElementById('btn-toggle-spotlight').click();
  await poll('spotlight closes', () =>
    ![...doc.querySelectorAll('[class*="mantine-Spotlight-"]')]
      .some((el) => el.textContent.includes('First action')));
  assert(true, 'mantine.spotlight/toggle closes the Spotlight');

  console.log('\nALL PATTERNS + DATES/CHARTS + MODALS/SPOTLIGHT VERIFIED ✅');
  process.exit(0);
} catch (e) {
  console.error('\n' + e.message);
  process.exit(1);
}
