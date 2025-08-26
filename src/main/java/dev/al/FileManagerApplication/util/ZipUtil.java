package dev.al.FileManagerApplication.util;

import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
@Component
public class ZipUtil {

    // Zip a folder recursively
    public void zipFolder(Path sourceDirPath, Path zipFilePath) throws IOException {
        if (Files.exists(zipFilePath)) {
            throw new IOException("ZIP file already exists: " + zipFilePath);
        }

        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
            Files.walk(sourceDirPath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(sourceDirPath.relativize(path).toString());
                        try {
                            zs.putNextEntry(zipEntry);
                            Files.copy(path, zs);
                            zs.closeEntry();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
    }

    // Unzip to a destination folder (does NOT overwrite existing files/folders)
    public void unzipFolder(Path zipFilePath, Path destDirPath) throws IOException {
        if (Files.exists(destDirPath)) {
            throw new IOException("Destination folder already exists: " + destDirPath);
        }

        Files.createDirectories(destDirPath);

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path newFilePath = destDirPath.resolve(entry.getName()).normalize();

                // Security check: ensure newFilePath is inside destDirPath
                if (!newFilePath.startsWith(destDirPath)) {
                    throw new IOException("Bad zip entry: " + entry.getName());
                }

                if (Files.exists(newFilePath)) {
                    throw new IOException("File already exists: " + newFilePath);
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(newFilePath);
                } else {
                    Files.createDirectories(newFilePath.getParent());
                    try (OutputStream os = Files.newOutputStream(newFilePath)) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            os.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    // Zip a single file
    public void zipFile(Path sourceFilePath, Path zipFilePath) throws IOException {
        if (Files.exists(zipFilePath)) {
            throw new IOException("ZIP file already exists: " + zipFilePath);
        }

        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
            ZipEntry zipEntry = new ZipEntry(sourceFilePath.getFileName().toString());
            zs.putNextEntry(zipEntry);
            Files.copy(sourceFilePath, zs);
            zs.closeEntry();
        }
    }

    // Unzip a single file
    public void unzipFile(Path zipFilePath, Path destinationFilePath) throws IOException {
        if (Files.exists(destinationFilePath)) {
            throw new IOException("Destination file already exists: " + destinationFilePath);
        }

        Files.createDirectories(destinationFilePath.getParent());

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFilePath))) {
            ZipEntry entry = zis.getNextEntry();

            if (entry == null || entry.isDirectory()) {
                throw new IOException("Invalid ZIP file content.");
            }

            Path newFilePath = destinationFilePath.normalize();

            // Security check
            if (!newFilePath.getParent().startsWith(destinationFilePath.getParent())) {
                throw new IOException("Bad zip entry");
            }

            try (OutputStream os = Files.newOutputStream(newFilePath)) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    os.write(buffer, 0, len);
                }
            }

            zis.closeEntry();
        }
    }
}