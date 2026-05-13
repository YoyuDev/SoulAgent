package cn.soulagent.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redis;

    public StringRedisTemplate getRedisTemplate() {
        return redis;
    }

    public void append(Long cid, String user, String ai) {
        String key = "chat:" + cid;
        redis.opsForList().rightPush(key, "用户: " + user);
        redis.opsForList().rightPush(key, "AI: " + ai);
        redis.opsForList().trim(key, -50, -1);
    }

    public List<String> get(Long cid) {
        return redis.opsForList().range("chat:" + cid, 0, -1);
    }

    public void clear(Long cid) {
        redis.delete("chat:" + cid);
    }
}