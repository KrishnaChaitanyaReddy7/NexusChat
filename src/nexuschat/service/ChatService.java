package nexuschat.service;

import nexuschat.exception.ChatValidationException;
import nexuschat.model.Message;
import java.util.List;

/**
 * Unit II Syllabus:
 * - Defining an interface
 * - Declaring abstract method contracts
 */
public interface ChatService {
    void sendMessage(Message message) throws ChatValidationException, Exception;
    List<Message> retrieveHistory(String target, boolean isGroup) throws Exception;
    void validateMessageContent(String content) throws ChatValidationException;
}
