package org.xbib.io.sshd.common.compression;

import org.xbib.io.sshd.common.NamedResource;

/**
 */
public interface CompressionInformation extends NamedResource {
    /**
     * Delayed compression is an Open-SSH specific feature which
     * informs both the client and server to not compress data before
     * the session has been authenticated.
     *
     * @return if the compression is delayed after authentication or not
     */
    boolean isDelayed();

    /**
     * @return {@code true} if there is any compression executed by
     * this &quot;compressor&quot; - special case for 'none'
     */
    boolean isCompressionExecuted();
}
