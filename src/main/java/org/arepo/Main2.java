package org.arepo;

import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;

import static org.arepo.AppGlobal.*;

@CommandLine.Command(name = "arepo", showEndOfOptionsDelimiterInUsageHelp = true, mixinStandardHelpOptions = true, version = "V1.0", description = "Displays the @|bold User|@ profile information.")
public class Main2 implements Callable<String> {
    public static Path currentRelativePath;
    @CommandLine.Parameters(index = "0",  description = "Action for commands")
    AppGlobal.Actions action = AppGlobal.Actions.status;
    @CommandLine.Option(names = {"-a"}, description = "access token")
    private String token = "";
    @CommandLine.Option(names = {"-u"}, description = "username")
    private String username = "";
    @CommandLine.Option(names = {"-pw"}, description = "password")
    private String password = "";
    @CommandLine.Option(names = {"-t"}, description = "user type")
    Integer type = 0;
    @CommandLine.Option(names = {"-s"}, description = "user status")
    Integer status = 0;
    @CommandLine.Option(names = {"-ui"}, description = "user id")
    private Long userId = 0L;
    @CommandLine.Option(names = {"-e"}, description = "email")
    private String email = "";
    @CommandLine.Option(names = {"-c"}, description = "comment")
    private String comment = "";
    @CommandLine.Option(names = {"-p"}, description = "patchname")
    private String patchname = ASettings.PA;
    @CommandLine.Option(names = {"-fn"}, split = ",", description = "filenames")
    private Set<String> filenames = new HashSet<String>();
    @CommandLine.Option(names = {"-d"}, split = ",", description = "directories")
    private List<String> ignoreDirs = new ArrayList<String>();
    @CommandLine.Option(names = {"-if"}, split = ",", description = "ignore files")
    private Set<String> ignoreFiles = new HashSet<String>();
    @CommandLine.Option(names = {"-sl"}, description = "short of logs. If true - loads from remote repo only 1 last logs.")
    boolean shortlogs = false; // For example: -f root_1 -sl -e root@mail.net
    @CommandLine.Option(names = {"-lp"}, description = "Local patch - false. If true - loads and send local patch from/to local file system.")
    boolean local = false;
    @CommandLine.Option(names = {"-count"}, description = "undo logs by specified count and write remaining files to the file system")
    Integer count = 10;
    @CommandLine.Option(names = {"-tm"}, description = "time of updated")
    Long time = 0L;
    @CommandLine.Option(names = {"-lv"}, description = "Log version - created field in Log record")
    Long version = 0L;
    // init -f ../../.gitignore -f deps/gson-2.8.9.jar
    //@CommandLine.Option(names = {"-f"}, description = "files")
    //private Set<Path> files = new HashSet<Path>();
    @CommandLine.Option(names = {"-f"}, split = ",", description = "files")
    private LinkedHashSet<String> files = new LinkedHashSet<>(2);
    @Override
    public String call() throws Exception {
        //System.out.println("act:"+action);
        Object result = List.of();
        var settings = getSettings(currentRelativePath);
        if(email.isEmpty()){
            email = settings.email;
        }else {
            settings.email = email;
        }

        var alist = new ArrayList<String>(files);//files.stream().toList();
        var dir = alist.size() > 0?alist.get(0):settings.repo;
        var root = alist.size() > 1?alist.get(1):ASettings.ROOT_PREFIX;
        var afiles = new AFiles(dir);
        try {
            switch (action) {
                case setup -> result = setup(afiles, settings, root, shortlogs);
                case status -> result = afiles.status();
                case save -> result = afiles.save(patchname, filenames);
                case remove -> afiles.removeFromPatch(patchname, filenames);
                case comment -> result = afiles.setComment(patchname, comment);
                case upload -> result = afiles.upload(patchname);
                case download -> result = afiles.download();
                case send -> result = afiles.sendPatch(patchname, local);
                case get -> result = afiles.getPatch(patchname, local);
                //case history -> result = afiles.applyHistory2FS(patchname, time);
                case clear -> afiles.clearFS();
                case diff -> result = afiles.diff(patchname);
                case apply -> result = afiles.applyPatch2FS(patchname);
                case erase -> result = afiles.clearPatch(patchname);
                case head -> afiles.clearAndHead();
                case logs -> result = afiles.getLogs(count);
                case logsbyname -> result = afiles.logsByFilename(filenames);
                case undo -> result = afiles.applyFSByLogsByCountLimit(count);
                case files -> result = afiles.getFiles();
                case patches -> result = afiles.patches();
                case show -> result = afiles.showPatch(patchname);
                case delete -> result = afiles.deletePatch(patchname);
                case deleteRemotePatch -> result = afiles.deleteRemotePatch(patchname);
                case patchHistory -> result = afiles.getHistory(patchname);
                case settings -> result = getRepoSettings(afiles);
                case accept -> result = afiles.accept(patchname);
                case updateRemoteLog -> result = afiles.updateLog(version, comment);
                case getRemoteLogs -> result = afiles.getLogsAfterVersion(version);
                case symlink -> result = createSymlink(ignoreDirs.get(0), ignoreDirs.get(1));
                case fs -> result = new Response.FSFiles(afiles.getFSFiles());
                case removeFiles -> result = afiles.removeFiles(filenames);
                case createTestFiles -> result = afiles.createFiles();//TODO for testing only !
                case init -> result = ASettings.defindedPatches;//TODO for testing only !
                default -> result = "Unexpected action: " + action;
            }
            return gson.toJson(result);
        } catch (Exception e) {
            return ERROR+e.getMessage();
        }
    }

    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new Main2());
        int exitCode = cmd.execute(args);
        String result = cmd.getExecutionResult();
        if(result != null) cmd.getOut().println(result);
        System.exit(exitCode);
    }
    /**
     * Setup root or clone repository files in the directory.
     *<p>
     * Usage example for root:<code> setup -f root/dir -e user@mail.net</code>
     *<p>
     * Usage example for clone: <code>setup -f clone/dir,root/dir -e user@mail.net</code>
     * <p>or
     * <p>
     * <code>setup -f clone/dir -f root/dir -e user@mail.net</code>
     * @param afiles
     * @param settings
     * @param root
     * @return RepoSettings
     * @throws Exception
     */
    private Response.RepoSettings setup(AFiles afiles, UserSettings settings, String root, boolean shortLogs) throws Exception {
        var eol = 0;
        System.out.println("setup x:"+root);
        var result = afiles.setup(root, email.trim(), eol, shortLogs);
        if(!email.trim().isEmpty()) {
            set(afiles, settings);
        }
        return result;
    }
    public UserSettings set(AFiles afiles, UserSettings settings) throws IOException {
        settings.repo = afiles.dirPath.toString();
        saveSettings(currentRelativePath, settings);
        return settings;
    }
    public UserSettings getSettings(Path dirPath) throws IOException {
        if(Files.notExists(dirPath.toAbsolutePath().resolve(ASettings.STATE_FILENAME))){
            saveSettings(dirPath, new UserSettings());
        }
        return gson.fromJson(Files.readString(dirPath.toAbsolutePath().resolve(ASettings.STATE_FILENAME)), UserSettings.class);
    }
    public void saveSettings(Path dirPath, UserSettings settings) throws IOException {
        Files.write(dirPath.toAbsolutePath().resolve(ASettings.STATE_FILENAME), gson.toJson(settings).getBytes());
    }
    /**
     * Update repo settings for ignore dirs or files or get settings if -id and -if arrays are empty.
     * <p>
     *     Usage for update ignore directories: <code>settings -id sub/directory,.project </code>
     *     <p>or update ignore files: <p>
     *     <code>settings -if dir/file.txt,readme.md</code>
     *     <p>
     *     or get settings: <p>
     *         <code>settings</code>
     *
     * @param afiles
     * @return
     * @throws Exception
     */
    private Response.RepoSettings getRepoSettings(AFiles afiles) throws Exception {
        return afiles.model.get();
    }


}
