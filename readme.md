Initial README draft:

## Complete Software List Summary

### Programs Installed:
1. ✅ **JDK 17+** (Java Development Kit)
2. ✅ **IntelliJ IDEA Community** (IDE)
3. ✅ **kotlinc** (Kotlin Compiler)
4. ✅ **Git** (Version Control)
5. ✅ **Gradle** (automatically via IntelliJ)

### Libraries/Frameworks We're Using:
1. ✅ **Compose for Desktop** - Modern UI framework
2. ✅ **Material 3** - UI components
3. ✅ **Kotlinx Coroutines** - Async operations
4. ✅ **Kotlin Standard Library** - Core functionality

### Why These Choices:

**Compose for Desktop:**
- Shows you understand Compose (internship relevant!)
- Modern, declarative UI
- Production-ready

**Material 3:**
- Professional look
- Consistent design system
- Built-in components

**Coroutines:**
- Handle async script execution
- Non-blocking UI
- Proper for long-running tasks

---

## Project Architecture Overview

We'll implement **Clean Architecture** with these layers:
```
┌─────────────────────────────────────┐
│  UI Layer (Compose)                 │  ← What user sees
│  - Screens, Components              │
└──────────────┬──────────────────────┘
│
┌──────────────▼──────────────────────┐
│  Presentation Layer                 │  ← State management
│  - ViewModels, UI State             │
└──────────────┬──────────────────────┘
│
┌──────────────▼──────────────────────┐
│  Domain Layer                       │  ← Business logic
│  - Use Cases, Entities              │
└──────────────┬──────────────────────┘
│
┌──────────────▼──────────────────────┐
│  Data Layer                         │  ← External interactions
│  - Repositories, Process Management │
└─────────────────────────────────────┘