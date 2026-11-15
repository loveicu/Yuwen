# 文萃刷题 · 网页版

- `index.html`：**构建产物，勿直接编辑**——由仓库根 `scripts/build_web.mjs` 把 `app/src/main/assets/question_bank.json` 注入 `template.html` 生成
- 改样式/逻辑 → 编辑 `template.html` → 本地 `node scripts/build_web.mjs` 重新生成
- 加题 → 直接改 `question_bank.json`（App 与网页共用同一数据源）→ 重跑构建脚本
- 线上地址：`https://loveicu.github.io/Yuwen/`（GitHub Pages，由 `.github/workflows/pages.yml` 自动部署）
