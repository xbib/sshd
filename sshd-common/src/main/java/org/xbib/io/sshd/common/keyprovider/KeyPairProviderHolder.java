package org.xbib.io.sshd.common.keyprovider;

/**
 *
 */
public interface KeyPairProviderHolder {
    /**
     * Retrieve the <code>KeyPairProvider</code> that will be used to find
     * the host key to use on the server side or the user key on the client side.
     *
     * @return the <code>KeyPairProvider</code>, never {@code null}
     */
    KeyPairProvider getKeyPairProvider();

    void setKeyPairProvider(KeyPairProvider keyPairProvider);
}
