package org.arepo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.arepo.ASettings.PB;
import static org.junit.jupiter.api.Assertions.*;
import static org.arepo.AppGlobal.*;

public class CliTest {
    String rootRepo = "root_repo";
    String cloneRepo = "clone_repo";
    String otherCloneRepo = "other_clone_repo";
    Path rootPath;
    AFiles rootFiles;
    AFiles cloneFiles;
    String email = "test@mail.net";
    CommandLine cmd;
    String keyPatch = "-p";
    String keyFilename = "-fn";
    String keyFile = "-f";
    String keyEmail = "-e";
    @BeforeEach
    void setUp(@TempDir(cleanup = CleanupMode.DEFAULT) Path temp) throws Exception {
        rootPath = temp;

        Files.createDirectories(rootPath.resolve(rootRepo));
        Files.createDirectories(rootPath.resolve(cloneRepo));

        rootFiles = new AFiles(rootPath.resolve(rootRepo));
        cloneFiles = new AFiles(rootPath.resolve(cloneRepo));
        Main2.currentRelativePath = temp;
        //cmd = new CommandLine(new Main());
        cmd = new CommandLine(new Main2());
    }
    @AfterEach
    void tearDown() {
    }
    @Test
    void one() throws IOException {
        var one = "src/index.php";
        var two = "readme.md";
        int exitCode = cmd.execute(Actions.setup.name(), "-f="+rootPath.resolve(rootRepo).toAbsolutePath().toString(), keyEmail, email);
        assertEquals(0, exitCode);
        String result = cmd.getExecutionResult();
        System.out.println("sw:"+result);
        assertFalse(result.startsWith(ERROR));

        exitCode = cmd.execute(Actions.setup.name(), keyFile, rootPath.resolve(cloneRepo).toAbsolutePath().toString(), keyFile, rootPath.resolve(rootRepo).toAbsolutePath().toString(), keyEmail, email);
        assertEquals(0, exitCode);
        result = cmd.getExecutionResult();
        System.out.println("sw:"+result);
        assertFalse(result.startsWith(ERROR));

        rootPath.resolve(cloneRepo).resolve(one).toFile().getParentFile().mkdirs();
        Files.write(rootPath.resolve(cloneRepo).resolve(one), "1 clone".getBytes());
        Files.write(rootPath.resolve(cloneRepo).resolve(two), "1 clone".getBytes());

        exitCode = cmd.execute(Actions.status.name());
        assertEquals(0, exitCode);
        result = cmd.getExecutionResult();
        assertFalse(result.startsWith(ERROR));
        var status = gson.fromJson(result, Response.StatusResult.class);
        assertFalse(status.added.isEmpty());
        assertTrue(status.deleted.isEmpty());
        assertTrue(status.modified.isEmpty());
        assertEquals(2, status.added.size());
        System.out.println("status 1:"+status);

        exitCode = cmd.execute(Actions.comment.name(), keyPatch, PB, "-c", "first comment for b");
        assertEquals(0, exitCode);
        result = cmd.getExecutionResult();
        assertFalse(result.startsWith(ERROR));
        var file1 = status.added.stream().toList().get(0);
        var file2 = status.added.stream().toList().get(1);
        exitCode = cmd.execute(Actions.save.name(), keyPatch, PB, keyFilename, file1.name, keyFilename, file2.name);
        assertEquals(0, exitCode);
        result = cmd.getExecutionResult();
        assertFalse(result.startsWith(ERROR));

        exitCode = cmd.execute(Actions.upload.name(), keyPatch, PB);
        assertEquals(0, exitCode);
        result = cmd.getExecutionResult();
        assertFalse(result.startsWith(ERROR));

        exitCode = cmd.execute(Actions.download.name());
        assertEquals(0, exitCode);
        result = cmd.getExecutionResult();
        assertFalse(result.startsWith(ERROR));
        System.out.println("download:"+result);
    }
    @Test
    void two() throws IOException {

        int exitCode = cmd.execute(Actions.init.name());
        assertEquals(0, exitCode);
        String result = cmd.getExecutionResult();
        assertFalse(result.startsWith(ERROR));
        System.out.println("sw:"+result);

        var subDir = "submodule";
        var symPath = Files.createSymbolicLink(rootPath.resolve(rootRepo).resolve(subDir), rootPath.resolve(cloneRepo)).toString();
        System.out.println("symlink:"+symPath);
    }
    @Test
    void three(){
        int exitCode = cmd.execute(Actions.setup.name(), keyFile, rootPath.resolve(rootRepo).toAbsolutePath().toString(), keyEmail, email);
        String result = cmd.getExecutionResult();
        System.out.println("2 test:"+result);
        assertFalse(result.startsWith(ERROR));
        exitCode = cmd.execute(Actions.init.name());
        result = cmd.getExecutionResult();
        System.out.println("2 test 2:"+result);
    }
}
