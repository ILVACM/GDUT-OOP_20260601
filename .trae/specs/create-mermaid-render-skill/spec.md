# 创建 Mermaid 渲染 Skill Spec

## Why
用户希望将 Mermaid 图转图片的能力封装为可复用的 Skill，归档到 `temp` 路径的独立子文件夹中，以便在任何项目中都能被 agent 自动识别和调用，实现稳定可靠的批量化操作。

## What Changes
- 在 `temp/mermaid-render-skill/` 下创建完整的 Skill 文件结构
- 创建 `SKILL.md` 定义 Skill 的触发条件和使用方式
- 包含 `mermaid-to-image.mjs` 脚本作为核心执行组件
- 添加 `README.md` 说明如何在其他项目中部署和配置

## Impact
- Affected specs: optimize-mermaid-to-image（本 spec 是其延续）
- Affected code: 无，新增 Skill 文件

## ADDED Requirements
### Requirement: Mermaid 渲染 Skill
系统 SHALL 在 `temp/mermaid-render-skill/` 下提供完整的 Skill 包，包含：
1. `SKILL.md` — Skill 定义文件（frontmatter + 使用说明）
2. `scripts/mermaid-to-image.mjs` — 核心渲染脚本
3. `README.md` — 部署指南

#### Scenario: Agent 自动调用
- **WHEN** 用户要求将 Mermaid 代码转为图片
- **THEN** agent 自动识别 mermaid-render skill 并调用脚本渲染

#### Scenario: 跨项目部署
- **WHEN** 用户在新项目中需要该能力
- **THEN** 只需复制 `temp/mermaid-render-skill/` 到目标项目并按 README 配置

## MODIFIED Requirements
### Requirement: Skill 存放位置
Skill 文件 SHALL 归档在 `temp/mermaid-render-skill/` 而非标准的 `.trae/skills/` 路径，用户明确要求此归档位置。
