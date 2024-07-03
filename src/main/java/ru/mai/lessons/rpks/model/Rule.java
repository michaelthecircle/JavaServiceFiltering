package ru.mai.lessons.rpks.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Rule {
    private Long filterId;
    private Long ruleId;
    private String fieldName;
    private String filterFunctionName;
    private String filterValue;
}
