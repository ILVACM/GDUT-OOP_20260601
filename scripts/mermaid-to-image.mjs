#!/usr/bin/env node
/**
 * Mermaid Diagram to Image Converter
 *
 * 使用 Playwright + 系统浏览器（Edge/Chrome）将 .mmd 文件渲染为 PNG/SVG 图片。
 * 避免 mermaid-cli (mmdc) 自动下载 Chromium 的问题。
 *
 * 用法:
 *   node scripts/mermaid-to-image.mjs input.mmd output.png
 *   node scripts/mermaid-to-image.mjs input.mmd output.svg
 *   node scripts/mermaid-to-image.mjs input.mmd output.png --theme dark
 *
 * @requires playwright (npm install playwright)
 * @requires 系统已安装 Microsoft Edge 或 Google Chrome
 */

import { chromium } from 'playwright';
import { readFileSync, writeFileSync, mkdirSync, unlinkSync, existsSync } from 'fs';
import { dirname, resolve, extname, basename } from 'path';
import { fileURLToPath } from 'url';
import os from 'os';
import { tmpdir } from 'os';
import { join } from 'path';

// ============================================================
// CLI 参数解析
// ============================================================

function parseArgs(argv) {
  const args = argv.slice(2); // skip node and script path
  const options = {
    inputFile: null,
    outputFile: null,
    theme: 'default',
    backgroundColor: '#ffffff',
    width: 1200,
    timeout: 30000,
  };

  for (let i = 0; i < args.length; i++) {
    switch (args[i]) {
      case '--theme':
        options.theme = args[++i] || 'default';
        break;
      case '--bg':
        options.backgroundColor = args[++i] || '#ffffff';
        break;
      case '--width':
        options.width = parseInt(args[++i], 10) || 1200;
        break;
      case '--timeout':
        options.timeout = parseInt(args[++i], 10) || 30000;
        break;
      case '--help':
      case '-h':
        printHelp();
        process.exit(0);
        break;
      default:
        if (!args[i].startsWith('-')) {
          if (!options.inputFile) {
            options.inputFile = resolve(args[i]);
          } else if (!options.outputFile) {
            options.outputFile = resolve(args[i]);
          }
        }
        break;
    }
  }

  if (!options.inputFile) {
    console.error('错误: 缺少输入文件路径\n');
    printHelp();
    process.exit(1);
  }

  if (!options.outputFile) {
    // 默认输出: 与输入同目录同名，扩展名为 .png
    const base = basename(options.inputFile, extname(options.inputFile));
    options.outputFile = resolve(dirname(options.inputFile), base + '.png');
  }

  return options;
}

function printHelp() {
  console.log(`
Mermaid Diagram to Image Converter

用法:
  node scripts/mermaid-to-image.mjs <input.mmd> [output.png|svg] [选项]

参数:
  input.mmd         Mermaid 源文件路径（必需）
  output.png|svg    输出文件路径（可选，默认与输入同目录同名 .png）

选项:
  --theme <name>    Mermaid 主题 (default|dark|forest|neutral|base)  [默认: default]
  --bg <color>      背景颜色  [默认: #ffffff]
  --width <px>      视口宽度  [默认: 1200]
  --timeout <ms>    渲染超时时间(毫秒)  [默认: 30000]
  -h, --help        显示帮助信息

示例:
  node scripts/mermaid-to-image.mjs diagram.mmd
  node scripts/mermaid-to-image.mjs diagram.mmd output.png
  node scripts/mermaid-to-image.mjs diagram.mmd output.svg --theme dark
  node scripts/mermaid-to-image.mjs er.mmd docs/er.png --width 1600
`);
}

// ============================================================
// 系统浏览器路径检测
// ============================================================

function findSystemBrowser() {
  const platform = os.platform();

  if (platform === 'win32') {
    const candidates = [
      // Microsoft Edge (最常见)
      'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
      'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
      // Google Chrome
      'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
      'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
      // 用户本地安装
      join(os.homedir(), 'AppData\\Local\\Microsoft\\Edge\\Application\\msedge.exe'),
      join(os.homedir(), 'AppData\\Local\\Google\\Chrome\\Application\\chrome.exe'),
    ];
    for (const p of candidates) {
      if (existsSync(p)) {
        console.log(`检测到系统浏览器: ${p}`);
        return p;
      }
    }
  }

  if (platform === 'darwin') {
    const candidates = [
      '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
      '/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge',
      '/Applications/Chromium.app/Contents/MacOS/Chromium',
    ];
    for (const p of candidates) {
      if (existsSync(p)) return p;
    }
  }

  if (platform === 'linux') {
    const candidates = [
      'google-chrome',
      'google-chrome-stable',
      'chromium-browser',
      'chromium',
      'microsoft-edge',
    ];
    for (const p of candidates) return p; // Linux 下直接返回命令名，由 PATH 解析
  }

  return null; // 使用 Playwright 默认 Chromium（可能需安装）
}

// ============================================================
// HTML 模板生成
// ============================================================

