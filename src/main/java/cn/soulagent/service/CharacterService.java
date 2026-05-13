package cn.soulagent.service;

import cn.soulagent.entity.SoulCharacter;
import cn.soulagent.entity.Personality;
import cn.soulagent.mapper.CharacterMapper;
import cn.soulagent.mapper.PersonalityMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.function.BiConsumer;

@Service
public class CharacterService {

    @Autowired
    private CharacterMapper characterMapper;

    @Autowired
    private PersonalityMapper personalityMapper;

    @Autowired
    private PersonalityService personalityService;

    @Autowired
    private MemoryService memoryService;

    public Long create(String name, String description, String chatData, MultipartFile avatar,
                        String apiKey, String apiUrl, String modelName,
                        String embeddingApiKey, String embeddingApiUrl, String embeddingModelName,
                        BiConsumer<String, Integer> progress) {

        progress.accept("正在创建角色...", 10);

        SoulCharacter c = new SoulCharacter();
        c.setName(name);
        c.setDescription(description);

        // 处理头像
        if (avatar != null && !avatar.isEmpty()) {
            try {
                String base64 = Base64.getEncoder().encodeToString(avatar.getBytes());
                String mime = avatar.getContentType() != null ? avatar.getContentType() : "image/png";
                c.setAvatar("data:" + mime + ";base64," + base64);
            } catch (Exception e) {
                // 头像处理失败不影响创建
            }
        }

        characterMapper.insert(c);

        progress.accept("正在导入聊天记录...", 25);

        List<String> msgs = parseChat(chatData);

        progress.accept("正在解析角色性格...", 40);

        Personality p = personalityService.generate(msgs, apiKey, apiUrl, modelName);
        p.setCharacterId(c.getId());
        personalityService.save(p);

        progress.accept("正在存储记忆...", 75);

        // 记忆存储容错，Qdrant 未启动不影响创建
        try {
            memoryService.store(c.getId(), msgs, apiKey, apiUrl, embeddingApiKey, embeddingApiUrl, embeddingModelName);
        } catch (Exception e) {
            System.err.println("记忆存储失败（Qdrant 可能未启动）: " + e.getMessage());
        }

        progress.accept("创建完成", 100);

        return c.getId();
    }

    public SoulCharacter getById(Long id) {
        return characterMapper.selectById(id);
    }

    public List<SoulCharacter> list() {
        return characterMapper.selectList(null);
    }

    public void delete(Long id) {
        characterMapper.deleteById(id);
        personalityMapper.delete(new QueryWrapper<cn.soulagent.entity.Personality>().eq("character_id", id));
    }

    private List<String> parseChat(String raw) {
        return Arrays.stream(raw.split("\n"))
                .filter(s -> s.contains(":"))
                .map(s -> s.split(":", 2)[1].trim())
                .toList();
    }
}
