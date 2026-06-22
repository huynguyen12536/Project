export interface RadarSeriesDto {
  language: string;
  seriesList: RadarSeries[];
}

export interface RadarSeries {
  name: string;        // "Security", "Database", "Architecture", "Code Quality"
  value: number;       // 0-100
  axis: string;        // Same as name
  level: string;       // "EXCELLENT", "GOOD", "FAIR", "POOR"
  gapAnalysis: string; // Actionable text
}

export interface Assessment {
  id: string;
  userId: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  resultsData?: RadarSeriesDto;
  createdAt: string;
  completedAt?: string;
}

export interface AssessmentProgressEvent {
  assessmentId: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  progressPercent: number;
  currentStep: string;
  estimatedSecondsRemaining?: number;
  queuePosition?: number;
  confidence: number;
  timestamp: string;
}
