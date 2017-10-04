package org.xbib.io.sshd.common.compression;

/**
 * ZLib delayed compression.
 */
public class CompressionDelayedZlib extends CompressionZlib {
    /**
     * Create a new instance of a delayed ZLib compression
     */
    public CompressionDelayedZlib() {
        super(BuiltinCompressions.Constants.DELAYED_ZLIB);
    }

    @Override
    public boolean isDelayed() {
        return true;
    }
}
