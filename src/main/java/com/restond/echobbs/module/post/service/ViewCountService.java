package com.restond.echobbs.module.post.service;

import com.restond.echobbs.module.post.repository.PostRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 浏览量统计服务。
 * <p>
 * 采用「内存累加 + 定时批量刷库」策略优化高频浏览量写入：
 * <ol>
 *   <li>{@link #recordView} 将浏览增量暂存到内存 {@link ConcurrentHashMap}</li>
 *   <li>{@link #flushViewCounts} 定时（默认 3 分钟）将增量批量更新到数据库</li>
 *   <li>{@link #shutdown} 应用关闭时兜底刷新，防止数据丢失</li>
 * </ol>
 */
@Service
public class ViewCountService {
    private static final Logger log = LoggerFactory.getLogger(ViewCountService.class);

    private final JdbcTemplate jdbcTemplate;

    private final AtomicReference<ConcurrentHashMap<Long, Integer>> viewIncrements = new AtomicReference<>(new ConcurrentHashMap<>());

    public ViewCountService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void recordView(Long postId) {
        viewIncrements.get().merge(postId, 1, Integer::sum);
    }

    @Scheduled(fixedRateString = "${app.view-count.flush-interval:180000}")
    @Transactional
    public void flushViewCounts() {
        ConcurrentHashMap<Long, Integer> oldMap = viewIncrements.getAndSet(new ConcurrentHashMap<>());

        if (oldMap.isEmpty()) {
            log.debug("浏览量增量为空，跳过刷新");
            return;
        }

        log.info("开始批量更新浏览量，共 {} 个帖子", oldMap.size());

        int updatedCount = 0;
        for (Map.Entry<Long, Integer> entry : oldMap.entrySet()) {
            Long postId = entry.getKey();
            Integer increment = entry.getValue();

            try {
                int rows = jdbcTemplate.update(
                        "UPDATE post SET view_count = view_count + ? WHERE id = ?",
                        increment, postId
                );
                if (rows > 0) {
                    updatedCount++;
                }
            } catch (Exception e) {
                log.error("更新帖子 {} 浏览量失败: {}", postId, e.getMessage());
            }
        }

        log.info("浏览量更新完成，成功更新 {} 个帖子", updatedCount);
    }

    @PreDestroy
    public void shutdown() {
        log.info("应用关闭，执行浏览量兜底刷新");
        flushViewCounts();
    }
}
