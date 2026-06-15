package com.senctraiq.conversations;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class ConversationChannelSummary {
    private String channel;
    private String label;
    private long total;
    private long unassigned;
    private Map<String, Long> statuses;
}
