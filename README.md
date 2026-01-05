# TextEditor

A modern, feature-rich text editor built with JavaFX that combines powerful editing capabilities with a sleek, dark-themed interface.

## Features

### Core Editing
- **Multi-tab Support** - Work with multiple files simultaneously in separate tabs
- **Syntax Highlighting** - Automatic styling for structured text including headers, titles, and lists
- **Auto-closing Brackets** - Automatically inserts closing brackets, quotes, and parentheses
- **Word Wrap Toggle** - Quick toggle with `Alt+W` for better readability
- **Adjustable Font Size** - Zoom in/out with keyboard shortcuts

### Smart Text Navigation
- **Clickable Links** - Automatically detects and styles URLs, making them clickable
- **Find & Replace** - Powerful search tool with next/previous navigation
- **Status Bar** - Real-time display of cursor position (line, column) and word wrap status

### Clipboard Management
- **Clipboard History** - Access your last 50 copied/cut items with `Ctrl+Shift+V`
- **Standard Operations** - Cut, copy, and paste with familiar shortcuts

### File Management
- **Open Multiple Files** - Load files through the file manager
- **Save/Save As** - Standard file operations with `Ctrl+S` and `Ctrl+Shift+S`
- **Unsaved Changes Detection** - Smart prompts when closing tabs or exiting with unsaved work
- **Dirty Tab Indicators** - Tabs with unsaved changes are marked with `*`

### User Interface
- **Dark Theme** - Easy on the eyes with a professional dark color scheme
- **Transparent Window** - Modern, semi-transparent window design
- **Draggable Window** - Move the editor by clicking and dragging anywhere
- **Custom Styling** - Supports external CSS themes

## Keyboard Shortcuts

### File Operations
- `Ctrl+O` - Open file
- `Ctrl+S` - Save file
- `Ctrl+Shift+S` - Save file as
- `Ctrl+Q` - Exit application

### Editing
- `Ctrl+C` - Copy
- `Ctrl+X` - Cut
- `Ctrl+V` - Paste
- `Ctrl+Shift+V` - Show clipboard history

### View & Navigation
- `Ctrl+F` - Open search bar
- `Alt+W` - Toggle word wrap
- `Ctrl+=` or `Ctrl++` - Increase font size
- `Ctrl+-` - Decrease font size
- `Ctrl+0` - Reset font size to default

### Search
- `Enter` - Find next (when search bar is focused)
- Next/Previous buttons in search bar for navigation

## Auto-Styling Features

The editor automatically applies styling to structured text:

- **Main Headers** - Lines ending with a colon (e.g., "Introduction:")
- **Titles** - Capitalized lines without colons
- **List Items** - Lines starting with lowercase letter followed by ")" (e.g., "a) First item")
- **Links** - Automatically detects and styles URLs (http://, https://, www.)

## Technical Details

### Built With
- **JavaFX** - Modern UI framework
- **RichTextFX** - Advanced text editing capabilities
- **Maven** - Dependency management

### Architecture
- `TextEditorWindow` - Main application window and coordinator
- `TabManager` - Handles multi-tab functionality
- `FileManager` - File I/O operations
- `AutoStyler` - Real-time text styling engine
- `LinkHandler` - URL detection and click handling
- `SearchTool` - Text search and navigation
- `ClipboardPopup` - Clipboard history management

### Requirements
- Java 11 or higher
- JavaFX 17 or higher
- RichTextFX library

## Getting Started

### Running the Application
```bash
# Clone the repository
git clone <your-repo-url>

# Navigate to project directory
cd text-editor

# Build with Maven
mvn clean install

# Run the application
mvn javafx:run
```

### First Launch
1. The editor opens with a default "Untitled" tab
2. Start typing or open an existing file with `Ctrl+O`
3. Use `Ctrl+F` to search within your document
4. Toggle word wrap with `Alt+W` for long lines
5. Adjust font size with `Ctrl+` and `Ctrl-`

## Configuration

The editor uses external CSS for theming. The dark theme is located at:
```
src/main/resources/styles/dark-theme.css
```

You can customize colors, fonts, and other visual elements by modifying this file.

## File Management

### Unsaved Changes
When you attempt to close a tab or exit the application with unsaved changes:
- A dialog prompts you to save, discard, or cancel
- Each unsaved tab is processed individually
- The tab is highlighted so you know which file is being referenced

### Auto-Save
Currently, auto-save is not implemented. Remember to save your work frequently with `Ctrl+S`.

## Contributing

Contributions are welcome! Please feel free to submit issues or pull requests.

## Credits

Developed with JavaFX and RichTextFX libraries.

---

**Version:** 1.0.0  
**Last Updated:** January 2026
