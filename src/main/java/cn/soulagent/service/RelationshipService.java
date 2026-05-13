package cn.soulagent.service;

import cn.soulagent.entity.CharacterRelationship;
import cn.soulagent.mapper.CharacterRelationshipMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RelationshipService {

    private static final Logger log = LoggerFactory.getLogger(RelationshipService.class);

    private static final double MAX_RELATIONSHIP = 1000.0;

    private static final double ACQUAINTANCE_THRESHOLD = 50;
    private static final double FRIEND_THRESHOLD = 200;
    private static final double CLOSE_FRIEND_THRESHOLD = 500;
    private static final double INTIMATE_THRESHOLD = 800;

    private static final long DECAY_START_HOURS = 24;
    private static final double DAILY_DECAY_RATE = 0.03;

    private final CharacterRelationshipMapper mapper;

    public void onChat(Long characterId, String userMessage) {
        CharacterRelationship rel = getOrCreate(characterId);

        long now = System.currentTimeMillis();
        if (rel.getFirstChatTime() == null || rel.getFirstChatTime() == 0) {
            rel.setFirstChatTime(now);
        }

        double score = rel.getIntimacyScore() != null ? rel.getIntimacyScore() : 0.0;
        double decay = calculateDecay(rel.getLastChatTime());
        score = Math.max(0.0, score - decay);
        score = Math.min(MAX_RELATIONSHIP, score + calculateGain(userMessage));

        rel.setLastChatTime(now);
        rel.setTotalMessages((rel.getTotalMessages() != null ? rel.getTotalMessages() : 0) + 2);
        rel.setIntimacyScore(score);
        rel.setTrustScore(score);
        rel.setRelationshipStage(determineStage(score));

        mapper.updateById(rel);
        log.debug("角色 {} 关系更新: 分数={:.1f}, 阶段={}, 衰减={:.1f}",
                characterId, score, rel.getRelationshipStage(), decay);
    }

    public CharacterRelationship get(Long characterId) {
        return getOrCreate(characterId);
    }

    public String getStageDescription(Long characterId) {
        CharacterRelationship rel = getOrCreate(characterId);
        applyDecayIfNeeded(rel);
        return describeStage(rel.getRelationshipStage(), rel.getIntimacyScore());
    }

    private CharacterRelationship getOrCreate(Long characterId) {
        CharacterRelationship rel = mapper.selectOne(
                new QueryWrapper<CharacterRelationship>().eq("character_id", characterId)
        );
        if (rel == null) {
            rel = new CharacterRelationship();
            rel.setCharacterId(characterId);
            rel.setIntimacyScore(0.0);
            rel.setTrustScore(0.0);
            rel.setFirstChatTime(0L);
            rel.setLastChatTime(0L);
            rel.setTotalMessages(0L);
            rel.setRelationshipStage("stranger");
            mapper.insert(rel);
        }
        return rel;
    }

    private double calculateGain(String userMessage) {
        double gain = 0.1;

        int msgLen = userMessage.length();
        if (msgLen > 50) gain += 0.15;
        if (msgLen > 100) gain += 0.25;
        if (msgLen > 200) gain += 0.4;

        String[] emotionalWords = {"爱你", "想你", "喜欢你", "在乎你", "担心你", "心疼你", "离不开你", "你真好", "谢谢你", "感谢你"};
        for (String word : emotionalWords) {
            if (userMessage.contains(word)) {
                gain += 0.3;
                break;
            }
        }

        String[] trustWords = {"秘密", "别告诉别人", "只有你知道", "心里话", "跟你说实话", "不瞒你说", "我只对你说"};
        for (String word : trustWords) {
            if (userMessage.contains(word)) {
                gain += 1.5;
                break;
            }
        }

        String[] personalWords = {"我小时候", "我爸妈", "我对象", "我男朋友", "我女朋友", "我曾经", "我的经历", "我的梦想", "我最怕", "我最讨厌"};
        for (String word : personalWords) {
            if (userMessage.contains(word)) {
                gain += 0.8;
                break;
            }
        }

        return gain;
    }

    private double calculateDecay(Long lastChatTime) {
        if (lastChatTime == null || lastChatTime == 0) return 0.0;

        long now = System.currentTimeMillis();
        long hoursSinceLastChat = (now - lastChatTime) / (1000 * 60 * 60);
        if (hoursSinceLastChat < DECAY_START_HOURS) return 0.0;

        double daysInactive = hoursSinceLastChat / 24.0;
        double decayRate = daysInactive <= 7 ? DAILY_DECAY_RATE * 2 : DAILY_DECAY_RATE;
        return daysInactive * decayRate * 100;
    }

    private void applyDecayIfNeeded(CharacterRelationship rel) {
        double decay = calculateDecay(rel.getLastChatTime());
        if (decay > 0) {
            double score = Math.max(0.0, rel.getIntimacyScore() - decay);
            rel.setIntimacyScore(score);
            rel.setTrustScore(score);
            rel.setRelationshipStage(determineStage(score));
            mapper.updateById(rel);
        }
    }

    private String determineStage(double score) {
        if (score >= INTIMATE_THRESHOLD) return "intimate";
        if (score >= CLOSE_FRIEND_THRESHOLD) return "close_friend";
        if (score >= FRIEND_THRESHOLD) return "friend";
        if (score >= ACQUAINTANCE_THRESHOLD) return "acquaintance";
        return "stranger";
    }

    private String describeStage(String stage, Double score) {
        return switch (stage) {
            case "stranger" -> "新认识（亲密度：" + String.format("%.0f", score) + "/1000）";
            case "acquaintance" -> "认识的人（亲密度：" + String.format("%.0f", score) + "/1000）";
            case "friend" -> "朋友（亲密度：" + String.format("%.0f", score) + "/1000）";
            case "close_friend" -> "好朋友（亲密度：" + String.format("%.0f", score) + "/1000）";
            case "intimate" -> "亲密的人（亲密度：" + String.format("%.0f", score) + "/1000）";
            default -> "新认识";
        };
    }
}