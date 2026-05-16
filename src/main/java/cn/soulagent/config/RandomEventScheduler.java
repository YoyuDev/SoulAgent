package cn.soulagent.config;

import cn.soulagent.service.RandomEventService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RandomEventScheduler {

    private static final Logger log = LoggerFactory.getLogger(RandomEventScheduler.class);

    private final RandomEventService randomEventService;

    @Scheduled(fixedRate = 3600000)
    public void checkAndGenerateEvents() {
        log.debug("定时任务：检查随机事件生成");
        randomEventService.checkAndGenerateEvents();
    }
}