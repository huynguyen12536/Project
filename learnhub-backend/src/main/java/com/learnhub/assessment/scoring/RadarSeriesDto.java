package com.learnhub.assessment.scoring;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RadarSeriesDto {
    private String language;
    private List<RadarSeries> seriesList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RadarSeries {
        private String name;           // e.g., "Security"
        private Integer value;         // 0-100
        private String axis;           // e.g., "Security"
        private String level;          // EXCELLENT, GOOD, FAIR, POOR
        private String gapAnalysis;    // Actionable text
    }
}
