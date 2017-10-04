package org.xbib.io.sshd.common;

import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.logging.LoggingUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This interface defines constants for the SSH protocol.
 */
public final class SshConstants {

    //
    // SSH message identifiers
    //

    public static final byte SSH_MSG_DISCONNECT = 1;
    public static final byte SSH_MSG_IGNORE = 2;
    public static final byte SSH_MSG_UNIMPLEMENTED = 3;
    public static final byte SSH_MSG_DEBUG = 4;
    public static final byte SSH_MSG_SERVICE_REQUEST = 5;
    public static final byte SSH_MSG_SERVICE_ACCEPT = 6;
    public static final byte SSH_MSG_KEXINIT = 20;
    public static final int MSG_KEX_COOKIE_SIZE = 16;
    public static final byte SSH_MSG_NEWKEYS = 21;

    public static final byte SSH_MSG_KEX_FIRST = 30;
    public static final byte SSH_MSG_KEX_LAST = 49;

    public static final byte SSH_MSG_KEXDH_INIT = 30;
    public static final byte SSH_MSG_KEXDH_REPLY = 31;

    public static final byte SSH_MSG_KEX_DH_GEX_REQUEST_OLD = 30;
    public static final byte SSH_MSG_KEX_DH_GEX_GROUP = 31;
    public static final byte SSH_MSG_KEX_DH_GEX_INIT = 32;
    public static final byte SSH_MSG_KEX_DH_GEX_REPLY = 33;
    public static final byte SSH_MSG_KEX_DH_GEX_REQUEST = 34;

    public static final byte SSH_MSG_USERAUTH_REQUEST = 50;
    public static final byte SSH_MSG_USERAUTH_FAILURE = 51;
    public static final byte SSH_MSG_USERAUTH_SUCCESS = 52;
    public static final byte SSH_MSG_USERAUTH_BANNER = 53;

    public static final byte SSH_MSG_USERAUTH_INFO_REQUEST = 60;
    public static final byte SSH_MSG_USERAUTH_INFO_RESPONSE = 61;

    public static final byte SSH_MSG_USERAUTH_PK_OK = 60;

    public static final byte SSH_MSG_USERAUTH_PASSWD_CHANGEREQ = 60;

    public static final byte SSH_MSG_USERAUTH_GSSAPI_MIC = 66;

    public static final byte SSH_MSG_GLOBAL_REQUEST = 80;
    public static final byte SSH_MSG_REQUEST_SUCCESS = 81;
    public static final byte SSH_MSG_REQUEST_FAILURE = 82;
    public static final byte SSH_MSG_CHANNEL_OPEN = 90;
    public static final byte SSH_MSG_CHANNEL_OPEN_CONFIRMATION = 91;
    public static final byte SSH_MSG_CHANNEL_OPEN_FAILURE = 92;
    public static final byte SSH_MSG_CHANNEL_WINDOW_ADJUST = 93;
    public static final byte SSH_MSG_CHANNEL_DATA = 94;
    public static final byte SSH_MSG_CHANNEL_EXTENDED_DATA = 95;
    public static final byte SSH_MSG_CHANNEL_EOF = 96;
    public static final byte SSH_MSG_CHANNEL_CLOSE = 97;
    public static final byte SSH_MSG_CHANNEL_REQUEST = 98;
    public static final byte SSH_MSG_CHANNEL_SUCCESS = 99;
    public static final byte SSH_MSG_CHANNEL_FAILURE = 100;

    //
    // Disconnect error codes
    //
    public static final int SSH2_DISCONNECT_HOST_NOT_ALLOWED_TO_CONNECT = 1;
    public static final int SSH2_DISCONNECT_PROTOCOL_ERROR = 2;
    public static final int SSH2_DISCONNECT_KEY_EXCHANGE_FAILED = 3;
    public static final int SSH2_DISCONNECT_HOST_AUTHENTICATION_FAILED = 4;
    public static final int SSH2_DISCONNECT_RESERVED = 4;
    public static final int SSH2_DISCONNECT_MAC_ERROR = 5;
    public static final int SSH2_DISCONNECT_COMPRESSION_ERROR = 6;
    public static final int SSH2_DISCONNECT_SERVICE_NOT_AVAILABLE = 7;
    public static final int SSH2_DISCONNECT_PROTOCOL_VERSION_NOT_SUPPORTED = 8;
    public static final int SSH2_DISCONNECT_HOST_KEY_NOT_VERIFIABLE = 9;
    public static final int SSH2_DISCONNECT_CONNECTION_LOST = 10;
    public static final int SSH2_DISCONNECT_BY_APPLICATION = 11;
    public static final int SSH2_DISCONNECT_TOO_MANY_CONNECTIONS = 12;
    public static final int SSH2_DISCONNECT_AUTH_CANCELLED_BY_USER = 13;
    public static final int SSH2_DISCONNECT_NO_MORE_AUTH_METHODS_AVAILABLE = 14;
    public static final int SSH2_DISCONNECT_ILLEGAL_USER_NAME = 15;

