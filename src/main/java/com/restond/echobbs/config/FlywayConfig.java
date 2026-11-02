package com.restond.echobbs.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

/**
 * Flyway 数据库迁移配置。
 * <p>
 * 应用启动时自动执行 {@code classpath:db/migration} 下的 SQL 迁移脚本，
 * 确保数据库表结构与代码版本一致。{@code baselineOnMigrate} 允许在已有数据库上启用迁移。
 */
@Configuration
public class FlywayConfig {
    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Bean
    public Flyway flyway(DataSource dataSource) {
        log.info("Configuring Flyway manually...");

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

        flyway.migrate();

        log.info("Flyway migration completed. Current version: {}", flyway.info().current().getVersion());
        return flyway;
    }
}
