package org.xbib.io.sshd.common.config.keys.loader.openssh;

import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.buffer.BufferUtils;

import java.util.function.Predicate;

/**
 *
 */
public class OpenSSHParserContext {
    public static final String NONE_CIPHER = "none";
    public static final Predicate<String> IS_NONE_CIPHER = c -> GenericUtils.isEmpty(c) || NONE_CIPHER.equalsIgnoreCase(c);

    public static final String NONE_KDF = "none";
    public static final Predicate<String> IS_NONE_KDF = c -> GenericUtils.isEmpty(c) || NONE_KDF.equalsIgnoreCase(c);

    private String cipherName;
    private String kdfName;
    private byte[] kdfOptions;

    public OpenSSHParserContext() {
        super();
    }

    public OpenSSHParserContext(String cipherName, String kdfName, byte... kdfOptions) {
        this.cipherName = cipherName;
        this.kdfName = kdfName;
        this.kdfOptions = kdfOptions;
    }

    public String getCipherName() {
        return cipherName;
    }

    public void setCipherName(String cipherName) {
        this.cipherName = cipherName;
    }

    public String getKdfName() {
        return kdfName;
    }

    public void setKdfName(String kdfName) {
        this.kdfName = kdfName;
    }

    public byte[] getKdfOptions() {
        return kdfOptions;
    }

    public void setKdfOptions(byte[] kdfOptions) {
        this.kdfOptions = kdfOptions;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "[cipher=" + getCipherName()
                + ", kdfName=" + getKdfName()
                + ", kdfOptions=" + BufferUtils.toHex(':', getKdfOptions())
                + "]";
    }
}
