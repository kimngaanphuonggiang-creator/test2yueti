---
name: 跃题
description: 以 Material 3 Expressive 构建的轻快、专注的数学刷题体验。
colors:
  ink: "#171318"
  ink-soft: "#24182B"
  purple: "#7029D5"
  purple-bright: "#8B3DFF"
  lavender: "#D8C2FF"
  lavender-soft: "#F2E9FF"
  lime: "#D7FF72"
  lime-soft: "#EEFFC6"
  coral: "#FF6D9E"
  pink-soft: "#FFE7F0"
  paper: "#FFF8FC"
  white: "#FFFFFF"
  error: "#BA1A1A"
typography:
  display:
    fontFamily: "sans-serif"
    fontSize: "56sp"
    fontWeight: 900
    lineHeight: "60sp"
    letterSpacing: "-1.2sp"
  headline:
    fontFamily: "sans-serif"
    fontSize: "32sp"
    fontWeight: 700
    lineHeight: "39sp"
  title:
    fontFamily: "sans-serif"
    fontSize: "22sp"
    fontWeight: 700
    lineHeight: "28sp"
  body:
    fontFamily: "sans-serif"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "25sp"
  label:
    fontFamily: "sans-serif"
    fontSize: "15sp"
    fontWeight: 700
    lineHeight: "20sp"
rounded:
  extra-small: "8dp"
  small: "12dp"
  medium: "20dp"
  large: "28dp"
  extra-large: "36dp"
  full: "999dp"
spacing:
  compact: "8dp"
  small: "12dp"
  content: "20dp"
  section: "28dp"
components:
  button-primary:
    backgroundColor: "{colors.purple}"
    textColor: "{colors.white}"
    typography: "{typography.label}"
    rounded: "{rounded.full}"
    height: "56dp"
  button-start:
    backgroundColor: "{colors.lime}"
    textColor: "{colors.ink}"
    typography: "{typography.title}"
    rounded: "{rounded.large}"
    height: "76dp"
    width: "100%"
  card-content:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.ink}"
    rounded: "{rounded.large}"
    padding: "20dp"
  answer-selected:
    backgroundColor: "{colors.purple}"
    textColor: "{colors.white}"
    rounded: "{rounded.medium}"
    height: "56dp"
  navigation-floating:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.white}"
    rounded: "22dp"
    height: "76dp"
---

# Design System: 跃题

## Overview

**Creative North Star: "会回应的练习台"**

跃题把刷题过程设计成一个有节奏的练习场：墨黑舞台收束注意力，跃题紫承载常规主行动，青柠突出首页启程与进度，珊瑚标出需要回看的分支。界面的表达力来自鲜明字级、深色 Lottie hero、几何色块和有目的的动效，而不是装饰堆叠。

这是一个以完成任务为先的 Operate 界面。每屏只设一个最强视觉焦点，答题时保持空间连续性，提交后才揭示对错；全局由 `MaterialExpressiveTheme` 与 `MotionScheme.expressive()` 驱动，手机和平板分别使用 `ShortNavigationBar` 与可展开的 `WideNavigationRail`。

**Key Characteristics:**

- 高对比墨黑舞台与柔和纸面内容层。
- 紫色主动作、青柠进度、珊瑚提醒的稳定语义。
- 固定学习概览、占据首页主舞台的 Emotion Ball 与安静的练习入口；首页不再播放装饰性背景动画。
- 官方 Material 3 Expressive 组件承担导航、组合动作、工具栏、加载和反馈；紧凑屏、大字体与魅族设备使用同语义的稳定控件回退。
- 动效短促、可解释，并完整支持 reduced motion。

## Colors

色彩采用“深色舞台 + 柔纸内容 + 高饱和状态点”的结构，前置 YAML 令牌是唯一规范值。

### Primary

- **跃题紫（purple）：** 用于主按钮、当前答案、当前导航状态和关键交互，不用于大段阅读背景。
- **高光紫（purple-bright）：** 仅用于品牌几何和强调层，避免与主动作竞争。

### Secondary

