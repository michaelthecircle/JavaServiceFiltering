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
    private volatile boolean isRunning;

    @Setter
    private Rule[] rules;

    public KafkaReaderImpl(Config config, KafkaWriter kafkaWriter, RuleProcessor ruleProcessor) {
        this.config = config;
        this.kafkaWriter = kafkaWriter;
        this.ruleProcessor = ruleProcessor;
        this.kafkaConsumer = new KafkaConsumer<>(
                kafkaConsumerConfig(),
                new StringDeserializer(),
                new StringDeserializer()
        );
        this.kafkaConsumer.subscribe(Collections.singletonList(config.getString("kafka.consumer.topic")));
        this.isRunning = true;
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
        try {
            while (isRunning) {
                ConsumerRecords<String, String> consumerRecords = kafkaConsumer.poll(Duration.ofMillis(1000));
                if (!consumerRecords.isEmpty()) {
                    for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                        log.debug("Received record: {}", consumerRecord.value());
                        processMessage(consumerRecord.value());
                        log.debug("Message {} processed", consumerRecord.value());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error during message processing", e);
        } finally {
            closeConsumer();
        }
    }
    private void processMessage(String value) {
        Message message = ruleProcessor.processing(
                Message.builder()
                        .value(value)
                        .filterState(false)
                        .build(),
                rules
        );
        if (message.isFilterState()) {
            kafkaWriter.processing(message);
        }
    }
    private void closeConsumer() {
        try {
            kafkaConsumer.close();
            log.info("Kafka consumer closed");
        } catch (Exception e) {
            log.error("Error closing Kafka consumer", e);
        }
    }
    @Override
    public void close() {
        log.info("Shutdown Kafka Reader initiated");
        isRunning = false;
        kafkaConsumer.wakeup();
    }
}
