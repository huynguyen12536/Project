-- Create table for detailed assessment results
CREATE TABLE assessment_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id UUID NOT NULL UNIQUE,
    overall_level VARCHAR(50) NOT NULL,
    all_gaps JSONB NOT NULL DEFAULT '[]',
    next_steps JSONB NOT NULL DEFAULT '[]',
    overall_confidence DECIMAL(3,2) NOT NULL,
    results_json JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_assessment_results_assessment
        FOREIGN KEY (assessment_id)
        REFERENCES assessments(id) ON DELETE CASCADE
);

-- Create indexes for result queries
CREATE INDEX idx_assessment_results_assessment_id ON assessment_results(assessment_id);
CREATE INDEX idx_assessment_results_overall_level ON assessment_results(overall_level);
CREATE INDEX idx_assessment_results_created_at ON assessment_results(created_at DESC);
