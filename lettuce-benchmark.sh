## key_count thread_size loop slot_size  key_length qps
java -cp lettuce-core-6.0.0.BUILD-SNAPSHOT.jar io.lettuce.core.benchmark.RedisClusterBenchmark 64 20  1000 32 32 > /dev/null 2>&1
java -cp lettuce-core-6.0.0.BUILD-SNAPSHOT.jar io.lettuce.core.benchmark.RedisClusterBenchmark 64 20  1000 32 64 > /dev/null 2>&1
java -cp lettuce-core-6.0.0.BUILD-SNAPSHOT.jar io.lettuce.core.benchmark.RedisClusterBenchmark 64 20  1000 32 128 > /dev/null 2>&1
java -cp lettuce-core-6.0.0.BUILD-SNAPSHOT.jar io.lettuce.core.benchmark.RedisClusterBenchmark 64 200 1000 32 32 > /dev/null 2>&1
java -cp lettuce-core-6.0.0.BUILD-SNAPSHOT.jar io.lettuce.core.benchmark.RedisClusterBenchmark 64 200 1000 32 64 > /dev/null 2>&1
java -cp lettuce-core-6.0.0.BUILD-SNAPSHOT.jar io.lettuce.core.benchmark.RedisClusterBenchmark 64 200 1000 32 128 > /dev/null 2>&1

java  -Xms10G -Xmx10G -XX:MetaspaceSize=800M -XX:+UseG1GC -cp lettuce-core-6.0.0.BUILD-SNAPSHOT.jar io.lettuce.core.benchmark.RedisClusterReactorBenchmark 64 20  1000 32 32 > /dev/null 2>&1
java  -cp lettuce-core-6.0.0.BUILD-SNAPSHOT.jar io.lettuce.core.benchmark.RedisClusterReactorBenchmark 64 20  1000 32 64 > /dev/null 2>&1
java -cp lettuce-core-6.0.0.BUILD-SNAPSHOT.jar io.lettuce.core.benchmark.RedisClusterReactorBenchmark 64 20  1000 32 128 > /dev/null 2>&1
java -cp lettuce-core-6.0.0.BUILD-SNAPSHOT.jar io.lettuce.core.benchmark.RedisClusterReactorBenchmark 64 200 1000 32 32 > /dev/null 2>&1
java -cp lettuce-core-6.0.0.BUILD-SNAPSHOT.jar io.lettuce.core.benchmark.RedisClusterReactorBenchmark 64 200 1000 32 64 > /dev/null 2>&1
java -cp lettuce-core-6.0.0.BUILD-SNAPSHOT.jar io.lettuce.core.benchmark.RedisClusterReactorBenchmark 64 200 1000 32 128 > /dev/null 2>&1
java -cp lettuce-core-6.0.0.BUILD-SNAPSHOT.jar io.lettuce.core.benchmark.RedisClusterReactorWithLimitBenchmark 64 20  1000 32 32 10000 > /dev/null 2>&1
java -cp lettuce-core-6.0.0.BUILD-SNAPSHOT.jar io.lettuce.core.benchmark.RedisClusterReactorWithLimitBenchmark 64 20  1000 32 32 100000 > /dev/null 2>&1
java -cp lettuce-core-6.0.0.BUILD-SNAPSHOT.jar io.lettuce.core.benchmark.RedisClusterReactorWithLimitBenchmark 64 20  1000 32 32 200000 > /dev/null 2>&1
java -cp lettuce-core-6.0.0.BUILD-SNAPSHOT.jar io.lettuce.core.benchmark.RedisClusterReactorWithLimitBenchmark 64 200 1000 32 32 10000 > /dev/null 2>&1
java -cp lettuce-core-6.0.0.BUILD-SNAPSHOT.jar io.lettuce.core.benchmark.RedisClusterReactorWithLimitBenchmark 64 200 1000 32 32 100000 > /dev/null 2>&1
java -cp lettuce-core-6.0.0.BUILD-SNAPSHOT.jar io.lettuce.core.benchmark.RedisClusterReactorWithLimitBenchmark 64 200 1000 32 32 200000 > /dev/null 2>&1