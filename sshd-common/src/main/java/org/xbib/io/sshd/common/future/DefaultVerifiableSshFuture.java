package org.xbib.io.sshd.common.future;

/**
 * @param <T> Type of future
 */
public abstract class DefaultVerifiableSshFuture<T extends SshFuture> extends DefaultSshFuture<T> implements VerifiableFuture<T> {
    protected DefaultVerifiableSshFuture(Object lock) {
        super(lock);
    }
}
