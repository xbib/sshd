package org.xbib.io.sshd.common.forward;

import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;
import org.xbib.io.sshd.common.util.net.SshdSocketAddress;

import java.util.Collection;
import java.util.Objects;

/**
 */
public class LocalForwardingEntry extends SshdSocketAddress {
    private static final long serialVersionUID = 423661570180889621L;
    private final String alias;

    public LocalForwardingEntry(String hostName, String alias, int port) {
        super(hostName, port);
        this.alias = ValidateUtils.checkNotNullAndNotEmpty(alias, "No host alias");
    }

    /**
     * @param host    The host - ignored if {@code null}/empty - i.e., no match reported
     * @param port    The port - ignored if non-positive - i.e., no match reported
     * @param entries The {@link Collection} of {@link LocalForwardingEntry} to check
     *                - ignored if {@code null}/empty - i.e., no match reported
     * @return The <U>first</U> entry whose host or alias matches the host name - case
     * <U>sensitive</U> <B>and</B> has a matching port - {@code null} if no match found
     */
    public static LocalForwardingEntry findMatchingEntry(String host, int port, Collection<? extends LocalForwardingEntry> entries) {
        if (GenericUtils.isEmpty(host) || (port <= 0) || (GenericUtils.isEmpty(entries))) {
            return null;
        }

        for (LocalForwardingEntry e : entries) {
            if ((port == e.getPort()) && (host.equals(e.getHostName()) || host.equals(e.getAlias()))) {
                return e;
            }
        }

        return null;    // no match found
    }

    public String getAlias() {
        return alias;
    }

    @Override
    protected boolean isEquivalent(SshdSocketAddress that) {
        if (super.isEquivalent(that) && (that instanceof LocalForwardingEntry)) {
            LocalForwardingEntry entry = (LocalForwardingEntry) that;
            if (Objects.equals(this.getAlias(), entry.getAlias())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hashCode(getAlias());
    }

    @Override
    public String toString() {
        return super.toString() + " - " + getAlias();
    }
}