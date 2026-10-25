package nexuschat.service;

import nexuschat.model.Message;
import java.util.List;

/**
 * Unit II Syllabus:
 * - Extending interfaces: An interface can extend another interface,
 *   inheriting all its abstract method declarations and adding new ones.
 */
public interface AdvancedChatService extends ChatService {
    // Additional capabilities extending base ChatService
    List<Message> searchHistoryByKeyword(String target, String keyword, boolean isGroup) throws Exception;
    void exportChatHistory(String target, String filePath, boolean isGroup) throws Exception;
}
