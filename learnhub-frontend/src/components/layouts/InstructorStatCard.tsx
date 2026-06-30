import React from 'react';
import { TrendingUp } from 'lucide-react';
import { cn } from '../../lib/cn';

interface InstructorStatCardProps {
  label: string;
  value: string | number;
  note?: string;
  icon: React.ComponentType<{ className?: string }>;
  toneClassName?: string;
}

export const InstructorStatCard: React.FC<InstructorStatCardProps> = ({
  label,
  value,
  note,
  icon: Icon,
  toneClassName = 'bg-[#EEF2FF] text-lh-blue',
}) => {
  return (
    <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
      <div className="flex items-start justify-between gap-4">
        <div>
          <div className="text-sm font-medium text-[#6B7280]">{label}</div>
          <div className="mt-3 font-inter text-[30px] font-semibold tracking-[-0.03em] text-[#111827]">
            {value}
          </div>
        </div>
        <div className={cn('rounded-2xl p-3', toneClassName)}>
          <Icon className="h-5 w-5" />
        </div>
      </div>
      {note ? (
        <div className="mt-4 flex items-center gap-2 text-sm text-[#6B7280]">
          <TrendingUp className="h-4 w-4 text-[#1F7A45]" />
          {note}
        </div>
      ) : null}
    </div>
  );
};

export default InstructorStatCard;
