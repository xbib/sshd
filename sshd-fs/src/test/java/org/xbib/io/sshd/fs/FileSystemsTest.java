package org.xbib.io.sshd.fs;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderNotFoundException;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
@Ignore
public class FileSystemsTest extends Assert {

    private static String url = "sftp://demo.wftpserver.com:2222";
    private static String user = "demo-user";
    private static String pwd = "demo-user";

    private static SftpFileSystemProvider sftpFileSystemProvider;

    private static SftpFileSystem sftpFileSystem;

    @BeforeClass
    static public void createResources() throws URISyntaxException, IOException {
        for (FileSystemProvider fileSystemProvider : FileSystemProvider.installedProviders()) {
            if ("sftp".equals(fileSystemProvider.getScheme())) {
                sftpFileSystemProvider = (SftpFileSystemProvider) fileSystemProvider;
            }
        }
        if (sftpFileSystemProvider == null) {
            throw new ProviderNotFoundException("Unable to get a SFTP file system provider");
        }
        URI uri = new URI(url);
        Map<String, Object> env = new HashMap<>();
        env.put("username", user);
        env.put("password", pwd);
        sftpFileSystem = sftpFileSystemProvider.newFileSystem(uri, env);
    }

    @AfterClass
    static public void closeResources() throws IOException {
        if (sftpFileSystem != null) {
            sftpFileSystem.close();
        }
    }

    @Test
    public void sftpFileSystemProviderIsNotNull() throws Exception {
        assertNotNull(sftpFileSystemProvider);
    }

    @Test
    public void sftpFileSystemIsNotNull() throws Exception {
        assertNotNull(sftpFileSystem);
    }

    @Test
    public void sftpFileSystemIsOpen() throws Exception {
        assertEquals("the file system must be opened", true, sftpFileSystem.isOpen());
    }

    @Ignore
    @Test
    public void testFileOperations() throws IOException {
        Path src = Paths.get("./src/test/resources/testFileWrite.txt");
        Path dst = sftpFileSystem.getPath( "upload/testFileWrite.txt");
        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
        assertTrue("files exist", Files.exists(dst));
        Path file = sftpFileSystem.getPath( "upload/testFileWrite.txt");
        if (!sftpFileSystemProvider.isSupportedFileAttributeView(file, SftpPosixFileAttributeView.class)) {
            throw new IllegalStateException("unsupported file attribute view");
        }
        PosixFileAttributes attrs = Files.readAttributes(file, PosixFileAttributes.class);
        assertNotNull("The file exist, we must get attributes", attrs);
        assertFalse("This is not a directory", attrs.isDirectory());
        assertTrue("This is a regular file", attrs.isRegularFile());
        assertFalse("This is not an symbolic link", attrs.isSymbolicLink());
        assertFalse("This is not an other file", attrs.isOther());
        assertEquals("The file size is", 4, attrs.size());
        assertTrue(attrs.permissions().contains(PosixFilePermission.OWNER_READ));
        assertTrue(attrs.permissions().contains(PosixFilePermission.OWNER_WRITE));
        assertTrue(attrs.permissions().contains(PosixFilePermission.GROUP_READ));
        assertTrue(attrs.permissions().contains(PosixFilePermission.OTHERS_READ));
    }

}