function generateHTML(mermaidCode, theme, backgroundColor) {
  // 转义模板字符串中的反引号和特殊字符
  const escapedCode = mermaidCode
    .replace(/\\/g, '\\\\')
    .replace(/`/g, '\\`')
    .replace(/\$\{/g, '\\${');

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Mermaid Diagram</title>
  <style>
    body {
      margin: 0;
      padding: 20px;
      background: ${backgroundColor};
      display: flex;
      justify-content: center;
    }
    #diagram {
      display: inline-block;
    }
  </style>
</head>
<body>
  <div id="diagram"></div>
  <script src="https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.min.js"><\/script>
  <script>
    mermaid.initialize({
      startOnLoad: false,
      theme: '${theme}',
      securityLevel: 'loose',
    });

    const diagramCode = \`${escapedCode}\`;

    mermaid.render('mermaid-diagram', diagramCode).then((result) => {
      document.getElementById('diagram').innerHTML = result.svg;
      window.__MERMAID_RENDERED = true;
    }).catch((err) => {
      document.getElementById('diagram').innerHTML = '<pre style="color:red">' +
        err.message + '</pre>';
      window.__MERMAID_ERROR = err.message;
    });
  <\/script>
</body>
</html>`;
}

// ============================================================
// 核心渲染逻辑
// ============================================================

async function renderMermaidToImage(options) {
  const { inputFile, outputFile, theme, backgroundColor, width, timeout } = options;

  // 1. 读取 Mermaid 源文件
  if (!existsSync(inputFile)) {
    console.error(`错误: 输入文件不存在: ${inputFile}`);
    process.exit(1);
  }

  const mermaidCode = readFileSync(inputFile, 'utf-8').trim();
  if (!mermaidCode) {
    console.error('错误: 输入文件为空');
    process.exit(1);
  }

  console.log(`输入文件: ${inputFile}`);
  console.log(`输出文件: ${outputFile}`);
  console.log(`主题: ${theme}`);

  // 2. 创建输出目录（如果不存在）
  const outputDir = dirname(outputFile);
  if (!existsSync(outputDir)) {
    mkdirSync(outputDir, { recursive: true });
    console.log(`创建输出目录: ${outputDir}`);
  }

  // 3. 生成临时 HTML 文件
  const tempHTML = join(tmpdir(), `mermaid-${Date.now()}.html`);
  const htmlContent = generateHTML(mermaidCode, theme, backgroundColor);
  writeFileSync(tempHTML, htmlContent, 'utf-8');
  console.log(`临时文件: ${tempHTML}`);

  let browser;
  try {
    // 4. 启动浏览器
    const executablePath = findSystemBrowser();
    const launchOptions = {
      headless: true,
      args: ['--no-sandbox', '--disable-setuid-sandbox'],
    };

    if (executablePath) {
      launchOptions.executablePath = executablePath;
    } else {
      console.warn('警告: 未检测到系统浏览器，尝试使用 Playwright 默认 Chromium');
      console.warn('如果失败，请安装 Edge/Chrome 或运行: npx playwright install chromium');
    }

    console.log('启动浏览器...');
    browser = await chromium.launch(launchOptions);

    const page = await browser.newPage({
      viewport: { width: width, height: 800 },
    });

    // 5. 加载临时 HTML
    const fileURL = `file:///${tempHTML.replace(/\\/g, '/')}`;
    console.log(`加载页面: ${fileURL}`);
    await page.goto(fileURL, { waitUntil: 'networkidle', timeout });

    // 6. 等待 Mermaid 渲染完成
    console.log('等待渲染...');
    const renderResult = await page.evaluate(() => {
      return new Promise((resolve) => {
        const check = () => {
          if (window.__MERMAID_RENDERED) resolve({ success: true });
          else if (window.__MERMAID_ERROR) resolve({ success: false, error: window.__MERMAID_ERROR });
          else setTimeout(check, 100);
        };
        check();
      });
    });

    if (!renderResult.success) {
      console.error(`Mermaid 渲染失败: ${renderResult.error}`);
      process.exit(1);
    }

    // 7. 根据输出格式截图
    const outputExt = extname(outputFile).toLowerCase();

    if (outputExt === '.svg') {
      // SVG 模式: 提取 SVG 内容
      console.log('导出 SVG...');
      const svgContent = await page.evaluate(() => {
        const svgEl = document.querySelector('#diagram svg');
        if (!svgEl) throw new Error('SVG element not found');
        return svgEl.outerHTML;
      });
      writeFileSync(outputFile, svgContent, 'utf-8');
    } else {
      // PNG 模式: 截图
      console.log('截图 PNG...');
      await page.screenshot({ path: outputFile, fullPage: true });
    }

    console.log(`成功: ${outputFile}`);

  } catch (err) {
    console.error(`渲染失败: ${err.message}`);
    process.exit(1);
  } finally {
    // 8. 清理
    if (browser) await browser.close();
    try { unlinkSync(tempHTML); } catch { /* 忽略清理失败 */ }
    console.log('已清理临时文件');
  }
}

// ============================================================
// 入口
// ============================================================

const options = parseArgs(process.argv);
renderMermaidToImage(options).catch((err) => {
  console.error(`未捕获错误: ${err.message}`);
  process.exit(1);
});
