package org.xbib.io.sshd.common.session;

/**
 * @param <S> Type of {@link org.xbib.io.sshd.common.session.Session} being held
 */
@FunctionalInterface
public interface SessionHolder<S extends Session> {
    S getSession();
}
