/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.examples;

import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.codec.RedisCodec;

import java.util.Arrays;

/**
 * @author Mark Paluch
 */
public class ConnectToRedisCluster {

    public static void main(String[] args) {

        // Syntax: redis://[password@]host[:port]
        RedisClusterClient redisClient = RedisClusterClient.create(Arrays.asList(RedisURI.create("redis://10.2.2.13:8030")));

       // StatefulRedisClusterConnection<String, String> connection = redisClient.connect(new RedisCodec<String, Integer>() {
       // });

//        connection.async().set("test-cluster","ABC");
//        System.out.println("Connected to Redis");
//
//        connection.close();
//        redisClient.shutdown();
    }
}
