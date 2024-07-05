package ru.mai.lessons.rpks.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class Message {
    private String value;
    private boolean filterState;
}
