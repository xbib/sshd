package org.xbib.io.sshd.common.util.io;

import org.xbib.io.sshd.common.PropertyResolver;
import org.xbib.io.sshd.common.util.buffer.BufferUtils;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dumps everything that is written to the stream to the logger.
 */
public class LoggingFilterOutputStream extends FilterOutputStream {

    private final String msg;
    private final int chunkSize;
    private final AtomicInteger writeCount = new AtomicInteger(0);

    public LoggingFilterOutputStream(OutputStream out, String msg, PropertyResolver resolver) {
        this(out, msg, resolver.getIntProperty(BufferUtils.HEXDUMP_CHUNK_SIZE, BufferUtils.DEFAULT_HEXDUMP_CHUNK_SIZE));
    }

    public LoggingFilterOutputStream(OutputStream out, String msg, int chunkSize) {
        super(out);
        this.msg = msg;
        this.chunkSize = chunkSize;
    }

    @Override
    public void write(int b) throws IOException {
        byte[] d = new byte[1];
        d[0] = (byte) b;
        write(d, 0, 1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }
}
