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

    private static final int STRANGER_THRESHOLD = 0;
    private static final int ACQUAINTANCE_THRESHOLD = 5;
    private static final int FRIEND_THRESHOLD = 20;
    private static final int CLOSE_FRIEND_THRESHOLD = 50;
    private static final int INTIMATE_THRESHOLD = 100;

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
        intimacy = Math.min(100.0, intimacy + calculateIntimacyGain(userMessage, aiReply));
        rel.setIntimacyScore(intimacy);

        double trust = rel.getTrustScore() != null ? rel.getTrustScore() : 0.0;
        trust = Math.min(100.0, trust + calculateTrustGain(userMessage, aiReply));
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

    private double calculateIntimacyGain(String userMessage, String aiReply) {
        double gain = 0.5;

        int msgLen = userMessage.length();
        if (msgLen > 20) gain += 0.3;
        if (msgLen > 50) gain += 0.5;
        if (msgLen > 100) gain += 1.0;

        String[] emotionalWords = {"开心", "难过", "喜欢", "讨厌", "谢谢", "对不起", "爱你", "想你", "伤心", "高兴", "累", "烦"};
        for (String word : emotionalWords) {
            if (userMessage.contains(word)) {
                gain += 0.5;
                break;
            }
        }

        if (aiReply.length() > 30) {
            gain += 0.2;
        }

        return gain;
    }

    private double calculateTrustGain(String userMessage, String aiReply) {
        double gain = 0.3;

        String[] trustWords = {"秘密", "信任", "告诉你", "只有你知道", "别告诉别人", "心里话", "坦白", "其实"};
        for (String word : trustWords) {
            if (userMessage.contains(word)) {
                gain += 2.0;
                break;
            }
        }

        String[] personalWords = {"我家", "我妈妈", "我爸爸", "我对象", "我朋友", "小时候", "以前", "曾经"};
        for (String word : personalWords) {
            if (userMessage.contains(word)) {
                gain += 1.0;
                break;
            }
        }

        return gain;
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
            case "stranger" -> "陌生人（亲密度：" + String.format("%.0f", intimacy) + "%）";
            case "acquaintance" -> "认识的人（亲密度：" + String.format("%.0f", intimacy) + "%）";
            case "friend" -> "朋友（亲密度：" + String.format("%.0f", intimacy) + "%）";
            case "close_friend" -> "好朋友（亲密度：" + String.format("%.0f", intimacy) + "%）";
            case "intimate" -> "亲密的人（亲密度：" + String.format("%.0f", intimacy) + "%）";
            default -> "陌生人";
        };
    }
}