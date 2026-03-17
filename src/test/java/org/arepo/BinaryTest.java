package org.arepo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.arepo.ASettings.*;
import static org.arepo.AppGlobal.gson;
import static org.junit.jupiter.api.Assertions.*;

public class BinaryTest {
    String rootRepo = "root_repo";
    String prodRepo = "prod_repo";
    String testRepo = "test_repo";
    String devRepo = "dev_repo";
    String teamleadRepo = "teamlead_repo";
    Path rootPath;
    AFiles rootFiles;
    AFiles prodFiles;
    AFiles testFiles;
    AFiles devFiles;
    AFiles teamleadFiles;

    String email = "alex@mail.net";
    String email2 = "vic@mail.net";
    String email3 = "san@mail.net";
    int count = 10;

    String four = "src/java/Main.java";
    String five = "src/java/Model.java";

    String ignoreFile = "project.settings";
    String ignoreFileHidden = ".testignore";
    String buildedFile = "build/file.prj";

    String ignoreDir = "build";
    String ignoreDir2 = "images";
    String ignoreDirHidden = ".dirhidden";

    String one = "images/brand.jpg";
    String two = "profile.png";
    String three = "site.ico";

    int eol = 1;
    long sleepInMillis = 10L;
    @BeforeEach
    void setUp(@TempDir(cleanup = CleanupMode.DEFAULT) Path temp) throws Exception {
        rootPath = temp;

        Files.createDirectories(rootPath.resolve(rootRepo));
        Files.createDirectories(rootPath.resolve(prodRepo));
        Files.createDirectories(rootPath.resolve(testRepo));
        Files.createDirectories(rootPath.resolve(devRepo));
        Files.createDirectories(rootPath.resolve(teamleadRepo));

        rootFiles = new AFiles(rootPath.resolve(rootRepo));
        prodFiles = new AFiles(rootPath.resolve(prodRepo));
        testFiles = new AFiles(rootPath.resolve(testRepo));
        devFiles = new AFiles(rootPath.resolve(devRepo));
        teamleadFiles = new AFiles(rootPath.resolve(teamleadRepo));
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void cloneRepo() throws Exception {
        Path resourceDirectory = Paths.get("src","test","resources");

        var local = false;

        var eol = 1;
        var settings = rootFiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);

        settings = prodFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email, eol, false);
        assertTrue(settings.useFS);
        Response.StatusResult status = prodFiles.status();
        assertTrue(status.added.isEmpty());
        rootPath.resolve(prodRepo).resolve(one).toFile().getParentFile().mkdirs();
        Files.copy(resourceDirectory.resolve(one), rootPath.resolve(prodRepo).resolve(one));
        Files.copy(resourceDirectory.resolve(three), rootPath.resolve(prodRepo).resolve(three));
        Files.write(rootPath.resolve(prodRepo).resolve(two), "2 clone".getBytes());

        status = prodFiles.status();
        assertFalse(status.added.isEmpty());
        assertEquals(3, status.added.size());

        System.out.println("comment:"+ prodFiles.setComment(PA, "comment for a 1"));
        prodFiles.save(PA, Set.of(one, two));
        var patch = prodFiles.showPatch(PA);
        assertEquals(2, patch.getFilenamesList().size());
        //System.out.println("clone files 1:"+patch);

        var patchname = prodFiles.sendPatch(PA, local);
        prodFiles.accept(patchname);

        TimeUnit.MILLISECONDS.sleep(sleepInMillis);
        var download = prodFiles.download();
        System.out.println("download 1:"+download);
        System.out.println("comment 2:"+ prodFiles.setComment(PB, "comment for b 2"));
        prodFiles.save(PB, Set.of(three));

        patchname = prodFiles.sendPatch(PB, local);
        prodFiles.accept(patchname);

        TimeUnit.MILLISECONDS.sleep(sleepInMillis);
        download = prodFiles.download();
        System.out.println("download 2:"+download);

        assertEquals(2, rootFiles.getLogs(count).size());
        assertEquals(2, prodFiles.getLogs(count).size());