- **珊瑚提醒（coral）：** 用于错题、需要关注的状态和少量装饰节拍。
- **柔粉提醒（pink-soft）：** 为错误相关内容提供低刺激容器。

### Tertiary

- **青柠进度（lime）：** 用于正确、完成、进度和首页全宽启程按钮。
- **柔青柠（lime-soft）：** 用于成功类低强调表面。

### Neutral

- **墨黑舞台（ink）：** 首页、导航和高专注场景的底色。
- **柔墨（ink-soft）：** 深色层级和次要深色表面。
- **柔薰衣草（lavender / lavender-soft）：** 选项、容器和轻量分组。
- **纸白（paper）：** 长文本、解析和答题内容表面。
- **纯白（white）：** 深色表面上的主要文字与图标。

**The One Loud Color Rule.** 同一屏只让一个高饱和角色成为主视觉；其余状态色退为小面积信号。

## Typography

**Display Font:** Android 系统无衬线体  
**Body Font:** Android 系统无衬线体

**Character:** 标题紧凑、粗壮、有冲劲；正文保持中性和舒适行高，避免数学内容因品牌化字形降低可读性。

### Hierarchy

- **Display：** 56sp、Black、60sp 行高，用于首页品牌词与关键结果。
- **Headline：** 32sp、Bold、39sp 行高，用于页面标题和结果结论。
- **Title：** 22sp、Bold、28sp 行高，用于卡片标题和题干层级。
- **Body：** 16sp、Regular、25sp 行高，用于题干、解析和说明。
- **Label：** 15sp、Bold、20sp 行高，用于按钮、选项和导航。

**The Math First Rule.** 数学题干和解析的清晰度优先于视觉个性；字号可随系统设置缩放，不使用固定高度截断文本。

## Layout

手机以约 20dp 页边距和 8dp 基础节奏组织内容；所有滚动页面为悬浮导航与系统栏预留安全区。宽屏在 720dp 起切换为 92dp 导航轨；首页改为最大 920dp 的双栏布局，左栏承载资料、累计数据和练习提示，右栏承载深色 hero 与题库选择。结果页最大 720dp，错题本最大 760dp，统计与答题页最大 840dp。130% 字体下，表单、卡片与题目允许换行或滚动，不以缩小文字换取空间。小于 600dp、font scale 大于 1.1 或魅族品牌设备不进入实验性组合控件布局，改用稳定的并排按钮与可换行筛选芯片，保持相同信息架构和触控语义。

## Elevation & Depth

系统以色面叠放、轮廓和留白建立层级，绝大多数卡片静止时无投影。悬浮底部导航与答题控制坞是例外，可使用 14dp 的结构性阴影来表达其位于内容上方；弹窗和 bottom sheet 沿用 Material 3 原生深度。

**The Flat-by-Default Rule.** 内容卡片默认平铺；只有真实浮在内容之上的导航、控制坞和模态层获得阴影。

## Shapes

8–12dp 用于小控件，20dp 用于选项与小容器，28–36dp 用于卡片、底部导航和模态表面。圆形保留给头像、计时、分数与答题卡题号；普通信息不能全部胶囊化。答题纸使用上缘 34dp 圆角衔接深色标题区，形成稳定的“试卷”轮廓。

## Components

### Buttons

- **Primary：** 跃题紫填充、白色粗标签、56dp 高，主要提交按钮占满内容宽度。
- **Home start：** 宽屏标准环境使用官方 Material 3 Expressive `SplitButton`；青柠主段负责开始或续答，尾段展开当前题库可用的真实题量并标记当前值。紧凑屏、大字体和魅族设备回退为稳定的青柠 `Button` + `Surface` 双段组合，保持 72dp 高、同一视觉层级和相同菜单行为；进行中的试卷禁用尾段，避免改变既有卷面。
- **Quick quantity：** 宽屏标准环境使用 `ButtonGroup` 提供快捷题量；兼容路径使用可换行的 `FilterChip`，从根源上避免实验性测量在厂商系统和大字体下发生约束冲突。
- **Secondary / Text：** 只用于上一步、返回或取消，不与主提交按钮争夺层级。
- **State：** 禁用状态仍需保留可辨文字；焦点、按压与水波反馈沿用 Material 3。

