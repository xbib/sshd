package org.xbib.io.sshd.eddsa.math;

import org.xbib.io.sshd.eddsa.Utils;

/**
 *
 */
final class Constants {
    public static final byte[] ZERO = Utils.hexToBytes("0000000000000000000000000000000000000000000000000000000000000000");
    public static final byte[] ONE = Utils.hexToBytes("0100000000000000000000000000000000000000000000000000000000000000");
    public static final byte[] TWO = Utils.hexToBytes("0200000000000000000000000000000000000000000000000000000000000000");
    public static final byte[] FOUR = Utils.hexToBytes("0400000000000000000000000000000000000000000000000000000000000000");
    public static final byte[] FIVE = Utils.hexToBytes("0500000000000000000000000000000000000000000000000000000000000000");
    public static final byte[] EIGHT = Utils.hexToBytes("0800000000000000000000000000000000000000000000000000000000000000");
}
