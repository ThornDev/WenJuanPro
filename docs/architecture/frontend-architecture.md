# 前端架构（Android UI）

> 说明：本节对应模板「Frontend Architecture」，在 WenJuanPro 语境下指 **Android Compose UI 层** 的组织与模式。

## 组件架构

### Composable 组织

```text
ui/
├── theme/
│   ├── Theme.kt                 # Material 3 默认浅色 + Primary=#1976D2 + Warning=#FB8C00
│   ├── Color.kt
│   └── Typography.kt            # 题干 22sp / 选项 18sp / Caption 14sp（front-end-spec 指定）
├── components/
│   ├── CountdownBar.kt          # 顶部进度条（Primary → 橙色阈值 5s）
│   ├── OptionCard.kt            # 选项卡片（文字 / 图片 / 混合三种形态）
│   ├── DotGrid.kt               # 8×8 网格容器
│   ├── DotCell.kt               # 单个点（空 / 蓝 / 闪烁黄 / 选中绿）
│   ├── AssessmentCard.kt        # 测评集卡片（有效/损坏 + 徽标）
│   ├── ErrorSheet.kt            # 损坏 config 错误详情 BottomSheet
│   └── HiddenLongPressArea.kt   # 右上角 5s 长按触发诊断页
└── screens/
    ├── permission/PermissionScreen.kt
    ├── configlist/ConfigListScreen.kt
    ├── scan/ScanScreen.kt
    ├── welcome/WelcomeScreen.kt
    ├── resume/ResumeScreen.kt
    ├── question/
    │   ├── QuestionScreen.kt              # Router：根据 Question 类型 + 阶段分发
    │   ├── SingleChoiceScreen.kt
    │   ├── MultiChoiceScreen.kt
    │   ├── MemoryQuestionScreen.kt        # 闪烁 + 复现阶段
    │   └── StagedQuestionScaffold.kt      # 分阶段模式的题干↔选项淡入淡出
    ├── complete/CompleteScreen.kt
    └── diagnostics/DiagnosticsScreen.kt
```

### Composable 模板

```kotlin
@Composable
fun QuestionScreen(
    viewModel: QuestionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val effects = viewModel.effects

    LaunchedEffect(Unit) {
        effects.collect { effect ->
            when (effect) {
                is Effect.NavigateNext -> /* NavController.navigate */
                is Effect.NavigateComplete -> /* ... */
                is Effect.ShowRetryFailure -> /* Toast / Dialog */
            }
        }
    }

    Scaffold(
        topBar = { CountdownBar(progress = state.countdownProgress, isWarning = state.isWarning) },
    ) { padding ->
        when (val q = state.current) {
            is QuestionUiState.Loading -> LoadingIndicator()
            is QuestionUiState.SingleChoiceAllInOne -> SingleChoiceScreen(q, viewModel::onIntent, padding)
            is QuestionUiState.SingleChoiceStaged -> StagedQuestionScaffold(q, viewModel::onIntent, padding)
            is QuestionUiState.MultiChoiceAllInOne -> MultiChoiceScreen(q, viewModel::onIntent, padding)
            is QuestionUiState.MultiChoiceStaged -> StagedQuestionScaffold(q, viewModel::onIntent, padding)
            is QuestionUiState.Memory -> MemoryQuestionScreen(q, viewModel::onIntent, padding)
            is QuestionUiState.Error -> ErrorView(q.message, onRetry = { viewModel.onIntent(Intent.Retry) })
        }
    }

    // 作答期间消费返回键（front-end-spec 返回键策略）
    BackHandler(enabled = true) { /* no-op */ }
}
```

## 状态管理架构

### 状态结构

```kotlin
sealed interface QuestionUiState {
    val countdownProgress: Float     // 0f..1f
    val isWarning: Boolean           // 剩余 ≤ 5s 时为 true

    data object Loading : QuestionUiState { /*...*/ }
    data class SingleChoiceAllInOne(/* stem, options, selectedIndex, submitEnabled, ... */) : QuestionUiState
    data class SingleChoiceStaged(val stage: Stage, /*...*/) : QuestionUiState
    data class MultiChoiceAllInOne(/* stem, options, selectedIndices, ... */) : QuestionUiState
    data class MultiChoiceStaged(val stage: Stage, /*...*/) : QuestionUiState
    data class Memory(val phase: MemoryPhase, /* dots, flashing, answers, ... */) : QuestionUiState
    data class Error(val message: String) : QuestionUiState

    enum class Stage { STEM, OPTIONS }
    sealed interface MemoryPhase {
        data object Idle : MemoryPhase                 // 进入后 500ms 静止
        data class Flashing(val currentIndex: Int, val flashingDot: Int) : MemoryPhase
        data object Recall : MemoryPhase
    }
}
```

