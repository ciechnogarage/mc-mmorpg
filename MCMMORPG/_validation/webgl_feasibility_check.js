const puppeteer = require('puppeteer');
const path = require('path');

async function main() {
  const browser = await puppeteer.launch({
    headless: 'new',
    args: ['--no-sandbox', '--use-gl=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'],
  });
  const page = await browser.newPage();
  page.on('console', (m) => console.log('PAGE:', m.text()));
  page.on('pageerror', (e) => console.log('PAGEERROR:', e.message));
  await page.goto('file://' + path.join(__dirname, 'webgl_feasibility_check.html'));
  await new Promise((r) => setTimeout(r, 500));
  const result = await page.evaluate(() => window.probeResult);
  console.log('RESULT:', result);
  await page.screenshot({ path: path.join(__dirname, 'webgl_feasibility_check.png') });
  await browser.close();
}
main();
