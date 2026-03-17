package org.arepo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.arepo.ASettings.*;
import static org.arepo.AppGlobal.*;
import static org.junit.jupiter.api.Assertions.*;

class MainTest {

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
    String one = "index.html";
    String two = "readme.md";
    String three = "src/first.txt";
    String four = "src/java/Main.java";
    String five = "src/java/Model.java";

    String ignoreFile = "project.settings";
    String ignoreFileHidden = ".testignore";
    String buildedFile = "build/file.prj";

    String ignoreDir = "build";
    String ignoreDir2 = "images";
    String ignoreDirHidden = ".dirhidden";
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
    void testStatus2() throws Exception {

        var two = "second.php";
        var three = "sub/folder/subfolder/somefile.txt";

        for(var file : Set.of(three, two)){
            var tmpArray = file.split(ASettings.SEP, 2);
            var top = tmpArray.length > 1?tmpArray[0]:DEFAULT_FOLDER_ROOT_NAME;
            System.out.println("root is top:"+top+";"+file);
        }
    }
    @Test
    void testStatus1() throws Exception {
        Path resourceDirectory = Paths.get("src","test","resources");
        //var two = "second.php";
        //var three = "sub/folder/somefile.txt";
        var three2 = "src/second.txt";
        var three3 = "sub/folder/somefile2.txt";
        //var three4 = "sub/somefile3.txt";
        var three5 = "first.php";

        rootPath.resolve(prodRepo).resolve(three).toFile().getParentFile().mkdirs();
        rootPath.resolve(prodRepo).resolve(four).toFile().getParentFile().mkdirs();
        rootPath.resolve(prodRepo).resolve(three3).toFile().getParentFile().mkdirs();

        Files.copy(resourceDirectory.resolve(one), rootPath.resolve(prodRepo).resolve(one));
        Files.copy(resourceDirectory.resolve(two), rootPath.resolve(prodRepo).resolve(two));
        Files.copy(resourceDirectory.resolve(three), rootPath.resolve(prodRepo).resolve(three));

        Files.copy(resourceDirectory.resolve(four), rootPath.resolve(prodRepo).resolve(four));

        Files.write(rootPath.resolve(prodRepo).resolve(three2), "2 clone 1".getBytes());
        Files.write(rootPath.resolve(prodRepo).resolve(three3), "2 clone 2".getBytes());
        //Files.write(rootPath.resolve(prodRepo).resolve(three4), "2 clone 3".getBytes());


        //var allFiles = FSUtils.getAllFiles4(rootPath.resolve(cloneRepo), Set.of(DIR_NAME));
        //System.out.println("files: "+allFiles);

        var otherFiles = FSUtils.getAllFiles7(rootPath.resolve(prodRepo), Set.of(three), Set.of(DIR_NAME));
        System.out.println("files 2:"+otherFiles);

        var eol = 1;
        var settings = rootFiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);
        var local = false;
        settings = prodFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email, eol, false);
        assertTrue(settings.useFS);

        prodFiles.download();
        var status = prodFiles.status3();
        System.out.println("st :"+status);
        assertFalse(status.added.isEmpty());
        assertTrue(status.modified.isEmpty());
        assertEquals(6, status.added.size());

        prodFiles.save(PA, Set.of(one, two, three, three2));
        System.out.println("comment:"+ prodFiles.setComment(PA, "comment for a 1"));
        prodFiles.upload(PA);
        prodFiles.clearPatch(PA);

        TimeUnit.MILLISECONDS.sleep(1);
        var download = prodFiles.download();
        System.out.println("download 1:"+download);

        status = prodFiles.status3();
        System.out.println("st 2:"+status);
        assertFalse(status.added.isEmpty());

        prodFiles.save(PB, Set.of(three3, four));
        System.out.println("comment:"+ prodFiles.setComment(PB, "comment for b 1"));
        prodFiles.upload(PB);
        prodFiles.clearPatch(PB);
        TimeUnit.MILLISECONDS.sleep(1);
        download = prodFiles.download();
        System.out.println("download 2:"+download);
        status = prodFiles.status3();
        System.out.println("st 3:"+status);
        assertTrue(status.added.isEmpty());

        Files.write(rootPath.resolve(prodRepo).resolve(two), "2 new content".getBytes());
        Files.write(rootPath.resolve(prodRepo).resolve(three5), "3 content".getBytes());
        status = prodFiles.status3();
        System.out.println("st 4:"+status);
        assertEquals(1, status.added.size());
        assertEquals(1, status.modified.size());
    }
    @Test
    void one() throws Exception {
        //var one = "index.php";

        //var three = "docs.txt";
        var eol = 1;
        rootPath.resolve(prodRepo).resolve(three).toFile().getParentFile().mkdirs();
        Files.write(rootPath.resolve(rootRepo).resolve(one), "1 clone".getBytes());

        var settings = rootFiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);

        settings = prodFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email, eol, false);
        assertTrue(settings.useFS);

        var str = "Hello content";
        System.out.println("bytes:"+ Arrays.toString(str.getBytes()));
        prodFiles.download();
        System.out.println("status:"+ prodFiles.status());

        settings = prodFiles.updateSettings(Set.of(), Set.of(one));
        assertTrue(settings.ignoreDirs.isEmpty());
        assertFalse(settings.ignoreFiles.isEmpty());

        prodFiles.clearFS();
        assertTrue(Files.exists(rootPath.resolve(rootRepo).resolve(one)));

        Files.write(rootPath.resolve(prodRepo).resolve(two), "2 clone".getBytes());
        rootPath.resolve(prodRepo).resolve(three).toFile().getParentFile().mkdirs();
        Files.write(rootPath.resolve(prodRepo).resolve(three), "2 clone".getBytes());

        prodFiles.save(PA, Set.of(two, three));
        System.out.println("comment:"+ prodFiles.setComment(PA, "comment for a 1"));
        prodFiles.upload(PA);
        prodFiles.clearPatch(PA);

        TimeUnit.MILLISECONDS.sleep(1);
        var download = prodFiles.download();
        System.out.println("download 1:"+download);
        var status = prodFiles.status();
        assertTrue(status.added.isEmpty());
        var patches = prodFiles.patches();
        assertEquals(defindedPatches.size(), patches.size());
        var patch = prodFiles.showPatch(PA);
        System.out.println("patch 1:"+patch);
        assertTrue(patch.comment.isEmpty());
    }
    @Test
    void localPatches() throws Exception {
        Path resourceDirectory = Paths.get("src","test","resources");
        //var one = "index.php";
        //var two = "readme.md";
        //var three = "docs.txt";
        var eol = 1;
        var local = true;

        var settings = rootFiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);

        settings = prodFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email, eol, false);
        assertTrue(settings.useFS);

        rootPath.resolve(prodRepo).resolve(three).toFile().getParentFile().mkdirs();

        Files.copy(resourceDirectory.resolve(one), rootPath.resolve(prodRepo).resolve(one));
        Files.copy(resourceDirectory.resolve(two), rootPath.resolve(prodRepo).resolve(two));
        Files.copy(resourceDirectory.resolve(three), rootPath.resolve(prodRepo).resolve(three));
        /*
        Files.write(rootPath.resolve(prodRepo).resolve(one), "1 clone".getBytes());
        Files.write(rootPath.resolve(prodRepo).resolve(two), "1 clone".getBytes());
        Files.write(rootPath.resolve(prodRepo).resolve(three), "1 clone".getBytes());
        */
        var status = prodFiles.status();
        assertFalse(status.added.isEmpty());
        assertEquals(3, status.added.size());

        System.out.println("comment:"+ prodFiles.setComment(PA, "comment for a 1"));
        prodFiles.save(PA, Set.of(one, two, three));

        var patchname = prodFiles.sendPatch(PA, local);

        rootPath.resolve(testRepo).toFile().mkdirs();
        var otherFiles = new AFiles(rootPath.resolve(testRepo));
        settings = otherFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email, eol, true);
        assertTrue(settings.useFS);
        var download = otherFiles.download();
        System.out.println("download 3:"+download);
        status = otherFiles.status();
        System.out.println("status 3:"+status);

        //send this patch to other repo to DIR_NAME folder in any way ...
        Files.copy(rootPath.resolve(prodRepo).resolve(DIR_NAME).resolve(patchname), rootPath.resolve(testRepo).resolve(DIR_NAME).resolve(patchname));

        var localPatches = otherFiles.getPatch(patchname, local);
        System.out.println("local patches:"+localPatches);
        assertFalse(localPatches.isEmpty());
        var patch = localPatches.get(0);
        System.out.println("local patch:"+patch);
        assertEquals(3, patch.getFilenamesList().size());
    }

    @Test
    void testDB2() throws Exception {

        var eol = 1;
        var settings = rootFiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);
        var local = false;

        settings = prodFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email, eol, false);
        assertTrue(settings.useFS);

        var status = prodFiles.status();
        assertTrue(status.added.isEmpty());
        Files.write(rootPath.resolve(prodRepo).resolve(one), "1 clone".getBytes());
        Files.write(rootPath.resolve(prodRepo).resolve(two), "2 clone".getBytes());

        status = prodFiles.status();
        assertFalse(status.added.isEmpty());

        System.out.println("comment:"+ prodFiles.setComment(PA, "comment for a 1"));
        prodFiles.save(PA, Set.of(one, two));
        var founded = prodFiles.model.patchModel.findByPatchname(PA);
        System.out.println("files final:"+founded);
        var findByFilename = prodFiles.model.patchModel.findInPatchByFilename(PA, "some.txt");
        System.out.println("find by filename:"+findByFilename);

        var patchname = prodFiles.sendPatch(PA, local);
        System.out.println("send to remote:"+patchname);

        var remotePatchFiles = prodFiles.getPatch(patchname, local);
        assertFalse(remotePatchFiles.isEmpty());
        System.out.println("get remote files from patch:"+remotePatchFiles);
    }
    @Test
    void uploadPatch() throws Exception {
        var eol = 1;
        var settings = rootFiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);

        settings = prodFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email, eol, false);
        assertTrue(settings.useFS);

        assertTrue(rootFiles.getLogs(count).isEmpty());
        assertTrue(prodFiles.getLogs(count).isEmpty());


        var status = prodFiles.status();
        assertTrue(status.added.isEmpty());
        Files.write(rootPath.resolve(prodRepo).resolve(one), "1 clone".getBytes());
        Files.write(rootPath.resolve(prodRepo).resolve(two), "2 clone".getBytes());

        status = prodFiles.status();
        assertFalse(status.added.isEmpty());

        System.out.println("comment:"+ prodFiles.setComment(PA, "comment for a 1"));
        prodFiles.save(PA, Set.of(one, two));

        var patchname = prodFiles.upload(PA);
        System.out.println("send this patch name to friend:"+patchname);
        prodFiles.download();

        assertEquals(1, rootFiles.getLogs(count).size());
        assertEquals(1, prodFiles.getLogs(count).size());
        status = prodFiles.status();
        var rootStatus = rootFiles.status();
        assertEquals(status.version, rootStatus.version);
    }
    @Test
    void acceptPatch() throws Exception {
        var eol = 1;
        var settings = rootFiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);

        settings = prodFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email, eol, false);
        assertTrue(settings.useFS);

        assertTrue(rootFiles.getLogs(count).isEmpty());
        assertTrue(prodFiles.getLogs(count).isEmpty());
        //var one = "first.txt";
        //var two = "second.php";
        var local = false;

        var status = prodFiles.status();
        assertTrue(status.added.isEmpty());
        Files.write(rootPath.resolve(prodRepo).resolve(one), "1 clone".getBytes());
        Files.write(rootPath.resolve(prodRepo).resolve(two), "2 clone".getBytes());

        status = prodFiles.status();
        assertFalse(status.added.isEmpty());

        System.out.println("comment:"+ prodFiles.setComment(PA, "comment for a 1"));
        prodFiles.save(PA, Set.of(one, two));

        var patchname = prodFiles.sendPatch(PA, local);
        prodFiles.accept(patchname);

        prodFiles.download();
        assertEquals(1, rootFiles.getLogs(count).size());
        assertEquals(1, prodFiles.getLogs(count).size());

        status = prodFiles.status();
        var rootStatus = rootFiles.status();
        assertEquals(status.version, rootStatus.version);

        Files.write(rootPath.resolve(prodRepo).resolve(one), "1 clone\n other line added.".getBytes());
        status = prodFiles.status();
        assertFalse(status.modified.isEmpty());
        prodFiles.save(PB, Set.of(one));
        System.out.println("comment:"+ prodFiles.setComment(PB, "comment for b changed one file."));

        patchname = prodFiles.upload(PB);
        prodFiles.download();

        assertEquals(2, rootFiles.getLogs(count).size());
        assertEquals(2, prodFiles.getLogs(count).size());
    }
    @Test
    void acceptPatchFromOtherRepo() throws Exception {
        var eol = 1;
        var settings = rootFiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);

        settings = prodFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email, eol, false);
        assertTrue(settings.useFS);

        //var one = "first.txt";
        //var two = "second.php";
        var local = false;

        var status = prodFiles.status();
        assertTrue(status.added.isEmpty());
        Files.write(rootPath.resolve(prodRepo).resolve(one), "1 clone".getBytes());
        Files.write(rootPath.resolve(prodRepo).resolve(two), "2 clone".getBytes());

        status = prodFiles.status();
        assertFalse(status.added.isEmpty());
        assertTrue(status.modified.isEmpty());
        assertTrue(status.deleted.isEmpty());
        assertTrue(status.erased.isEmpty());


        System.out.println("comment:"+ prodFiles.setComment(PA, "comment for a 1"));
        prodFiles.save(PA, Set.of(one, two));

        var patchname = prodFiles.sendPatch(PA, local);
        prodFiles.clearPatch(PA);
        prodFiles.clearFS();

        var email2 = "dev@mail.net";
        rootPath.resolve(testRepo).toFile().mkdirs();
        var otherFiles = new AFiles(rootPath.resolve(testRepo));
        settings = otherFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email2, eol, true);
        assertTrue(settings.useFS);
        var download = otherFiles.download();
        System.out.println("download 3:"+download);
        status = otherFiles.status();
        System.out.println("status 3:"+status);
        var res = otherFiles.accept(patchname);

        download = otherFiles.download();
        assertEquals(2, download.files.size());
        assertEquals(2, download.logs.size());
        otherFiles.clearAndHead();
        status = otherFiles.status();
        System.out.println("status 4:"+status);
        assertTrue(status.added.isEmpty());
        assertEquals(2, otherFiles.getFiles().size());

        download = prodFiles.download();
        assertEquals(2, download.files.size());
        assertEquals(2, download.logs.size());
        prodFiles.clearAndHead();
        assertTrue(Files.exists(rootPath.resolve(prodRepo).resolve(one)));
        assertTrue(Files.exists(rootPath.resolve(prodRepo).resolve(two)));

        assertTrue(Files.exists(rootPath.resolve(testRepo).resolve(one)));
        assertTrue(Files.exists(rootPath.resolve(testRepo).resolve(two)));

    }
    @Test
    void getPatch() throws Exception {
        var eol = 1;
        var settings = rootFiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);
        var local = false;

        settings = prodFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email, eol, false);
        assertTrue(settings.useFS);

        assertTrue(rootFiles.getLogs(count).isEmpty());
        assertTrue(prodFiles.getLogs(count).isEmpty());
        //var one = "first.txt";
        //var two = "second.php";

        var status = prodFiles.status();
        assertTrue(status.added.isEmpty());
        Files.write(rootPath.resolve(prodRepo).resolve(one), "1 clone".getBytes());
        Files.write(rootPath.resolve(prodRepo).resolve(two), "2 clone".getBytes());

        status = prodFiles.status();
        assertFalse(status.added.isEmpty());

        System.out.println("comment:"+ prodFiles.setComment(PA, "comment for a 1"));
        prodFiles.save(PA, Set.of(one, two));

        var patchname = prodFiles.sendPatch(PA, local);
        prodFiles.getPatch(patchname, local);
        var patches = prodFiles.patches();
        assertEquals(defindedPatches.size()+1, patches.size());

        prodFiles.clearPatch(PA);
        Files.delete(rootPath.resolve(prodRepo).resolve(one));

        var patchfiles = prodFiles.applyPatch2FS(patchname);
        assertFalse(patchfiles.isEmpty());
        assertTrue(Files.exists(rootPath.resolve(prodRepo).resolve(one)));

        prodFiles.deletePatch(patchname);
        var deleteRemotePatchResp = prodFiles.deleteRemotePatch(patchname);
        assertEquals(1, deleteRemotePatchResp);

    }
    @Test
    void workPatch() throws Exception {
        var eol = 1;
        var settings = rootFiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);

        settings = prodFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email, eol, false);
        assertTrue(settings.useFS);

        // clone this repo from prodRepo folder
        var settings3 = testFiles.setup(rootPath.resolve(prodRepo).toAbsolutePath().toString(), email2, eol, false);
        assertTrue(settings3.useFS);
        var status3 = testFiles.status();
        System.out.println("test status 3:"+status3);

        assertTrue(rootFiles.getLogs(count).isEmpty());
        assertTrue(prodFiles.getLogs(count).isEmpty());
        //var one = "first.txt";
        //var two = "second.php";

        var status = prodFiles.status();
        assertTrue(status.added.isEmpty());
        assertTrue(status.modified.isEmpty());
        assertTrue(status.deleted.isEmpty());
        Files.write(rootPath.resolve(prodRepo).resolve(one), "1 clone".getBytes());
        Files.write(rootPath.resolve(prodRepo).resolve(two), "2 clone".getBytes());

        status = prodFiles.status();
        assertFalse(status.added.isEmpty());

        prodFiles.save(PA, Set.of(two));
        var patch = prodFiles.showPatch(PA);
        System.out.println("show:"+patch);

        System.out.println("comment:"+ prodFiles.setComment(PA, "comment for a 1"));
        prodFiles.save(PA, Set.of(one, two));
        patch = prodFiles.showPatch(PA);
        System.out.println("show 2:"+patch);

        prodFiles.removeFromPatch(PA, Set.of(one, "wrong_file.txt"));
        patch = prodFiles.showPatch(PA);
        assertEquals(1, patch.getFilenamesList().size());
        System.out.println("show 3:"+patch);
        prodFiles.upload(PA);
        TimeUnit.MILLISECONDS.sleep(count);
        prodFiles.download();
        prodFiles.clearPatch(PA);

        status = prodFiles.status();
        System.out.println("status:"+status);
        assertFalse(status.added.isEmpty());
        assertEquals(2, status.count);
        var contentA = "2 clone 222";
        Files.write(rootPath.resolve(prodRepo).resolve(two), contentA.getBytes());
        prodFiles.save(PA, Set.of(one, two));
        patch = prodFiles.showPatch(PA);
        System.out.println("show 4:"+patch);
        System.out.println("comment 2:"+ prodFiles.setComment(PA, "comment for a 2"));
        prodFiles.upload(PA);
        TimeUnit.MILLISECONDS.sleep(count);
        prodFiles.download();
        prodFiles.clearPatch(PA);
        var files = prodFiles.getFiles();
        for(var file : files){
            if(file.getName().equals(two)){
                System.out.println("a check:"+ Arrays.toString(contentA.getBytes()) +";"+file.toString());
                assertEquals(contentA.getBytes().length, file.getContent().length);
            }
        }

        status3 = testFiles.status();
        System.out.println("test status 3:"+status3);
        var download3 = testFiles.download();
        System.out.println("test download:"+download3);
        var files3 = testFiles.getFiles();
        System.out.println("test files:"+files3);
        status3 = testFiles.status();
        System.out.println("test status 3 2:"+status3);
        assertFalse(status3.deleted.isEmpty());
        assertEquals(2, status3.count);
        testFiles.clearAndHead();
        status3 = testFiles.status();
        System.out.println("test status 3 3:"+status3);
        assertTrue(status3.added.isEmpty());
        assertTrue(status3.modified.isEmpty());
        assertTrue(status3.deleted.isEmpty());
        // try to upload file to parent repo

        var newfile = "index.java";
        Files.write(rootPath.resolve(testRepo).resolve(newfile), "2 clone 111".getBytes());
        testFiles.save(PA, Set.of(newfile));
        var patch3 = testFiles.showPatch(PA);
        System.out.println("show :"+patch3);
        status3 = testFiles.status();
        System.out.println("test status 3 4:"+status3);
        assertFalse(status3.added.isEmpty());
        assertEquals(1, status3.added.size());
        assertTrue(status3.modified.isEmpty());
        assertTrue(status3.deleted.isEmpty());

        System.out.println("comment 2:"+testFiles.setComment(PA, "comment for c 1"));
        testFiles.upload(PA);

        //check this file from parent
        status = prodFiles.status();
        System.out.println("parent status:"+status);
        assertEquals(3, status.count);
    }
    @Test
    void twoUsers() throws Exception {
        var eol = 1;
        var settings = rootFiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);

        var email2 = "user@mail.net";
        settings = prodFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email2, eol, false);
        assertTrue(settings.useFS);

        assertTrue(rootFiles.getLogs(count).isEmpty());
        assertTrue(prodFiles.getLogs(count).isEmpty());
        //var one = "src/first.txt";
        //var two = "second.php";
        //var three = "sub/folder/somefile.txt";

        var status = prodFiles.status();
        assertTrue(status.added.isEmpty());
        //rootPath.resolve(prodRepo).resolve(one).toFile().getParentFile().mkdirs();
        rootPath.resolve(prodRepo).resolve(three).toFile().getParentFile().mkdirs();
        Files.write(rootPath.resolve(prodRepo).resolve(one), "1 clone".getBytes());
        Files.write(rootPath.resolve(prodRepo).resolve(two), "2 clone".getBytes());
        Files.write(rootPath.resolve(prodRepo).resolve(three), "2 clone".getBytes());
        status = prodFiles.status();
        assertFalse(status.added.isEmpty());

        System.out.println("comment:"+ prodFiles.setComment(PA, "comment for a 1"));
        prodFiles.save(PA, Set.of(one, two, three));
        prodFiles.upload(PA);

        prodFiles.download();
        prodFiles.clearPatch(PA);
        var patch = prodFiles.showPatch(PA);

        assertEquals(0, patch.getFilenamesList().size());

        prodFiles.clearFS();
        status = prodFiles.status();
        System.out.println("status 3:"+status);
        assertTrue(status.added.isEmpty());
        assertEquals(3, status.deleted.size());
        assertFalse(Files.exists(rootPath.resolve(prodRepo).resolve(two)));

        prodFiles.clearAndHead();
        status = prodFiles.status();
        System.out.println("status 4:"+status);
        assertTrue(status.added.isEmpty());
        assertTrue(status.deleted.isEmpty());
        assertTrue(Files.exists(rootPath.resolve(prodRepo).resolve(two)));
    }
    @Test
    void localHistory() throws Exception {
        // local history functionality by defined patches like 'a'
        var eol = 1;
        var settings = rootFiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);

        var email2 = "user@mail.net";
        settings = prodFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email2, eol, false);
        assertTrue(settings.useFS);

        assertTrue(rootFiles.getLogs(count).isEmpty());
        assertTrue(prodFiles.getLogs(count).isEmpty());


        var status = prodFiles.status();
        assertTrue(status.added.isEmpty());
        Files.write(rootPath.resolve(prodRepo).resolve(one), "1 clone".getBytes());
        Files.write(rootPath.resolve(prodRepo).resolve(two), "2 clone".getBytes());
        long sizeOne = Files.size(rootPath.resolve(prodRepo).resolve(one));
        status = prodFiles.status();
        assertFalse(status.added.isEmpty());
        //write content for defined patch 'a'
        System.out.println("comment:"+ prodFiles.setComment(PA, "comment for a 1"));
        prodFiles.save(PA, Set.of(one, two));
        //write other content in these files
        Files.write(rootPath.resolve(prodRepo).resolve(one), "1 clone 222".getBytes());
        Files.write(rootPath.resolve(prodRepo).resolve(two), "2 clone 333".getBytes());
        //save new changes in other defined patch 'b'
        System.out.println("comment 2:"+ prodFiles.setComment(PB, "comment for b 1"));
        prodFiles.save(PB, Set.of(one, two));
        long sizeTwo = Files.size(rootPath.resolve(prodRepo).resolve(one));

        prodFiles.upload(PA);

        prodFiles.download();

        assertThrows(Exception.class, ()->{
            prodFiles.upload(PB); // send patch with error response because files conflic must be detected
        });
        prodFiles.save(PB, Set.of(one, two)); // you must to do this because PA changes these files in root repo !
        prodFiles.upload(PB); //send patch again - save method saves to patch files hashes from root repo

        prodFiles.clearPatch(PA);
        prodFiles.clearPatch(PB);

        prodFiles.download();
        status = prodFiles.status();
        assertTrue(status.added.isEmpty());

        assertEquals(2, rootFiles.getLogs(count).size());
        assertEquals(2, prodFiles.getLogs(count).size());

        var pastLogs = prodFiles.applyFSByLogsByCountLimit(1);
        System.out.println("past history:"+pastLogs);
        assertEquals(1, pastLogs.size());
        assertEquals(sizeOne, Files.size(rootPath.resolve(prodRepo).resolve(one)));
        prodFiles.clearAndHead();
        assertEquals(sizeTwo, Files.size(rootPath.resolve(prodRepo).resolve(one)));
    }
    @Test
    void shortLogs() throws Exception {
        var eol = 1;
        var settings = rootFiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);

        var email2 = "user@mail.net";
        settings = prodFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email, eol, true);
        assertTrue(settings.useFS);

        Files.write(rootPath.resolve(prodRepo).resolve(one), "1 clone".getBytes());
        Files.write(rootPath.resolve(prodRepo).resolve(two), "2 clone".getBytes());
        System.out.println("comment:"+ prodFiles.setComment(PA, "comment for a 1"));
        prodFiles.save(PA, Set.of(one, two));
        prodFiles.upload(PA);

        var download = prodFiles.download();
        System.out.println("download 1:"+download);
        Files.write(rootPath.resolve(prodRepo).resolve(one), "1 clone 222".getBytes());
        Files.write(rootPath.resolve(prodRepo).resolve(two), "2 clone 333".getBytes());
        System.out.println("comment:"+ prodFiles.setComment(PA, "comment for a 2"));
        prodFiles.save(PA, Set.of(one, two));
        prodFiles.upload(PA);

        download = prodFiles.download();
        System.out.println("download 2:"+download);

        rootPath.resolve(testRepo).toFile().mkdirs();
        var otherFiles = new AFiles(rootPath.resolve(testRepo));
        settings = otherFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email2, eol, true);
        assertTrue(settings.useFS);
        assertEquals(1, otherFiles.getLogs(count).size());
        assertEquals(2, otherFiles.getFiles().size());

        download = otherFiles.download();
        System.out.println("download 3:"+download);
    }
    @Test
    void filesConflict() throws Exception {
        var eol = 1;
        var settings = rootFiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);

        settings = prodFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email, eol, false);
        assertTrue(settings.useFS);
        rootPath.resolve(prodRepo).resolve(three).toFile().getParentFile().mkdirs();
        Files.write(rootPath.resolve(prodRepo).resolve(one), "1 clone".getBytes());
        Files.write(rootPath.resolve(prodRepo).resolve(two), "2 clone".getBytes());
        Files.write(rootPath.resolve(prodRepo).resolve(three), "3 clone".getBytes());
        System.out.println("comment:"+ prodFiles.setComment(PA, "comment for a 1"));
        prodFiles.save(PA, Set.of(three));

        rootPath.resolve(testRepo).toFile().mkdirs();
        var otherFiles = new AFiles(rootPath.resolve(testRepo));
        settings = otherFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email, eol, true);
        assertTrue(settings.useFS);
        Files.write(rootPath.resolve(testRepo).resolve(one), "1 clone \n other clone".getBytes());
        Files.write(rootPath.resolve(testRepo).resolve(two), "2 clone \n other clone".getBytes());
        System.out.println("comment 2:"+otherFiles.setComment(PA, "comment for a other"));
        otherFiles.save(PA, Set.of(one, two));
        var patchname = otherFiles.upload(PA);
        System.out.println("upload patch:"+patchname);
        var patchname2 = prodFiles.upload(PA);
        System.out.println("upload patch 2:"+patchname2);
    }
    @Test
    void diffAfterConflict() throws Exception {
        var eol = 1;
        var settings = rootFiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);

        settings = prodFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email, eol, false);
        assertTrue(settings.useFS);
        var diff = prodFiles.diff(PA);
        assertTrue(diff.isEmpty());

        Files.write(rootPath.resolve(prodRepo).resolve(one), "1 clone".getBytes());
        Files.write(rootPath.resolve(prodRepo).resolve(two), "2 clone".getBytes());
        System.out.println("comment:"+ prodFiles.setComment(PA, "comment for a 1"));
        prodFiles.save(PA, Set.of(one, two));

        rootPath.resolve(testRepo).toFile().mkdirs();
        var otherFiles = new AFiles(rootPath.resolve(testRepo));
        settings = otherFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email, eol, true);
        assertTrue(settings.useFS);

        Files.write(rootPath.resolve(testRepo).resolve(one), "1 clone \n other clone".getBytes());
        Files.write(rootPath.resolve(testRepo).resolve(two), "2 clone \n other clone".getBytes());
        System.out.println("comment:"+otherFiles.setComment(PA, "comment for a other"));
        otherFiles.save(PA, Set.of(one, two));
        var patchname = otherFiles.upload(PA);
        System.out.println("upload patch:"+patchname);
        //

        assertThrows(Exception.class, ()->{
            prodFiles.upload(PA);
        });
        var patchA = prodFiles.showPatch(PA);
        assertEquals(2, patchA.getFilenamesList().size());
        var download = prodFiles.download();

        prodFiles.clearAndHead();

        diff = prodFiles.diff(PA);
        System.out.println("diff :"+diff);
        assertFalse(diff.isEmpty());
        assertEquals(2, diff.size());
        assertTrue(diff.containsKey(one));
        assertTrue(diff.containsKey(two));

        //ignore other content in files and writes ours...
        Files.deleteIfExists(rootPath.resolve(prodRepo).resolve(one));
        Files.move(rootPath.resolve(prodRepo).resolve(one+FILE_CONFLICT), rootPath.resolve(prodRepo).resolve(one));

        Files.deleteIfExists(rootPath.resolve(prodRepo).resolve(two));
        Files.move(rootPath.resolve(prodRepo).resolve(two+FILE_CONFLICT), rootPath.resolve(prodRepo).resolve(two));

        var status = prodFiles.status();
        System.out.println("status 2:"+status);
        assertEquals(0, status.added.size());
        assertEquals(0, status.erased.size());
        assertEquals(2, status.modified.size());
        assertEquals(0, status.deleted.size());

        //
        prodFiles.save(PA, Set.of(one, two));
        prodFiles.upload(PA);
        prodFiles.clearPatch(PA);
        //
        download = prodFiles.download();
        var versionClone = download.version;

        assertEquals(2, download.files.size());
        assertEquals(2, download.logs.size());

        //

        otherFiles.clearPatch(PA);
        download = otherFiles.download();
        var versionOther = download.version;
        assertEquals(versionClone, versionOther);
        assertEquals(2, download.files.size());
        assertEquals(2, download.logs.size());

        otherFiles.clearAndHead();
        status = otherFiles.status();
        System.out.println("status 3:"+status);
        assertEquals(0, status.added.size());
        assertEquals(0, status.erased.size());
        assertEquals(0, status.modified.size());
        assertEquals(0, status.deleted.size());
    }
    @Test
    void diff() throws Exception {
        var eol = 1;
        var settings = rootFiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);

        var email2 = "user@mail.net";
        settings = prodFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email, eol, false);
        assertTrue(settings.useFS);
        var diff = prodFiles.diff(PA);
        assertTrue(diff.isEmpty());
        rootPath.resolve(prodRepo).resolve(three).toFile().getParentFile().mkdirs();
        Files.write(rootPath.resolve(prodRepo).resolve(one), "1 clone".getBytes());
        Files.write(rootPath.resolve(prodRepo).resolve(two), "2 clone".getBytes());
        System.out.println("comment:"+ prodFiles.setComment(PA, "comment for a 1"));
        prodFiles.save(PA, Set.of(one, two));

        prodFiles.clearFS();
        rootPath.resolve(prodRepo).resolve(three).toFile().getParentFile().mkdirs();
        Files.write(rootPath.resolve(prodRepo).resolve(three), "3 clone".getBytes());

        diff = prodFiles.diff(PA);
        System.out.println("diff :"+diff);
        assertFalse(diff.isEmpty());
        assertEquals(2, diff.size());
        assertTrue(diff.containsKey(one));
        assertTrue(diff.containsKey(two));

        var status = prodFiles.status();
        System.out.println("status :"+status);
        assertFalse(status.added.isEmpty());
        assertEquals(1, status.added.size());
    }
    @Test
    void findLogs() throws Exception {
        var eol = 1;
        var settings = rootFiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);

        var email2 = "user@mail.net";
        settings = prodFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email, eol, true);
        assertTrue(settings.useFS);

        Files.write(rootPath.resolve(prodRepo).resolve(one), "1 clone".getBytes());
        Files.write(rootPath.resolve(prodRepo).resolve(two), "2 clone".getBytes());
        System.out.println("comment:"+ prodFiles.setComment(PA, "comment for a 1"));
        prodFiles.save(PA, Set.of(one, two));
        prodFiles.upload(PA);

        var download = prodFiles.download();
        System.out.println("download 1:"+download);
        assertEquals(2, download.files.size());
        assertEquals(2, download.logs.size());
        Files.write(rootPath.resolve(prodRepo).resolve(one), "1 clone 222".getBytes());
        System.out.println("comment:"+ prodFiles.setComment(PA, "comment for a 2"));
        prodFiles.save(PA, Set.of(one));
        prodFiles.upload(PA);

        download = prodFiles.download();
        System.out.println("download 2:"+download);
        assertEquals(1, download.files.size());
        assertEquals(1, download.logs.size());

        assertEquals(2, rootFiles.getLogs(count).size());
        assertEquals(2, prodFiles.getLogs(count).size());
        var foundLogs = prodFiles.logsByFilename(Set.of(two));
        System.out.println("found logs:"+foundLogs);
        assertEquals(1, foundLogs.size());

        foundLogs = prodFiles.logsByFilename(Set.of(one));
        System.out.println("found logs 2:"+foundLogs);
        assertEquals(2, foundLogs.size());
    }
    @Disabled
    @Test
    void benchmarkStatus() throws Exception {
        var eol = 1;
        var settings = rootFiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);

        var totalFiles = 500;
        var totalFiles2 = 500;
        String content = "some string \n new line.";
        var listFiles = new HashSet<String>(totalFiles);
        var listFiles2 = new HashSet<String>(totalFiles2);

        settings = prodFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email, eol, true);
        assertTrue(settings.useFS);

        for (int i = 0; i < totalFiles; i++) {
            listFiles.add(new String("file_"+i+".txt"));
        }
        for(var file : listFiles){
            Files.write(rootPath.resolve(prodRepo).resolve(file), content.getBytes());
        }

        System.out.println("comment:"+ prodFiles.setComment(PA, "comment for a 1"));

        prodFiles.save(PA, listFiles);

        prodFiles.upload(PA);

        TimeUnit.SECONDS.sleep(1);
        var download = prodFiles.download();
        var status = prodFiles.status();
        for (int i = 0; i < totalFiles2; i++) {
            listFiles2.add(new String("added_file_"+i+".txt"));
        }
        for(var file : listFiles2){
            Files.write(rootPath.resolve(prodRepo).resolve(file), content.getBytes());
        }
        long start = System.currentTimeMillis();
        System.out.println("start:"+start);
        status = prodFiles.status();
        assertEquals(totalFiles2, status.added.size());
        long end = System.currentTimeMillis();
        System.out.println("DEBUG: Logic A took " + (end - start) + " MilliSeconds");
    }
    @Test
    void subModulesFunctionality() throws Exception {

        var eol = 1;
        var settings = rootFiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);

        settings = prodFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email, eol, false);
        assertTrue(settings.useFS);

        rootPath.resolve(testRepo).toFile().mkdirs();
        var otherFiles = new AFiles(rootPath.resolve(testRepo));
        settings = otherFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email, eol, false);
        assertTrue(settings.useFS);

        var other = "main.js";
        Files.write(rootPath.resolve(testRepo).resolve(other), "javascript file.".getBytes());
        var status2 = otherFiles.status();
        assertEquals(1, status2.added.size());


        Files.write(rootPath.resolve(prodRepo).resolve(one), "1 clone".getBytes());
        Files.write(rootPath.resolve(prodRepo).resolve(two), "2 clone".getBytes());

        //create submodule in repo like symbolic link.
        var subDir = "submodule";
        var symPath = createSymlink(rootPath.resolve(prodRepo).resolve(subDir).toString(), rootPath.resolve(testRepo).toString()).toString();
        System.out.println("symlink:"+symPath);

        var status = prodFiles.status();
        assertEquals(2, status.added.size());
        assertEquals(0, status.deleted.size());
        assertEquals(0, status.modified.size());

        System.out.println("comment:"+ prodFiles.setComment(PA, "comment for a 1"));
        prodFiles.save(PA, Set.of(one, two));
        prodFiles.upload(PA);


    }
    @Test
    void updateLogsWithComment() throws Exception {
        var eol = 1;
        var settings = rootFiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);

        settings = prodFiles.setup(rootPath.resolve(rootRepo).toAbsolutePath().toString(), email, eol, false);
        assertTrue(settings.useFS);

        var comment1 = "one comment";
        var comment2 = "two comment";
        var comment3 = "three comment";

        Files.write(rootPath.resolve(prodRepo).resolve(one), "1 clone".getBytes());
        Files.write(rootPath.resolve(prodRepo).resolve(two), "2 clone".getBytes());
        System.out.println("comment 1:"+ prodFiles.setComment(PA, comment1));
        prodFiles.save(PA, Set.of(one, two));
        prodFiles.upload(PA);

        var download = prodFiles.download();
        System.out.println("download 1:"+download);
        assertEquals(2, download.files.size());
        assertEquals(2, download.logs.size());

        //TimeUnit.SECONDS.sleep(1);
        prodFiles.clearPatch(PA);
        Files.write(rootPath.resolve(prodRepo).resolve(one), "3 clone".getBytes());
        System.out.println("comment 1:"+ prodFiles.setComment(PA, comment2));

        prodFiles.save(PA, Set.of(one));
        prodFiles.upload(PA);

        download = prodFiles.download();
        System.out.println("download 2:"+download);
        assertEquals(1, download.files.size());
        assertEquals(1, download.logs.size());

        //TimeUnit.SECONDS.sleep(1);
        assertEquals(2, prodFiles.getLogs(count).size());
        var version = prodFiles.getLogs(count).get(1).id;
        var updated = prodFiles.updateLog(version, comment3);
        assertEquals(1, updated);

        var version2 = 0L;// get all updated logs from start
        var logs = prodFiles.getLogsAfterVersion(version2);
        System.out.println("logs 1:"+logs);

        logs = prodFiles.getLogsAfterVersion(version);
        System.out.println("logs 2:"+logs);

        var localLogs = prodFiles.getLogs(count);
        System.out.println("local logs:"+localLogs);
        var changedLog = localLogs.get(1);
        assertEquals(comment3, changedLog.comment);
        assertEquals(version, changedLog.id);
    }
}

