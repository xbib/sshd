package org.xbib.io.sshd.common.util.io.der;

import org.xbib.io.sshd.common.util.GenericUtils;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 *
 */
public enum ASN1Type {
    ANY((byte) 0x00),
    BOOLEAN((byte) 0x01),
    INTEGER((byte) 0x02),
    BIT_STRING((byte) 0x03),
    OCTET_STRING((byte) 0x04),
    NULL((byte) 0x05),
    OBJECT_IDENTIFIER((byte) 0x06),
    REAL((byte) 0x09),
    ENUMERATED((byte) 0x0a),
    RELATIVE_OID((byte) 0x0d),
    SEQUENCE((byte) 0x10),
    SET((byte) 0x11),
    NUMERIC_STRING((byte) 0x12),
    PRINTABLE_STRING((byte) 0x13),
    T61_STRING((byte) 0x14),
    VIDEOTEX_STRING((byte) 0x15),
    IA5_STRING((byte) 0x16),
    GRAPHIC_STRING((byte) 0x19),
    ISO646_STRING((byte) 0x1A),
    GENERAL_STRING((byte) 0x1B),
    UTF8_STRING((byte) 0x0C),
    UNIVERSAL_STRING((byte) 0x1C),
    BMP_STRING((byte) 0x1E),
    UTC_TIME((byte) 0x17),
    GENERALIZED_TIME((byte) 0x18);

    public static final Set<ASN1Type> VALUES =
            Collections.unmodifiableSet(EnumSet.allOf(ASN1Type.class));

    private final byte typeValue;

    ASN1Type(byte typeVal) {
        typeValue = typeVal;
    }

    public static final ASN1Type fromName(String s) {
        if (GenericUtils.isEmpty(s)) {
            return null;
        }

        for (ASN1Type t : VALUES) {
            if (s.equalsIgnoreCase(t.name())) {
                return t;
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
     * @return The {@link ASN1Type} value - <code>null</code> if no match found
     * @see #fromTypeValue(int)
     */
    public static ASN1Type fromDERValue(int value) {
        return fromTypeValue(value & 0x1F);
    }

    /**
     * @param value The &quot;pure&quot; type value - with no extra bits set
     * @return The {@link ASN1Type} value - <code>null</code> if no match found
     */
    public static ASN1Type fromTypeValue(int value) {
        if ((value < 0) || (value > 0x1F)) {    // only 5 bits are used
            return null;
        }

        for (ASN1Type t : VALUES) {
            if (t.getTypeValue() == value) {
                return t;
            }
        }

        return null;
    }

    public byte getTypeValue() {
        return typeValue;
    }
}
