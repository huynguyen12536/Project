import React from 'react';
import { motion } from 'framer-motion';
import { useLocation, useNavigate } from 'react-router-dom';
import { cn } from '../../lib/cn';
import { instructorStudioNavItems } from './instructorStudioNavigation';

export const InstructorSidebar: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();

  return (
    <motion.aside
      initial={{ opacity: 0, x: -20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.22, ease: 'easeOut' }}
      className="w-full lg:fixed lg:left-0 lg:top-[73px] lg:h-[calc(100vh-73px)] lg:w-[272px] lg:border-r lg:border-[#E5E7EB] lg:bg-[#F9FAFB]"
    >
      <div className="h-full px-4 py-5 lg:px-5 lg:py-6">
        <div className="rounded-2xl border border-[#E5E7EB] bg-white p-3 shadow-sm lg:border-none lg:bg-transparent lg:p-0 lg:shadow-none">
          <div className="mb-4 px-3">
            <div className="text-sm font-semibold text-[#111827]">Instructor Studio</div>
            <p className="mt-1 text-sm leading-6 text-[#6B7280]">
              Quan ly noi dung, hoc vien va dong tien trong cung mot workspace.
            </p>
          </div>

          <nav className="space-y-1.5">
            {instructorStudioNavItems.map((item) => {
              const Icon = item.icon;
              const active = location.pathname === item.path;

              return (
                <button
                  key={item.path}
                  onClick={() => navigate(item.path)}
                  className={cn(
                    'flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm font-medium transition',
                    active
                      ? 'bg-[#EEF2FF] text-lh-blue'
                      : 'text-[#4B5563] hover:bg-white hover:text-[#111827] lg:hover:bg-white'
                  )}
                >
                  <div
                    className={cn(
                      'flex h-9 w-9 items-center justify-center rounded-xl border transition',
                      active ? 'border-[#D9DEF2] bg-white text-lh-blue' : 'border-transparent bg-[#F3F4F6] text-[#6B7280]'
                    )}
                  >
                    <Icon className="h-4.5 w-4.5" />
                  </div>
                  <span className="truncate">{item.label}</span>
                </button>
              );
            })}
          </nav>
        </div>
      </div>
    </motion.aside>
  );
};

export default InstructorSidebar;
