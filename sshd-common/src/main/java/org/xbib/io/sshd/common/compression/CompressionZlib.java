package org.xbib.io.sshd.common.compression;

import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * ZLib based Compression.
 */
public class CompressionZlib extends BaseCompression {

    private static final int BUF_SIZE = 4096;

    private byte[] tmpbuf = new byte[BUF_SIZE];
    private Deflater compresser;
    private Inflater decompresser;

    /**
     * Create a new instance of a ZLib base compression
     */
    public CompressionZlib() {
        this(BuiltinCompressions.Constants.ZLIB);
    }

    protected CompressionZlib(String name) {
        super(name);
    }

    @Override
    public boolean isDelayed() {
        return false;
    }

    @Override
    public void init(Type type, int level) {
        compresser = new Deflater(level);
        decompresser = new Inflater();
    }

    @Override
    public void compress(Buffer buffer) throws IOException {
        compresser.setInput(buffer.array(), buffer.rpos(), buffer.available());
        buffer.wpos(buffer.rpos());
        for (int len = compresser.deflate(tmpbuf, 0, tmpbuf.length, Deflater.SYNC_FLUSH);
             len > 0;
             len = compresser.deflate(tmpbuf, 0, tmpbuf.length, Deflater.SYNC_FLUSH)) {
            buffer.putRawBytes(tmpbuf, 0, len);
        }
    }

    @Override
    public void uncompress(Buffer from, Buffer to) throws IOException {
        decompresser.setInput(from.array(), from.rpos(), from.available());
        try {
            for (int len = decompresser.inflate(tmpbuf); len > 0; len = decompresser.inflate(tmpbuf)) {
                to.putRawBytes(tmpbuf, 0, len);
            }
        } catch (DataFormatException e) {
            throw new IOException("Error decompressing data", e);
        }
    }
}
