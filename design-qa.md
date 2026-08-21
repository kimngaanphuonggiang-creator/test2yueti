# 跃题 Design QA

## 对照基线

- 视觉真值：`D:\desktop\test2\design\yueti-material3-expressive-board.png`
- 模板真值：`C:\Users\wolf\.codex\skills\artifact-template-expressive\assets\reference.png`
- 手机视口：1080 × 2400 px / 420 dpi（约 411 × 914 dp）
- 平板视口：1600 × 2560 px / 320 dpi
- 对照方法：源文件为四屏概念板，不做失真的逐像素叠图；逐状态比较构图、颜色、形状、层级、内容与交互，并在同一轮审查中同时查看概念板和模拟器原始截图。

## 最终证据

- 首页：`.impeccable/review/phone-home-final.png`
- 答题初始态：`.impeccable/review/phone-question-final.png`
- 已选 B / 已收藏：`.impeccable/review/phone-question-selected-final.png`
- 答对反馈与 Android Lottie：`.impeccable/review/phone-feedback-final.png`
- 答对结果（1 / 1）：`.impeccable/review/phone-results-correct-final.png`
- 答错反馈：`.impeccable/review/phone-feedback-wrong-final.png`
- 答错结果（0 / 1）：`.impeccable/review/phone-results-wrong-final.png`
- 错题复习：`.impeccable/review/phone-wrong-review-final.png`
- 标记掌握后的真实空态：`.impeccable/review/phone-wrong-empty-final.png`
- 深色主题 + 130% 字体：`.impeccable/review/phone-question-dark-font130-final.png`
- 平板宽屏：`.impeccable/review/tablet-home-final.png`
- 关闭系统动画的完整操作录像：`.impeccable/review/reduced-motion-final.mp4`
- 官方 Skottie 播放器终帧：`.impeccable/review/lottie-skottie-final.png`

## 可见一致性

- 首页保持参考的黑色世界、超大标题、薰衣草任务卡、紫色圆形主行动、青柠箭头、紫红科目条与底部导航层级。
- 答题页保持紫色顶区、青柠进度、浅色内容面、药丸选项、强选中反馈与黑色底部控制面；深色模式改用 Material 语义色后仍保持层级与对比。
- 反馈页以 Lottie 的薰衣草弹性徽章、紫色对勾与珊瑚彩屑呈现答对动效，解释卡直接显示公式 `x = -b / (2a)`。
- 结果与错题路径完全由本次 1 道示例题驱动：答对显示 1/1，答错显示 0/1；错题清理后进入真实空态，没有硬编码成绩或虚假错因。
- 平板使用 `NavigationRail` 和受控内容宽度；手机使用底部导航。130% 字体无裁切，核心动作仍在首屏可达。

## 交互与无障碍检查

- 核心流程全部可操作：首页 → 选项 → 收藏 → 提交 → 反馈 → 结果 → 错题复习 → 标记掌握。
- 返回行为按来源返回，不再把错题复习错误地送回答题页。
- 题目、反馈和结果使用标题/页面语义；反馈使用 live region；收藏暴露状态描述；图标按钮均有内容描述。
- 状态使用 `rememberSaveable` 与显式 Saver，配置变化后保留阶段、选择、提交、收藏和错题状态。
- 当 `ValueAnimator.areAnimatorsEnabled()` 为 false 时，页面转场使用 `EnterTransition.None` / `ExitTransition.None`，Lottie 直接显示终帧且不循环；录像在系统 `animator_duration_scale = 0` 下采集。

## 动画验证

- 动画 JSON：`app/src/main/res/raw/answer_correct.json`
- 规格：512 × 512、60 fps、72 帧、透明背景、单次播放。
- 仅使用 Skottie/Lottie 支持的矢量形状、填充、描边与变换。
- 同一份 JSON 已在官方 Skottie 播放器和 Lottie Android 6.7.1 中验证；终帧的颜色、透明背景、对勾和彩屑一致。

## 比对与修正历史

1. 首轮视觉对照发现首页圆形 CTA 被弱化、答题页缺少底部承载面、选中单选标记对比不足；随后恢复圆形结构、黑色底部控制面与明确选中态。
2. 首轮独立审查发现成绩/错题数据硬编码、部分控件无真实行为、深色主题未完全生效、返回与状态恢复不可靠；已改为单题诚实模型、真实收藏/错题动作、Material 语义色、`rememberSaveable` 状态和来源感知返回。
3. 补齐自适应启动图标、无动画分支、live region、状态描述、正确数学公式，以及正确/错误两条全流程证据。
4. 第二轮独立审查发现重复点击当前“错题本”会覆盖返回来源；导航模型现对同目的地重选保持原状态，并新增回归测试，确保返回结果页。

## 剩余非阻塞项

- [P3] 尚未在开启 TalkBack 的实体手机上完成人工逐焦点朗读；当前语义树与内容描述已具备发布前测试基础。
- [P3] 平板骨架采用单主栏 + 导航轨；接入真实科目与进度模块后可自然扩展双栏。

最终结果：通过。最终独立发布闸门结论为 `CLEARED`，无残留 P0/P1/P2。
