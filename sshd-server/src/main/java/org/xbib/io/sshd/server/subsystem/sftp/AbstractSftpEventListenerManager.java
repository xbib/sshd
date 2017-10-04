package org.xbib.io.sshd.server.subsystem.sftp;

import org.xbib.io.sshd.common.util.EventListenerUtils;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 *
 */
public abstract class AbstractSftpEventListenerManager implements SftpEventListenerManager {
    private final Collection<SftpEventListener> sftpEventListeners = new CopyOnWriteArraySet<>();
    private final SftpEventListener sftpEventListenerProxy;

    protected AbstractSftpEventListenerManager() {
        sftpEventListenerProxy = EventListenerUtils.proxyWrapper(SftpEventListener.class, getClass().getClassLoader(), sftpEventListeners);
    }

    public Collection<SftpEventListener> getRegisteredListeners() {
        return sftpEventListeners;
    }

    @Override
    public SftpEventListener getSftpEventListenerProxy() {
        return sftpEventListenerProxy;
    }


    @Override
    public boolean addSftpEventListener(SftpEventListener listener) {
        return sftpEventListeners.add(SftpEventListener.validateListener(listener));
    }

    @Override
    public boolean removeSftpEventListener(SftpEventListener listener) {
        if (listener == null) {
            return false;
        }

        return sftpEventListeners.remove(SftpEventListener.validateListener(listener));
    }
}
