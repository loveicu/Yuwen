# WenCuiQuiz 文萃刷题

一款 **液态玻璃风格** 的语文文化常识刷题 Android 应用。

## ✨ 特性

- 🪟 **液态玻璃 UI**：全部卡片 / 按钮采用实时背景模糊 + 边缘折射 + 高光描边，
  背景光晕缓慢流动时玻璃里的倒影也跟着流动
- 🎨 **Compose + Material 3**：青花蓝 × 宣纸米色 × 朱砂红的文化主题配色，
  深浅色模式自适应
- 📚 **60 题内置题库**：13 个分类（科举制度、天文历法、文学典籍、历史典故……）
- 🎯 **完整刷题闭环**：随机练习 / 分类练习 / 答题解析 / 结算页 / 错题本重练
- 💾 **学习进度持久化**：累计刷题数、正确率、错题自动记录
- 🌊 **流畅动画**：切题滑动动画、按压缩放反馈，全部走 GPU 管线

## 🛠️ 技术栈

| 组件 | 说明 |
|---|---|
| UI 框架 | Jetpack Compose + Material 3 |
| 液态玻璃 | [Kyant0 Backdrop](https://github.com/Kyant0/AndroidLiquidGlass) 2.0.0（源码内嵌于 `third-party/kyant-backdrop`） |
| 语言 | Kotlin 2.3.10 |
| 构建 | Gradle 9.6.1 + AGP 9.2.1 |
| 最低支持 | Android 8.0（API 26） |
| 数据 | kotlinx.serialization + SharedPreferences |

## 📦 云端构建（GitHub Actions）

推送到 GitHub 后 Actions 自动构建 APK：

1. Push 代码到 `main` 分支
2. Actions 页面查看 `Build APK` 工作流
3. 构建完成后在 Artifacts 下载 `WenCuiQuiz-apk`
4. 打 tag（如 `v1.0.0`）推送会同时发布到 Releases

> 下载加速：可在下载链接前加 `https://gh-proxy.org/` 前缀

## 📁 项目结构

```
app/src/main/java/com/wencui/quiz/
├── MainActivity.kt          # 入口 + 页面路由 + 玻璃背景宿主
├── QuizViewModel.kt         # 全局状态（会话/进度/错题）
├── QuizPrefs.kt             # 进度持久化
├── data/
│   ├── QuizQuestion.kt      # 题目模型
│   └── QuizRepository.kt    # 题库加载与组卷
└── ui/
    ├── glass/               # 液态玻璃核心
    │   ├── GlassBackdropHost.kt   # layerBackdrop 录制层
    │   └── GlassPanel.kt          # glassPanel 修饰符（blur+vibrancy+lens）
    ├── components/
    │   ├── AuroraBackground.kt    # 流动光晕背景画布
    │   └── GlassButton.kt         # 按压弹性玻璃容器
    ├── screens/             # 四个页面
    └── theme/               # 主题配色
```

## 🙏 致谢

- [xiaomanjun233/SleepDown-Schedule](https://github.com/xiaomanjun233/SleepDown-Schedule) ——
  液态玻璃实现方式参考（kyant-backdrop 源码移植自该项目）
- [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) —— Backdrop 库原作者（Apache-2.0）
- [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix) —— Miuix 设计组件

## 📄 许可

仅供学习交流使用。液态玻璃部分遵循 Apache-2.0（见 THIRD_PARTY 出处）。
