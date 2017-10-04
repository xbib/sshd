package org.xbib.io.sshd.common.mac;

import org.xbib.io.sshd.common.util.NumberUtils;

/**
 * Message Authentication Code for use in SSH.
 * It usually wraps a javax.crypto.Mac class.
 */
public interface Mac extends MacInformation {
    void init(byte[] key) throws Exception;

    default void update(byte[] buf) {
        update(buf, 0, NumberUtils.length(buf));
    }

    void update(byte[] buf, int start, int len);

    void updateUInt(long foo);

    default byte[] doFinal() throws Exception {
        int blockSize = getBlockSize();
        byte[] buf = new byte[blockSize];
        doFinal(buf);
        return buf;
    }

    default void doFinal(byte[] buf) throws Exception {
        doFinal(buf, 0);
    }

    void doFinal(byte[] buf, int offset) throws Exception;
}
