package org.xbib.io.sshd.common.agent;

import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.logging.LoggingUtils;

import java.util.Map;

/**
 */
public final class SshAgentConstants {
    // Generic replies from agent to client
    public static final byte SSH_AGENT_FAILURE = 5;
    public static final byte SSH_AGENT_SUCCESS = 6;

    // Replies from agent to client for protocol 1 key operations
    public static final byte SSH_AGENT_RSA_IDENTITIES_ANSWER = 2;
    public static final byte SSH_AGENT_RSA_RESPONSE = 4;

    // Requests from client to agent for protocol 1 key operations
    public static final byte SSH_AGENTC_REQUEST_RSA_IDENTITIES = 1;
    public static final byte SSH_AGENTC_RSA_CHALLENGE = 3;
    public static final byte SSH_AGENTC_ADD_RSA_IDENTITY = 7;
    public static final byte SSH_AGENTC_REMOVE_RSA_IDENTITY = 8;
    public static final byte SSH_AGENTC_REMOVE_ALL_RSA_IDENTITIES = 9;
    public static final byte SSH_AGENTC_ADD_RSA_ID_CONSTRAINED = 24;

    // Requests from client to agent for protocol 2 key operations
    public static final byte SSH2_AGENTC_REQUEST_IDENTITIES = 11;
    public static final byte SSH2_AGENTC_SIGN_REQUEST = 13;
    public static final byte SSH2_AGENTC_ADD_IDENTITY = 17;
    public static final byte SSH2_AGENTC_REMOVE_IDENTITY = 18;
    public static final byte SSH2_AGENTC_REMOVE_ALL_IDENTITIES = 19;
    public static final byte SSH2_AGENTC_ADD_ID_CONSTRAINED = 25;

    // Key-type independent requests from client to agent
    public static final byte SSH_AGENTC_ADD_SMARTCARD_KEY = 20;
    public static final byte SSH_AGENTC_REMOVE_SMARTCARD_KEY = 21;
    public static final byte SSH_AGENTC_LOCK = 22;
    public static final byte SSH_AGENTC_UNLOCK = 23;
    public static final byte SSH_AGENTC_ADD_SMARTCARD_KEY_CONSTRAINED = 26;

    public static final byte SSH2_AGENT_FAILURE = 30;

    // Replies from agent to client for protocol 2 key operations
    public static final byte SSH2_AGENT_IDENTITIES_ANSWER = 12;
    public static final byte SSH2_AGENT_SIGN_RESPONSE = 14;

    // Key constraint identifiers
    public static final byte SSH_AGENT_CONSTRAIN_LIFETIME = 1;
    public static final byte SSH_AGENT_CONSTRAIN_CONFIRM = 2;

    private SshAgentConstants() {
    }

    /**
     * Converts a command value to a user-friendly name
     *
     * @param cmd The command value
     * @return The user-friendly name - if not one of the defined {@code SSH2_AGENT}
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

    private static class LazyMessagesMapHolder {
        private static final Map<Integer, String> MESSAGES_MAP =
                LoggingUtils.generateMnemonicMap(SshAgentConstants.class, f -> {
                    String name = f.getName();
                    return !name.startsWith("SSH_AGENT_CONSTRAIN")
                            && (name.startsWith("SSH_AGENT") || name.startsWith("SSH2_AGENT"));

                });
    }
}