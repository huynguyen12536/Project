import React, { useEffect, useMemo, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import {
  Bell,
  ChevronDown,
  LogOut,
  Search,
  Settings2,
  ShieldCheck,
  UserCircle2,
} from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';
import apiClient from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { Avatar } from '../../ui-kit/Avatar';
import { cn } from '../../lib/cn';
import { getAdminWorkspaceLabel } from './adminWorkspaceNavigation';

interface AdminWorkspaceHeaderProps {
  title: string;
}

export const AdminWorkspaceHeader: React.FC<AdminWorkspaceHeaderProps> = ({ title }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const user = useAuthStore((state) => state.user);
  const logout = useAuthStore((state) => state.logout);
  const [accountOpen, setAccountOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement | null>(null);

  const displayName = useMemo(() => {
    const fullName = [user?.firstName, user?.lastName].filter(Boolean).join(' ').trim();
    return fullName || user?.email || 'Quan tri vien';
  }, [user?.email, user?.firstName, user?.lastName]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setAccountOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleLogout = async () => {
    setAccountOpen(false);
    try {
      await apiClient.post('/v1/auth/logout');
    } catch {
      /* ignore logout API errors and clear client state anyway */
    }
    logout();
    navigate('/login');
  };

  const breadcrumbLabel = title || getAdminWorkspaceLabel(location.pathname);

  return (
    <header className="sticky top-0 z-40 border-b border-[#E5E7EB] bg-white/95 backdrop-blur">
      <div className="flex h-[72px] items-center gap-4 px-4 sm:px-6 lg:px-8">
        <button onClick={() => navigate('/admin')} className="flex shrink-0 items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-lh-navy shadow-sm">
            <div className="ml-1 h-0 w-0 border-b-[7px] border-l-[11px] border-t-[7px] border-b-transparent border-l-lh-pink border-t-transparent" />
          </div>
          <span className="font-inter text-lg font-bold tracking-[-0.03em] text-lh-navy">LearnHub</span>
        </button>

        <div className="hidden items-center gap-2 rounded-full border border-[#D9DEF2] bg-[#EEF2FF] px-3 py-1 text-[11px] font-semibold text-lh-blue md:inline-flex">
          <ShieldCheck className="h-3.5 w-3.5" />
          Admin Console
        </div>

        <div className="hidden min-w-0 flex-1 lg:block">
          <div className="mx-auto max-w-xl">
            <div className="relative">
              <Search className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-[#9CA3AF]" />
              <input
                type="search"
                placeholder="Tim nguoi dung, don hang, khoa hoc, su kien he thong..."
                className="h-11 w-full rounded-full border border-[#E5E7EB] bg-[#F9FAFB] pl-11 pr-4 text-sm text-[#111827] outline-none transition placeholder:text-[#9CA3AF] focus:border-[#D9DEF2] focus:bg-white focus:ring-4 focus:ring-[#EEF2FF]"
              />
            </div>
          </div>
        </div>

        <div className="ml-auto flex items-center gap-3">
          <button className="inline-flex h-10 w-10 items-center justify-center rounded-xl border border-[#E5E7EB] bg-white text-[#4B5563] transition hover:bg-[#F9FAFB]">
            <Bell className="h-4.5 w-4.5" />
          </button>

          <div ref={menuRef} className="relative">
            <button
              onClick={() => setAccountOpen((current) => !current)}
              className="flex h-11 items-center gap-3 rounded-xl border border-[#E5E7EB] bg-white px-3 text-left transition hover:bg-[#F9FAFB]"
            >
              <Avatar src={user?.avatarUrl} name={displayName} size="sm" className="bg-[#EEF2FF] text-lh-blue" />
              <div className="hidden min-w-0 lg:block">
                <div className="max-w-[132px] truncate text-sm font-semibold text-[#111827]">{displayName}</div>
                <div className="max-w-[132px] truncate text-xs text-[#6B7280]">{user?.email}</div>
              </div>
              <ChevronDown className={cn('h-4 w-4 text-[#6B7280] transition', accountOpen && 'rotate-180')} />
            </button>

            <AnimatePresence>
              {accountOpen ? (
                <motion.div
                  initial={{ opacity: 0, y: 10, scale: 0.98 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  exit={{ opacity: 0, y: 6, scale: 0.98 }}
                  transition={{ duration: 0.16, ease: 'easeOut' }}
                  className="absolute right-0 top-[52px] z-50 w-[240px] rounded-2xl border border-[#E5E7EB] bg-white p-2 shadow-lg"
                >
                  <button
                    onClick={() => {
                      setAccountOpen(false);
                      navigate('/profile/me');
                    }}
                    className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm font-medium text-[#374151] transition hover:bg-[#F9FAFB]"
                  >
                    <UserCircle2 className="h-4 w-4 text-lh-blue" />
                    Ho so quan tri
                  </button>
                  <button
                    onClick={() => {
                      setAccountOpen(false);
                      navigate('/admin/system');
                    }}
                    className="mt-1 flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm font-medium text-[#374151] transition hover:bg-[#F9FAFB]"
                  >
                    <Settings2 className="h-4 w-4 text-[#6B7280]" />
                    Cai dat van hanh
                  </button>
                  <button
                    onClick={handleLogout}
                    className="mt-1 flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm font-medium text-[#B42318] transition hover:bg-[#FEF3F2]"
                  >
                    <LogOut className="h-4 w-4" />
                    Dang xuat
                  </button>
                </motion.div>
              ) : null}
            </AnimatePresence>
          </div>
        </div>
      </div>

      <div className="border-t border-[#F3F4F6] bg-white">
        <div className="flex items-center gap-2 px-4 py-3 text-sm text-[#6B7280] sm:px-6 lg:px-8">
          <span>Admin</span>
          <span>{'>'}</span>
          <span className="font-medium text-[#111827]">{breadcrumbLabel}</span>
        </div>
      </div>
    </header>
  );
};

export default AdminWorkspaceHeader;
