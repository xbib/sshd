/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sshd.common;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.sshd.common.channel.ChannelFactory;
import org.apache.sshd.common.channel.ChannelListenerManager;
import org.apache.sshd.common.channel.RequestHandler;
import org.apache.sshd.common.channel.throttle.ChannelStreamWriterResolverManager;
import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.forward.ForwarderFactory;
import org.apache.sshd.common.forward.PortForwardingEventListenerManager;
import org.apache.sshd.common.io.IoServiceEventListenerManager;
import org.apache.sshd.common.io.IoServiceFactory;
import org.apache.sshd.common.kex.KexFactoryManager;
import org.apache.sshd.common.random.Random;
import org.apache.sshd.common.session.ConnectionService;
import org.apache.sshd.common.session.ReservedSessionMessagesManager;
import org.apache.sshd.common.session.SessionDisconnectHandlerManager;
import org.apache.sshd.common.session.SessionHeartbeatController;
import org.apache.sshd.common.session.SessionListenerManager;
import org.apache.sshd.common.session.UnknownChannelReferenceHandlerManager;

/**
 * This interface allows retrieving all the <code>NamedFactory</code> used in the SSH protocol.
 *
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
public interface FactoryManager
        extends KexFactoryManager,
        SessionListenerManager,
        ReservedSessionMessagesManager,
        SessionDisconnectHandlerManager,
        ChannelListenerManager,
        ChannelStreamWriterResolverManager,
        UnknownChannelReferenceHandlerManager,
        PortForwardingEventListenerManager,
        IoServiceEventListenerManager,
        AttributeStore,
        SessionHeartbeatController {

    /**
     * The default {@code REPORTED_VERSION} of {@link FactoryManager#getVersion()} if the built-in version information
     * cannot be accessed
     */
    String DEFAULT_VERSION = "SSHD-UNKNOWN";

    /**
     * An upper case string identifying the version of the software used on client or server side. This version includes
     * the name and version of the software and usually looks like this: <code>SSHD-CORE-1.0</code>
     *
     * @return the version of the software
     */
    String getVersion();

    IoServiceFactory getIoServiceFactory();

    /**
     * Retrieve the <code>Random</code> factory to be used.
     *
     * @return The <code>Random</code> factory, never {@code null}
     */
    Factory<Random> getRandomFactory();

    /**
     * Retrieve the list of named factories for <code>Channel</code> objects.
     *
     * @return A list of {@link ChannelFactory}-ies, never {@code null}
     */
    List<ChannelFactory> getChannelFactories();

    /**
     * Retrieve the <code>ScheduledExecutorService</code> to be used.
     *
     * @return The {@link ScheduledExecutorService}, never {@code null}
     */
    ScheduledExecutorService getScheduledExecutorService();

    /**
     * Retrieve the forwarder factory used to support forwarding.
     *
     * @return The {@link ForwarderFactory}
     */
    ForwarderFactory getForwarderFactory();

    /**
     * Retrieve the <code>FileSystemFactory</code> to be used to traverse the file system.
     *
     * @return a valid {@link FileSystemFactory} instance or {@code null} if file based interactions are not supported
     *         on this server
     */
    FileSystemFactory getFileSystemFactory();

    /**
     * Retrieve the list of SSH <code>Service</code> factories.
     *
     * @return a list of named <code>Service</code> factories, never {@code null}
     */
    List<ServiceFactory> getServiceFactories();

    /**
     * Retrieve the list of global request handlers.
     *
     * @return a list of named <code>GlobalRequestHandler</code>
     */
    List<RequestHandler<ConnectionService>> getGlobalRequestHandlers();

    @Override
    default <T> T resolveAttribute(AttributeKey<T> key) {
        return resolveAttribute(this, key);
    }

    /**
     * @param  <T>     The generic attribute type
     * @param  manager The {@link FactoryManager} - ignored if {@code null}
     * @param  key     The attribute key - never {@code null}
     * @return         Associated value - {@code null} if not found
     */
    static <T> T resolveAttribute(FactoryManager manager, AttributeKey<T> key) {
        Objects.requireNonNull(key, "No key");
        return (manager == null) ? null : manager.getAttribute(key);
    }
}
