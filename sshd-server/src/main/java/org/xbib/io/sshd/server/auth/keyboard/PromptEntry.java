package org.xbib.io.sshd.server.auth.keyboard;

import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 */
public class PromptEntry implements Serializable, Cloneable {
    private static final long serialVersionUID = 8206049800536373640L;

    private String prompt;
    private boolean echo;

    public PromptEntry() {
        super();
    }

    public PromptEntry(String prompt, boolean echo) {
        this.prompt = prompt;
        this.echo = echo;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public boolean isEcho() {
        return echo;
    }

    public void setEcho(boolean echo) {
        this.echo = echo;
    }

    public <B extends Buffer> B append(B buffer) {
        buffer.putString(getPrompt());
        buffer.putBoolean(isEcho());
        return buffer;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getPrompt()) + (isEcho() ? 1 : 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        PromptEntry other = (PromptEntry) obj;
        return Objects.equals(getPrompt(), other.getPrompt()) && (isEcho() == other.isEcho());
    }

    @Override
    public PromptEntry clone() {
        try {
            return getClass().cast(super.clone());
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Failed to clone " + toString() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return getPrompt() + "(echo=" + isEcho() + ")";
    }
}
