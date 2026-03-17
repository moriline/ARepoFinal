package org.arepo;

import com.google.gson.annotations.Expose;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.*;

import static org.arepo.ASettings.*;
import static org.arepo.AppGlobal.*;

public class Response {
    public static void main(String[] args) throws MalformedURLException {
        var repo = new RepoSettings(Path.of("bin/repi1"), "root@mail.net", 1);
        var repo2 = new RepoSettings(Path.of("bin/repi2"), "root@mail.net", 1);
        System.out.println("repo 1:"+repo.toString());
        System.out.println("repo 2:"+repo2.toString());

    }
    public static class StatusResult {
        @Expose
        public Long version = 0L;

        //only new files on filesystem
        @Expose
        public Set<StatusFile> added = new HashSet<StatusFile>();
        @Expose
        public Set<StatusFile> modified = new HashSet<StatusFile>();
        @Expose
        public Set<StatusFile> deleted = new HashSet<StatusFile>();
        @Expose
        public Set<StatusFile> erased = new HashSet<StatusFile>();
        @Expose
        public int count = 0;
        public StatusResult() {
        }

        public void update(List<StatusFile> files, Long version) {
            this.version = version;
            for(var file : files){
                switch (file.action){
                    case FILE_ADDED: added.add(file);break;
                    case FILE_MODIFIED: modified.add(file);break;
                    case FILE_DELETED: deleted.add(file);break;
                    case FILE_ERASED: erased.add(file);break;
                }
                count++;
            }
        }

        @Override
        public String toString() {
            return "StatusResult{" +
                    "version=" + version +
                    ", added=" + added +
                    ", modified=" + modified +
                    ", deleted=" + deleted +
                    ", erased=" + erased +
                    ", count=" + count +
                    '}';
        }
    }
    public static class StatusFile {
        @Expose
        String name;
        @Expose
        long hash;
        @Expose
        String action;
        public StatusFile(String name, long hash, String action) {
            this.name = name;
            this.hash = hash;
            this.action = action;
        }

        public StatusFile() {
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;

            StatusFile that = (StatusFile) object;
            return hash == that.hash && name.equals(that.name);
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + Long.hashCode(hash);
            return result;
        }

        @Override
        public String toString() {
            return "StatusFile{" +
                    "name='" + name + '\'' +
                    ", hash=" + hash +
                    ", action='" + action + '\'' +
                    '}';
        }
    }

    public static class DownloadResult {
        public List<String> files =new ArrayList<String>();
        public List<String> logs =new ArrayList<String>();
        public Long version = 0L;

        @Override
        public String toString() {
            return "DownloadResult{" +
                    "files=" + files +
                    ", logs=" + logs +
                    ", version=" + version +
                    '}';
        }
    }

    public static class RepoSettings {
        @Expose
        public String remote = "";// If remote is empty - this is root repo!
        @Expose
        public boolean useFS = true;
        @Expose
        public Integer eol = 0;
        @Expose
        public String email = "";
        @Expose
        public Long hash = 0L; //unique hash for every repo!
        @Expose
        public Set<String> ignoreDirs = new HashSet<String>(); // ignore all files in directory like this: Set.of(".gradle", "build", ".idea");
        @Expose
        public Set<String> ignoreFiles = new HashSet<String>();// ignore files like this: Set.of("readme.md", "src/One.java");
        @Expose
        public Long version = 0L;

        public RepoSettings(Path path, String email, Integer eol) {
            //this.hash = AppCheckSum.getCRC32Checksum(new StringBuilder(email).append(path.toAbsolutePath().toString()).toString().getBytes(StandardCharsets.UTF_8));
            this.hash = getCRC32Checksum(new StringBuilder(email).append(System.nanoTime()).toString().getBytes());
            this.eol = eol;
            this.email = email;
        }

        @Override
        public String toString() {
            return "RepoSettings{" +
                    "remote='" + remote + '\'' +
                    ", useFS=" + useFS +
                    ", eol=" + eol +
                    ", email='" + email + '\'' +
                    ", hash=" + hash +
                    ", ignoreDirs=" + ignoreDirs +
                    ", ignoreFiles=" + ignoreFiles +
                    ", version=" + version +
                    '}';
        }
    }
    public static class FSFiles{
        public Map<String, FSList> files = new HashMap<>();
        public FSFiles(Map<String, Map<String, byte[]>> files) {
            for(var fold : files.entrySet()) {
                this.files.put(fold.getKey(), new FSList(fold.getValue()));
            }
        }

        @Override
        public String toString() {
            return "FSFiles{" +
                    "files=" + files +
                    '}';
        }
    }
    public static class FSList{
        public Integer count;
        public List<String> files=new ArrayList<>();
        public FSList(Map<String, byte[]> filesMap) {
            this.files.addAll(filesMap.keySet());
            this.count = files.size();
        }

        @Override
        public String toString() {
            return "FSList{" +
                    "count=" + count +
                    ", files=" + files +
                    '}';
        }
    }
}
