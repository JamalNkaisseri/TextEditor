package com.texteditor.io;

import java.io.*;  // Import necessary classes for input/output (IO) operations

// This class handles reading from and writing to text files
public class FileHandler {

    // This method reads content from a file and returns it as a String
    public static String readFromFile(File file) {
        // StringBuilder is used to efficiently build the content from the file
        StringBuilder content = new StringBuilder();

        // Use a try-with-resources statement to automatically close the reader after use
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;  // Variable to store each line read from the file

            // Loop to read each line from the file until end-of-file is reached
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");  // Append each line with a newline character
            }
        } catch (IOException e) {  // Catch potential IOExceptions that may occur during file reading
            e.printStackTrace();  // Print the stack trace for debugging purposes
            // In a real application, you could show a dialog to the user instead of printing the stack trace
        }

        return content.toString();  // Return the content read from the file as a string
    }

    // This method writes the provided content to a file
    public static void writeToFile(File file, String content) {
        // Use a try-with-resources statement to automatically close the writer after use
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content);  // Write the provided content to the file
        } catch (IOException e) {  // Catch potential IOExceptions that may occur during file writing
            e.printStackTrace();  // Print the stack trace for debugging purposes
            // In a real application, you could show a dialog to the user instead of printing the stack trace
        }
    }
}
