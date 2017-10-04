package org.xbib.io.sshd.server.shell;

import org.xbib.io.sshd.common.channel.PtyMode;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.io.IoUtils;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;
import org.xbib.io.sshd.server.Environment;
import org.xbib.io.sshd.server.channel.PuttyRequestHandler;
import org.xbib.io.sshd.server.session.ServerSession;
import org.xbib.io.sshd.server.session.ServerSessionHolder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bridges the I/O streams between the SSH command and the process that executes it.
 */
public class ProcessShell extends AbstractLoggingBean implements InvertedShell, ServerSessionHolder {
    private final List<String> command;
    private String cmdValue;
    private ServerSession session;
    private Process process;
    private TtyFilterOutputStream in;
    private TtyFilterInputStream out;
    private TtyFilterInputStream err;

    /**
     * @param command The command components which when joined (with space separator)
     *                create the full command to be executed by the shell
     */
    public ProcessShell(String... command) {
        this(GenericUtils.isEmpty(command) ? Collections.emptyList() : Arrays.asList(command));
    }

    public ProcessShell(Collection<String> command) {
        // we copy the original list so as not to change it
        this.command = new ArrayList<>(ValidateUtils.checkNotNullAndNotEmpty(command, "No process shell command(s)"));
        this.cmdValue = GenericUtils.join(command, ' ');
    }

    @Override
    public ServerSession getServerSession() {
        return session;
    }

    @Override
    public void setSession(ServerSession session) {
        this.session = Objects.requireNonNull(session, "No server session");
        ValidateUtils.checkTrue(process == null, "Session set after process started");
    }

    @Override
    public void start(Environment env) throws IOException {
        Map<String, String> varsMap = resolveShellEnvironment(env.getEnv());
        for (int i = 0; i < command.size(); i++) {
            String cmd = command.get(i);
            if ("$USER".equals(cmd)) {
                cmd = varsMap.get("USER");
                command.set(i, cmd);
                cmdValue = GenericUtils.join(command, ' ');
            }
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        if (GenericUtils.size(varsMap) > 0) {
            try {
                Map<String, String> procEnv = builder.environment();
                procEnv.putAll(varsMap);
            } catch (Exception e) {
            }
        }

        process = builder.start();

        Map<PtyMode, ?> modes = resolveShellTtyOptions(env.getPtyModes());
        out = new TtyFilterInputStream(process.getInputStream(), modes);
        err = new TtyFilterInputStream(process.getErrorStream(), modes);
        in = new TtyFilterOutputStream(process.getOutputStream(), err, modes);
    }

    protected Map<String, String> resolveShellEnvironment(Map<String, String> env) {
        return env;
    }

    // for some reason these modes provide best results BOTH with Linux SSH client and PUTTY
    protected Map<PtyMode, Integer> resolveShellTtyOptions(Map<PtyMode, Integer> modes) {
        if (PuttyRequestHandler.isPuttyClient(getServerSession())) {
            return PuttyRequestHandler.resolveShellTtyOptions(modes);
        } else {
            return modes;
        }
    }

    @Override
    public OutputStream getInputStream() {
        return in;
    }

    @Override
    public InputStream getOutputStream() {
        return out;
    }

    @Override
    public InputStream getErrorStream() {
        return err;
    }

    @Override
    public boolean isAlive() {
        return process.isAlive();
    }

    @Override
    public int exitValue() {
        if (isAlive()) {
            try {
                return process.waitFor();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            return process.exitValue();
        }
    }

    @Override
    public void destroy() {
        // NOTE !!! DO NOT NULL-IFY THE PROCESS SINCE "exitValue" is called subsequently
        if (process != null) {
            process.destroy();
        }

        IOException e = IoUtils.closeQuietly(getInputStream(), getOutputStream(), getErrorStream());
        if (e != null) {
        }
    }

    @Override
    public String toString() {
        return GenericUtils.isEmpty(cmdValue) ? super.toString() : cmdValue;
    }
}
