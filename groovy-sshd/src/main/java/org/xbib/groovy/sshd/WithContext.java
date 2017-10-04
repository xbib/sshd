package org.xbib.groovy.sshd;

/**
 *
 * @param <T> the context parameter
 */
public interface WithContext<T> {
    T perform(SFTPContext ctx) throws Exception;
}
