/**
 * Component: RadarChartCard
 *
 * Displays skill scores in a radar chart using Recharts.
 * - Shows multiple skill categories
 * - Color-coded by category (Frontend, Backend, DevOps, etc.)
 * - Hover tooltip with exact scores
 * - Responsive design
 *
 * Usage:
 *   const skillScores = [
 *     { skillName: 'React', score: 85, category: 'Frontend' },
 *     { skillName: 'TypeScript', score: 90, category: 'Backend' },
 *   ];
 *   <RadarChartCard skillScores={skillScores} overallScore={87} />
 */

import React, { useMemo } from 'react';
import { Radar, RadarChart, PolarGrid, PolarAngleAxis, PolarRadiusAxis, Legend, Tooltip, ResponsiveContainer } from 'recharts';
import { SkillScore } from '../../types';

export interface RadarChartCardProps {
  skillScores: SkillScore[];
  overallScore: number;
}

const getCategoryColor = (category: string): string => {
  const colorMap: Record<string, string> = {
    Frontend: '#3b82f6', // blue
    Backend: '#10b981', // emerald
    DevOps: '#f59e0b', // amber
    Database: '#ec4899', // pink
    Mobile: '#8b5cf6', // purple
    DataScience: '#06b6d4', // cyan
  };
  return colorMap[category] || '#6b7280'; // gray fallback
};

export const RadarChartCard: React.FC<RadarChartCardProps> = ({ skillScores, overallScore }) => {
  /**
   * Prepare data for radar chart
   */
  const chartData = useMemo(() => {
    return skillScores.map((skill) => ({
      skill: skill.skillName,
      score: skill.score,
      category: skill.category,
      fullMark: 100,
    }));
  }, [skillScores]);

  /**
   * Group skills by category for legend
   */
  const categories = useMemo(() => {
    const unique = new Set(skillScores.map((s) => s.category));
    return Array.from(unique).sort();
  }, [skillScores]);

  if (skillScores.length === 0) {
    return (
      <div className="w-full bg-white rounded-lg shadow p-8">
        <h2 className="text-xl font-bold text-gray-900 mb-4">Skill Assessment</h2>
        <div className="text-center py-8 text-gray-600">
          <p>No skill scores available yet.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="w-full bg-white rounded-lg shadow p-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Skill Assessment</h2>
          <p className="text-sm text-gray-600 mt-1">Performance across {skillScores.length} skills</p>
        </div>
        <div className="text-center">
          <div className="text-4xl font-bold text-blue-600">{Math.round(overallScore)}</div>
          <div className="text-xs text-gray-600 mt-1">Overall Score</div>
        </div>
      </div>

      {/* Radar Chart */}
      <div className="w-full h-96">
        <ResponsiveContainer width="100%" height="100%">
          <RadarChart data={chartData} margin={{ top: 20, right: 20, bottom: 20, left: 20 }}>
            <PolarGrid stroke="#e5e7eb" />
            <PolarAngleAxis dataKey="skill" tick={{ fontSize: 12, fill: '#6b7280' }} />
            <PolarRadiusAxis angle={90} domain={[0, 100]} tick={{ fontSize: 12, fill: '#6b7280' }} />

            {/* Render separate radar for each category to color-code */}
            {categories.map((category) => {
              const categoryData = chartData.map((item) =>
                item.category === category
                  ? item
                  : { ...item, score: 0 } // Hide other categories in this radar
              );

              return (
                <Radar
                  key={category}
                  name={category}
                  dataKey="score"
                  data={categoryData as any}
                  stroke={getCategoryColor(category)}
                  fill={getCategoryColor(category)}
                  fillOpacity={0.3}
                  dot={false}
                  isAnimationActive={true}
                />
              );
            })}

            <Tooltip
              formatter={(value: any) => {
                if (typeof value === 'number') {
                  return `${value}/100`;
                }
                return value;
              }}
              contentStyle={{
                backgroundColor: '#ffffff',
                border: `1px solid #e5e7eb`,
                borderRadius: '0.5rem',
                padding: '0.5rem',
              }}
            />
            <Legend
              wrapperStyle={{ paddingTop: '20px' }}
              iconType="circle"
              formatter={(value: string) => <span className="text-sm">{value}</span>}
            />
          </RadarChart>
        </ResponsiveContainer>
      </div>

      {/* Skills List */}
      <div className="mt-6 pt-6 border-t border-gray-200">
        <h3 className="text-sm font-semibold text-gray-900 mb-3">Top Skills</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {skillScores
            .sort((a, b) => b.score - a.score)
            .slice(0, 6)
            .map((skill, idx) => (
              <div key={idx} className="flex items-center justify-between p-2 bg-gray-50 rounded">
                <div>
                  <p className="text-sm font-medium text-gray-900">{skill.skillName}</p>
                  <p className="text-xs text-gray-600">{skill.category}</p>
                </div>
                <div className="text-right">
                  <p className="text-lg font-bold text-blue-600">{skill.score}</p>
                  <div className="w-16 bg-gray-200 rounded-full h-1 mt-1">
                    <div
                      className="bg-blue-600 h-1 rounded-full"
                      style={{ width: `${skill.score}%` }}
                    />
                  </div>
                </div>
              </div>
            ))}
        </div>
      </div>
    </div>
  );
};
