package org.xbib.io.sshd.common.compression;

import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.io.IOException;
import java.io.StreamCorruptedException;

/**
 *
 */
public class CompressionNone extends BaseCompression {
    private Type type;
    private int level;

    public CompressionNone() {
        super(BuiltinCompressions.Constants.NONE);
    }

    @Override
    public void init(Type type, int level) {
        this.type = type;
        this.level = level;
    }

    @Override
    public boolean isCompressionExecuted() {
        return false;
    }

    @Override
    public void compress(Buffer buffer) throws IOException {
        if (!Type.Deflater.equals(type)) {
            throw new StreamCorruptedException("Not set up for compression: " + type);
        }
    }

    @Override
    public void uncompress(Buffer from, Buffer to) throws IOException {
        if (!Type.Inflater.equals(type)) {
            throw new StreamCorruptedException("Not set up for de-compression: " + type);
        }

        if (from != to) {
            throw new StreamCorruptedException("Separate de-compression buffers provided");
        }
    }

    @Override
    public boolean isDelayed() {
        return false;
    }

    @Override
    public String toString() {
        return super.toString() + "[" + type + "/" + level + "]";
    }
}
