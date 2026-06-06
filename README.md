<p align="center">
  <img src="Data/Vue/logo.png" alt="Online Learning System Logo" width="180" />
</p>

<h1 align="center">Online Learning System / 在线学习系统</h1>

<p align="center">
  <em>A modern English online learning platform built with Spring Boot 4 and Vue 3</em>
</p>

<p align="center">
  <a href="https://github.com/topics/spring-boot">
    <img src="https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen.svg" alt="Spring Boot" />
  </a>
  <a href="https://github.com/topics/vue">
    <img src="https://img.shields.io/badge/Vue-3.x-blue.svg" alt="Vue 3" />
  </a>
  <a href="https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html">
    <img src="https://img.shields.io/badge/JDK-21-orange.svg" alt="JDK 21" />
  </a>
  <a href="https://www.sqlite.org/">
    <img src="https://img.shields.io/badge/Database-SQLite-lightgrey.svg" alt="SQLite" />
  </a>
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License" />
  </a>
</p>

***

## Table of Contents

- [Introduction](#-introduction)
- [Features](#-features)
- [Tech Stack](#️-tech-stack)
- [Architecture](#️-architecture)
- [Quick Start](#-quick-start)
- [Project Structure](#-project-structure)
- [Documentation](#-documentation)
- [About the Author](#-about-the-author)
- [Contributing & Acknowledgements](#-contributing--acknowledgements)
- [License](#-license)

***

## Introduction

This is an **English Online Learning System** developed as the final project for the *Object-Oriented Software Design & Modeling* course. The system streamlines the entire teaching workflow — from question bank management, exam assembly, online testing, to score analysis — providing a unified platform for students, teachers, and administrators.

In traditional English teaching, instructors face the overhead of maintaining question banks, manually composing exam papers, organizing timed tests, and analyzing student performance. Students, on the other hand, lack convenient tools for online practice and personalized mistake tracking. This system bridges that gap by offering a complete, end-to-end solution.

The system supports **three distinct roles** (Student / Teacher / Admin), each with tailored functionality views and permission boundaries. All data is stored in a structured SQLite database, ensuring historical exams are traceable, question quality is measurable, and performance statistics are actionable.

***

## Features

### Student Portal

- **Online Exam** — Participate in published exams within the designated time window
- **Score Review** — View detailed score breakdowns for each exam attempt
- **Mistake Notebook** — Automatically aggregated wrong questions for targeted review
- **Personal Stats** — Track individual accuracy rates and learning progress

### Teacher Portal

- **Question Bank Management** — Full CRUD for questions across 5 types: Single Choice, Multiple Choice, True/False, Fill-in-the-Blank, Essay
- **Manual Exam Assembly** — Hand-pick questions from the bank and compose custom exams
- **Automatic Exam Assembly** — Configure rules (question count, type filters, usage-based weighting) and let the system randomly draw questions
- **Exam Lifecycle Management** — Publish, withdraw, and delete exams with a 4-state state machine (draft → publish → running → done)
- **Essay Grading** — Manual scoring for subjective questions
- **Multi-dimensional Reports** — Exam pass rates, score distributions, question quality analysis (difficulty identification)

### Admin Portal

- **User Management** — Create, update, enable/disable, and delete users across all roles; batch operations supported
- **Global Dashboard** — System-wide statistics and data overview
- **Full Permissions** — Access to all teacher and student features plus administrative controls

***

## Tech Stack

| Layer                       | Technology                                              | Version                                         |
| --------------------------- | ------------------------------------------------------- | ----------------------------------------------- |
| **Backend Framework**       | Spring Boot                                             | 4.0.6                                           |
| **Language**                | JDK                                                     | 21 (Records, Pattern Matching, Virtual Threads) |
| **ORM**                     | Spring Data JPA + Hibernate                             | Community Dialect 7.2.12.Final                  |
| **Authentication**          | JWT (JSON Web Token)                                    | —                                               |
| **Code Generator**          | Lombok                                                  | —                                               |
| **Database**                | SQLite (xerial JDBC driver)                             | —                                               |
| **Build Tool**              | Maven                                                   | 3.9+                                            |
| **Frontend Framework**      | Vue 3 (Composition API + `<script setup>`)              | —                                               |
| **Frontend Build**          | Vite                                                    | Latest Stable                                   |
| **Frontend Router**         | Vue Router                                              | 4                                               |
| **State Management**        | Pinia                                                   | —                                               |
| **UI Library**              | Element Plus                                            | —                                               |
| **HTTP Client**             | Axios                                                   | —                                               |
| **AI-Assisted Development** | [Trae CN](https://www.trae.ai/) / Qwen3.7 MAX / GLM 5.1 | —                                               |

> The backend is fully implemented and production-ready. The Vue 3 frontend is pending creation.

***

## Architecture

### Overall Design

```mermaid
graph TD
    subgraph Frontend["🖥️ Frontend Layer (Vue 3)"]
        direction LR
        F1["Vue Router 4"]
        F2["Pinia State"]
        F3["Element Plus UI"]
        F4["Axios HTTP"]
    end

    subgraph Backend["⚙️ Backend Layer (Spring Boot 4)"]
        direction TB
        C["📡 Controller\n(Routing & Validation)"]
        S["🧩 Service\n(Business Logic)"]
        R["🗄️ Repository\n(Data Access)"]

        subgraph Common["🔧 Common Infrastructure"]
            direction LR
            C1["Result<T> / PageResult<T>"]
            C2["Global Exception Handler"]
            C3["JWT Auth & @RequireRole"]
        end
    end

    subgraph Database["💾 Data Layer (SQLite)"]
        direction LR
        T1["user"]
        T2["question"]
        T3["exam"]
        T4["score"]
    end

    Frontend -->|"RESTful API /api/v1/\nJWT Bearer Token"| Backend
    C --> S
    S --> R
    Common -.-> C
    Common -.-> S
    R --> Database
```

### Data Flow

```mermaid
flowchart LR
    subgraph Teacher["👨‍🏫 Teacher / Admin"]
        Q["Manage Questions"]
        E["Assemble Exam"]
    end

    subgraph System["🔄 System Processing"]
        QB["Question Bank\n(5 Types)"]
        AS["Auto/Manual\nAssembly"]
        EX["Exam Lifecycle\ndraft → publish → running → done"]
    end

    subgraph Student["👨‍🎓 Student"]
        TK["Take Exam"]
        SC["View Scores"]
        MS["Mistake Review"]
    end

    subgraph Analytics["📊 Analytics Engine"]
        GR["Auto Grading\n+ Essay Review"]
        RP["Reports\nPass Rate / Distribution\nQuality Analysis"]
    end

    Q --> QB
    E --> AS
    AS --> EX
    EX --> TK
    TK --> SC
    SC --> MS
    TK --> GR
    GR --> RP
```

### Key Design Patterns

- **Unified Response Contract** — All APIs return `Result<T>` with standardized `{ code, message, data }` format, processed through a global exception handler
- **Entity-DTO Isolation** — JPA entities never leak to the API layer; all responses pass through DTO/VO conversion in the Service layer, preventing JSON circular references and sensitive field exposure
- **Single-Table + JSON Polymorphic Design** — Three strategic JSON fields (`question.answer`, `exam.question_sum`, `score.detail`) enable flexible data structures while keeping the relational schema simple and performant
- **4-Table Independent Architecture** — No JPA `@ManyToOne`/`@OneToMany` relationships; cross-table queries use `findAllById` batch loading, eliminating N+1 query risks entirely
- **Snapshot-Based Exam Assembly** — The `question_sum` JSON field captures a point-in-time snapshot of selected questions, so subsequent question modifications don't retroactively affect existing exams

***

## Quick Start

### Prerequisites

| Requirement | Version  |
| ----------- | -------- |
| JDK         | 21+      |
| Node.js     | 24.15.0+ |
| Maven       | 3.9+     |
| Git         | Latest   |

### Backend

```bash
# 1. Clone the repository
git clone https://github.com/YOUR_USERNAME/GDUT-OOP_20260601.git
cd GDUT-OOP_20260601

# 2. Navigate to backend directory
cd backend

# 3. Run with Maven
mvn spring-boot:run

# 4. Verify: visit http://localhost:8080
#    (Default error page is expected since no root mapping exists)
```

### Run Tests

```bash
cd backend
mvn test
```

Expected output: `Tests run: 73, Failures: 0, Errors: 0, Skipped: 0`

### Frontend (Pending)

```bash
# Frontend is not yet initialized in this repository.
# Planned tech stack: Vue 3 + Vite + Element Plus + Pinia

# Once created:
cd frontend
npm install
npm run dev
```

***

## Project Structure

```
GDUT-OOP_20260601/
│
├── backend/                                      # Spring Boot Backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/cps/backend/
│   │   │   │   ├── common/                       # Shared Infrastructure
│   │   │   │   │   ├── api/
│   │   │   │   │   │   ├── Result<T>             # Unified API response wrapper
│   │   │   │   │   │   └── PageResult<T>         # Pagination wrapper
│   │   │   │   │   ├── exception/
│   │   │   │   │   │   ├── BusinessException     # Custom business exception
│   │   │   │   │   │   └── GlobalExceptionHandler
│   │   │   │   │   ├── config/
│   │   │   │   │   │   └── WebMvcConfig          # Interceptor registration
│   │   │   │   │   └── security/
│   │   │   │   │       ├── JwtUtil               # JWT token generation/validation
│   │   │   │   │       ├── JwtInterceptor        # Auth interceptor
│   │   │   │   │       └── @RequireRole          # Role-based annotation
│   │   │   │   │
│   │   │   │   └── modules/                      # Business Modules (Vertical Slice)
│   │   │   │       ├── M01userauth/              # 👤 User Authentication & Management
│   │   │   │       │   ├── controller/  UserController
│   │   │   │       │   ├── service/     UserService
│   │   │   │       │   ├── repository/  UserRepository
│   │   │   │       │   ├── entity/      User
│   │   │   │       │   ├── dto/         7 DTOs (LoginReq, RegisterReq, UserVO...)
│   │   │   │       │   └── enums/       UserType
│   │   │   │       │
│   │   │   │       ├── M02questionbank/          # 📝 Question Bank CRUD
│   │   │   │       │   ├── controller/  QuestionController
│   │   │   │       │   ├── service/     QuestionService
│   │   │   │       │   ├── repository/  QuestionRepository
│   │   │   │       │   ├── entity/      Question
│   │   │   │       │   ├── dto/         12 DTOs (incl. polymorphic Answer types)
│   │   │   │       │   └── enums/       QuestionType
│   │   │   │       │
│   │   │   │       ├── M03examassembly/          # 📋 Exam Assembly & Lifecycle
│   │   │   │       │   ├── controller/  ExamController, DraftController
│   │   │   │       │   ├── service/     ExamService, DraftCacheService
│   │   │   │       │   ├── repository/  ExamRepository
│   │   │   │       │   ├── entity/      Exam
│   │   │   │       │   ├── dto/         10 DTOs
│   │   │   │       │   └── enums/       ExamStatus
│   │   │   │       │
│   │   │   │       └── M04scorestatistics/       # 📊 Scoring & Statistics
│   │   │   │           ├── controller/  ScoreController
│   │   │   │           ├── service/     ScoreService
│   │   │   │           ├── repository/  ScoreRepository
│   │   │   │           ├── entity/      Score
│   │   │   │           └── dto/         14 DTOs
│   │   │   │
│   │   │   └── resources/
│   │   │       └── application.yaml              # Production config (PRAGMA, HikariCP, JPA)
│   │   │
│   │   └── test/                                 # 73 Tests ✅ All Passing
│   │       └── resources/
│   │           ├── application-test.yaml
│   │           └── schema/                       # DDL scripts
│   │
│   └── pom.xml
│
├── frontend/                                     # Vue 3 Frontend (Pending)
│
├── Data/
│   ├── English.sqlite                            # SQLite database file
│   └── img/                                      # Question images (matched by ID)
│
├── scripts/                                      # SQL DDL scripts
│   ├── table_user.sql
│   ├── table_question.sql
│   ├── table_exam.sql
│   └── table_score.sql
│
├── wiki/                                         # Project Documentation
│   ├── 00-INDEX.md                               # Master index
│   ├── 00-Course-Guidelines.md                   # Course guidelines archive
│   ├── 01-Global-Standards.md                    # API contracts, JPA specs
│   ├── 02-Data-Dictionary.md                     # Database schema
│   ├── modules/
│   │   ├── M01-User-Auth.md
│   │   ├── M02-Question-Bank.md
│   │   ├── M03-Exam-Assembly.md
│   │   ├── M04-Score-Statistics.md
│   │   └── _legacy_course-modules.md
│   └── references/
│       └── SQLite-Optimization.md
│
└── LICENSE
```

***

## Documentation

Detailed technical documentation is available in the `wiki/` directory:

| Document                                                         | Description                                                 |
| ---------------------------------------------------------------- | ----------------------------------------------------------- |
| [00-INDEX.md](wiki/00-INDEX.md)                                  | Master index, project overview, module navigation           |
| [01-Global-Standards.md](wiki/01-Global-Standards.md)            | API contracts, exception handling, JPA specs, code layering |
| [02-Data-Dictionary.md](wiki/02-Data-Dictionary.md)              | Database schema, entity mapping, JSON field specifications  |
| [M01-User-Auth.md](wiki/modules/M01-User-Auth.md)                | User authentication & permission management                 |
| [M02-Question-Bank.md](wiki/modules/M02-Question-Bank.md)        | Question bank CRUD & polymorphic answer JSON                |
| [M03-Exam-Assembly.md](wiki/modules/M03-Exam-Assembly.md)        | Exam lifecycle, manual/automatic assembly strategies        |
| [M04-Score-Statistics.md](wiki/modules/M04-Score-Statistics.md)  | Scoring, grading, statistics & reporting                    |
| [SQLite-Optimization.md](wiki/references/SQLite-Optimization.md) | SQLite-specific tuning & pitfalls in JPA context            |

***

## About the Author

<table>
  <tr>
    <td width="120" align="center">
      <img src="Data/Vue/logo.png" alt="Author Avatar" width="80" style="border-radius: 50%;" />
    </td>
    <td>
      <strong>黄泊凯 (BoKai Huang)</strong><br/>
      🎓 广东工业大学 · 计算机学院 · 软件工程专业 · 23级本科生（大三）
      🔬 师从 <strong>物理信息融合实验室国家地方工程中心（CPS）</strong> · 黄国恒 副教授<br/>
      📧 <a href="mailto:3347620766@qq.com">3347620766@qq.com</a> &nbsp;|&nbsp; <br/>
      📱 <code>13600323338</code>
    </td>
  </tr>
</table>

> 💡 *"Code is a medium of thought — and AI is becoming the most fluent collaborator."*

***

## Contributing & Acknowledgements

### 🙏 Acknowledgements

First and foremost, sincere gratitude to:

- **广东工业大学计算机学院** — For providing the academic foundation and curriculum that inspired this project.
- **物理信息融合实验室国家地方工程中心（CPS）** — For the research environment and resources that made this work possible.
- **导师 黄国恒 副教授** — For guidance, support, and encouragement throughout the development process.

### 💌 Why Open Source?

> **不敢说给大家参考，十有八九是献丑。**

This project is primarily a personal milestone — a digital time capsule of what a junior-year student achieved during the final weeks of a semester. The code may have its rough edges, the architecture may not be perfect, and there are certainly better ways to do many things.

But if this repository can serve as even a tiny reference point for future students tackling their own course projects — whether it's the **system architecture design**, the **AI Agent-assisted development workflow**, or simply the courage to try something unconventional — then opening this repository will have been worth it.

Think of it not as a tutorial, but as a proof of concept: *what happens when a student who truly understands their domain hands the keyboard to an AI Agent and says "let's build this together."*

### 🤝 Contributing

Contributions, issues, and suggestions are always welcome! Whether it's a bug fix, a feature request, or just a "hey, this inspired me" — feel free to reach out.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

***

## License

This project is licensed under the [MIT License](LICENSE).

***

<p align="center">
  ❤️❤️❤️ Developed by an ordinary student majoring in software engineering at the School of Computer Science of Guangdong University of Technology ❤️❤️❤️
</p>
