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

package org.apache.sshd.common.config.keys.putty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.sshd.common.config.keys.loader.KeyPairResourceParser;
import org.apache.sshd.common.util.security.SecurityUtils;

/**
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
public final class PuttyKeyUtils {
    public static final List<PuttyKeyPairResourceParser<?, ?>> DEFAULT_PARSERS =
            Collections.unmodifiableList(new ArrayList<PuttyKeyPairResourceParser<?, ?>>() {
                // Not serializing it
                private static final long serialVersionUID = 1L;

                {
                    add(RSAPuttyKeyDecoder.INSTANCE);
                    add(DSSPuttyKeyDecoder.INSTANCE);

                    if (SecurityUtils.isECCSupported()) {
                        add(ECDSAPuttyKeyDecoder.INSTANCE);
                    }

                    if (SecurityUtils.isEDDSACurveSupported()) {
                        add(EdDSAPuttyKeyDecoder.INSTANCE);
                    }
                }
            });

    public static final NavigableMap<String, PuttyKeyPairResourceParser<?, ?>> BY_KEY_TYPE =
            Collections.unmodifiableNavigableMap(
                    new TreeMap<String, PuttyKeyPairResourceParser<?, ?>>(String.CASE_INSENSITIVE_ORDER) {
                        // Not serializing it
                        private static final long serialVersionUID = 1L;

                        {
                            for (PuttyKeyPairResourceParser<?, ?> p : DEFAULT_PARSERS) {
                                Collection<String> supported = p.getSupportedTypeNames();
                                for (String k : supported) {
                                    put(k, p);
                                }
                            }
                        }
            });

    public static final KeyPairResourceParser DEFAULT_INSTANCE =
            KeyPairResourceParser.aggregate(DEFAULT_PARSERS);

    private PuttyKeyUtils() {
        throw new UnsupportedOperationException("No instance");
    }
}
