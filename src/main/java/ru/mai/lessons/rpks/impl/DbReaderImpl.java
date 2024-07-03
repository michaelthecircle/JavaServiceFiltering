package ru.mai.lessons.rpks.impl;

import com.typesafe.config.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import ru.mai.lessons.rpks.DbReader;
import ru.mai.lessons.rpks.model.Rule;

@Slf4j
public class DbReaderImpl implements DbReader {
    private final HikariDataSource dataSource;

    public DbReaderImpl(Config config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getString("db.jdbcUrl"));
        hikariConfig.setUsername(config.getString("db.user"));
        hikariConfig.setPassword(config.getString("db.password"));
        hikariConfig.setDriverClassName(config.getString("db.driver"));
        this.dataSource = new HikariDataSource(hikariConfig);
    }
    @Override
    public Rule[] readRulesFromDB() {
        try (var connection = dataSource.getConnection()) {
            DSLContext dslContext = DSL.using(connection, SQLDialect.POSTGRES);
            var result = dslContext.select().from("filter_rules").fetch();
            Rule[] rules = new Rule[result.size()];

            for (int index = 0; index < result.size(); index++) {
                rules[index] = Rule.builder()
                        .filterId(result.get(index).getValue("filter_id", Long.class))
                        .ruleId(result.get(index).getValue("rule_id", Long.class))
                        .fieldName(result.get(index).getValue("field_name", String.class))
                        .filterFunctionName(result.get(index).getValue("filter_function_name", String.class))
                        .filterValue(result.get(index).getValue("filter_value", String.class))
                        .build();
            }
            log.info("Rules from " + dataSource.getJdbcUrl() + " were read successfully");
            return rules;
        } catch (Exception e) {
            log.error("Error reading rules from database", e);
            return new Rule[0];
        }
    }
}
