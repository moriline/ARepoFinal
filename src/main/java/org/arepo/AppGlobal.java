package org.arepo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class AppGlobal {
    public static Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
    public static enum Actions{
        init, clone, set, setup, upload, download, save, remove, send, get, status, comment, history, clear, diff,
        apply, erase, head, logs, logsbyname, undo, files, patches, show, delete, deleteRemotePatch, accept, settings, updateRemoteLog, getRemoteLogs,
        symlink, patchHistory, fs, removeFiles, createTestFiles;
    }
    public static class UserSettings {
        @Expose
        public String email = "";
        @Expose
        public String repo = "";

        public UserSettings() {
        }
    }
    public static String PROJECT_SUFFIX = ".db";
    public static String DELIMITER = "\n";
    public static Integer BINARY_CHARSET = -1;
    public static String ERROR = "ERROR:";
    public static Long INIT_ID = 0L;
    public static Long DEFAULT_ID = 1L;
    public static Map<String, Long> foldersNameIndex = new HashMap<String, Long>();
    static Charset[] charsets = {StandardCharsets.UTF_8,StandardCharsets.UTF_16, StandardCharsets.US_ASCII};//StandardCharsets.ISO_8859_1
    public static String CNE = "Clone not exists.";
    public static String CIE = "Clone is exists:";
    public static String DNE = "Directory not exists:";
    public static String FNE = "File not exists:";
    public static String FNF = "File not founded:";
    public static String WAFR = "Wrong action for root repo:"; // Only clone repo should to use file system with file. Root repo - only for centralized point for storage from clones.
    public static String PIE = "Patch is empty:";


    public static String makeError(Exception e){
        return ERROR+e.getMessage();
    }

    static String commandNotFounded() {
        return makeError(new SQLException("command not founded"));
    }
    static Checksum crc32 = new CRC32();
    public static long getCRC32Checksum(byte[] bytes) {
        crc32.reset();
        crc32.update(bytes, 0, bytes.length);
        return crc32.getValue();
    }
    public static String generateHashByContent(byte[] content) {
        var result = "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            result = Base64.getEncoder().encodeToString(md.digest(content));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return result;
    }
    public static String getTimeInSec(){
        Instant instant = Instant.now();
        return String.valueOf(instant.getEpochSecond());
    }
    public static Long getCurrentTimeInSeconds() {
        Instant instant = Instant.now();
        return instant.getEpochSecond();
    }

    /**
     * Create symlink for submodule functionality.
     * @param  symlinkPath targetPath from cmd: symlink -d /repo/src -d /tmp/otherrepo
     * @return result of creating new symlink folder
     * @throws IOException
     */
    public static String createSymlink(String symlinkPath, String targetPath) throws IOException {
        return Files.createSymbolicLink(Path.of(symlinkPath), Path.of(targetPath)).toString();
    }
    public static void main(String[] args) {
        System.out.println("actions:"+Actions.values());
    }
}
