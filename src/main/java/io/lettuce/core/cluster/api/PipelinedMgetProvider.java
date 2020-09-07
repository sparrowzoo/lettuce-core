/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.lettuce.core.cluster.api;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @author harry
 */
public class PipelinedMgetProvider {
    private static final String DEFAULT_PROVIDER = "io.lettuce.core.cluster.api.SyncPipelinedMgetImpl";
    private volatile static PipelinedMget pipelinedMget;

    public static PipelinedMget getProvider() {
        if (pipelinedMget != null) {
            return pipelinedMget;
        }
        synchronized (PipelinedMgetProvider.class) {
            if (pipelinedMget != null) {
                return pipelinedMget;
            }

            ServiceLoader<PipelinedMget> loader = ServiceLoader.load(PipelinedMget.class);
            Iterator<PipelinedMget> it = loader.iterator();
            if (it.hasNext()) {
                pipelinedMget = it.next();
                return pipelinedMget;
            }

            try {
                Class<?> jsonClazz = Class.forName(DEFAULT_PROVIDER);
                pipelinedMget = (PipelinedMget) jsonClazz.newInstance();
                return pipelinedMget;
            } catch (Exception x) {
                throw new RuntimeException(
                        "Provider " + DEFAULT_PROVIDER + " could not be instantiated: " + x,
                        x);
            }
        }
    }
}
