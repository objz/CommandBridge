package dev.objz.commandbridge.velocity.dump;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public final class DumpExporter {

    private static final boolean SAVE_LOCAL_COPY = true;

    private final DumpFileStore fileStore;
    private final CloudflareDumpUploader uploader;

    public DumpExporter(Path dataDir) {
        this.fileStore = new DumpFileStore(dataDir);
        this.uploader = new CloudflareDumpUploader();
    }

    public DumpExportResult export(String jsonPayload) {
        int bytes = jsonPayload.getBytes(StandardCharsets.UTF_8).length;

        Path localPath = null;
        Exception localError = null;
        if (SAVE_LOCAL_COPY) {
            try {
                localPath = fileStore.write(jsonPayload);
            } catch (Exception ex) {
                localError = ex;
            }
        }

        DumpUploadResult upload = null;
        Exception uploadError = null;
        try {
            upload = uploader.upload(jsonPayload);
        } catch (Exception ex) {
            uploadError = ex;
        }

        return new DumpExportResult(bytes, localPath, localError, upload, uploadError);
    }

    public String defaultPublicUrl(String id) {
        return uploader.publicUrlFor(id);
    }
}
