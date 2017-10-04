package org.xbib.io.sshd.client.subsystem.sftp.extensions;

import java.io.IOException;

/**
 * @see <A HREF="https://tools.ietf.org/html/draft-ietf-secsh-filexfer-extensions-00#section-6">copy-file extension</A>
 */
public interface CopyFileExtension extends SftpClientExtension {
    /**
     * @param src                  The (<U>remote</U>) file source path
     * @param dst                  The (<U>remote</U>) file destination path
     * @param overwriteDestination If {@code true} then OK to override destination if exists
     * @throws IOException If failed to execute the command or extension not supported
     */
    void copyFile(String src, String dst, boolean overwriteDestination) throws IOException;
}
