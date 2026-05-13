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

    private static final double MAX_INTIMACY = 1000.0;
    private static final double MAX_TRUST = 1000.0;

    private static final double ACQUAINTANCE_THRESHOLD = 50;
    private static final double FRIEND_THRESHOLD = 200;
    private static final double CLOSE_FRIEND_THRESHOLD = 500;
    private static final double INTIMATE_THRESHOLD = 800;

    private static final long DECAY_START_HOURS = 24;
    private static final double DAILY_DECAY_RATE = 0.02;

    private final CharacterRelationshipMapper mapper;

    public void onChat(Long characterId, String userMessage, String aiReply) {
        CharacterRelationship rel = getOrCreate(characterId);

        long now = System.currentTimeMillis();
        if (rel.getFirstChatTime() == null || rel.getFirstChatTime() == 0) {
            rel.setFirstChatTime(now);
        }
        rel.setLastChatTime(now);

        long total = rel.getTotalMessages() != null ? rel.getTotalMessages() : 0;
        rel.setTotalMessages(total + 2);

        double intimacy = rel.getIntimacyScore() != null ? rel.getIntimacyScore() : 0.0;
        double decay = calculateDecay(rel.getLastChatTime(), now);
        intimacy = Math.max(0.0, intimacy - decay);
        intimacy = Math.min(MAX_INTIMACY, intimacy + calculateIntimacyGain(userMessage));
        rel.setIntimacyScore(intimacy);

        double trust = rel.getTrustScore() != null ? rel.getTrustScore() : 0.0;
        trust = Math.max(0.0, trust - decay);
        trust = Math.min(MAX_TRUST, trust + calculateTrustGain(userMessage));
        rel.setTrustScore(trust);

        rel.setRelationshipStage(determineStage(intimacy, trust));

        mapper.updateById(rel);
        log.debug("角色 {} 关系更新: 亲密度={:.1f}, 信任度={:.1f}, 阶段={}",
                characterId, intimacy, trust, rel.getRelationshipStage());
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

    private double calculateIntimacyGain(String userMessage) {
        double gain = 0.1;

        int msgLen = userMessage.length();
        if (msgLen > 50) gain += 0.15;
        if (msgLen > 100) gain += 0.25;
        if (msgLen > 200) gain += 0.4;

        String[] emotionalWords = {"爱你", "想你", "喜欢你", "在乎你", "想你", "担心你", "心疼你"};
        for (String word : emotionalWords) {
            if (userMessage.contains(word)) {
                gain += 0.3;
                break;
            }
        }

        return gain;
    }

    private double calculateTrustGain(String userMessage) {
        double gain = 0.05;

        String[] trustWords = {"秘密", "别告诉别人", "只有你知道", "心里话", "跟你说实话", "不瞒你说"};
        for (String word : trustWords) {
            if (userMessage.contains(word)) {
                gain += 1.5;
                break;
            }
        }

        String[] personalWords = {"我小时候", "我爸妈", "我对象", "我男朋友", "我女朋友", "我曾经", "我的经历"};
        for (String word : personalWords) {
            if (userMessage.contains(word)) {
                gain += 0.8;
                break;
            }
        }

        return gain;
    }

    private double calculateDecay(Long lastChatTime, long now) {
        if (lastChatTime == null || lastChatTime == 0) return 0.0;

        long hoursSinceLastChat = (now - lastChatTime) / (1000 * 60 * 60);
        if (hoursSinceLastChat < DECAY_START_HOURS) return 0.0;

        double daysInactive = hoursSinceLastChat / 24.0;
        return daysInactive * DAILY_DECAY_RATE * 100;
    }

    private void applyDecayIfNeeded(CharacterRelationship rel) {
        long now = System.currentTimeMillis();
        double decay = calculateDecay(rel.getLastChatTime(), now);
        if (decay > 0) {
            double intimacy = Math.max(0.0, rel.getIntimacyScore() - decay);
            double trust = Math.max(0.0, rel.getTrustScore() - decay);
            rel.setIntimacyScore(intimacy);
            rel.setTrustScore(trust);
            rel.setRelationshipStage(determineStage(intimacy, trust));
            mapper.updateById(rel);
        }
    }

    private String determineStage(double intimacy, double trust) {
        double combined = intimacy * 0.6 + trust * 0.4;
        if (combined >= INTIMATE_THRESHOLD) return "intimate";
        if (combined >= CLOSE_FRIEND_THRESHOLD) return "close_friend";
        if (combined >= FRIEND_THRESHOLD) return "friend";
        if (combined >= ACQUAINTANCE_THRESHOLD) return "acquaintance";
        return "stranger";
    }

    private String describeStage(String stage, Double intimacy) {
        return switch (stage) {
            case "stranger" -> "陌生人（亲密度：" + String.format("%.0f", intimacy) + "/1000）";
            case "acquaintance" -> "认识的人（亲密度：" + String.format("%.0f", intimacy) + "/1000）";
            case "friend" -> "朋友（亲密度：" + String.format("%.0f", intimacy) + "/1000）";
            case "close_friend" -> "好朋友（亲密度：" + String.format("%.0f", intimacy) + "/1000）";
            case "intimate" -> "亲密的人（亲密度：" + String.format("%.0f", intimacy) + "/1000）";
            default -> "陌生人";
        };
    }
}