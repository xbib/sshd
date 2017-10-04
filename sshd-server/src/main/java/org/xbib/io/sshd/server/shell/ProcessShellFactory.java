package org.xbib.io.sshd.server.shell;

import org.xbib.io.sshd.common.Factory;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.OsUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;
import org.xbib.io.sshd.server.Command;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A {@link Factory} of {@link Command} that will create a new process and bridge
 * the streams.
 */
public class ProcessShellFactory extends AbstractLoggingBean implements Factory<Command> {
    private List<String> command;

    public ProcessShellFactory() {
        this(Collections.emptyList());
    }

    public ProcessShellFactory(String... command) {
        this(GenericUtils.isEmpty(command) ? Collections.emptyList() : Arrays.asList(command));
    }

    public ProcessShellFactory(List<String> command) {
        this.command = ValidateUtils.checkNotNullAndNotEmpty(command, "No command");
    }

    public List<String> getCommand() {
        return command;
    }

    public void setCommand(List<String> command) {
        this.command = ValidateUtils.checkNotNullAndNotEmpty(command, "No command");
    }

    public void setCommand(String... command) {
        setCommand(GenericUtils.isEmpty(command) ? Collections.emptyList() : Arrays.asList(command));
    }

    @Override
    public Command create() {
        return new InvertedShellWrapper(createInvertedShell());
    }

    protected InvertedShell createInvertedShell() {
        return new ProcessShell(resolveEffectiveCommand(getCommand()));
    }

    protected List<String> resolveEffectiveCommand(List<String> original) {
        if (!OsUtils.isWin32()) {
            return original;
        }

        // Turns out that running a command with no arguments works just fine
        if (GenericUtils.size(original) <= 1) {
            return original;
        }

        // For windows create a "cmd.exe /C "..."" string
        String cmdName = original.get(0);
        if (OsUtils.WINDOWS_SHELL_COMMAND_NAME.equalsIgnoreCase(cmdName)) {
            return original;    // assume callers knows what they're doing
        }

        return Arrays.asList(OsUtils.WINDOWS_SHELL_COMMAND_NAME, "/C", GenericUtils.join(original, ' '));
    }
}
