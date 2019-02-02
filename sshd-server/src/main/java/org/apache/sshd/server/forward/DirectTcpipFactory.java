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
package org.apache.sshd.server.forward;

import org.apache.sshd.common.channel.Channel;
import org.apache.sshd.common.forward.TcpipFactory;
import org.apache.sshd.common.forward.WrappedForwardingFilter;

/**
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
public class DirectTcpipFactory extends TcpipFactory {
    public static final DirectTcpipFactory INSTANCE = new DirectTcpipFactory();

    public DirectTcpipFactory() {
        super(WrappedForwardingFilter.Type.Direct);
    }

    @Override
    public Channel create() {
        TcpipServerChannel channel = new TcpipServerChannel(getType());
        channel.setExecutorService(getExecutorService());
        channel.setShutdownOnExit(isShutdownOnExit());
        return channel;
    }
}