package nexuschat.util;

import nexuschat.model.Message;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit I Syllabus:
 * - Recursion: Methods that call themselves to solve problems
 * - Base condition and recursive step
 * - Exploring String class (toLowerCase, contains)
 */
public final class HistorySearchUtil {

    private HistorySearchUtil() {}

    /**
     * Recursively searches through a list of messages for a keyword.
     * Demonstrates recursion with a base case and recursive calls.
     * 
     * @param messages List of messages
     * @param keyword Keyword to search for
     * @param currentIndex Current index in recursive scan
     * @param matchingResults Accumulator list collecting matches
     */
    public static void searchRecursive(List<Message> messages, String keyword, int currentIndex, List<Message> matchingResults) {
        // Base Case 1: Null inputs or end of list reached
        if (messages == null || keyword == null || currentIndex >= messages.size()) {
            return; // Terminate recursion
        }

        // Process current element
        Message currentMsg = messages.get(currentIndex);
        if (currentMsg != null && currentMsg.getContent() != null) {
            // Exploring String class methods: toLowerCase(), contains()
            if (currentMsg.getContent().toLowerCase().contains(keyword.toLowerCase())) {
                matchingResults.add(currentMsg);
            }
        }

        // Recursive Step: Call self with next index
        searchRecursive(messages, keyword, currentIndex + 1, matchingResults);
    }

    /**
     * Public entry method initiating recursive search
     */
    public static List<Message> searchByKeyword(List<Message> messages, String keyword) {
        List<Message> results = new ArrayList<>();
        // Start recursive traversal at index 0
        searchRecursive(messages, keyword, 0, results);
        return results;
    }

    /**
     * Recursive calculation of total character count across all message history
     */
    public static int calculateTotalCharactersRecursive(List<Message> messages, int index) {
        // Base Case
        if (messages == null || index >= messages.size()) {
            return 0;
        }
        Message msg = messages.get(index);
        int currentLength = (msg != null && msg.getContent() != null) ? msg.getContent().length() : 0;
        
        // Recursive Step
        return currentLength + calculateTotalCharactersRecursive(messages, index + 1);
    }
}
