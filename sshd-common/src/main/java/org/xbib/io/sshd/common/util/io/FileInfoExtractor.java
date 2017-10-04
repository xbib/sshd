package org.xbib.io.sshd.common.util.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * @param <T> Type of information being extracted.
 */
@FunctionalInterface
public interface FileInfoExtractor<T> {

    FileInfoExtractor<Boolean> EXISTS = Files::exists;

    FileInfoExtractor<Boolean> ISDIR = Files::isDirectory;

    FileInfoExtractor<Boolean> ISREG = Files::isRegularFile;

    FileInfoExtractor<Boolean> ISSYMLINK = (file, options) -> Files.isSymbolicLink(file);

    FileInfoExtractor<Long> SIZE = (file, options) -> Files.size(file);

    FileInfoExtractor<Set<PosixFilePermission>> PERMISSIONS = IoUtils::getPermissions;

    FileInfoExtractor<FileTime> LASTMODIFIED = Files::getLastModifiedTime;

    T infoOf(Path file, LinkOption... options) throws IOException;

}
