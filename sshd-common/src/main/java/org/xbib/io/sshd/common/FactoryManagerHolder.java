package org.xbib.io.sshd.common;

/**
 *
 */
@FunctionalInterface
public interface FactoryManagerHolder {
    /**
     * @return The currently associated {@link FactoryManager}
     */
    FactoryManager getFactoryManager();
}
