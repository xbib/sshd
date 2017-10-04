package org.xbib.io.sshd.common.scp.helpers;

import org.xbib.io.sshd.common.scp.ScpFileOpener;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

/**
 *
 */
public class DefaultScpFileOpener extends AbstractLoggingBean implements ScpFileOpener {
    public static final DefaultScpFileOpener INSTANCE = new DefaultScpFileOpener();

    public DefaultScpFileOpener() {
        super();
    }

    @Override
    public InputStream openRead(Session session, Path file, OpenOption... options) throws IOException {
        return Files.newInputStream(file, options);
    }

    @Override
    public OutputStream openWrite(Session session, Path file, OpenOption... options) throws IOException {
        return Files.newOutputStream(file, options);
    }
}