    //
    // Open error codes
    //

    public static final int SSH_OPEN_ADMINISTRATIVELY_PROHIBITED = 1;
    public static final int SSH_OPEN_CONNECT_FAILED = 2;
    public static final int SSH_OPEN_UNKNOWN_CHANNEL_TYPE = 3;
    public static final int SSH_OPEN_RESOURCE_SHORTAGE = 4;

    // Some more constants
    public static final int SSH_EXTENDED_DATA_STDERR = 1;   // see RFC4254 section 5.2
    public static final int SSH_PACKET_HEADER_LEN = 5;  // 32-bit length + 8-bit pad length

    private SshConstants() {
        throw new UnsupportedOperationException("No instance allowed");
    }

    /**
     * @param cmd The command value
     * @return {@code true} if this value is used by several <U>different</U> messages
     * @see #getAmbiguousOpcodes()
     */
    public static boolean isAmbiguousOpcode(int cmd) {
        return getAmbiguousOpcodes().contains(cmd);
    }

    /**
     * @return A {@link Set} of opcodes that are used by several <U>different</U> messages
     */
    @SuppressWarnings("synthetic-access")
    public static Set<Integer> getAmbiguousOpcodes() {
        return LazyAmbiguousOpcodesHolder.AMBIGUOUS_OPCODES;
    }

    /**
     * Converts a command value to a user-friendly name
     *
     * @param cmd The command value
     * @return The user-friendly name - if not one of the defined {@code SSH_MSG_XXX}
     * values then returns the string representation of the command's value
     */
    public static String getCommandMessageName(int cmd) {
        @SuppressWarnings("synthetic-access")
        String name = LazyMessagesMapHolder.MESSAGES_MAP.get(cmd);
        if (GenericUtils.isEmpty(name)) {
            return Integer.toString(cmd);
        } else {
            return name;
        }
    }

    /**
     * Converts a disconnect reason value to a user-friendly name
     *
     * @param reason The disconnect reason value
     * @return The user-friendly name - if not one of the defined {@code SSH2_DISCONNECT_}
     * values then returns the string representation of the reason's value
     */
    public static String getDisconnectReasonName(int reason) {
        @SuppressWarnings("synthetic-access")
        String name = LazyReasonsMapHolder.REASONS_MAP.get(reason);
        if (GenericUtils.isEmpty(name)) {
            return Integer.toString(reason);
        } else {
            return name;
        }
    }

    /**
     * Converts an open error value to a user-friendly name
     *
     * @param code The open error value
     * @return The user-friendly name - if not one of the defined {@code SSH_OPEN_}
     * values then returns the string representation of the reason's value
     */
    public static String getOpenErrorCodeName(int code) {
        @SuppressWarnings("synthetic-access")
        String name = LazyOpenCodesMapHolder.OPEN_CODES_MAP.get(code);
        if (GenericUtils.isEmpty(name)) {
            return Integer.toString(code);
        } else {
            return name;
        }
    }

    private static class LazyAmbiguousOpcodesHolder {
        private static final Set<Integer> AMBIGUOUS_OPCODES =
                Collections.unmodifiableSet(
                        new HashSet<>(
                                LoggingUtils.getAmbiguousMenmonics(SshConstants.class, "SSH_MSG_").values()));
    }

    private static class LazyMessagesMapHolder {
        private static final Map<Integer, String> MESSAGES_MAP =
                LoggingUtils.generateMnemonicMap(SshConstants.class, f -> {
                    String name = f.getName();
                    if (!name.startsWith("SSH_MSG_")) {
                        return false;
                    }

                    try {
                        return !isAmbiguousOpcode(f.getByte(null));
                    } catch (Exception e) {
                        return false;
                    }
                });
    }

    private static class LazyReasonsMapHolder {
        private static final Map<Integer, String> REASONS_MAP = LoggingUtils.generateMnemonicMap(SshConstants.class, "SSH2_DISCONNECT_");
    }

    private static class LazyOpenCodesMapHolder {
        private static final Map<Integer, String> OPEN_CODES_MAP = LoggingUtils.generateMnemonicMap(SshConstants.class, "SSH_OPEN_");
    }
}
