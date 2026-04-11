package com.project.nagarSetu.service.redis;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@AllArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    public <T> T get(String key, Class<T> responseClass) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                log.debug("Redis service: key {} not found", key);
                return null;
            }
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(value, responseClass);
        } catch (Exception e) {
            log.error("Redis service get failed for key {}: {}", key, e.getMessage(), e);
            return null;
        }
    }

    public void set(String key, Object o, long Ttl) {
        try {
            redisTemplate.opsForValue().set(key, o.toString(), Ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis service set failed for key {}: {}", key, e.getMessage(), e);
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Redis service delete failed for key {}: {}", key, e.getMessage(), e);
        }
    }

}