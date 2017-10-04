package org.xbib.io.sshd.server.shell;

import org.xbib.io.sshd.common.util.OsUtils;

import java.util.List;

/**
 * A simplistic interactive shell factory.
 */
public class InteractiveProcessShellFactory extends ProcessShellFactory {
    public static final InteractiveProcessShellFactory INSTANCE = new InteractiveProcessShellFactory();

    public InteractiveProcessShellFactory() {
        super(OsUtils.resolveDefaultInteractiveCommand());
    }

    @Override
    protected List<String> resolveEffectiveCommand(List<String> original) {
        return original;
    }
}
