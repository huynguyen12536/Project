import React, { useState } from 'react';
import {
  RadarChart,
  PolarGrid,
  PolarAngleAxis,
  PolarRadiusAxis,
  Radar,
  Legend,
  Tooltip,
  ResponsiveContainer
} from 'recharts';
import { RadarSeriesDto } from '@/types/assessment';
import './RadarChartCard.css';

interface RadarChartCardProps {
  data: RadarSeriesDto;
  onAxisSelect?: (axis: string) => void;
}

export const RadarChartCard: React.FC<RadarChartCardProps> = ({
  data,
  onAxisSelect
}) => {
  const [selectedAxis, setSelectedAxis] = useState<string | undefined>(undefined);

  const chartData = data.seriesList.map(series => ({
    name: series.name,
    value: series.value,
    fullMark: 100
  }));

  const handleAxisClick = (axis: string) => {
    setSelectedAxis(axis);
    onAxisSelect?.(axis);
  };

  return (
    <div className="radar-card">
      <header className="radar-header">
        <h3>📊 Competency Radar</h3>
        <p className="radar-subtitle">Language: {data.language}</p>
      </header>

      <div className="radar-chart-container">
        <ResponsiveContainer width="100%" height={400}>
          <RadarChart data={chartData}>
            <PolarGrid stroke="#ddd" />
            <PolarAngleAxis
              dataKey="name"
              tick={{ fill: '#666', fontSize: 12 }}
            />
            <PolarRadiusAxis
              angle={90}
              domain={[0, 100]}
              tick={{ fill: '#999', fontSize: 10 }}
            />
            <Radar
              name="Score"
              dataKey="value"
              stroke="#3b82f6"
              fill="#3b82f6"
              fillOpacity={0.6}
            />
            <Tooltip
              contentStyle={{
                background: '#fff',
                border: '1px solid #ccc',
                borderRadius: '4px',
                padding: '8px'
              }}
              formatter={(value) => `${value}/100`}
            />
          </RadarChart>
        </ResponsiveContainer>
      </div>

      <div className="axis-buttons">
        {data.seriesList.map(series => (
          <button
            key={series.name}
            className={`axis-btn ${selectedAxis === series.name ? 'active' : ''}`}
            onClick={() => handleAxisClick(series.name)}
            title={`${series.name}: ${series.value}/100 (${series.level})`}
          >
            <span className="axis-name">{series.name}</span>
            <span className={`axis-score level-${series.level.toLowerCase()}`}>
              {series.value}
            </span>
          </button>
        ))}
      </div>

      <footer className="radar-footer">
        <p className="footer-text">Click on an axis to view detailed improvement suggestions</p>
      </footer>
    </div>
  );
};
