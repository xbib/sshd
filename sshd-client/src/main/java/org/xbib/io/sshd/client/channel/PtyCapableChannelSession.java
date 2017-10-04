package org.xbib.io.sshd.client.channel;

import org.xbib.io.sshd.common.agent.SshAgentFactory;
import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.channel.PtyMode;
import org.xbib.io.sshd.common.channel.SttySupport;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.OsUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.common.util.buffer.ByteArrayBuffer;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <P>Serves as the base channel session for executing remote commands - including
 * a full shell. <B>Note:</B> all the configuration changes via the various
 * {@code setXXX} methods must be made <U>before</U> the channel is actually
 * open. If they are invoked afterwards then they have no effect (silently
 * ignored).</P>
 * <P>A typical code snippet would be:</P>
 * <PRE>
 * try (client = SshClient.setUpDefaultClient()) {
 * client.start();
 * try (ClientSession s = client.connect(getCurrentTestName(), "localhost", port).verify(7L, TimeUnit.SECONDS).getSession()) {
 * s.addPasswordIdentity(getCurrentTestName());
 * s.auth().verify(5L, TimeUnit.SECONDS);
 * try (ChannelExec shell = s.createExecChannel("my super duper command")) {
 * shell.setEnv("var1", "val1");
 * shell.setEnv("var2", "val2");
 * ...etc...
 * shell.setPtyType(...);
 * shell.setPtyLines(...);
 * ...etc...
 * shell.open().verify(5L, TimeUnit.SECONDS);
 * shell.waitFor(ClientChannel.CLOSED, TimeUnit.SECONDS.toMillis(17L));    // can use zero for infinite wait
 * Integer status = shell.getExitStatus();
 * if (status.intValue() != 0) {
 * ...error...
 * }
 * }
 * } finally {
 * client.stop();
 * }
 * }
 * </PRE>
 */
public class PtyCapableChannelSession extends ChannelSession {
    public static final int DEFAULT_COLUMNS_COUNT = 80;
    public static final int DEFAULT_ROWS_COUNT = 24;
    public static final int DEFAULT_WIDTH = 640;
    public static final int DEFAULT_HEIGHT = 480;
    public static final Map<PtyMode, Integer> DEFAULT_PTY_MODES =
            GenericUtils.<PtyMode, Integer>mapBuilder()
                    .put(PtyMode.ISIG, 1)
                    .put(PtyMode.ICANON, 1)
                    .put(PtyMode.ECHO, 1)
                    .put(PtyMode.ECHOE, 1)
                    .put(PtyMode.ECHOK, 1)
                    .put(PtyMode.ECHONL, 0)
                    .put(PtyMode.NOFLSH, 0)
                    .immutable();
    private final Map<String, String> env = new LinkedHashMap<>();
    private boolean agentForwarding;
    private boolean usePty;
    private String ptyType;
    private int ptyColumns = DEFAULT_COLUMNS_COUNT;
    private int ptyLines = DEFAULT_ROWS_COUNT;
    private int ptyWidth = DEFAULT_WIDTH;
    private int ptyHeight = DEFAULT_HEIGHT;
    private Map<PtyMode, Integer> ptyModes = new EnumMap<>(PtyMode.class);

    public PtyCapableChannelSession(boolean usePty) {
        this.usePty = usePty;
        ptyType = System.getenv("TERM");
        if (GenericUtils.isEmpty(ptyType)) {
            ptyType = "dummy";
        }

        ptyModes.putAll(DEFAULT_PTY_MODES);
    }

    public void setupSensibleDefaultPty() {
        try {
            if (OsUtils.isUNIX()) {
                ptyModes = SttySupport.getUnixPtyModes();
                ptyColumns = SttySupport.getTerminalWidth();
                ptyLines = SttySupport.getTerminalHeight();
            } else {
                ptyType = "windows";
            }
        } catch (Throwable t) {
            // Ignore exceptions
        }
    }

    public boolean isAgentForwarding() {
        return agentForwarding;
    }

    public void setAgentForwarding(boolean agentForwarding) {
        this.agentForwarding = agentForwarding;
    }

    public boolean isUsePty() {
        return usePty;
    }

