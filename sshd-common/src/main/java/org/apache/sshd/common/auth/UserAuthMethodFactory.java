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

package org.apache.sshd.common.auth;

import java.io.IOException;
import java.util.Collection;

import org.apache.sshd.common.NamedResource;
import org.apache.sshd.common.session.SessionContext;

/**
 * Represents a user authentication method
 *
 * @param <S> The type of {@link SessionContext} being provided
 * to the instance creator
 * @param <M> The authentication method factory type
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
public interface UserAuthMethodFactory<S extends SessionContext, M extends UserAuthInstance<S>> extends NamedResource {
    /**
     * Password authentication method name
     */
    String PASSWORD = "password";

    /**
     * Public key authentication method name
     */
    String PUBLIC_KEY = "publickey";

    /**
     * Keyboard interactive authentication method
     */
    String KB_INTERACTIVE = "keyboard-interactive";

    /**
     * Host-based authentication method
     */
    String HOST_BASED = "hostbased";

    /**
     * @param session The session for which authentication is required
     * @return The authenticator instance
     * @throws IOException If failed to create the instance
     */
    M createUserAuth(S session) throws IOException;

    /**
     * @param <S> The type of {@link SessionContext} being provided
     * to the instance creator
     * @param <M> The authentication method factory type
     * @param session The session through which the request is being made
     * @param factories The available factories
     * @param name The requested factory name
     * @return The created authenticator instance - {@code null} if no matching factory
     * @throws IOException If failed to create the instance
     */
    static <S extends SessionContext, M extends UserAuthInstance<S>> M createUserAuth(
            S session, Collection<? extends UserAuthMethodFactory<S, M>> factories, String name)
                throws IOException {
        UserAuthMethodFactory<S, M> f =
            NamedResource.findByName(name, String.CASE_INSENSITIVE_ORDER, factories);
        if (f != null) {
            return f.createUserAuth(session);
        } else {
            return null;
        }
    }
}
