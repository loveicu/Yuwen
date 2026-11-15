// 网页版构建：把题库 JSON 注入模板，产出 docs/index.html（仓库内使用，路径相对仓库根）
import { readFileSync, writeFileSync } from 'fs';

const bankPath = 'app/src/main/assets/question_bank.json';
const tplPath = 'docs/template.html';
const outPath = 'docs/index.html';

const bank = JSON.parse(readFileSync(bankPath, 'utf8'));
const tpl = readFileSync(tplPath, 'utf8');

if (!tpl.includes('const BANK = /*__BANK__*/null;')) {
  console.error('模板缺少注入点 const BANK = /*__BANK__*/null;');
  process.exit(1);
}

// </ 转义为 <\/ 防止题目文本意外闭合 <script> 标签
const bankLiteral = JSON.stringify(bank).replace(/<\//g, '<\\/');
const html = tpl.replace('const BANK = /*__BANK__*/null;', 'const BANK = ' + bankLiteral + ';');

writeFileSync(outPath, html);
console.log('docs/index.html 构建完成:', (html.length / 1024).toFixed(1), 'KB，题库', bank.questions.length, '题 / 版本', bank.version);