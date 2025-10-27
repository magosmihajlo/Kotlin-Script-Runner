# Kotlin Script Runner

A production-grade desktop application for executing Kotlin scripts with real-time output, syntax highlighting, and interactive error navigation.

## Overview

This application demonstrates Clean Architecture, MVVM pattern, and modern Kotlin development practices. It executes Kotlin scripts via `kotlinc -script` with live output streaming and clickable error navigation.
It was meant to implement both syntax highlighting and error navigation. Error navigation is partially implemented and fully explained later
in this document in the Known Limitations section. I would love to fix this issue in the future.

## Features

- **Dual-pane interface** - Code editor and output display side-by-side
- **Real-time execution** - Live output streaming as scripts run
- **Syntax highlighting** - Keywords, strings, and comments colored
- **Error navigation** - Click errors to jump to the exact location
- **Process management** - Start, stop, and monitor script execution
- **Visual indicators** - Status dots show idle/running/success/failed states
- **Memory safe** - Output limited to 1000 lines

## Architecture

Clean Architecture with four layers:
```
UI (Compose) → Presentation (ViewModel) → Domain (Use Cases) → Data (Executors)
```

**Why Clean Architecture?**
- Testable business logic isolated from UI
- Framework-independent domain layer
- Easy to modify and extend
- Industry best practice

## Technology Stack

- **Kotlin** - Modern JVM language
- **Compose Desktop** - Declarative UI framework
- **Material3** - Design system
- **Coroutines** - Async operations
- **StateFlow** - Reactive state management

## Prerequisites

### Required Software

1. **JDK 17+** (JDK 21 recommended)
    - Download: https://adoptium.net/
    - Verify: `java -version`

2. **Kotlin Compiler** (`kotlinc`)
    - Windows: `choco install kotlin` (Requires choco. Not recommended, see below)
    - macOS: `brew install kotlin` (Requires brew)
    - Linux: `snap install --classic kotlin`
    - Verify: `kotlinc -version`

3. **Git**
    - Verify: `git --version`

### Recommended Software for Kotlin
    - Download latest version of Kotlin and unzip it in a path without spaces
      manually add it to PATH and everything should work

### Verify kotlinc Works
```bash
echo 'println("Test")' > test.kts
kotlinc -script test.kts
# Should output: Test
rm test.kts
```

**This test MUST work for the application to function.**

## Installation
```bash
# Clone repository
git clone https://github.com/magosmihajlo/Kotlin-Script-Runner
cd kotlin-script-runner

# Build (first time takes 2-5 minutes)
./gradlew build

# Run
./gradlew run
```

**Windows:** Use `gradlew.bat` instead of `./gradlew`

## Usage

### Basic Workflow

1. Write Kotlin script in left editor pane
2. Click "Run" button
3. Watch real-time output in the right pane
4. Click underlined errors to jump to location
5. Click "Stop" to cancel long-running scripts
6. Click "Clear" to reset the output

### Example: Hello World
```kotlin
println("Hello, Kotlin!")
```

### Example: Loop with Delay
```kotlin
for (i in 1..5) {
    println("Count: $i")
    Thread.sleep(500)
}
println("Done!")
```

### Example: Error Navigation
```kotlin
println("Line 1")
println("Line 2")
val broken = undefinedVariable  // Click this error!
println("Line 4")
```

**Expected:** Error appears underlined in the output. Click it → the cursor jumps to line 3.

### Status Indicators

| Indicator        | Meaning                     |
|------------------|-----------------------------|
| Gray dot         | Idle - Ready to execute     |
| Blue pulsing dot | Running - Script executing  |
| Green dot        | Success - Exit code 0       |
| Red dot          | Failed - Non-zero exit code |

## Project Structure
```
src/main/kotlin/
├── Main.kt                          # Entry point
├── di/
│   └── AppModule.kt                 # Dependency injection
├── domain/                          # Business logic
│   ├── model/
│   │   ├── ScriptExecution.kt       # Execution state
│   │   └── ScriptError.kt           # Error models
│   ├── repository/
│   │   ├── ScriptExecutor.kt        # Executor interface
│   │   └── ScriptFileManager.kt     # File manager interface
│   └── usecase/
│       └── ExecuteScriptUseCase.kt  # Execution workflow
├── data/                            # Implementations
│   ├── executor/
│   │   └── KotlinScriptExecutor.kt  # Process execution
│   └── file/
│       └── TempScriptFileManager.kt # File operations
├── presentation/                    # State management
│   ├── ScriptExecutionState.kt      # UI state model
│   └── ScriptExecutionViewModel.kt  # State coordinator
└── ui/                              # User interface
    ├── theme/
    │   ├── Theme.kt                 # Colors
    │   └── Typography.kt            # Text styles
    ├── components/
    │   ├── CodeEditor.kt            # Code input
    │   ├── OutputPanel.kt           # Output display
    │   ├── StatusIndicator.kt       # Status visualization
    │   ├── ControlBar.kt            # Action buttons
    │   ├── SyntaxHighlighter.kt     # Syntax coloring
    │   └── SyntaxHighlightTransformation.kt
    └── screens/
        └── MainScreen.kt            # Main layout
```

## How It Works

### Script Execution Flow

1. User writes code in the editor
2. User clicks "Run" button
3. ViewModel receives action
4. Use case creates temporary `.kts` file
5. Use case spawns `kotlinc -script` process
6. Separate coroutines read stdout and stderr
7. Each line emitted to ViewModel
8. ViewModel updates StateFlow
9. UI recomposes with new output
10. Process completes, exit code captured
11. Temporary files cleaned up
12. Status updated to success/failed

### Syntax Highlighting