### Cards / Containers

- **Content card：** 纸白或语义容器色，28–36dp 圆角，常用 18–22dp 内边距。
- **Answer option：** 未选为柔薰衣草，选中为跃题紫；同时通过圆形单选标记和可访问语义表达状态。
- **Wrong record：** 保留题干、当时选择、正确答案、解析与时间；长按、右滑或更多菜单均只切换当前记录。允许同时置顶多题，置顶区按最近一次置顶时间倒序排列，取消置顶不影响其他记录。
- **Wrong-book entrance：** 仅前 6 张卡片在首次进入时一次性左右交替飞入，间隔 52ms，使用强调缓动；520ms 后关闭入场状态，滚动复用时不得重播。
- **Wrong-book reorder：** 卡片越过阈值后必须先完整回中，再写入置顶状态；数据库返回后只播放一次位置移动，不叠加淡入、缩放或首次飞入。卡片按下期间锁定父级分页手势，避免侧滑与整页切换互抢。

### Inputs / Fields

- 首次使用表单采用 Material 3 OutlinedTextField，最大宽度 480dp，姓名与学校均使用明确标签和前导图标。
- 单行字段不以占位文字替代标签；保存按钮仅在必填内容有效时可用。
- 资料保存结果通过全局 `Snackbar` 明确反馈：头像读取失败时保留既有头像并告知“其他资料已保存”，资料整体保存失败时提示重试；提示消费后必须从状态中清除，避免页面重组或重启时重复出现。
- 错题本使用 `AppBarWithSearch` 与新版 `TextFieldState` 搜索题目、答案或解析，`FilterChip` / `InputChip` 表达筛选与已启用关键词，`PullToRefreshBox` 提供本地刷新反馈。
- 错题卡用 `SwipeToDismissBox`：右滑置顶或取消置顶，左滑删除；`Snackbar` 提供状态提示与撤销。

### Navigation

- 手机使用“左侧图标导航 + 右侧独立工具方框”：四个目的地不显示可见标签，但完整保留语义与 Tooltip；工具方框以 Text-to-Lottie 加号/关闭形变打开扫描、函数图像与敬请期待。
- 宽屏使用可展开/收起的 `WideNavigationRail`，并延续同一颜色语义。
- 错题数量通过两种导航表面上的 `Badge` 同步显示，最大文案值为 99。
- 四个主页面由单一 `PagerState` 驱动的 `HorizontalPager` 承载，并预组合全部页面；内容区、底部导航横滑和导航点击共享同一页状态。错题页为避免手势竞争，卡片区只处理置顶/删除，标题区与底部导航处理整页横滑。
- 页面切换使用跟手位移和 Expressive 缓动；关闭系统动画时持续时间归零。

### Signature Components

- **首页互动机器人：** 本地受限 WebView 承载 Emotion Ball 32 种表情引擎；只加载 APK 内资源，禁止网络、文件访问与任意 URL 跳转。离开首页即停帧，魅族/reduced-motion 使用 lite 或静态模式。
- **离线扫描：** 拍照或相册导入后生成白底黑字页，支持多页删除和 PDF 分享；OpenCV 四 ABI 随 APK 打包并在进入工具时加载。
- **函数图像：** 原生 Compose Canvas 最多绘制 8 条 `y=f(x)`，支持缩放、平移、复位与白名单表达式；AI 生成入口在离线版明确禁用。

