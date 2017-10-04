package org.xbib.io.sshd.common.agent;

import org.xbib.io.sshd.common.agent.SshAgent;
import org.xbib.io.sshd.common.agent.SshAgentConstants;
import org.xbib.io.sshd.common.config.keys.KeyUtils;
import org.xbib.io.sshd.common.util.Pair;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.List;

/**
 *
 */
public abstract class AbstractAgentClient extends AbstractLoggingBean {

    private final Buffer buffer = new ByteArrayBuffer();
    private final SshAgent agent;

    protected AbstractAgentClient(SshAgent agent) {
        this.agent = agent;
    }

    public synchronized void messageReceived(Buffer message) throws IOException {
        buffer.putBuffer(message);
        int avail = buffer.available();
        if (avail < 4) {
            return;
        }

        int rpos = buffer.rpos();
        int len = buffer.getInt();
        buffer.rpos(rpos);

        avail = buffer.available();
        if (avail < (len + 4)) {
            return;
        }

        Buffer rep = new ByteArrayBuffer();
        rep.putInt(0);
        rep.rpos(rep.wpos());

        Buffer req = new ByteArrayBuffer(buffer.getBytes());
        int cmd = -1;
        try {
            cmd = req.getUByte();
            process(cmd, req, rep);
        } catch (Exception e) {
            rep.clear();
            rep.putInt(0);
            rep.rpos(rep.wpos());
            rep.putInt(1);
            rep.putByte(SshAgentConstants.SSH2_AGENT_FAILURE);
        }
        reply(prepare(rep));
    }

    protected void process(int cmd, Buffer req, Buffer rep) throws Exception {
        switch (cmd) {
            case SshAgentConstants.SSH2_AGENTC_REQUEST_IDENTITIES: {
                List<Pair<PublicKey, String>> keys = agent.getIdentities();
                rep.putByte(SshAgentConstants.SSH2_AGENT_IDENTITIES_ANSWER);
                rep.putInt(keys.size());
                for (Pair<PublicKey, String> key : keys) {
                    rep.putPublicKey(key.getFirst());
                    rep.putString(key.getSecond());
                }
                break;
            }
            case SshAgentConstants.SSH2_AGENTC_SIGN_REQUEST: {
                PublicKey signingKey = req.getPublicKey();
                byte[] data = req.getBytes();
                int flags = req.getInt();
                String keyType = ValidateUtils.checkNotNullAndNotEmpty(
                        KeyUtils.getKeyType(signingKey),
                        "Cannot resolve key type of %s",
                        signingKey.getClass().getSimpleName());
                byte[] signature = agent.sign(signingKey, data);
                Buffer sig = new ByteArrayBuffer(keyType.length() + signature.length + Long.SIZE, false);
                sig.putString(keyType);
                sig.putBytes(signature);
                rep.putByte(SshAgentConstants.SSH2_AGENT_SIGN_RESPONSE);
                rep.putBytes(sig.array(), sig.rpos(), sig.available());
                break;
            }
            case SshAgentConstants.SSH2_AGENTC_ADD_IDENTITY: {
                KeyPair keyToAdd = req.getKeyPair();
                String comment = req.getString();
                agent.addIdentity(keyToAdd, comment);
                rep.putByte(SshAgentConstants.SSH_AGENT_SUCCESS);
                break;
            }
            case SshAgentConstants.SSH2_AGENTC_REMOVE_IDENTITY: {
                PublicKey keyToRemove = req.getPublicKey();
                agent.removeIdentity(keyToRemove);
                rep.putByte(SshAgentConstants.SSH_AGENT_SUCCESS);
                break;
            }
            case SshAgentConstants.SSH2_AGENTC_REMOVE_ALL_IDENTITIES:
                agent.removeAllIdentities();
                rep.putByte(SshAgentConstants.SSH_AGENT_SUCCESS);
                break;
            default:
                rep.putByte(SshAgentConstants.SSH2_AGENT_FAILURE);
                break;
        }
    }

    protected Buffer prepare(Buffer buf) {
        int len = buf.available();
        int rpos = buf.rpos();
        int wpos = buf.wpos();
        buf.rpos(rpos - 4);
        buf.wpos(rpos - 4);
        buf.putInt(len);
        buf.wpos(wpos);
        return buf;
    }

    protected abstract void reply(Buffer buf) throws IOException;

}
