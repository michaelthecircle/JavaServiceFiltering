package ru.mai.lessons.rpks.impl;

import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import ru.mai.lessons.rpks.*;

@Slf4j
public class ServiceFiltering implements Service {
    @Override
    public void start(Config config) {
        KafkaReader kafkaReader = new KafkaReaderImpl(config);
        kafkaReader.processing();
    }
}