        status = prodFiles.status();
        System.out.println("status:"+status);
        var rootStatus = rootFiles.status();
        assertEquals(status.version, rootStatus.version);

        var email2 = "dev@mail.net";
        rootPath.resolve(testRepo).toFile().mkdirs();

        settings = testFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email2, eol, true);
        assertTrue(settings.useFS);
        // write some binary data to the file with same filename from root repo
        byte[] originalBytes = new byte[] { 1, 2, 3, 4, 5};
        Files.write(rootPath.resolve(testRepo).resolve(one), originalBytes);
        status = testFiles.status();
        System.out.println("status 3:"+status);
        assertTrue(status.added.isEmpty());
        assertTrue(status.deleted.isEmpty());
        assertTrue(status.erased.isEmpty());
        assertEquals(3, status.count);
        var modifiedFile = status.modified.stream().findFirst().get();
        System.out.println("modified:"+modifiedFile);
        long hash = modifiedFile.hash;
        var files = testFiles.getFiles();
        System.out.println("other clone files 1:"+files);
        assertEquals(3, files.size());

        download = testFiles.download();
        System.out.println("download 3:"+download);
        status = testFiles.status();
        System.out.println("status 4:"+status);
        assertEquals(DEFAULT_SHORT_LOGS_SIZE, testFiles.getLogs(count).size());
        files = testFiles.getFiles();
        System.out.println("other clone files 2:"+files);
        assertEquals(3, files.size());
        for(var file : files){
            if(file.getName().equals(one)){
                assertNotEquals(hash, file.getHash());
                break;
            }
        }
        testFiles.clearAndHead();
        status = testFiles.status();
        System.out.println("status 5:"+status);
        assertTrue(status.added.isEmpty());
        assertTrue(status.modified.isEmpty());
        assertTrue(status.deleted.isEmpty());
        assertTrue(status.erased.isEmpty());
    }

    @Test
    void adminPermissionsForActions() throws Exception {
        // users for actions send patch, accept and delete remote patch according admin mode, may be changed in repo settings file
        Path resourceDirectory = Paths.get("src", "test", "resources");
        var local = false;

        var eol = 1;
        var email2 = "writer@email.net";
        var settings = rootFiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);

        var settings2 = prodFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email2, eol, false);
        assertTrue(settings2.useFS);

        rootPath.resolve(prodRepo).resolve(one).toFile().getParentFile().mkdirs();

        Files.copy(resourceDirectory.resolve(one), rootPath.resolve(prodRepo).resolve(one));
        Files.copy(resourceDirectory.resolve(three), rootPath.resolve(prodRepo).resolve(three));
        Files.write(rootPath.resolve(prodRepo).resolve(two), "2 clone".getBytes());

        var status = prodFiles.status();
        System.out.println("status 1:"+status);
        assertFalse(status.added.isEmpty());
        assertEquals(3, status.added.size());

        System.out.println("comment:"+ prodFiles.setComment(PA, "comment for a 1"));
        prodFiles.save(PA, Set.of(one, two, three));
        prodFiles.upload(PA);

        //TODO add more actions !
        var download = prodFiles.download();
        System.out.println("download:"+download);
        status = prodFiles.status();
        System.out.println("status 2:"+status);
        assertTrue(status.added.isEmpty());
        assertTrue(status.deleted.isEmpty());
        assertTrue(status.modified.isEmpty());
        prodFiles.clearPatch(PA);
        var patches = prodFiles.patches();
        System.out.println("patches :"+patches);
    }
    @Test
    void manyRepos() throws Exception {

        Path resourceDirectory = Paths.get("src", "test", "resources");
        var local = false;

        var eol = 1;
        var email2 = "writer@email.net";
        // root repo
        var settings = rootFiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);
        var status = rootFiles.status();
        System.out.println("status 2:"+status);
        assertTrue(status.added.isEmpty());

        var files = rootFiles.getFiles();
        assertTrue(files.isEmpty());
        // prod repo
        var settings2 = prodFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email2, eol, false);
        assertTrue(settings2.useFS);
        var status2 = prodFiles.status();
        System.out.println("status 2:"+status2);
        var files2 = prodFiles.getFiles();
        assertTrue(files2.isEmpty());
        //var email3 = "dev@mail.net";
        // test repo
        //rootPath.resolve(testRepo).toFile().mkdirs();
        Files.createDirectories(rootPath.resolve(testRepo));


        settings = testFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email2, eol, false);
        assertTrue(settings.useFS);

        rootPath.resolve(testRepo).resolve(one).toFile().getParentFile().mkdirs();
        Files.copy(resourceDirectory.resolve(one), rootPath.resolve(testRepo).resolve(one));
        Files.copy(resourceDirectory.resolve(three), rootPath.resolve(testRepo).resolve(three));

        var status3 = testFiles.status();
        System.out.println("status 3:"+status3);
        assertFalse(status3.added.isEmpty());
        assertTrue(status3.deleted.isEmpty());
        assertTrue(status3.erased.isEmpty());
        assertEquals(2, status3.count);

        System.out.println("comment:"+testFiles.setComment(PA, "comment for a 1"));
        testFiles.save(PA, Set.of(one, three));
        testFiles.upload(PA);
        files2 = prodFiles.getFiles();
        assertTrue(files2.isEmpty());
        System.out.println("files 1:"+files2);
        // check in root repo files
        var download2 = prodFiles.download();
        System.out.println("download:"+download2);

        prodFiles.clearAndHead();// fill File System by our files !
        status2 = prodFiles.status();
        System.out.println("status 2 2:"+status2);
        files2 = prodFiles.getFiles();
        System.out.println("files 2:"+files2);
        assertEquals(2, files2.size());
    }

    @Test
    void ignoreFiles() throws Exception {
        Path resourceDirectory = Paths.get("src","test","resources");
        // root repo
        var settings = rootFiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);
        var status = rootFiles.status();
        System.out.println("status 1:"+status);
        assertTrue(status.added.isEmpty());
        // prod repo
        var settings2 = prodFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email2, eol, false);
        assertTrue(settings2.useFS);
        var status2 = prodFiles.status();
        System.out.println("status 2:"+status2);
        Files.createDirectories(rootPath.resolve(prodRepo).resolve(ignoreDir));
        Files.createDirectories(rootPath.resolve(prodRepo).resolve(ignoreDir2));
        Files.createDirectories(rootPath.resolve(prodRepo).resolve(ignoreDirHidden));
        Files.copy(resourceDirectory.resolve(two), rootPath.resolve(prodRepo).resolve(ignoreDir).resolve(two));
        Files.copy(resourceDirectory.resolve(ignoreFile), rootPath.resolve(prodRepo).resolve(ignoreDir2).resolve(ignoreFile));
        Files.copy(resourceDirectory.resolve(ignoreFileHidden), rootPath.resolve(prodRepo).resolve(ignoreDirHidden).resolve(ignoreFileHidden));

        settings2 = prodFiles.updateSettings(Set.of(ignoreDir, ignoreDir2, ignoreDirHidden), Set.of(ignoreDir+ SEP+two, three));
        System.out.println("settings prod:"+settings2);
        var binfile = "fileOne.bin";
        var binfile2 = "fileTwo.bin";
        byte[] originalBytes = new byte[] { 1, 2, 3, 4, 5};
        byte[] originalBytes2 = new byte[] { -3, -4, -5, -100};

        Files.write(rootPath.resolve(prodRepo).resolve(binfile), originalBytes);
        Files.write(rootPath.resolve(prodRepo).resolve(binfile2), originalBytes2);
        status2 = prodFiles.status();
        System.out.println("status 2 2:"+status2);
        assertFalse(status2.added.isEmpty());
        assertTrue(status2.modified.isEmpty());
        assertTrue(status2.deleted.isEmpty());
        assertEquals(2, status2.count);

        System.out.println("comment:"+ prodFiles.setComment(PA, "comment for a 1"));
        prodFiles.save(PA, Set.of(binfile, binfile2));
        var patch = prodFiles.showPatch(PA);
        assertEquals(2, patch.getFilenamesList().size());
        //System.out.println("clone files 1:"+patch);

        var patchname = prodFiles.upload(PA);
        System.out.println("upload:"+patchname);

        TimeUnit.MILLISECONDS.sleep(sleepInMillis);
        var download = prodFiles.download();
        System.out.println("download 1:"+download);
        status2 = prodFiles.status();
        System.out.println("status 2 3:"+status2);
        assertTrue(status2.added.isEmpty());
        assertTrue(status2.modified.isEmpty());
        assertTrue(status2.deleted.isEmpty());
        assertEquals(2, status2.count);
    }
    @Test
    void localPatchHistoryLongTime() throws Exception {
        Path resourceDirectory = Paths.get("src", "test", "resources");
        // root repo
        var settings = rootFiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);
        var status = rootFiles.status();
        System.out.println("status 1:" + status);
        assertTrue(status.added.isEmpty());
        // prod repo
        var settings2 = prodFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email2, eol, false);
        assertTrue(settings2.useFS);
        TimeUnit.SECONDS.sleep(1);
        var status2 = prodFiles.status();
        System.out.println("status 2:" + status2);
        var binfile = "fileOne.bin";
        var binfile2 = "fileTwo.bin";
        byte[] originalBytes = new byte[] { 1, 2, 3, 4, 5};
        byte[] originalBytes2 = new byte[] { -3, -4, -5, -100};

        Files.write(rootPath.resolve(prodRepo).resolve(binfile), originalBytes);
        Files.write(rootPath.resolve(prodRepo).resolve(binfile2), originalBytes2);
        status2 = prodFiles.status();
        System.out.println("status 2 2:"+status2);
        assertFalse(status2.added.isEmpty());
        assertTrue(status2.modified.isEmpty());
        assertTrue(status2.deleted.isEmpty());
        assertEquals(2, status2.count);
        var diff = prodFiles.diff(PA);
        System.out.println("diff:"+diff);
        assertTrue(diff.isEmpty());
        var patchHistory = prodFiles.getHistory(PA);
        System.out.println("patch history:"+patchHistory);
        assertTrue(patchHistory.isEmpty());
        System.out.println("comment:"+ prodFiles.setComment(PA, "comment for a 1"));
        patchHistory = prodFiles.getHistory(PA);
        System.out.println("patch history 2:"+patchHistory);
        assertFalse(patchHistory.isEmpty());
        prodFiles.save(PA, Set.of(binfile, binfile2));
        var patch = prodFiles.showPatch(PA);
        assertEquals(2, patch.getFilenamesList().size());
        TimeUnit.SECONDS.sleep(1);
        byte[] originalBytes3 = new byte[] { 10, 20, 30, 40, 50};
        Files.write(rootPath.resolve(prodRepo).resolve(binfile), originalBytes3);
        //prodFiles.save(PA, Set.of(binfile, binfile2));
        TimeUnit.SECONDS.sleep(1);
        patchHistory = prodFiles.getHistory(PA);
        System.out.println("patch history 3:"+patchHistory);
        assertFalse(patchHistory.isEmpty());
        prodFiles.clearPatch(PA);
        patchHistory = prodFiles.getHistory(PA);
        assertTrue(patchHistory.isEmpty());
        System.out.println("fs:"+new Response.FSFiles(prodFiles.getFSFiles()));

    }
    @Test
    void diffPatch() throws Exception {

        // root repo
        var settings = rootFiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);
        var status = rootFiles.status();
        System.out.println("status 1:" + status);
        assertTrue(status.added.isEmpty());
        // prod repo
        var settings2 = prodFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email2, eol, false);
        assertTrue(settings2.useFS);
        var status2 = prodFiles.status();
        System.out.println("status 2:" + status2);

        var binfile = "One.bin";
        var binfile2 = "Two.bin";
        byte[] originalBytes = new byte[] { 1, 2, 3, 4, 5};
        byte[] originalBytes2 = new byte[] { -3, -4, -5, -100};

        Files.write(rootPath.resolve(prodRepo).resolve(binfile), originalBytes);
        Files.write(rootPath.resolve(prodRepo).resolve(binfile2), originalBytes2);
        var diff = prodFiles.diff(PA);
        System.out.println("diff:"+diff);
        assertTrue(diff.isEmpty());
        status2 = prodFiles.status();
        System.out.println("status 2 2:"+status2);
        assertFalse(status2.added.isEmpty());

        var comment = """
                Header for comment.
                
                multi line comment
                with some content...
                """;
        System.out.println("comment:"+ prodFiles.setComment(PA, comment));

        prodFiles.save(PA, Set.of(binfile, binfile2));
        var patch = prodFiles.showPatch(PA);
        assertEquals(2, patch.getFilenamesList().size());
        System.out.println("saved patch:"+patch);
        Files.write(rootPath.resolve(prodRepo).resolve(binfile), new byte[] { 1, 2});

        diff = prodFiles.diff(PA);
        System.out.println("diff 2:"+diff);
        assertFalse(diff.isEmpty());
        assertEquals(1, diff.size());
        assertTrue(diff.containsKey(binfile));
        var files = prodFiles.getFSFiles();
        System.out.println("files:"+files);

        status2 = prodFiles.status();
        System.out.println("status 2 3:"+status2);
        assertEquals(3, status2.added.size());
        prodFiles.removeFiles(Set.of(binfile+ASettings.FILE_CONFLICT));
        status2 = prodFiles.status();
        System.out.println("status 2 4:"+status2);
        assertEquals(2, status2.added.size());
        var countTestFiles = prodFiles.removeFiles(Set.of(binfile, binfile2));
        // deleting our files for checking creating test files functionality ...
        status2 = prodFiles.status();
        System.out.println("status 2 5:"+status2);
        assertTrue(status2.added.isEmpty());
        assertTrue(status2.modified.isEmpty());
        assertTrue(status2.deleted.isEmpty());
        var countTestFiles2 = prodFiles.createFiles();
        status2 = prodFiles.status();
        System.out.println("status 2 6:"+status2);
        assertEquals(countTestFiles2, status2.added.size());
        assertEquals(countTestFiles2, status2.count);
        prodFiles.applyPatch2FS(PA);
        status2 = prodFiles.status();
        System.out.println("status 2 7:"+status2);
        assertEquals(countTestFiles+countTestFiles2, status2.added.size());
        assertEquals(countTestFiles+countTestFiles2, status2.count);

        prodFiles.clearPatch(PA);
        prodFiles.clearFS();
        countTestFiles2 = prodFiles.createFiles();
        status2 = prodFiles.status();
        System.out.println("status 2 8:"+status2);
        assertEquals(countTestFiles2, status2.added.size());
        assertEquals(countTestFiles2, status2.count);

        System.out.println("comment:"+ prodFiles.setComment(PA, comment));
        prodFiles.save(PA, Set.of(TEST_TEXT_FILENAME, TEST_BINARY_FILENAME));
        prodFiles.upload(PA);
        prodFiles.clearPatch(PA);
        prodFiles.download();

        TimeUnit.SECONDS.sleep(1);
        prodFiles.clearFS();
        prodFiles.clearAndHead();
        status2 = prodFiles.status();
        System.out.println("status 2 9:"+status2);

        Files.write(rootPath.resolve(prodRepo).resolve(TEST_TEXT_FILENAME), "new content".getBytes());
        status2 = prodFiles.status();
        System.out.println("status 2 10:"+status2);
        var countTestFiles3 = prodFiles.removeFiles(Set.of(TEST_TEXT_FILENAME));
        assertEquals(1, countTestFiles3);
        // adding deleted file to patch and check if this file will be deleted in applyPatch method
        System.out.println("comment:"+ prodFiles.setComment(PA, "patch with deleted file, but in root repo!"));
        prodFiles.save(PA, Set.of(TEST_TEXT_FILENAME));
        patch = prodFiles.showPatch(PA);
        assertEquals(1, patch.getFilenamesList().size());
        System.out.println("saved patch 2:"+patch);

        Files.write(rootPath.resolve(prodRepo).resolve(TEST_TEXT_FILENAME), "new content".getBytes());
        prodFiles.applyPatch2FS(PA); //TEST_TEXT_FILENAME - this file should be deleted if exists!
        status2 = prodFiles.status();
        System.out.println("status 2 11:"+status2);
    }

}
