import React from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { useLocation } from 'react-router-dom';
import AdminSidebar from './AdminSidebar';
import AdminWorkspaceHeader from './AdminWorkspaceHeader';

interface AdminWorkspaceLayoutProps {
  title: string;
  description?: string;
  actions?: React.ReactNode;
  children: React.ReactNode;
}

export const AdminWorkspaceLayout: React.FC<AdminWorkspaceLayoutProps> = ({
  title,
  description,
  actions,
  children,
}) => {
  const location = useLocation();

  return (
    <div className="min-h-screen bg-[#F9FAFB]">
      <AdminWorkspaceHeader title={title} />
      <AdminSidebar />

      <div className="px-4 py-5 sm:px-6 lg:pl-[312px] lg:pr-8">
        <div className="mx-auto max-w-[1380px]">
          <div className="mb-6 rounded-2xl border border-[#E5E7EB] bg-white px-6 py-6 shadow-sm">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
              <div>
                <h1 className="font-inter text-[28px] font-semibold tracking-[-0.03em] text-[#111827]">{title}</h1>
                {description ? <p className="mt-3 max-w-3xl text-sm leading-6 text-[#6B7280]">{description}</p> : null}
              </div>
              {actions ? <div className="flex flex-wrap gap-3">{actions}</div> : null}
            </div>
          </div>

          <AnimatePresence mode="wait">
            <motion.div
              key={location.pathname}
              initial={{ opacity: 0, x: 18 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -12 }}
              transition={{ duration: 0.2, ease: 'easeOut' }}
            >
              {children}
            </motion.div>
          </AnimatePresence>
        </div>
      </div>
    </div>
  );
};

export default AdminWorkspaceLayout;
