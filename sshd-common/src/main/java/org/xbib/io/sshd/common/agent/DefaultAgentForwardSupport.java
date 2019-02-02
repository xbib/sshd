package org.xbib.io.sshd.common.agent;

import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.SshException;
import org.xbib.io.sshd.common.session.ConnectionService;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.closeable.AbstractCloseable;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public class DefaultAgentForwardSupport extends AbstractCloseable implements AgentForwardSupport {

    private final ConnectionService serviceInstance;
    private final AtomicReference<SshAgentServer> agentServerHolder = new AtomicReference<>();

    public DefaultAgentForwardSupport(ConnectionService service) {
        serviceInstance = Objects.requireNonNull(service, "No connection service");
    }

    @Override
    public String initialize() throws IOException {
        Session session = serviceInstance.getSession();
        try {
            SshAgentServer agentServer;
            synchronized (agentServerHolder) {
                agentServer = agentServerHolder.get();
                if (agentServer != null) {
                    return agentServer.getId();
                }

                agentServer = Objects.requireNonNull(createSshAgentServer(serviceInstance, session), "No agent server created");
                agentServerHolder.set(agentServer);
            }
            return agentServer.getId();

        } catch (Throwable t) {
            if (t instanceof IOException) {
                throw (IOException) t;
            } else {
                throw new SshException(t);
            }
        }
    }

    protected SshAgentServer createSshAgentServer(ConnectionService service, Session session) throws Throwable {
        FactoryManager manager = Objects.requireNonNull(session.getFactoryManager(), "No session factory manager");
        SshAgentFactory factory = Objects.requireNonNull(manager.getAgentFactory(), "No agent factory");
        return factory.createServer(service);
    }

    @Override
    public void close() throws IOException {
        SshAgentServer agentServer = agentServerHolder.getAndSet(null);
        if (agentServer != null) {
            agentServer.close();
        }
    }

    @Override
    protected void doCloseImmediately() {
        try {
            close();
        } catch (IOException e) {
            throw new RuntimeException("Failed (" + e.getClass().getSimpleName() + ") to close agent: " + e.getMessage(), e);
        }
        super.doCloseImmediately();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + serviceInstance.getSession() + "]";
    }
}
