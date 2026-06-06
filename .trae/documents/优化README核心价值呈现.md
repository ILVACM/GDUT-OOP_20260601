# 计划：优化 README.md 核心价值呈现（精简版）

## 当前状态分析

读取 `README.md` 后，聚焦以下 3 个核心问题：

1. **Slogan 过于泛化**（第 8 行）：`A modern English online learning platform built with Spring Boot 4 and Vue 3` 未体现"AI Agent 辅助开发"这一独特卖点
2. **Introduction 缺乏重点**（第 46-53 行）：三段纯文字平铺业务背景，读者无法快速理解项目的双重定位（业务系统 + AI 工作流实验）
3. **双重定位不突出**：AI Agent 辅助开发实验是本项目最大的开源价值，但在当前文档中被埋在 Introduction 第二段后半部分，极不醒目

## Proposed Changes

### 修改文件：`d:/GDUT-OOP_20260601/README.md`

---

### 改动 1：优化 Header Slogan

**位置**：第 8 行 `<em>` 标签内

**当前**：
```html
<em>A modern English online learning platform built with Spring Boot 4 and Vue 3</em>
```

**改为**：
```html
<em>English Learning Platform · Built with AI Agent Assistance · 73 Tests Passing</em>
```

**理由**：一句话传达三个关键信息——做什么（学习平台）、怎么做（AI Agent 辅助）、做得怎样（73 测试全过）。

---

### 改动 2：重写 Introduction 章节

**位置**：第 46-53 行

**当前**：三段纯文字，业务背景平铺，实验性质不突出。

**改为**：拆分为两个子章节，突出双重定位。

```markdown
## Introduction

### 📚 What is This?

An **English Online Learning System** built for the *Object-Oriented Software Design & Modeling* course at Guangdong University of Technology. The system covers the complete teaching workflow:

> **Question Bank** → **Exam Assembly** → **Online Testing** → **Score Analysis**

Three roles, one platform:

| Role    | Core Needs                                  | Solution Provided                        |
|---------|---------------------------------------------|------------------------------------------|
| 🧑‍🎓 Student | Take exams, review mistakes               | Online答题, 自动判分, 错题本, 成绩追踪     |
| 👨‍🏫 Teacher | Maintain questions, assemble exams, grade  | 5种题型CRUD, 手动/自动组卷, 多维统计报表  |
| 🛡️ Admin  | Manage users, oversee system               | 用户全生命周期管理, 全局数据看板           |

### 🧪 Why This Exists?

> **作业本身不难，但这次想玩点不一样的。**

Beyond the course requirements, this project serves as a **pilot validation of an AI Agent-assisted development workflow**. The core experiment:

> **Given complete domain understanding and thorough system design crafted by a human, how much of the actual coding implementation can a local AI Agent autonomously handle?**

From the first `Entity` class to the last `Service` method, from 73 passing unit tests to wiki documentation — the entire pipeline was executed with a local AI Agent (Trae CN) as the primary code generator, while the human remained the architect and decision-maker.

It's also a practice ground for essential engineering skills: technical documentation, GitHub management, Git version control, and cross-environment deployment.
```

**理由**：
- What is This? 用表格 + 引用块在 5 秒内传达 "什么系统 + 为谁服务 + 解决什么问题"
- Why This Exists? 将 AI 实验定位从后文提前到首屏，成为与业务定位并列的第二个核心叙事

---

### 改动 3（联动）：移除 Introduction 中重复的 AI 实验段落

**位置**：Introduction 中已有的 Vibe Coding 实验段落（如果存在）

**处理**：改动 2 已将实验定位整合到 Introduction 中，需确保后文 `Why Open Source?` 章节不再重复相同的实验描述，保持各有侧重。

---

## 影响范围

| 改动 | 行号范围 | 类型 |
|------|----------|------|
| Slogan | 第 8 行 | 替换 |
| Introduction | 第 46-53 行 | 替换（3 段 → 2 子章节） |

**不受影响的章节**：Features、Tech Stack、Architecture、Quick Start、Project Structure、Documentation、About the Author、Contributing & Acknowledgements、License

## Assumptions & Decisions

1. 不新增 "At a Glance" 独立章节（去掉原计划中的问题 3），将关键指标融入 Slogan 和 What is This? 表格中
2. 不修改徽章行（去掉原计划中的问题 5），保持现有 5 个徽章不变
3. Slogan 和 Introduction 是唯一修改区域，其他章节保持不动

## Verification

1. README.md 首屏应包含：Logo、标题、新 Slogan（突出 AI Agent + 73 Tests）
2. Introduction 的 What is This? 子章节应让读者在 5 秒内理解系统是什么、为谁服务
3. Introduction 的 Why This Exists? 子章节应清晰传达 AI Agent 工作流实验的核心定位
4. 所有 Markdown 语法正确，表格渲染正常
