package ru.mai.lessons.rpks.impl;

import com.typesafe.config.Config;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import ru.mai.lessons.rpks.KafkaReader;
import ru.mai.lessons.rpks.KafkaWriter;
import ru.mai.lessons.rpks.RuleProcessor;
import ru.mai.lessons.rpks.model.Message;
import ru.mai.lessons.rpks.model.Rule;

import java.util.concurrent.*;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class KafkaReaderImpl implements KafkaReader {
    private final Config config;
    private final KafkaWriter kafkaWriter;
    private final RuleProcessor ruleProcessor;
    private final KafkaConsumer<String, String> kafkaConsumer;
    private final DbReaderImpl dbReader;

    @Setter
    private Rule[] rules;

    public KafkaReaderImpl(Config config) {
        this.config = config;
        this.dbReader = new DbReaderImpl(config);
        this.kafkaConsumer = new KafkaConsumer<>(
                kafkaConsumerConfig(),
                new StringDeserializer(),
                new StringDeserializer()
        );
        this.kafkaWriter = new KafkaWriterImpl(config);
        this.ruleProcessor = new RuleProcessorImpl();
        this.kafkaConsumer.subscribe(Collections.singletonList(config.getString("kafka.consumer.topic")));
        log.info("Created Kafka Consumer");
    }
    private Map<String, Object> kafkaConsumerConfig() {
        return config.getConfig("kafka.consumer")
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().unwrapped()));
    }

    @Override
    public void processing() {
        ScheduledExecutorService ruleUpdater = Executors.newSingleThreadScheduledExecutor();
        ruleUpdater.scheduleAtFixedRate(this::updateRules,
                0, config.getLong("application.updateIntervalSec") * 1000L, TimeUnit.MILLISECONDS);
        boolean isRun = true;
        try {
            while (isRun) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, String> consumerRecord : records) {
                    String recordMessage = consumerRecord.value();
                    Message processedMessage = ruleProcessor.processing(new Message(recordMessage, false), rules);
                    if (processedMessage.isFilterState()){
                        kafkaWriter.processing(processedMessage);
                    }
                }
            }
        } catch (Exception e) {
            log.error("error", e);
        } finally {
            ruleUpdater.shutdown();
            try {
                if (!ruleUpdater.isTerminated()) {
                    ruleUpdater.shutdownNow();
                }
            } catch (Exception e) {
                log.error("error", e);
            }
        }
    }
    private void updateRules() {
        rules = dbReader.readRulesFromDB();
    }
}
