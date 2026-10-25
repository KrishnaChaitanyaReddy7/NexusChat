package nexuschat.service;

import nexuschat.dao.DatabaseOperations;
import nexuschat.exception.ChatValidationException;
import nexuschat.model.Message;
import nexuschat.util.GenericFilterUtil;
import nexuschat.util.HistorySearchUtil;
import nexuschat.util.NexusConstants;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Unit II Syllabus:
 * - Implementing and applying interfaces: Implements AdvancedChatService
 * 
 * Unit IV Syllabus:
 * - java.io package: File, FileWriter, BufferedWriter for exporting chat transcripts
 * 
 * Unit III Syllabus:
 * - Exception handling: throws ChatValidationException, IOException
 */
public class ChatServiceImpl implements AdvancedChatService {

    private final DatabaseOperations dbOps;

    public ChatServiceImpl(DatabaseOperations dbOps) {
        this.dbOps = dbOps;
    }

    @Override
    public void validateMessageContent(String content) throws ChatValidationException {
        // Unit I: String class operations
        if (content == null || content.trim().isEmpty()) {
            throw new ChatValidationException("Message cannot be empty or contain only whitespace.");
        }
        if (content.length() > NexusConstants.MAX_MESSAGE_LENGTH) {
            throw new ChatValidationException("Message length exceeds maximum limit of " + NexusConstants.MAX_MESSAGE_LENGTH + " characters.");
        }
    }

    @Override
    public void sendMessage(Message message) throws ChatValidationException, Exception {
        if (message == null) {
            throw new ChatValidationException("Message object cannot be null.");
        }
        validateMessageContent(message.getContent());
        dbOps.saveMessage(message);
    }

    @Override
    public List<Message> retrieveHistory(String target, boolean isGroup) throws Exception {
        if (isGroup) {
            return dbOps.getGroupMessageHistory(target, 100);
        } else {
            // Retrieve direct history between logged-in user and target
            return dbOps.getDirectMessageHistory(target, target, 100);
        }
    }

    @Override
    public List<Message> searchHistoryByKeyword(String target, String keyword, boolean isGroup) throws Exception {
        List<Message> history = retrieveHistory(target, isGroup);
        // Unit I: Recursion applied via HistorySearchUtil
        return HistorySearchUtil.searchByKeyword(history, keyword);
    }

    @Override
    public void exportChatHistory(String target, String filePath, boolean isGroup) throws IOException, Exception {
        List<Message> history = retrieveHistory(target, isGroup);

        File outputFile = new File(filePath);
        // Unit IV: java.io Package - File, FileWriter, BufferedWriter
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("====================================================\n");
            writer.write(" NexusChat Transcript Export: " + target + " (" + (isGroup ? "Group" : "Direct") + ")\n");
            writer.write(" Exported At: " + new java.util.Date() + "\n");
            writer.write(" Total Messages: " + history.size() + "\n");
            writer.write("====================================================\n\n");

            for (Message msg : history) {
                writer.write(msg.getFormattedDisplay());
                writer.newLine();
            }
            writer.flush();
        }
    }
}
