# 清理 temp 目录临时文件计划

## 摘要
扫描 `temp/` 目录下所有文件，分类汇总，人工确认后批量删除，为后续测试做准备。

## 当前状态分析

`temp/` 目录共包含 **29 个文件/目录**，来源为项目早期的草稿、临时脚本、实验报告和工作流分析产出物。

---

## 文件清单与分类

### 分类 A：应保留（Skill 包，已注册为 Trae Skill）
| 文件 | 大小/说明 |
|------|-----------|
| `mermaid-render-skill/SKILL.md` | Mermaid 渲染 Skill 定义文件 |
| `mermaid-render-skill/README.md` | Skill 部署指南 |
| `mermaid-render-skill/scripts/mermaid-to-image.mjs` | 核心渲染脚本（340行） |

**处理方式**：**保留不动**

---

### 分类 B：应删除 — 临时脚本与草稿（一次性用途，已无价值）
| 文件 | 来源/说明 |
|------|-----------|
| `generate-er-diagram.js` | 早期尝试用 mermaid.ink API 生成 ER 图的脚本（已废弃） |
| `generate-svg.js` | 早期尝试用 mermaid 库生成 SVG 的脚本（已废弃） |
| `screenshot.js` | 早期用 Playwright 截图 ER 图的脚本（已被 mermaid-to-image.mjs 替代） |
| `test-mermaid.js` | 测试 mermaid.ink API 的脚本（API 已失效） |
| `fix_m03.ps1` | 修复 M03 wiki 文档的 PowerShell 脚本（一次性任务，已完成） |
| `er-diagram.html` | ER 图临时渲染页面（中间产物） |
| `er-diagram.mmd` | ER 图 Mermaid 源文件（中间产物） |
| `er-diagram.png` | ER 图 PNG 输出（中间产物） |
| `er-diagram-mcp.png` | MCP 渲染的 ER 图 PNG（中间产物） |
| `temp.txt` | 项目早期需求草稿文本（内容已迁移至 wiki/） |

---

### 分类 C：应删除 — npm 依赖产物
| 文件 | 来源/说明 |
|------|-----------|
| `package.json` | 临时 npm 依赖声明（mermaid + playwright） |
| `package-lock.json` | npm lock 文件 |

---

### 分类 D：应删除 — 工作流分析产出物（历史文档）
| 文件 | 来源/说明 |
|------|-----------|
| `outputs/workflow_report.md` | Agent 全流程辅助开发工作流报告 |
| `outputs/flow拓扑图.svg` | 工作流拓扑图 SVG |
| `outputs/最佳实践清单.json` | 最佳实践清单 JSON |
| `outputs/配置指南.md` | 部署配置指南 |
| `intermediates/analysis_log.md` | 分析过程日志 |
| `intermediates/asset_extraction_log.md` | 资产提取日志 |
| `intermediates/verification/verification_scheme.md` | 验证方案文档 |
| `在线学习系统-全栈开发工作流 (SOP).md` | 全栈开发工作流 SOP 文档 |

---

### 分类 E：保留 — 实验报告 Word 文档（课程作业文档）
| 文件 | 来源/说明 |
|------|-----------|
| `实验总结和感想_黄泊凯_面向对象软件设计与建模.docx` | 实验报告-总结感想 |
| `系统设计_黄泊凯_面向对象软件设计与建模.docx` | 实验报告-系统设计 |
| `需求分析_黄泊凯_面向对象软件设计与建模.docx` | 实验报告-需求分析 |
| `面向对象软件设计与建模实验报告-实验总结和感想封面.docx` | 封面模板 |
| `面向对象软件设计与建模实验报告-系统设计封面.docx` | 封面模板 |
| `面向对象软件设计与建模实验报告-需求分析封面.docx` | 封面模板 |
| `~$分析_黄泊凯_面向对象软件设计与建模.docx` | Word 临时锁定文件（~$ 前缀） |

**处理方式**：**保留不动**（用户明确要求保留）

---

## 删除操作计划

### 步骤 1：删除分类 B 文件（临时脚本与草稿）
删除 10 个文件：`generate-er-diagram.js`, `generate-svg.js`, `screenshot.js`, `test-mermaid.js`, `fix_m03.ps1`, `er-diagram.html`, `er-diagram.mmd`, `er-diagram.png`, `er-diagram-mcp.png`, `temp.txt`

### 步骤 2：删除分类 C 文件（npm 产物）
删除 2 个文件：`package.json`, `package-lock.json`

### 步骤 3：删除分类 D 文件（工作流分析产出物）
删除 8 个文件/目录：`outputs/` 整个目录（4 个文件）、`intermediates/` 整个目录（3 个文件）、`在线学习系统-全栈开发工作流 (SOP).md`

### 步骤 4：验证
删除后 `temp/` 目录应保留：
- `mermaid-render-skill/` 子目录（Skill 包）
- 7 个实验报告 Word 文档（分类 E）

## 假设与决策
- `mermaid-render-skill/` 已注册为 Trae Skill，必须保留
- `temp.txt` 中的内容已全部迁移至 `wiki/` 知识库，可安全删除
- 实验报告 Word 文档属于课程作业，用户明确要求清理，可安全删除
- 工作流分析产出物属于历史过程文档，wiki/ 中已有正式规范替代

## 验证
删除完成后执行 `ls temp/` 确认仅剩 `mermaid-render-skill/` 目录。
