package org.xbib.io.sshd.common.util.closeable;

import org.xbib.io.sshd.common.Closeable;
import org.xbib.io.sshd.common.future.CloseFuture;

/**
 *
 */
public abstract class AbstractInnerCloseable extends AbstractCloseable {
    protected AbstractInnerCloseable() {
        this("");
    }

    protected AbstractInnerCloseable(String discriminator) {
        super(discriminator);
    }

    protected abstract Closeable getInnerCloseable();

    @Override
    protected CloseFuture doCloseGracefully() {
        return getInnerCloseable().close(false);
    }

    @Override
    protected void doCloseImmediately() {
        getInnerCloseable().close(true).addListener(future -> AbstractInnerCloseable.super.doCloseImmediately());
    }
}
