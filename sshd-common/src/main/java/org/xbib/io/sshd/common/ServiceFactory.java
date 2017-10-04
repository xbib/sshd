package org.xbib.io.sshd.common;

import org.xbib.io.sshd.common.session.Session;

import java.io.IOException;
import java.util.Collection;

/**
 *
 */
public interface ServiceFactory extends NamedResource {
    /**
     * Create an instance of the specified name by looking up the needed factory
     * in the list (case <U>insensitive</U>.
     *
     * @param factories list of available factories
     * @param name      the factory name to use
     * @param session   the referenced {@link Session}
     * @return a newly created object or {@code null} if the factory is not in the list
     * @throws IOException if session creation failed
     * @see ServiceFactory#create(Session)
     */
    static Service create(Collection<? extends ServiceFactory> factories, String name, Session session) throws IOException {
        ServiceFactory factory = NamedResource.findByName(name, String.CASE_INSENSITIVE_ORDER, factories);
        if (factory == null) {
            return null;
        } else {
            return factory.create(session);
        }
    }

    Service create(Session session) throws IOException;
}
