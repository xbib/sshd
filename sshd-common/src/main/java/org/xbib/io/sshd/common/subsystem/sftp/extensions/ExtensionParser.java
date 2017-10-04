package org.xbib.io.sshd.common.subsystem.sftp.extensions;

import org.xbib.io.sshd.common.NamedResource;
import org.xbib.io.sshd.common.util.NumberUtils;

import java.util.function.Function;

/**
 * @param <T> Result type
 */
public interface ExtensionParser<T> extends NamedResource, Function<byte[], T> {
    default T parse(byte[] input) {
        return parse(input, 0, NumberUtils.length(input));
    }

    T parse(byte[] input, int offset, int len);

    @Override
    default T apply(byte[] input) {
        return parse(input);
    }
}
