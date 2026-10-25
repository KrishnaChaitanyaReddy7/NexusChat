package nexuschat.network;

/**
 * Unit II Syllabus:
 * - Defining an interface
 * - Variables in interface: Implicitly 'public static final' constants
 * - Interface specification
 */
public interface NetworkProtocol {

    // Variables in interface (implicitly public static final constants)
    String CMD_LOGIN = "LOGIN";
    String CMD_REGISTER = "REGISTER";
    String CMD_LOGOUT = "LOGOUT";
    String CMD_MSG_DIRECT = "MSG_DIRECT";
    String CMD_MSG_GROUP = "MSG_GROUP";
    String CMD_GET_USERS = "GET_USERS";
    String CMD_USER_LIST = "USER_LIST";
    String CMD_CREATE_GROUP = "CREATE_GROUP";
    String CMD_DELETE_GROUP = "DELETE_GROUP";
    String CMD_GROUP_DELETED = "GROUP_DELETED";
    String CMD_ADD_GROUP_MEMBER = "ADD_GROUP_MEMBER";
    String CMD_GET_GROUP_CREATOR = "GET_GROUP_CREATOR";
    String CMD_GROUP_CREATOR = "GROUP_CREATOR";
    String CMD_GET_GROUPS = "GET_GROUPS";
    String CMD_GROUP_LIST = "GROUP_LIST";
    String CMD_GET_HISTORY = "GET_HISTORY";
    String CMD_HISTORY_DATA = "HISTORY_DATA";
    String CMD_DELETE_MSG = "DELETE_MSG";
    String CMD_MSG_DELETED = "MSG_DELETED";
    String CMD_MSG_READ = "MSG_READ";
    String CMD_MSG_STATUS_UPDATE = "MSG_STATUS_UPDATE";
    String CMD_STATUS_UPDATE = "STATUS_UPDATE";
    String CMD_SYSTEM_ALERT = "SYSTEM_ALERT";
    String CMD_SUCCESS = "SUCCESS";
    String CMD_ERROR = "ERROR";

    // Protocol delimiter for string socket messages
    String DELIMITER = "||";

    // Method signatures in interface (implicitly public abstract)
    void start();
    void stop();
}
