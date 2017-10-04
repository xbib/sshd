package org.xbib.io.sshd.common.io;

import org.xbib.io.sshd.common.FactoryManager;

/**
 *
 */
@FunctionalInterface
public interface IoServiceFactoryFactory {

    IoServiceFactory create(FactoryManager manager);
}