- **首页练习入口：** 位于机器人下方的静态深色练习容器，保留题库、题量、开始/继续和组合菜单，不再承载循环背景动画。
- **今日节奏：** 紧凑目标卡展示今日完成题数、10/20/30/50 每日目标和连续学习天数；`LinearWavyProgressIndicator` 表达进度，`SingleChoiceSegmentedButtonRow` 在 `ModalBottomSheet` 中设置目标。reduced motion 下波幅归零。
- **无界计时器：** 答题页右上角以不确定态环形动效包围累计用时，不显示轨道终点、不把秒数映射为完成进度；长按说明“仅记录累计用时，本练习不限时”。
- **答题卡：** Modal Bottom Sheet 内使用 5 列题号网格，区分已答、当前、未答与稍后检查；`FilterChip` 切换全部/未答/已标记，并提供“跳到下一道未答题”。每个题号同时提供“第 N 题”的 `contentDescription` 和当前题/已作答/未作答的 `stateDescription`。
- **题目翻页：** 独立 `HorizontalPager` 支持跟手左右切题；选择答案后保留 350ms 明确反馈再自动前进，最后一题自动打开答题卡。手动拖动可中断待执行自动翻页。
- **练习连续转场：** 顶层 `AnimatedContent` 以 520ms 容器缩放、背景色插值和 fade-through 连接首页、答题与成绩；答题卡先收拢，再进入提交形变与成绩揭示。共享 Lookahead 不包裹长列表，避免滚动时持续重测量。
- **答题工具栏：** `HorizontalFloatingToolbar` 集中上一题、答题卡进度与下一题，避让系统导航栏。
- **形变加载：** 开屏资源预热与整卷提交均使用新版 `LoadingIndicator`；开屏预解析首页 Lottie，最多等待 1.8 秒后进入主界面。reduced motion 下直接显示稳定终态。
- **成绩环：** 以环形分数、正确率和笑脸/哭脸 Lottie 汇总整卷结果。
- **结果快捷动作：** `FloatingActionButtonMenu` 提供复习错题、查看统计和返回首页。
- **趋势图：** 深色趋势卡内按日期绘制平滑三次贝塞尔曲线；无数据日期保留空白断点，选中日期使用 1dp 细导线与双层点标记，支持点按、`RichTooltip` 日期详情和无障碍前后日期操作。
- **统计范围：** `SegmentedButton` 在近 7 天和近 30 天间切换，`DateRangePicker` 提供自定义日期区间。

Motion 的 Compose 基线来自 `MaterialExpressiveTheme` 中的 `MotionScheme.expressive()`；跨 Compose/View 的补充令牌由 `MotionUtils` 从 Material 主题解析：`motionDurationShort3` 为 quick（回退 150ms）、`motionDurationMedium2` 为 standard（回退 300ms）、`motionDurationLong2` 为 expressive（回退 500ms），缓动使用 `motionEasingEmphasizedInterpolator`。主导航使用跟手 Pager；错题本前 6 项仅首次左右错峰进入，置顶只做位置移动。首页“跃迁阶梯”为 6 秒低对比透明循环，只在首页稳定可见时播放，离页即暂停但保留已预解析 composition；品牌揭示与形变预热一次播放，结果表情 1.2 秒单次播放。系统关闭动画时，循环冻结且状态动画直接显示终态。

## Do's and Don'ts

### Do:

- **Do** 让每屏只有一个最强视觉重点，并用颜色语义稳定区分行动、进度和错误。
- **Do** 在整卷提交后统一反馈分数、正确率与解析。
- **Do** 为悬浮导航、系统栏、深色主题、宽屏和 reduced motion 保留完整适配路径。
- **Do** 使用 Material 图标、语义和至少 48dp 的触控目标表达可操作性。
- **Do** 优先使用官方 Material 3 Expressive 组件承载组合动作、导航、加载、搜索、筛选和反馈状态。
- **Do** 在紧凑屏、大字体与魅族设备上保持同一视觉语言，但切换到稳定 Material 控件，稳定性优先于实验性组件覆盖率。
- **Do** 让资料与头像保存失败成为可见、可理解、可恢复的全局反馈，不让失败表现为空操作或闪退。

### Don't:

- **Don't** 在逐题作答后立即揭示对错。
- **Don't** 硬编码用户成绩、错题或统计趋势。
- **Don't** 用 emoji、渐变或重阴影代替正式图标与层级。
- **Don't** 让悬浮导航遮挡内容，或让系统导航栏产生断色。
- **Don't** 把所有卡片、筛选器和信息行都做成胶囊。
- **Don't** 把多个置顶压成互斥状态，或用错误时间决定置顶区顺序；最近置顶必须排在最前。