### 状态管理模式

- **单一 StateFlow / ViewModel**：每个 Screen 对应一个 ViewModel，持有一个 `MutableStateFlow<UiState>`；UI 通过 `collectAsStateWithLifecycle()` 订阅
- **Intent → Reducer → State**：所有 UI 事件以 `sealed interface Intent` 发给 VM；VM 在内部 reducer 中 `_uiState.update { current -> newState }`
- **Effect 通道**：`Channel<Effect>` → `Flow<Effect>`；UI 侧用 `LaunchedEffect` 消费一次性效果（导航 / Toast）
- **savedStateHandle 只保存 `qid` / `configId`**：对 `Config` 本身不走 savedStateHandle（大对象），而是在 VM 启动时从 Repository 重新加载（解析一次性完成，缓存在进程内）
- **会话级状态**：放在 `@ActivityRetainedScoped` 的 `SessionState` Holder（Hilt 提供），跨 Screen ViewModel 共享；进程被杀后由续答流程恢复

## 路由架构

### 路由组织

```text
navHost (startDestination = "permission")
├── permission/
├── configlist/
├── scan?configId={configId}
├── welcome?studentId={studentId}&configId={configId}
├── resume?studentId={studentId}&configId={configId}&resultFile={resultFile}
├── question/{qid}              # NavBackStackEntry 读取 SessionState 获取 Config + Session
├── complete/
└── diagnostics/                # 隐藏入口（长按 5s）
```

### 受保护路由模式

所有路由（除 `permission`）在 `NavHost` 外层包裹 `PermissionGate`：若 `isExternalStorageManager() == false` 则强制回 `permission`。

```kotlin
@Composable
fun WenJuanProNavHost(navController: NavHostController) {
    val permViewModel: PermissionViewModel = hiltViewModel()
    val permState by permViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(permState) {
        if (!permState.granted && navController.currentDestination?.route != "permission") {
            navController.navigate("permission") { popUpTo(0) }
        }
    }

    NavHost(navController, startDestination = "permission") {
        composable("permission") { PermissionScreen() }
        composable("configlist") { ConfigListScreen() }
        // 作答/完成 Screen 消费返回键，详见 BackHandler
        composable("question/{qid}", arguments = listOf(navArgument("qid") { type = NavType.StringType })) {
            QuestionScreen()
        }
        // ... 其他
    }
}
```

## 前端服务层

> 在 WenJuanPro 语境下，"前端服务层" = Composable/VM 到 UseCase 的协程调用边界，不存在 HTTP Client。

### API 客户端配置

**N/A** —— 无 HTTP 客户端。VM 通过注入的 UseCase 调用 Repository；所有 IO 经 `Dispatchers.IO` 执行。

### 服务示例（VM 调用 UseCase 的典型形态）

```kotlin
@HiltViewModel
class QuestionViewModel @Inject constructor(
    private val sessionState: SessionStateHolder,
    private val appendResult: AppendResultUseCase,
    private val scoreSingle: ScoreSingleChoiceUseCase,
    private val scoreMulti: ScoreMultiChoiceUseCase,
    private val scoreMemory: ScoreMemoryUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuestionUiState>(QuestionUiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun onIntent(intent: Intent) {
        when (intent) {
            is Intent.Submit -> viewModelScope.launch {
                val record = when (val q = sessionState.currentQuestion()) {
                    is Question.SingleChoice -> scoreSingle(q, intent.answer)
                    is Question.MultiChoice -> scoreMulti(q, intent.answer)
                    is Question.Memory -> scoreMemory(q, intent.answer as MemoryAnswer)
                }
                withContext(ioDispatcher) { appendResult(record) }
                    .onSuccess { _effects.send(Effect.NavigateNext) }
                    .onFailure { _uiState.update { QuestionUiState.Error("数据保存失败，请联系研究员") } }
            }
            /* ... */
        }
    }
}
```

---
