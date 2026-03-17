package org.arepo;

import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.arepo.ASettings.DEFAULT_FOLDER_ROOT_NAME;
import static org.arepo.AppGlobal.*;

public class Entities {
    public static class TextFiles {
        private Long id;
        @Expose
        public String rootfolder;
        @Expose
        private String name;
        private int size = 0;
        private byte[] content; // when file deleted content must be null
        @Expose
        private Long hash;
        @Expose
        private Long parent = INIT_ID; // id of previous text file when it moved
        @Expose
        private Long version = INIT_ID;
        @Expose
        private Integer charset = FileCharsets.U8.ordinal();

    public TextFiles(Long id, String rootfolder, String name, byte[] content) {
        this.id = id;
        this.rootfolder = rootfolder;
        this.name = name;
        this.content = content;
        update(content);
    }

        public TextFiles() {
        }

        public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public Long getHash() {
        return hash;
    }

    public void setHash(Long hash) {
        this.hash = hash;
    }

    public Long getParent() {
        return parent;
    }

    public void setParent(Long parent) {
        this.parent = parent;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Integer getCharset() {
        return charset;
    }

    public void setCharset(Integer charset) {
        this.charset = charset;
    }
    public void update(byte[] content){
        this.content = content;
        this.hash = content != null? getCRC32Checksum(content):null;
        this.size = content != null?content.length:0;
    }
    @Override
    public String toString() {
        return "TextFiles{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", hash='" + hash + '\'' +
                ", parent=" + parent +
                ", version=" + version +
                ", charset=" + charset +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TextFiles textFiles = (TextFiles) o;
        return Objects.equals(id, textFiles.id) && Objects.equals(name, textFiles.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
    }
    public static class APatch {
        public String patchname;
        public String filenames;
        public Long version = 0L;
        public String comment = "";

        public APatch() {
        }

        public APatch(String patchname, List<Entities.ATempFiles> tempList) {
            this.patchname = patchname;
            this.filenames = gson.toJson(tempList);
        }
        public List<Entities.ATempFiles> getFilenamesList(){
            return gson.fromJson(this.filenames, new TypeToken<List<Entities.ATempFiles>>() {}.getType());
        }

        @Override
        public String toString() {
            return "APatch{" +
                    "patchname='" + patchname + '\'' +
                    ", filenames='" + filenames + '\'' +
                    ", version=" + version +
                    ", comment='" + comment + '\'' +
                    '}';
        }
    }
    public static class APatchHistory {
        public String patchname;
        public String filenames;
        public String comment = "";
        public Long updated = 0L;// timestamp in seconds from sqlite unixepoch()
        public APatchHistory() {
        }
        public String getUpdatedAsString(){
            return AppUtils.convertLocal(updated*1000);// convert to millis
        }
        @Override
        public String toString() {
            return "APatchHistory{" +
                    "patchname='" + patchname + '\'' +
                    ", filenames='" + filenames + '\'' +
                    ", comment='" + comment + '\'' +
                    ", updated=" + updated +
                    '}';
        }
    }
    /**
     * Changes in local files.
     */
    public static class APatches {
        @Expose
        private String name;
        private byte[] content; // when file deleted content must be null
        @Expose
        private Long hash;
        @Expose
        private Long version = INIT_ID;
        @Expose
        private Integer charset = BINARY_CHARSET; // -1 = for binary files
        @Expose
        private Long updated = 0L;
        @Expose
        private String comment = "";
        public APatches(String name, byte[] content, Long hash, Integer charset) {
            this.name = name;
            this.content = content;
            this.hash = hash;
            this.updated = System.currentTimeMillis();
            this.charset = charset;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public byte[] getContent() {
            return content;
        }

        public void setContent(byte[] content) {
            this.content = content;
        }

        public Long getHash() {
            return hash;
        }

        public void setHash(Long hash) {
            this.hash = hash;
        }

        public Long getVersion() {
            return version;
        }

        public void setVersion(Long version) {
            this.version = version;
        }

        public Integer getCharset() {
            return charset;
        }

        public void setCharset(Integer charset) {
            this.charset = charset;
        }

        public Long getUpdated() {
            return updated;
        }

        public void setUpdated(Long updated) {
            this.updated = updated;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        @Override
        public String toString() {
            return "APatches{" +
                    "name='" + name + '\'' +
                    ", hash=" + hash +
                    ", version=" + version +
                    ", charset=" + charset +
                    ", updated=" + updated +
                    ", comment='" + comment + '\'' +
                    '}';
        }
    }
    public static class ATempFiles {
        @Expose
        public String rootfolder;
        @Expose
        private String name;
        @Expose
        private Long hash;
        @Expose
        private Long prevHash = ASettings.DEFAULT_LAST_HASH;
        private byte[] content;
        @Expose
        private Integer charset= BINARY_CHARSET; // if charset is -1 - this is binary file! 0 - for text file with UTF-8.

        public ATempFiles(Path dirPath, String filename, Long prevHash) throws IOException {
            var local = dirPath.resolve(filename).toAbsolutePath();
            this.name = filename;//.replace("\\", AppSystem.SEP);
            var tmpArray = filename.split(ASettings.SEP, 2);
            this.rootfolder = tmpArray.length > 1?tmpArray[0]:DEFAULT_FOLDER_ROOT_NAME;
            if(Files.exists(local)){
                try{
                    var lines = Files.readAllLines(local, FileCharsets.U8.getCharset());
                    this.charset = FileCharsets.U8.ordinal();
                }catch (IOException e){
                    System.out.println("binary!");
                }finally {
                    this.content = Files.readAllBytes(local);
                    this.hash = getCRC32Checksum(content);
                    this.prevHash = prevHash;
                }
            }else {
                this.hash = null;
                this.content = null;
            }

        }

        public ATempFiles(String name, byte[] content) {
            this.name = name;
            this.hash = content != null? getCRC32Checksum(content):null;
            this.content = content;
        }

        public ATempFiles() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getHash() {
            return hash;
        }

        public void setHash(Long hash) {
            this.hash = hash;
        }

        public Long getPrevHash() {
            return prevHash;
        }

        public void setPrevHash(Long prevHash) {
            this.prevHash = prevHash;
        }

        public byte[] getContent() {
            return content;
        }

        public void setContent(byte[] content) {
            this.content = content;
        }

        public Integer getCharset() {
            return charset;
        }

        public void setCharset(Integer charset) {
            this.charset = charset;
        }

        @Override
        public String toString() {
            return "ATempFiles{" +
                    "rootfolder='" + rootfolder + '\'' +
                    ", name='" + name + '\'' +
                    ", hash=" + hash +
                    ", prevHash=" + prevHash +
                    ", charset=" + charset +
                    '}';
        }
    }
    public static class LogFiles{
        public Long id = 0L;
        public String comment;
        public String email;
        public String files;//JSON like this [{"name":"one.php"}, {"name":"two.php"}] of TextFiles
        public Long created;
        public Long updated = 0L;

        public LogFiles() {
        }

        public LogFiles(Long id, String comment, String email, String files, Long created) {
            this.id = id;
            this.comment = comment;
            this.email = email;
            this.files = files;
            this.created = created;
        }
        public List<TextFiles> getFilesList(){
            return gson.fromJson(this.files, new TypeToken<List<TextFiles>>() {}.getType());
        }

        @Override
        public String toString() {
            return "LogFiles{" +
                    "id=" + id +
                    ", comment='" + comment + '\'' +
                    ", email='" + email + '\'' +
                    ", files='" + files + '\'' +
                    ", created=" + created +
                    ", updated=" + updated +
                    '}';
        }
    }
    public class LogFiles2 {
        @Expose
        private Long id;
        @Expose
        private String comment;
        @Expose
        private String email;
        @Expose
        private String files; //JSON like this with textfileid : ["22", "31"]
        @Expose
        private String types; // JSON like with ["t", "b"] where t -text, b - binary files
        private byte[] content; // ArrayList<byte[]> content = new ArrayList<>();
        @Expose
        private Long modified;

        public LogFiles2(Long id, String comment, String email, String files, String types, byte[] content, Long modified) {
            this.id = id;
            this.comment = comment;
            this.email = email;
            this.files = files;
            this.types = types;
            this.content = content;
            this.modified = modified;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getFiles() {
            return files;
        }

        public void setFiles(String files) {
            this.files = files;
        }

        public String getTypes() {
            return types;
        }

        public void setTypes(String types) {
            this.types = types;
        }

        public byte[] getContent() {
            return content;
        }

        public void setContent(byte[] content) {
            this.content = content;
        }

        public Long getModified() {
            return modified;
        }

        public void setModified(Long modified) {
            this.modified = modified;
        }

        @Override
        public String toString() {
            return "LogFiles2{" +
                    "id=" + id +
                    ", comment='" + comment + '\'' +
                    ", email='" + email + '\'' +
                    ", files='" + files + '\'' +
                    ", types='" + types + '\'' +
                    ", modified=" + modified +
                    '}';
        }
    }

    public static class RemoteResult {
        public final List<LogFiles> logs = new ArrayList<LogFiles>();
        public final List<TextFiles> files = new ArrayList<TextFiles>();
        public Long version = 0L;
    }
}
