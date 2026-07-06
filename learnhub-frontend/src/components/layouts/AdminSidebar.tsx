import React from 'react';
import { motion } from 'framer-motion';
import { PanelLeftClose, PanelLeftOpen } from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';
import { cn } from '../../lib/cn';
import { useUIStore } from '../../stores/uiStore';
import { adminWorkspaceNavItems } from './adminWorkspaceNavigation';

export const AdminSidebar: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const sidebarCollapsed = useUIStore((state) => state.sidebarCollapsed);
  const toggleSidebar = useUIStore((state) => state.toggleSidebar);

  return (
    <motion.aside
      initial={{ opacity: 0, x: -20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.22, ease: 'easeOut' }}
      className={cn(
        'w-full lg:fixed lg:left-0 lg:top-[73px] lg:h-[calc(100vh-73px)] lg:overflow-hidden lg:border-r lg:border-[#E5E7EB] lg:bg-[#F9FAFB] lg:transition-[width] lg:duration-200',
        sidebarCollapsed ? 'lg:w-[104px]' : 'lg:w-[280px]'
      )}
    >
      <div className={cn('h-full px-4 py-5 lg:py-6', sidebarCollapsed ? 'lg:px-3' : 'lg:px-5')}>
        <div className="rounded-2xl border border-[#E5E7EB] bg-white p-3 shadow-sm lg:border-none lg:bg-transparent lg:p-0 lg:shadow-none">
          <div className={cn('mb-4 flex items-start', sidebarCollapsed ? 'justify-center' : 'justify-between gap-3 px-3')}>
            {sidebarCollapsed ? (
              <div className="flex h-11 w-11 items-center justify-center rounded-2xl border border-[#E5E7EB] bg-white text-lh-blue shadow-sm">
                AC
              </div>
            ) : (
              <div className="min-w-0">
                <div className="text-sm font-semibold text-[#111827]">Admin Console</div>
                <p className="mt-1 text-sm leading-6 text-[#6B7280]">
                  Workspace van hanh de quan tri nguoi dung, giao dich, noi dung va suc khoe he thong.
                </p>
              </div>
            )}

            <button
              type="button"
              onClick={toggleSidebar}
              title={sidebarCollapsed ? 'Mo rong thanh dieu huong' : 'Thu gon thanh dieu huong'}
              className="hidden h-10 w-10 items-center justify-center rounded-xl border border-[#E5E7EB] bg-white text-[#4B5563] shadow-sm transition hover:bg-[#F9FAFB] lg:inline-flex relative z-50"
            >
              {sidebarCollapsed ? <PanelLeftOpen className="h-4.5 w-4.5" /> : <PanelLeftClose className="h-4.5 w-4.5" />}
            </button>
          </div>

          {sidebarCollapsed ? (
            <div className="mb-4 flex justify-center lg:hidden">
              <button
                type="button"
                onClick={toggleSidebar}
                className="relative z-50 inline-flex h-10 w-10 items-center justify-center rounded-xl border border-[#E5E7EB] bg-white text-[#4B5563] shadow-sm transition hover:bg-[#F9FAFB]"
              >
                <PanelLeftOpen className="h-4.5 w-4.5" />
              </button>
            </div>
          ) : null}

          <nav className="space-y-1.5">
            {adminWorkspaceNavItems.map((item) => {
              const Icon = item.icon;
              const active = item.path === '/admin'
                ? location.pathname === '/admin'
                : location.pathname.startsWith(item.path);

              return (
                <button
                  key={item.path}
                  onClick={() => navigate(item.path)}
                  title={item.label}
                  className={cn(
                    'flex w-full items-center rounded-xl text-left text-sm font-medium transition',
                    sidebarCollapsed ? 'justify-center px-0 py-3' : 'gap-3 px-3 py-2.5',
                    active
                      ? 'bg-[#EEF2FF] text-lh-blue'
                      : 'text-[#4B5563] hover:bg-white hover:text-[#111827] lg:hover:bg-white'
                  )}
                >
                  <div
                    className={cn(
                      'flex h-9 w-9 shrink-0 items-center justify-center rounded-xl border transition',
                      active ? 'border-[#D9DEF2] bg-white text-lh-blue' : 'border-transparent bg-[#F3F4F6] text-[#6B7280]'
                    )}
                  >
                    <Icon className="h-4.5 w-4.5" />
                  </div>
                  {!sidebarCollapsed ? <span className="truncate">{item.label}</span> : null}
                </button>
              );
            })}
          </nav>
        </div>
      </div>
    </motion.aside>
  );
};

export default AdminSidebar;