- Custom tokenizer parses character-by-character
- Recognizes keywords, strings, comments
- Applies colors via AnnotatedString
- Uses VisualTransformation for non-intrusive rendering
- O(n) complexity, faster than regex

### Error Navigation

- Parses kotlinc error format: `file:line:column: error: message`
- Extracts line and column numbers with regex
- Calculates character offset from line/column
- Updates TextFieldValue selection to move the cursor
- Underlines clickable errors

### Memory Management

- Output limited to 1000 lines
- Older lines dropped as new ones arrive
- LazyColumn virtualizes rendering
- Prevents OutOfMemoryError on infinite output

## Design Decisions

### Why Clean Architecture?

**Problem:** Mixing UI, business logic, and data access creates untestable, unmaintainable code.

**Solution:** Separate into layers with dependency inversion.

**Benefits:**
- Domain layer has zero dependencies (pure Kotlin)
- Easy to test with mocks
- Can swap UI frameworks without touching business logic
- Changes isolated to a single layer

### Why MVVM?

**Problem:** Complex state management in UI leads to bugs.

**Solution:** ViewModel coordinates state, UI is a pure function of state.

**Benefits:**
- Single source of truth (StateFlow)
- Unidirectional data flow
- Trivial to test ViewModel
- UI automatically updates on state changes

### Why Coroutines?

**Problem:** Blocking I/O freezes UI.

**Solution:** Async execution with coroutines.

**Benefits:**
- Non-blocking I/O keeps UI responsive
- Structured concurrency ensures cleanup
- Easy cancellation support
- No callback hell

### Why Compose Desktop?

**Problem:** Swing is imperative and verbose.

**Solution:** Declarative UI with Compose.

**Benefits:**
- UI = function of state
- Less boilerplate
- Smart recomposition (only changed parts update)
- Directly relevant to JetBrains Compose Multiplatform work

## Design Patterns Used

1. **Clean Architecture** - Layered separation of concerns
2. **MVVM** - Model-View-ViewModel for state management
3. **Repository Pattern** - ScriptExecutor interface abstracts implementation
4. **Use Case Pattern** - ExecuteScriptUseCase encapsulates workflow
5. **Observer Pattern** - StateFlow for reactive updates
6. **Strategy Pattern** - Interface-based executor implementation
7. **Dependency Injection** - Manual DI container (AppModule)

## Troubleshooting

### kotlinc not found

**Symptom:** "Cannot run program kotlinc"

**Solution:**
- Verify installation: `kotlinc -version`
- Check PATH contains kotlinc bin directory
- Windows: Restart terminal after installation
- Try full path: `C:\kotlinc\bin\kotlinc.bat`

### Compilation failed with the unresolved reference 'println'

**Symptom:** Script won't compile

**Solution:**
- This is actually a test case for error navigation
- `println` should work in normal scripts
- If genuine issue, reinstall kotlinc
- Changing to a path without spaces helped me fix this and a lot of other issues

### Application won't start

**Symptom:** Window doesn't open

**Solution:**
- Verify JDK 17+: `java -version`
- Check terminal for error messages
- Try: `./gradlew clean build run`
- Check JAVA_HOME is set correctly

### Syntax highlighting is not visible

**Symptom:** All text the same color

**Solution:**
- Verify `SyntaxHighlightTransformation.filter()` called in CodeEditor
- Check theme colors defined in Theme.kt
- Try typing `fun` - should turn orange

### Error navigation doesn't work

**Symptom:** Clicking error does nothing

**Solution (Work in progress):**
- Verify ViewModel has `updateTextFieldValue()` function
- Check CodeEditor receives `textFieldValue` from state
- Verify MainScreen passes state.textFieldValue to CodeEditor
- See fix instructions at the top of README

## Performance Considerations

- **Output limiting:** 1000 line maximum prevents memory exhaustion
- **Lazy rendering:** LazyColumn only renders visible lines
- **Smart recomposition:** Compose only updates changed UI elements
- **Coroutine efficiency:** Non-blocking I/O, structured concurrency
- **Memory cleanup:** Temporary files deleted immediately after execution

## Known Limitations

### Error Navigation
Error navigation is partially implemented. The application detects and underlines error lines in the output, and clicking them triggers the navigation logic (visible in console logs). However, due to technical limitations with Compose Desktop's `BasicTextField` cursor management and state synchronization, the cursor does not visually jump to the error location. This is a known issue with bidirectional data flow in Compose's text field implementation.

**What works:**
- Error detection and parsing
- Click detection on error lines
- Cursor offset calculation
- State updates with the correct position

**What doesn't work:**
- Visual cursor repositioning in the editor

**Workaround:** Users can manually navigate to the line and column numbers displayed in the error message.

**Future fix:** This could be resolved by using a custom text editor component or waiting for Compose Desktop text field improvements in future releases.

## Future Enhancements

- [ ] Error navigation fully completed
- [ ] Script save/load functionality
- [ ] Execution history tracking
- [ ] Multiple script tabs
- [ ] Customizable themes and fonts
- [ ] Line numbers in the editor
- [ ] Search and replace
- [ ] Keyboard shortcuts (Ctrl+R, Ctrl+K, Ctrl+S)
- [ ] Auto-completion
- [ ] Integrated debugger
- [ ] Performance metrics

## License

Created for the JetBrains internship application. Educational purposes.

## Author Notes

This project demonstrates:
- Advanced Kotlin programming skills
- Clean Architecture and SOLID principles
- Compose Desktop proficiency
- Process management and concurrency
- Professional code organization
- IDE tooling development capabilities

**For interview:** Prepared to explain any architectural decision, design pattern, or implementation detail.

---

**Contact:** Available for questions, demonstrations, and code walkthroughs.