package org.xbib.io.sshd.common.kex;

import org.xbib.io.sshd.common.NamedResource;
import org.xbib.io.sshd.common.OptionalFeature;

/**
 *
 */
public interface DHFactory extends NamedResource, OptionalFeature {
    boolean isGroupExchange();

    AbstractDH create(Object... params) throws Exception;
}
