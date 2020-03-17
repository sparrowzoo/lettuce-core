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

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;

import java.io.IOException;

/**
 * @author Mark Paluch
 */
public class ReadWriteExample {
    public static void main(String[] args) throws IOException {
        // Syntax: redis://[password@]host[:port][/databaseNumber]
        RedisClient redisClient = RedisClient.create(RedisURI.create("redis://localhost:6379/0"));
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        for (int i=0;i<10;i++) {
            redisClient.connect();
        }
        System.in.read();
        System.out.println("Connected to Redis");
        RedisCommands<String, String> sync = connection.sync();
        sync.set("foo", "bar");
        String value = sync.get("foo");
        System.out.println(value);
        connection.close();
        redisClient.shutdown();
    }
}
