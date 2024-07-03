package ru.mai.lessons.rpks.impl;

import lombok.extern.slf4j.Slf4j;
import ru.mai.lessons.rpks.RuleProcessor;
import ru.mai.lessons.rpks.model.Message;
import ru.mai.lessons.rpks.model.Rule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.function.BiPredicate;

@Slf4j
public class RuleProcessorImpl implements RuleProcessor {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Message processing(Message message, Rule[] rules) {
        if (isInvalidInput(message, rules)) {
            message.setFilterState(false);
            return message;
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(message.getValue());

            for (Rule rule : rules) {
                if (!applyRule(jsonNode, rule)) {
                    message.setFilterState(false);
                    return message;
                }
            }

            message.setFilterState(true);
        } catch (IOException ex) {
            log.error("Exception while reading json message: {}", ex.getMessage(), ex);
        }

        return message;
    }

    private boolean isInvalidInput(Message message, Rule[] rules) {
        return rules == null || rules.length == 0 || message.getValue() == null || message.getValue().isEmpty();
    }

    private boolean applyRule(JsonNode jsonNode, Rule rule) {
        JsonNode field = jsonNode.get(rule.getFieldName());
        if (field == null) {
            return false;
        }

        return applyFilter(field.asText(), rule.getFilterFunctionName(), rule.getFilterValue());
    }

    private boolean applyFilter(String fieldValue, String filterFunctionName, String filterValue) {
        BiPredicate<String, String> filterFunction = getFilterFunction(filterFunctionName.toLowerCase());
        return filterFunction != null && filterFunction.test(fieldValue, filterValue);
    }

    private BiPredicate<String, String> getFilterFunction(String filterFunctionName) {
        return switch (filterFunctionName) {
            case "equals" -> String::equals;
            case "contains" -> String::contains;
            case "not_equals" -> (fieldValue, filterValue) -> !fieldValue.equals(filterValue);
            case "not_contains" -> (fieldValue, filterValue) -> !fieldValue.contains(filterValue);
            default -> null;
        };
    }
}
