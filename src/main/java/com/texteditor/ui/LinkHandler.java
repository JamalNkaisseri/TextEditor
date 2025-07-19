package com.texteditor.ui;

import javafx.application.HostServices;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import org.fxmisc.richtext.CodeArea;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles clickable links in the text editor
 * Detects URLs and makes them clickable to open in browser
 */
public class LinkHandler {

    // Matches URLs with http, https, or ftp
    private static final Pattern URL_PATTERN = Pattern.compile(
            "\\b(?:https?|ftp)://[\\w\\-._~:/?\\[\\]@!$&'()*+,;=%]+",
            Pattern.CASE_INSENSITIVE
    );

    // Matches URLs with http, https, ftp, or starting with www.
    private static final Pattern EXTENDED_URL_PATTERN = Pattern.compile(
            "\\b(?:(?:https?|ftp)://|www\\.)[\\w\\-._~:/?\\[\\]@!$&'()*+,;=%]+",
            Pattern.CASE_INSENSITIVE
    );



    private final HostServices hostServices;
    private CodeArea codeArea;
    private boolean useExtendedPattern;

    /**
     * Creates a new LinkHandler
     * @param hostServices JavaFX HostServices for opening URLs in browser
     */
    public LinkHandler(HostServices hostServices) {
        this.hostServices = hostServices;
        this.useExtendedPattern = true; // Default to extended pattern
    }

    /**
     * Binds the link handler to a CodeArea
     * @param codeArea The CodeArea to make links clickable in
     */
    public void bindTo(CodeArea codeArea) {
        this.codeArea = codeArea;
        setupMouseHandlers();
    }

    /**
     * Sets whether to use extended URL pattern (includes www. domains)
     * @param extended true to detect www. domains, false for http/https/ftp only
     */
    public void setExtendedPattern(boolean extended) {
        this.useExtendedPattern = extended;
    }

    /**
     * Sets up mouse event handlers for link detection and clicking
     */
    private void setupMouseHandlers() {
        if (codeArea == null) return;

        // Handle mouse movement for cursor changes
        codeArea.setOnMouseMoved(this::handleMouseMove);

        // Handle mouse clicks for opening links
        codeArea.setOnMouseClicked(this::handleMouseClick);
    }

    /**
     * Handles mouse movement to change cursor when hovering over links
     */
    private void handleMouseMove(MouseEvent event) {
        if (codeArea == null) return;

        try {
            int position = getCharacterPosition(event);
            if (position >= 0 && isOverLink(position)) {
                codeArea.setCursor(Cursor.HAND);
            } else {
                codeArea.setCursor(Cursor.TEXT);
            }
        } catch (Exception e) {
            // Reset cursor on any error
            codeArea.setCursor(Cursor.TEXT);
        }
    }

    /**
     * Handles mouse clicks to open links
     */
    private void handleMouseClick(MouseEvent event) {
        if (codeArea == null || !event.isStillSincePress()) return;

        try {
            int position = getCharacterPosition(event);
            if (position >= 0) {
                String url = getLinkAtPosition(position);
                if (url != null && !url.isEmpty()) {
                    openUrl(url);
                    event.consume(); // Prevent other handlers from processing this click
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling link click: " + e.getMessage());
        }
    }

    /**
     * Gets the character position from mouse coordinates
     */
    private int getCharacterPosition(MouseEvent event) {
        try {
            return codeArea.hit(event.getX(), event.getY()).getCharacterIndex().orElse(-1);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Checks if the cursor is positioned over a link
     */
    private boolean isOverLink(int position) {
        return getLinkAtPosition(position) != null;
    }

    /**
     * Gets the URL at the specified character position
     * @param position Character position in the text
     * @return URL string if found, null otherwise
     */
    private String getLinkAtPosition(int position) {
        if (codeArea == null || position < 0 || position >= codeArea.getLength()) {
            return null;
        }

        String text = codeArea.getText();
        Pattern pattern = useExtendedPattern ? EXTENDED_URL_PATTERN : URL_PATTERN;
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            if (position >= matcher.start() && position <= matcher.end()) {
                String url = matcher.group();
                // Add http:// prefix for www. links
                if (url.startsWith("www.") && !url.startsWith("http")) {
                    url = "http://" + url;
                }
                return url;
            }
        }

        return null;
    }

    /**
     * Opens a URL in the default browser
     * @param url The URL to open
     */
    private void openUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }

        try {
            // Validate URL format
            if (!url.matches("^https?://.*")) {
                System.err.println("Invalid URL format: " + url);
                return;
            }

            if (hostServices != null) {
                hostServices.showDocument(url);
                System.out.println("Opening URL: " + url);
            } else {
                System.err.println("HostServices not available - cannot open URL: " + url);
            }
        } catch (Exception e) {
            System.err.println("Failed to open URL: " + url + " - " + e.getMessage());
        }
    }

    /**
     * Manually opens a URL (useful for testing or programmatic access)
     * @param url The URL to open
     * @return true if successful, false otherwise
     */
    public boolean openUrlManually(String url) {
        try {
            openUrl(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets all URLs found in the current text
     * @return Array of URLs found in the text
     */
    public String[] getAllUrls() {
        if (codeArea == null) {
            return new String[0];
        }

        String text = codeArea.getText();
        Pattern pattern = useExtendedPattern ? EXTENDED_URL_PATTERN : URL_PATTERN;
        Matcher matcher = pattern.matcher(text);

        return matcher.results()
                .map(matchResult -> {
                    String url = matchResult.group();
                    if (url.startsWith("www.") && !url.startsWith("http")) {
                        url = "http://" + url;
                    }
                    return url;
                })
                .toArray(String[]::new);
    }

    /**
     * Unbinds the link handler from the current CodeArea
     */
    public void unbind() {
        if (codeArea != null) {
            codeArea.setOnMouseMoved(null);
            codeArea.setOnMouseClicked(null);
            codeArea.setCursor(Cursor.TEXT);
            codeArea = null;
        }
    }
}