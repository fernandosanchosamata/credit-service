package com.ntt.credit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

  @Bean
  @Primary
  public ReactiveRedisOperations<String, String> stringRedisOperations(
      ReactiveRedisConnectionFactory factory) {
    RedisSerializationContext<String, String> context =
        RedisSerializationContext.<String, String>newSerializationContext(
                new StringRedisSerializer())
            .key(new StringRedisSerializer())
            .value(new StringRedisSerializer())
            .hashKey(new StringRedisSerializer())
            .hashValue(new StringRedisSerializer())
            .build();

    return new ReactiveRedisTemplate<>(factory, context);
  }
}
