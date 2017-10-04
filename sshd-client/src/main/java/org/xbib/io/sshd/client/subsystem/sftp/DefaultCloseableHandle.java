package org.xbib.io.sshd.client.subsystem.sftp;

import org.xbib.io.sshd.client.subsystem.sftp.SftpClient.CloseableHandle;
import org.xbib.io.sshd.common.util.ValidateUtils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 */
public class DefaultCloseableHandle extends CloseableHandle {
    private final AtomicBoolean open = new AtomicBoolean(true);
    private final SftpClient client;

    public DefaultCloseableHandle(SftpClient client, String path, byte[] id) {
        super(path, id);
        this.client = ValidateUtils.checkNotNull(client, "No client for path=%s", path);
    }

    public final SftpClient getSftpClient() {
        return client;
    }

    @Override
    public boolean isOpen() {
        return open.get();
    }

    @Override
    public void close() throws IOException {
        if (open.getAndSet(false)) {
            client.close(this);
        }
    }

    @Override   // to avoid Findbugs[EQ_DOESNT_OVERRIDE_EQUALS]
    public int hashCode() {
        return super.hashCode();
    }

    @Override   // to avoid Findbugs[EQ_DOESNT_OVERRIDE_EQUALS]
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
