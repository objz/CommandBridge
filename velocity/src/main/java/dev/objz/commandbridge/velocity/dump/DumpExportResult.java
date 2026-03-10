package dev.objz.commandbridge.velocity.dump;

import java.nio.file.Path;

public record DumpExportResult(
        int bytes,
        Path localPath,
        Exception localError,
        DumpUploadResult upload,
        Exception uploadError) {

    public boolean uploaded() {
        return upload != null && upload.url() != null && !upload.url().isBlank();
    }

    public boolean hasLocalCopy() {
        return localPath != null;
    }
}
