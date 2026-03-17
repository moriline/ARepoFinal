package org.arepo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.arepo.AppGlobal.DNE;

public class RemotePatchService {

    public void sendPatch(String root, Entities.APatch patch, String email) throws Exception {
        var rootPath = Path.of(root).toAbsolutePath();
        if(Files.notExists(rootPath)) throw new Exception(DNE+rootPath);
        var rootModel = new Model(rootPath);
        var rootSettings = rootModel.get();
        rootModel.patchModel.add(patch);

    }
    public long deletePatch(String root, String patchname, String email) throws Exception {
        var rootPath = Path.of(root).toAbsolutePath();
        if(Files.notExists(rootPath)) throw new Exception(DNE+rootPath);
        var rootModel = new Model(rootPath);
        var rootSettings = rootModel.get();
        return rootModel.patchModel.delete(patchname);
    }
    public List<Entities.APatch> getPatch(String root, String patchname) throws Exception {
        var rootPath = Path.of(root).toAbsolutePath();
        if(Files.notExists(rootPath)) throw new Exception(DNE+rootPath);
        var rootModel = new Model(rootPath);
        return rootModel.patchModel.findByPatchname(patchname);
    }
    public Entities.RemoteResult getDataAfterVersion(String root, Long version) throws Exception {
        var result = new Entities.RemoteResult();
        var rootPath = Path.of(root).toAbsolutePath();
        if(Files.notExists(rootPath)) throw new Exception(DNE+rootPath);
        var rootModel = new Model(rootPath);
        var rootVersion = rootModel.get().version;
        if(rootVersion > version){
            result.logs.addAll(rootModel.logsModel.getLogsAfterCreated(version));
            result.files.addAll(rootModel.textModel.getFilesAfterVersion(version));
            result.version = rootVersion;
        }
        return result;
    }
    public void accept(String root, String remotePatchname, String email, Long version) throws Exception {
        var rootPath = Path.of(root).toAbsolutePath();
        if(Files.notExists(rootPath)) throw new Exception(DNE+rootPath);
        var rootModel = new Model(rootPath);
        var rootSettings = rootModel.get();
        //if(rootSettings.version > version) throw new Exception("Version %d is not valid.".formatted(version));
        rootSettings.version = rootModel.addPatchToLogs(remotePatchname, email);
        rootModel.save(rootSettings);
    }

    public long updateLog(String root, String email, Long version, String comment) throws Exception {
        var rootPath = Path.of(root).toAbsolutePath();
        if(Files.notExists(rootPath)) throw new Exception(DNE+rootPath);
        var rootModel = new Model(rootPath);
        var rootSettings = rootModel.get();
        return rootModel.logsModel.updateLog(email, version, comment);
    }

    public List<Entities.LogFiles> getLogsAfterVersion(String root, Long version) throws Exception {
        var rootPath = Path.of(root).toAbsolutePath();
        if(Files.notExists(rootPath)) throw new Exception(DNE+rootPath);
        var rootModel = new Model(rootPath);
        return rootModel.logsModel.getLogsAfterVersion(version);
    }
}
