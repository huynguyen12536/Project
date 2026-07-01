import React, { useMemo, useState } from 'react';
import { Search, ShieldAlert, UserCheck, UserPlus, Users } from 'lucide-react';
import AdminWorkspaceLayout from '../../components/layouts/AdminWorkspaceLayout';
import { adminUsers } from '../../data/adminOperations';
import { useAdminStore } from '../../stores/adminStore';
import { cn } from '../../lib/cn';

const filters = [
  { label: 'Tat ca', value: 'all' },
  { label: 'Cho duyet', value: 'Cho duyet' },
  { label: 'OTP pending', value: 'OTP pending' },
  { label: 'Tam khoa', value: 'Tam khoa' },
] as const;

const AdminUsersPage: React.FC = () => {
  const [query, setQuery] = useState('');
  const [activeFilter, setActiveFilter] = useState<(typeof filters)[number]['value']>('all');
  const selectedUserIds = useAdminStore((state) => state.selectedUserIds);
  const toggleSelection = useAdminStore((state) => state.toggleSelection);
  const selectAll = useAdminStore((state) => state.selectAll);
  const clearSelection = useAdminStore((state) => state.clearSelection);

  const filteredUsers = useMemo(() => {
    return adminUsers.filter((user) => {
      const matchesFilter = activeFilter === 'all' || user.status === activeFilter;
      const keyword = query.trim().toLowerCase();
      const matchesQuery = !keyword
        || user.name.toLowerCase().includes(keyword)
        || user.email.toLowerCase().includes(keyword)
        || user.role.toLowerCase().includes(keyword);

      return matchesFilter && matchesQuery;
    });
  }, [activeFilter, query]);

  const allVisibleSelected =
    filteredUsers.length > 0 && filteredUsers.every((user) => selectedUserIds.includes(user.id));

  return (
    <AdminWorkspaceLayout
      title="Quan ly nguoi dung"
      description="Loc nhanh nhom tai khoan can xu ly, xu ly bulk action va theo doi cac ho so co rui ro cao."
      actions={
        <>
          <button className="inline-flex h-11 items-center justify-center gap-2 rounded-xl border border-[#D9DEF2] bg-white px-4 text-sm font-semibold text-lh-blue transition hover:bg-[#F8FAFF]">
            <UserPlus className="h-4 w-4" />
            Tao tai khoan
          </button>
          <button className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-lh-blue px-4 text-sm font-semibold text-white transition hover:bg-lh-navy">
            <UserCheck className="h-4 w-4" />
            Duyet batch pending
          </button>
        </>
      }
    >
      <div className="space-y-6">
        <div className="grid gap-4 md:grid-cols-3">
          <div className="rounded-2xl border border-[#E5E7EB] bg-white p-5 shadow-sm">
            <div className="flex items-center gap-3 text-sm font-medium text-[#6B7280]">
              <Users className="h-4 w-4 text-lh-blue" />
              Tong tai khoan can theo doi
            </div>
            <div className="mt-3 text-3xl font-semibold tracking-[-0.03em] text-[#111827]">{adminUsers.length}</div>
          </div>
          <div className="rounded-2xl border border-[#E5E7EB] bg-white p-5 shadow-sm">
            <div className="flex items-center gap-3 text-sm font-medium text-[#6B7280]">
              <ShieldAlert className="h-4 w-4 text-lh-pink" />
              Tai khoan can review ngay
            </div>
            <div className="mt-3 text-3xl font-semibold tracking-[-0.03em] text-[#111827]">
              {adminUsers.filter((item) => item.status !== 'Hoat dong').length}
            </div>
          </div>
          <div className="rounded-2xl border border-[#E5E7EB] bg-white p-5 shadow-sm">
            <div className="flex items-center gap-3 text-sm font-medium text-[#6B7280]">
              <UserCheck className="h-4 w-4 text-[#1F7A45]" />
              Bulk selection dang bat
            </div>
            <div className="mt-3 text-3xl font-semibold tracking-[-0.03em] text-[#111827]">{selectedUserIds.length}</div>
          </div>
        </div>

        <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div className="flex flex-wrap gap-2">
              {filters.map((filter) => (
                <button
                  key={filter.value}
                  onClick={() => setActiveFilter(filter.value)}
                  className={cn(
                    'rounded-full px-4 py-2 text-sm font-medium transition',
                    activeFilter === filter.value
                      ? 'bg-[#111827] text-white'
                      : 'bg-[#F3F4F6] text-[#4B5563] hover:bg-[#E5E7EB]'
                  )}
                >
                  {filter.label}
                </button>
              ))}
            </div>

            <div className="relative w-full max-w-md">
              <Search className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-[#9CA3AF]" />
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Tim theo ten, email, vai tro..."
                className="h-11 w-full rounded-xl border border-[#E5E7EB] bg-[#F9FAFB] pl-11 pr-4 text-sm text-[#111827] outline-none transition placeholder:text-[#9CA3AF] focus:border-[#D9DEF2] focus:bg-white focus:ring-4 focus:ring-[#EEF2FF]"
              />
            </div>
          </div>

          {selectedUserIds.length > 0 ? (
            <div className="mt-4 flex flex-wrap items-center gap-3 rounded-2xl border border-[#D9DEF2] bg-[#EEF2FF] px-4 py-3">
              <span className="text-sm font-semibold text-lh-blue">{selectedUserIds.length} tai khoan dang duoc chon</span>
              <button className="rounded-lg bg-white px-3 py-2 text-sm font-semibold text-lh-blue shadow-sm">Gui lai OTP</button>
              <button className="rounded-lg bg-white px-3 py-2 text-sm font-semibold text-lh-blue shadow-sm">Mo khoa</button>
              <button onClick={clearSelection} className="rounded-lg px-3 py-2 text-sm font-semibold text-[#4B5563]">Bo chon</button>
            </div>
          ) : null}

          <div className="mt-6 overflow-x-auto">
            <table className="min-w-full divide-y divide-[#EEF2F7]">
              <thead>
                <tr className="text-left text-xs font-semibold text-[#6B7280]">
                  <th className="pb-3 pr-4">
                    <input
                      type="checkbox"
                      checked={allVisibleSelected}
                      onChange={() => (allVisibleSelected ? clearSelection() : selectAll(filteredUsers.map((user) => user.id)))}
                      className="h-4 w-4 rounded border-[#CBD5E1]"
                    />
                  </th>
                  <th className="pb-3 pr-4">Nguoi dung</th>
                  <th className="pb-3 pr-4">Vai tro</th>
                  <th className="pb-3 pr-4">Trang thai</th>
                  <th className="pb-3 pr-4">Chi tieu</th>
                  <th className="pb-3 pr-4">Ngay tao</th>
                  <th className="pb-3">Rui ro</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#F3F4F6]">
                {filteredUsers.map((user) => {
                  const checked = selectedUserIds.includes(user.id);
                  return (
                    <tr key={user.id} className="text-sm text-[#111827]">
                      <td className="py-4 pr-4">
                        <input
                          type="checkbox"
                          checked={checked}
                          onChange={() => toggleSelection(user.id)}
                          className="h-4 w-4 rounded border-[#CBD5E1]"
                        />
                      </td>
                      <td className="py-4 pr-4">
                        <div className="font-medium">{user.name}</div>
                        <div className="mt-1 text-xs text-[#6B7280]">{user.email}</div>
                      </td>
                      <td className="py-4 pr-4 text-[#4B5563]">{user.role}</td>
                      <td className="py-4 pr-4">
                        <span className="rounded-full bg-[#F4F2F8] px-2.5 py-1 text-xs font-medium text-lh-purple">
                          {user.status}
                        </span>
                      </td>
                      <td className="py-4 pr-4 font-medium">{user.spending}</td>
                      <td className="py-4 pr-4 text-[#4B5563]">{user.joinedAt}</td>
                      <td className="py-4">
                        <span className={cn(
                          'rounded-full px-2.5 py-1 text-xs font-medium',
                          user.risk === 'Cao' ? 'bg-[#FDE7EC] text-lh-pink' : 'bg-[#EEF8F2] text-[#1F7A45]'
                        )}>
                          {user.risk}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </AdminWorkspaceLayout>
  );
};

export default AdminUsersPage;
