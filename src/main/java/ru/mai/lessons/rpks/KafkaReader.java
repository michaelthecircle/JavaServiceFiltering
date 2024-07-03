package ru.mai.lessons.rpks;

import java.io.Closeable;

public interface KafkaReader extends Closeable {
    public void processing();
}
