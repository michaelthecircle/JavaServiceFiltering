package ru.mai.lessons.rpks;

import ru.mai.lessons.rpks.model.Message;

import java.io.Closeable;

public interface KafkaWriter extends Closeable {
    public void processing(Message message);
}
