package org.xbib.io.sshd.common.agent;

import org.xbib.io.sshd.common.agent.SshAgent;
import org.xbib.io.sshd.common.agent.SshAgentConstants;
import org.xbib.io.sshd.common.SshException;
import org.xbib.io.sshd.common.util.Pair;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;
import org.xbib.io.sshd.common.util.threads.ExecutorServiceConfigurer;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 *
 */
public abstract class AbstractAgentProxy extends AbstractLoggingBean implements SshAgent, ExecutorServiceConfigurer {
    private ExecutorService executor;
    private boolean shutdownExecutor;

    protected AbstractAgentProxy() {
        super();
    }

    @Override
    public ExecutorService getExecutorService() {
        return executor;
    }

    @Override
    public void setExecutorService(ExecutorService service) {
        executor = service;
    }

    @Override
    public boolean isShutdownOnExit() {
        return shutdownExecutor;
    }

    @Override
    public void setShutdownOnExit(boolean shutdown) {
        shutdownExecutor = shutdown;
    }

    @Override
    public List<Pair<PublicKey, String>> getIdentities() throws IOException {
        Buffer buffer = createBuffer(SshAgentConstants.SSH2_AGENTC_REQUEST_IDENTITIES, 1);
        buffer = request(prepare(buffer));
        int type = buffer.getUByte();
        if (type != SshAgentConstants.SSH2_AGENT_IDENTITIES_ANSWER) {
            throw new SshException("Bad agent identities answer: " + SshAgentConstants.getCommandMessageName(type));
        }

        int nbIdentities = buffer.getInt();
        if (nbIdentities > 1024) {
            throw new SshException("Bad identities count: " + nbIdentities);
        }

        List<Pair<PublicKey, String>> keys = new ArrayList<>(nbIdentities);
        for (int i = 0; i < nbIdentities; i++) {
            PublicKey key = buffer.getPublicKey();
            String comment = buffer.getString();
            keys.add(new Pair<>(key, comment));
        }

        return keys;
    }

    @Override
    public byte[] sign(PublicKey key, byte[] data) throws IOException {
        Buffer buffer = createBuffer(SshAgentConstants.SSH2_AGENTC_SIGN_REQUEST);
        buffer.putPublicKey(key);
        buffer.putBytes(data);
        buffer.putInt(0);
        buffer = request(prepare(buffer));

        int responseType = buffer.getUByte();
        if (responseType != SshAgentConstants.SSH2_AGENT_SIGN_RESPONSE) {
            throw new SshException("Bad signing response type: " + SshAgentConstants.getCommandMessageName(responseType));
        }

        Buffer buf = new ByteArrayBuffer(buffer.getBytes());
        String algorithm = buf.getString();
        byte[] signature = buf.getBytes();

        return signature;
    }

    @Override
    public void addIdentity(KeyPair kp, String comment) throws IOException {
        Buffer buffer = createBuffer(SshAgentConstants.SSH2_AGENTC_ADD_IDENTITY);
        buffer.putKeyPair(kp);
        buffer.putString(comment);
        buffer = request(prepare(buffer));

        int available = buffer.available();
        int response = (available >= 1) ? buffer.getUByte() : -1;
        if ((available != 1) || (response != SshAgentConstants.SSH_AGENT_SUCCESS)) {
            throw new SshException("Bad addIdentity response (" + SshAgentConstants.getCommandMessageName(response) + ") - available=" + available);
        }
    }

    @Override
    public void removeIdentity(PublicKey key) throws IOException {
        Buffer buffer = createBuffer(SshAgentConstants.SSH2_AGENTC_REMOVE_IDENTITY);
        buffer.putPublicKey(key);
        buffer = request(prepare(buffer));

        int available = buffer.available();
        int response = (available >= 1) ? buffer.getUByte() : -1;
        if ((available != 1) || (response != SshAgentConstants.SSH_AGENT_SUCCESS)) {
            throw new SshException("Bad removeIdentity response (" + SshAgentConstants.getCommandMessageName(response) + ") - available=" + available);
        }
    }

    @Override
    public void removeAllIdentities() throws IOException {
        Buffer buffer = createBuffer(SshAgentConstants.SSH2_AGENTC_REMOVE_ALL_IDENTITIES, 1);
        buffer = request(prepare(buffer));

        int available = buffer.available();
        int response = (available >= 1) ? buffer.getUByte() : -1;
        if ((available != 1) || (response != SshAgentConstants.SSH_AGENT_SUCCESS)) {
            throw new SshException("Bad removeAllIdentities response (" + SshAgentConstants.getCommandMessageName(response) + ") - available=" + available);
        }
    }

    @Override
    public void close() throws IOException {
        ExecutorService service = getExecutorService();
        if ((service != null) && isShutdownOnExit() && (!service.isShutdown())) {
            Collection<?> runners = service.shutdownNow();
        }
    }

    protected Buffer createBuffer(byte cmd) {
        return createBuffer(cmd, 0);
    }

    protected Buffer createBuffer(byte cmd, int extraLen) {
        Buffer buffer = new ByteArrayBuffer((extraLen <= 0) ? ByteArrayBuffer.DEFAULT_SIZE : extraLen + Byte.SIZE, false);
        buffer.putInt(0);
        buffer.putByte(cmd);
        return buffer;
    }

    protected Buffer prepare(Buffer buffer) {
        int wpos = buffer.wpos();
        buffer.wpos(0);
        buffer.putInt(wpos - 4);
        buffer.wpos(wpos);
        return buffer;
    }

    protected abstract Buffer request(Buffer buffer) throws IOException;

}
