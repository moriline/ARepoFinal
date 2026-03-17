package org.arepo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ASettings {
    String DIR_NAME = ".arepo"; // palindrome for OPERA !
    String SEP = "/";
    String FILE_SETTINGS = "settings.json";
    String ROOT_DB_NAME = "root.db";
    String PATCH_EXTENSION = ".db";
    String UPLOAD_EXTENSION = ".db3";
    String DB_NAME = "main.db";
    String SIGN_ERASIED_FILE = "*";

    String TEXT_FILE_TYPE = "t";
    String BINARY_FILE_TYPE = "b";
    String DEFAULT_GROUP_NAME = "master";
    String DEFAULT_FOLDER_ROOT_NAME = "";
    String SCHEMA_PREFIX = "s_";
    String DEFAULT_SCHEMA = "public";

    String TEST_TEXT_FILENAME = "testtextfile.txt";
    String TEST_BINARY_FILENAME = "testbinaryfile.bin";
    String TEST_TEXT_CONTENT = "content for test.";

    String IGNORE_FOLDERS_NAME = ".ignored_folders";
    String IGNORE_FILES_NAME = ".ignored_files";
    String APP_HISTORY_FILE = "history.json";

    String DIR_PATCH = "patch";
    String DIR_UPLOAD = "upload";
    String FILE_CONFLICT = ".temp";
    String PATCH_DELETED = ".deleted";
    String STATE_FILENAME = "state.json";
    String CONFIG_FILE = ".config";
    String ROOT_PREFIX = "";
    Integer LINUX = 1;
    Integer WINDOWS = 2;
    Integer MAC = 3;
    Map<Integer, String> eol = Map.of(LINUX, "\n", //LF
            WINDOWS, "\r\n", //CR+LF
            MAC, "\r");//CR
    String PA = "a"; // Patch with name 'a'
    String PB = "b";
    String PC = "c";
    String PD = "d";
    String PE = "e";
    List<String> defindedPatches = List.of(PA, PB, PC, PD, PE);
    Set<String> charsets = Set.of("UTF-8","UTF-16","Windows-1251");

    String DB_PREFIX = "jdbc:sqlite:";
    int MAX_FILES_IN_PATCH = 1600;
    int DEFAULT_SHORT_LOGS_SIZE = 1;
    Long DEFAULT_LAST_HASH = 0L;
    String FILE_ADDED = "A";
    String FILE_MODIFIED = "M";
    String FILE_DELETED = "D";
    String FILE_ERASED = "E";
    String FILE_NONE = "";
    String ERROR = "Error";

    String TABLE_USERS = "users";
    String TABLE_PROJECTS = "projects";

    String FILE_TABLE = "files";
    String LOG_TABLE = "logs";
    String TEMP_TABLE = "tempfiles";
    String PATCH_TABLE = "patches";
    String PATCH_HISTORY_TABLE = "history";
    String SETTINGS_TABLE = "settings";

    String findById = "SELECT * FROM %s WHERE id = ? ;";
    String count = "SELECT COUNT(*) FROM %s ;";
    String all = "SELECT * FROM %s ;";
    String deleteById = "DELETE FROM %s WHERE id = ?";
}