    public void setUsePty(boolean usePty) {
        this.usePty = usePty;
    }

    public String getPtyType() {
        return ptyType;
    }

    public void setPtyType(String ptyType) {
        this.ptyType = ptyType;
    }

    public int getPtyColumns() {
        return ptyColumns;
    }

    public void setPtyColumns(int ptyColumns) {
        this.ptyColumns = ptyColumns;
    }

    public int getPtyLines() {
        return ptyLines;
    }

    public void setPtyLines(int ptyLines) {
        this.ptyLines = ptyLines;
    }

    public int getPtyWidth() {
        return ptyWidth;
    }

    public void setPtyWidth(int ptyWidth) {
        this.ptyWidth = ptyWidth;
    }

    public int getPtyHeight() {
        return ptyHeight;
    }

    public void setPtyHeight(int ptyHeight) {
        this.ptyHeight = ptyHeight;
    }

    public Map<PtyMode, Integer> getPtyModes() {
        return ptyModes;
    }

    public void setPtyModes(Map<PtyMode, Integer> ptyModes) {
        this.ptyModes = (ptyModes == null) ? Collections.emptyMap() : ptyModes;
    }

    public void setEnv(String key, String value) {
        env.put(key, value);
    }

    public void sendWindowChange(int columns, int lines) throws IOException {
        sendWindowChange(columns, lines, ptyHeight, ptyWidth);
    }

    public void sendWindowChange(int columns, int lines, int height, int width) throws IOException {

        ptyColumns = columns;
        ptyLines = lines;
        ptyHeight = height;
        ptyWidth = width;

        Session session = getSession();
        Buffer buffer = session.createBuffer(SshConstants.SSH_MSG_CHANNEL_REQUEST, Long.SIZE);
        buffer.putInt(getRecipient());
        buffer.putString("window-change");
        buffer.putBoolean(false);   // want-reply
        buffer.putInt(ptyColumns);
        buffer.putInt(ptyLines);
        buffer.putInt(ptyHeight);
        buffer.putInt(ptyWidth);
        writePacket(buffer);
    }

    protected void doOpenPty() throws IOException {
        Session session = getSession();
        if (agentForwarding) {

            String channelType = session.getStringProperty(SshAgentFactory.PROXY_AUTH_CHANNEL_TYPE, SshAgentFactory.DEFAULT_PROXY_AUTH_CHANNEL_TYPE);
            Buffer buffer = session.createBuffer(SshConstants.SSH_MSG_CHANNEL_REQUEST, Long.SIZE);
            buffer.putInt(getRecipient());
            buffer.putString(channelType);
            buffer.putBoolean(false);   // want-reply
            writePacket(buffer);
        }

        if (usePty) {

            Buffer buffer = session.createBuffer(SshConstants.SSH_MSG_CHANNEL_REQUEST, Byte.MAX_VALUE);
            buffer.putInt(getRecipient());
            buffer.putString("pty-req");
            buffer.putBoolean(false);   // want-reply
            buffer.putString(ptyType);
            buffer.putInt(ptyColumns);
            buffer.putInt(ptyLines);
            buffer.putInt(ptyHeight);
            buffer.putInt(ptyWidth);

            Buffer modes = new ByteArrayBuffer(GenericUtils.size(ptyModes) * (1 + Integer.BYTES) + Long.SIZE, false);
            ptyModes.forEach((mode, value) -> {
                modes.putByte((byte) mode.toInt());
                modes.putInt(value.longValue());
            });
            modes.putByte(PtyMode.TTY_OP_END);
            buffer.putBytes(modes.getCompactData());
            writePacket(buffer);
        }

        if (GenericUtils.size(env) > 0) {

            // Cannot use forEach because of the IOException being thrown by writePacket
            for (Map.Entry<String, String> entry : env.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                Buffer buffer = session.createBuffer(SshConstants.SSH_MSG_CHANNEL_REQUEST, key.length() + value.length() + Integer.SIZE);
                buffer.putInt(getRecipient());
                buffer.putString("env");
                buffer.putBoolean(false);   // want-reply
                buffer.putString(key);
                buffer.putString(value);
                writePacket(buffer);
            }
        }
    }
}