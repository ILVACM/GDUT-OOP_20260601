# 优化 Mermaid 图转图片流程 Spec

## Why
在最近的 Mermaid ER 图转图片操作中，经历了多次失败尝试（mermaid-cli 因 Chromium 下载卡住、mermaid.ink API 返回 404），最终才通过 Playwright + Edge 方案成功。这种试错过程浪费了大量时间和 token，需要建立一套可靠的标准化流程。

## What Changes
- 建立 Mermaid 图转图片的标准化操作流程
- 记录各方案的依赖条件和失败场景
- 创建可复用的工具脚本，避免每次重新试错

## Impact
- Affected specs: 无（新增优化流程）
- Affected code: `temp/` 目录下可能新增工具脚本

## ADDED Requirements
### Requirement: Mermaid 图转图片标准流程
系统 SHALL 提供一套可靠的、可复用的 Mermaid 图转图片方案，能够在 Windows 环境下稳定生成 PNG/SVG 图片。

#### Scenario: 成功生成图片
- **WHEN** 用户提供 Mermaid 图代码和输出路径
- **THEN** 系统应能一步到位生成图片，无需多次试错

### Requirement: 方案选择优先级
系统 SHALL 按照以下优先级选择方案：
1. **首选**: Playwright + 本地浏览器（已验证可用）
2. **备选**: mmdc CLI（需预装 Chromium）
3. **不推荐**: 在线 API（如 mermaid.ink，接口不稳定）

## MODIFIED Requirements
### Requirement: 工具脚本复用
现有的 Playwright + HTML 渲染方案 SHALL 被封装为可复用脚本，包含：
- 自动创建输出目录
- 自动生成临时 HTML 文件
- 自动调用浏览器截图
- 支持 PNG 和 SVG 两种格式输出
