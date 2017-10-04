package org.xbib.io.sshd.common.util.io.der;

import org.xbib.io.sshd.common.util.GenericUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public enum ASN1Class {
    // NOTE: order is crucial, so DON'T change it
    UNIVERSAL((byte) 0x00),
    APPLICATION((byte) 0x01),
    CONTEXT((byte) 0x02),
    PRIVATE((byte) 0x03);

    public static final List<ASN1Class> VALUES =
            Collections.unmodifiableList(Arrays.asList(values()));

    private final byte byteValue;

    ASN1Class(byte classValue) {
        byteValue = classValue;
    }

    public static ASN1Class fromName(String s) {
        if (GenericUtils.isEmpty(s)) {
            return null;
        }

        for (ASN1Class c : VALUES) {
            if (s.equalsIgnoreCase(c.name())) {
                return c;
            }
        }

        return null;
    }

    /**
     * <P>The first byte in DER encoding is made of following fields</P>
     * <pre>
     * -------------------------------------------------
     * |Bit 8|Bit 7|Bit 6|Bit 5|Bit 4|Bit 3|Bit 2|Bit 1|
     * -------------------------------------------------
     * |  Class    | CF  |        Type                 |
     * -------------------------------------------------
     * </pre>
     *
     * @param value The original DER encoded byte
     * @return The {@link ASN1Class} value - <code>null</code> if no match found
     * @see #fromTypeValue(int)
     */
    public static ASN1Class fromDERValue(int value) {
        return fromTypeValue((value >> 6) & 0x03);
    }

    /**
     * @param value The &quot;pure&quot; value - unshifted and with no extras
     * @return The {@link ASN1Class} value - <code>null</code> if no match found
     */
    public static ASN1Class fromTypeValue(int value) {
        // all 4 values are defined
        if ((value < 0) || (value >= VALUES.size())) {
            return null;
        }

        return VALUES.get(value);
    }

    public byte getClassValue() {
        return byteValue;
    }
}
