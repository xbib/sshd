package org.xbib.io.sshd.common.kex;

import org.xbib.io.sshd.common.NamedResource;
import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.digest.Digest;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.logging.LoggingUtils;

import java.security.PublicKey;
import java.util.Collections;
import java.util.Map;

/**
 * Key exchange algorithm.
 */
public interface KeyExchange extends NamedResource {
    Map<Integer, String> GROUP_KEX_OPCODES_MAP =
            Collections.unmodifiableMap(LoggingUtils.generateMnemonicMap(SshConstants.class, "SSH_MSG_KEX_DH_GEX_"));

    Map<Integer, String> SIMPLE_KEX_OPCODES_MAP =
            Collections.unmodifiableMap(LoggingUtils.generateMnemonicMap(SshConstants.class, "SSH_MSG_KEXDH_"));

    static String getGroupKexOpcodeName(int cmd) {
        String name = GROUP_KEX_OPCODES_MAP.get(cmd);
        if (GenericUtils.isEmpty(name)) {
            return SshConstants.getCommandMessageName(cmd);
        } else {
            return name;
        }
    }

    static String getSimpleKexOpcodeName(int cmd) {
        String name = SIMPLE_KEX_OPCODES_MAP.get(cmd);
        if (GenericUtils.isEmpty(name)) {
            return SshConstants.getCommandMessageName(cmd);
        } else {
            return name;
        }
    }

    /**
     * Initialize the key exchange algorithm.
     *
     * @param session the session using this algorithm
     * @param v_s     the server identification string
     * @param v_c     the client identification string
     * @param i_s     the server key initialization packet
     * @param i_c     the client key initialization packet
     * @throws Exception if an error occurs
     */
    void init(Session session, byte[] v_s, byte[] v_c, byte[] i_s, byte[] i_c) throws Exception;

    /**
     * Process the next packet
     *
     * @param cmd    the command
     * @param buffer the packet contents positioned after the command
     * @return a boolean indicating if the processing is complete or if more packets are to be received
     * @throws Exception if an error occurs
     */
    boolean next(int cmd, Buffer buffer) throws Exception;

    /**
     * The message digest used by this key exchange algorithm.
     *
     * @return the message digest
     */
    Digest getHash();

    /**
     * Retrieves the computed {@code h} parameter
     *
     * @return The {@code h} parameter
     */
    byte[] getH();

    /**
     * Retrieves the computed k parameter
     *
     * @return The {@code k} parameter
     */
    byte[] getK();

    /**
     * Retrieves the server's key
     *
     * @return The server's {@link PublicKey}
     */
    PublicKey getServerKey();
}
