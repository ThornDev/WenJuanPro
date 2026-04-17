# 下一步

## UX Expert 提示

WenJuanPro 是一款 Android 离线认知测评 App。请基于本 PRD 的「用户界面设计目标」与「Epics（特别是 Epic 2 / Epic 3 的 interaction 字段）」生成 `front-end-spec`，重点细化：

1. 测评集选择页、扫码页、欢迎确认页、单选/多选/记忆题作答页、完成页、续答提示页 7 个核心页面的 Compose 组件层次与状态机；
2. 8×8 点阵的精确视觉规格（蓝点直径 = 网格单元宽 × 0.7、命中半径 = 视觉半径 × 1.0、闪烁配色 #1976D2 → #FFB300、选中态绿 #4CAF50）；
3. 倒计时进度条的颜色阈值（>5s Primary 蓝，≤5s 橙）与状态过渡；
4. 分阶段模式题干↔选项切换的淡入淡出动画规格（200ms）；
5. 适配 5"-11" 屏幕（最小 360dp 宽）的 portrait 布局规则。

请使用 `*create-doc front-end-spec` 启动。

## Architect 提示

请基于本 PRD 的「技术假设」与全部 Epic 定义生成 `architecture`，重点细化：

1. **包结构与模块边界**：单 module `app/` 下的 feature 包划分（auth / scanner / config / question / memory / result / resume / common）；
2. **MVI 状态机设计**：每个核心页面的 State / Intent / Effect 定义；尤其是作答控制器的状态机（按题序推进、staged 模式两阶段切换、续答跳过逻辑）；
3. **TXT 解析器**：手写解析器 vs 引入 antlr/手写 parser 的取舍；schema 校验的错误聚合策略；
4. **结果文件 IO 原子性**：FileChannel + force(true) vs RandomAccessFile + sync 的实现选择；崩溃恢复测试方案；
5. **Compose 动画时序基准**：在目标中端机型（如 Redmi Pad SE 2026 等价物）上实测 1000ms 闪烁的抖动分布；若超过 ±50ms 阈值需提供降级方案；
6. **CameraX + ML Kit Barcode 集成**：低端机型 fallback 到 ZXing 的判定标准；
7. **Build / CI**：依赖白名单守门（Gradle 依赖审计任务）；ktlint + Detekt 配置；本地化打包脚本。

请使用 `*create-doc architecture` 启动。
