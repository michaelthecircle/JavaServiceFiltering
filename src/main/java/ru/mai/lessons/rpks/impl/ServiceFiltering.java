package ru.mai.lessons.rpks.impl;

import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import ru.mai.lessons.rpks.*;

@Slf4j
public class ServiceFiltering implements Service {
    @Override
    public void start(Config config) {
        RuleProcessor ruleProcessor = new RuleProcessorImpl();
        KafkaWriter kafkaWriter = new KafkaWriterImpl(config);
        try (KafkaReader kafkaReader = new KafkaReaderImpl(config, kafkaWriter, ruleProcessor)) {
            kafkaReader.processing();
        } catch (Exception e) {
            log.error("Error while filtering messages", e);
        }
    }
}
