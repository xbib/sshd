package org.xbib.io.sshd.server.auth.keyboard;

import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Represents a server &quot;challenge&quot; as per
 * <A HREF="https://www.ietf.org/rfc/rfc4256.txt">RFC-4256</A>.
 */
public class InteractiveChallenge implements Cloneable {
    private String interactionName;
    private String interactionInstruction;
    private String languageTag;
    private List<PromptEntry> prompts = new ArrayList<>();

    public InteractiveChallenge() {
        super();
    }

    public String getInteractionName() {
        return interactionName;
    }

    public void setInteractionName(String interactionName) {
        this.interactionName = interactionName;
    }

    public String getInteractionInstruction() {
        return interactionInstruction;
    }

    public void setInteractionInstruction(String interactionInstruction) {
        this.interactionInstruction = interactionInstruction;
    }

    public String getLanguageTag() {
        return languageTag;
    }

    public void setLanguageTag(String languageTag) {
        this.languageTag = languageTag;
    }

    public void addPrompt(String prompt, boolean echo) {
        addPrompt(new PromptEntry(prompt, echo));
    }

    public void addPrompt(PromptEntry entry) {
        this.prompts.add(Objects.requireNonNull(entry, "No entry"));
    }

    public List<PromptEntry> getPrompts() {
        return prompts;
    }

    // NOTE: prompts are COPIED to the local one
    public void setPrompts(Collection<? extends PromptEntry> prompts) {
        clearPrompts();

        if (GenericUtils.size(prompts) > 0) {
            this.prompts.addAll(prompts);
        }
    }

    public void clearPrompts() {
        this.prompts.clear();
    }

    public <B extends Buffer> B append(B buffer) {
        buffer.putString(getInteractionName());
        buffer.putString(getInteractionInstruction());
        buffer.putString(getLanguageTag());

        List<PromptEntry> entries = getPrompts();
        int numEntries = GenericUtils.size(entries);
        buffer.putInt(numEntries);

        for (int index = 0; index < numEntries; index++) {
            PromptEntry e = entries.get(index);
            e.append(buffer);
        }

        return buffer;
    }

    @Override
    public InteractiveChallenge clone() {
        try {
            InteractiveChallenge other = getClass().cast(super.clone());
            other.prompts = new ArrayList<>();
            for (PromptEntry entry : getPrompts()) {
                other.addPrompt(entry.clone());
            }
            return other;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Failed to clone " + toString() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return getInteractionName() + "[" + getInteractionInstruction() + "](" + getLanguageTag() + "): " + getPrompts();
    }
}
