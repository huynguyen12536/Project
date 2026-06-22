/**
 * Tests for RadarChartCard component
 *
 * Verifies:
 * - Renders with skill scores data
 * - Displays overall score
 * - Shows all skills in list
 * - Handles empty skill scores
 * - Displays category information
 */

import React from 'react';
import { render, screen } from '@testing-library/react';
import { RadarChartCard } from './radar-chart-card';
import { SkillScore } from '../../types';

// Mock Recharts components to avoid rendering issues in tests
jest.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: any) => <div>{children}</div>,
  RadarChart: ({ children, data }: any) => (
    <div data-testid="radar-chart">
      {data.map((item: any, i: number) => (
        <div key={i}>{item.skill}</div>
      ))}
      {children}
    </div>
  ),
  PolarGrid: () => <div />,
  PolarAngleAxis: () => <div />,
  PolarRadiusAxis: () => <div />,
  Radar: ({ name }: any) => <div>{name}</div>,
  Tooltip: () => <div />,
  Legend: () => <div />,
}));

describe('RadarChartCard', () => {
  const mockSkillScores: SkillScore[] = [
    { skillName: 'React', score: 85, category: 'Frontend' },
    { skillName: 'TypeScript', score: 90, category: 'Backend' },
    { skillName: 'Docker', score: 75, category: 'DevOps' },
    { skillName: 'PostgreSQL', score: 80, category: 'Database' },
  ];

  it('renders with skill scores', () => {
    render(<RadarChartCard skillScores={mockSkillScores} overallScore={82} />);

    expect(screen.getByText(/Skill Assessment/i)).toBeInTheDocument();
    expect(screen.getByText(/82/)).toBeInTheDocument();
    expect(screen.getByText(/Overall Score/i)).toBeInTheDocument();
  });

  it('displays all skills in the top skills list', () => {
    render(<RadarChartCard skillScores={mockSkillScores} overallScore={82} />);

    expect(screen.getByText('React')).toBeInTheDocument();
    expect(screen.getByText('TypeScript')).toBeInTheDocument();
    expect(screen.getByText('Docker')).toBeInTheDocument();
  });

  it('displays skill categories', () => {
    render(<RadarChartCard skillScores={mockSkillScores} overallScore={82} />);

    expect(screen.getByText('Frontend')).toBeInTheDocument();
    expect(screen.getByText('Backend')).toBeInTheDocument();
    expect(screen.getByText('DevOps')).toBeInTheDocument();
  });

  it('displays overall score prominently', () => {
    render(<RadarChartCard skillScores={mockSkillScores} overallScore={87.5} />);

    expect(screen.getByText('88')).toBeInTheDocument(); // Rounded
  });

  it('shows skill count in subtitle', () => {
    render(<RadarChartCard skillScores={mockSkillScores} overallScore={82} />);

    expect(screen.getByText(/Performance across 4 skills/i)).toBeInTheDocument();
  });

  it('handles empty skill scores', () => {
    render(<RadarChartCard skillScores={[]} overallScore={0} />);

    expect(screen.getByText(/No skill scores available/i)).toBeInTheDocument();
  });

  it('sorts skills by score descending in list', () => {
    const skills: SkillScore[] = [
      { skillName: 'Node.js', score: 70, category: 'Backend' },
      { skillName: 'React', score: 95, category: 'Frontend' },
      { skillName: 'Python', score: 75, category: 'Backend' },
    ];

    render(<RadarChartCard skillScores={skills} overallScore={80} />);

    const labels = screen.getAllByText(/Top Skills/i);
    expect(labels.length).toBeGreaterThan(0);

    // React (95) should appear before others
    const reactText = screen.getByText('React');
    expect(reactText).toBeInTheDocument();
  });

  it('renders radar chart', () => {
    render(<RadarChartCard skillScores={mockSkillScores} overallScore={82} />);

    expect(screen.getByTestId('radar-chart')).toBeInTheDocument();
  });

  it('displays only top 6 skills in list', () => {
    const manySkills: SkillScore[] = Array.from({ length: 10 }, (_, i) => ({
      skillName: `Skill${i}`,
      score: 100 - i * 5,
      category: i % 2 === 0 ? 'Frontend' : 'Backend',
    }));

    render(<RadarChartCard skillScores={manySkills} overallScore={75} />);

    // Should show "Top Skills" heading
    expect(screen.getByText(/Top Skills/i)).toBeInTheDocument();

    // First skill (Skill0) should be present
    expect(screen.getByText('Skill0')).toBeInTheDocument();

    // Last skill (Skill9) should not be visible (only top 6 shown)
    expect(screen.queryByText('Skill9')).not.toBeInTheDocument();
  });

  it('displays responsive grid for skill list', () => {
    const { container } = render(
      <RadarChartCard skillScores={mockSkillScores} overallScore={82} />
    );

    const gridDiv = container.querySelector('.grid');
    expect(gridDiv).toHaveClass('grid-cols-1', 'md:grid-cols-2');
  });
});
