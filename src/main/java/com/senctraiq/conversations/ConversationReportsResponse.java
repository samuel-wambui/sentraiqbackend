package com.senctraiq.conversations;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversationReportsResponse {
    private long totalCases;
    private long positiveCases;
    private long negativeCases;
    private long neutralCases;
    private List<CategoryCaseReport> positiveByCategory;
    private List<CategoryCaseReport> negativeByCategory;
    private List<ChannelCaseReport> byChannel;
    private List<ChannelCategoryCaseReport> byChannelAndCategory;
    private List<HandledCaseReport> handledByUsernameCategoryChannel;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryCaseReport {
        private String category;
        private long totalCases;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChannelCaseReport {
        private String channel;
        private String label;
        private long totalCases;
        private long positiveCases;
        private long negativeCases;
        private long neutralCases;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChannelCategoryCaseReport {
        private String channel;
        private String label;
        private String category;
        private long totalCases;
        private long positiveCases;
        private long negativeCases;
        private long neutralCases;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HandledCaseReport {
        private String username;
        private String category;
        private String channel;
        private String label;
        private long totalCases;
    }
}
