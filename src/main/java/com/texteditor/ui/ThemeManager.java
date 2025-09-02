
package com.texteditor.ui;

public class ThemeManager {
    public static final String BACKGROUND_COLOR = "#1E1E2E";
    public static final String TEXT_COLOR = "#E0E0E0";
    public static final String TAB_SELECTED_COLOR = "#2A2A3A";
    public static final String TAB_UNSELECTED_COLOR = "#252535";
    public static final String TAB_HEADER_COLOR = "#1E1E2E";
    public static final String SCROLLBAR_THUMB_COLOR = "#6D5D8C";
    public static final String SCROLLBAR_TRACK_COLOR = "#2A2A3A";
    public static final String CURSOR_COLOR = "#FF79C6"; // Vibrant pink-purple for visibility
    public static final String CURRENT_LINE_COLOR = "#2A2A3A";

    // ✅Add this reusable style string
    public static final String ROOT_STYLE = "-fx-background-color: " + BACKGROUND_COLOR + ";";

    // Add transparency settings
    public static final double WINDOW_OPACITY = 0.9; // 90% opacity (10% transparent)
    public static final String TRANSPARENT_BACKGROUND = "rgba(30, 30, 46, 0.9)"; // Semi-transparent background
    public static final String ROOT_TRANSPARENT_STYLE = "-fx-background-color: rgba(30, 30, 46, 0.9);";


}
