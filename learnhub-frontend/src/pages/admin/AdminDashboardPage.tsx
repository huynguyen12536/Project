import React from 'react';
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { ArrowUpRight, RefreshCw, ShieldCheck } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import AdminWorkspaceLayout from '../../components/layouts/AdminWorkspaceLayout';
import InstructorStatCard from '../../components/layouts/InstructorStatCard';
import {
  adminActivityFeed,
  adminGrowthData,
  adminOverviewStats,
  adminPriorityQueue,
  adminUsers,
} from '../../data/adminOperations';

const AdminDashboardPage: React.FC = () => {
  const navigate = useNavigate();

  return (
    <AdminWorkspaceLayout
      title="Tong quan van hanh"
      description="Theo doi suc khoe nen tang, hang doi can xu ly va cac chi so kinh doanh quan trong trong cung mot workspace danh cho admin."
      actions={
        <>
          <button
            onClick={() => navigate('/admin/system')}
            className="inline-flex h-11 items-center justify-center gap-2 rounded-xl border border-[#D9DEF2] bg-white px-4 text-sm font-semibold text-lh-blue transition hover:bg-[#F8FAFF]"
          >
            <ShieldCheck className="h-4 w-4" />
            Kiem tra he thong
          </button>
          <button
            onClick={() => navigate('/admin/taxonomy')}
            className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-lh-blue px-4 text-sm font-semibold text-white transition hover:bg-lh-navy"
          >
            <RefreshCw className="h-4 w-4" />
            Quan ly taxonomy
          </button>
        </>
      }
    >
      <div className="space-y-6">
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          {adminOverviewStats.map((item) => (
            <InstructorStatCard
              key={item.label}
              label={item.label}
              value={item.value}
              note={item.note}
              icon={item.icon}
              toneClassName={item.toneClassName}
            />
          ))}
        </div>

        <div className="grid gap-6 xl:grid-cols-[minmax(0,1.55fr)_380px]">
          <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <div className="text-sm font-medium text-[#6B7280]">Tang truong van hanh</div>
                <h2 className="mt-2 font-inter text-2xl font-semibold tracking-[-0.02em] text-[#111827]">
                  Dang ky, don thanh cong va nhom refund
                </h2>
              </div>
              <button
                onClick={() => navigate('/admin/orders')}
                className="inline-flex items-center gap-2 text-sm font-medium text-lh-blue"
              >
                Xem don hang
                <ArrowUpRight className="h-4 w-4" />
              </button>
            </div>

            <div className="mt-6 h-[320px]">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={adminGrowthData} margin={{ left: -18, right: 8, top: 12, bottom: 0 }}>
                  <defs>
                    <linearGradient id="adminSignups" x1="0" x2="0" y1="0" y2="1">
                      <stop offset="5%" stopColor="#4F46E5" stopOpacity={0.24} />
                      <stop offset="95%" stopColor="#4F46E5" stopOpacity={0.03} />
                    </linearGradient>
                    <linearGradient id="adminPaidOrders" x1="0" x2="0" y1="0" y2="1">
                      <stop offset="5%" stopColor="#EC4899" stopOpacity={0.18} />
                      <stop offset="95%" stopColor="#EC4899" stopOpacity={0.01} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid stroke="#EEF2F7" vertical={false} />
                  <XAxis dataKey="label" tickLine={false} axisLine={false} tick={{ fill: '#6B7280', fontSize: 12 }} />
                  <YAxis tickLine={false} axisLine={false} tick={{ fill: '#6B7280', fontSize: 12 }} />
                  <Tooltip
                    contentStyle={{
                      borderRadius: 14,
                      border: '1px solid #E5E7EB',
                      boxShadow: '0 12px 30px rgba(15, 23, 42, 0.08)',
                    }}
                  />
                  <Area type="monotone" dataKey="signups" stroke="#4F46E5" strokeWidth={2.5} fill="url(#adminSignups)" />
                  <Area type="monotone" dataKey="paidOrders" stroke="#EC4899" strokeWidth={2.5} fill="url(#adminPaidOrders)" />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
            <div>
              <div className="text-sm font-medium text-[#6B7280]">Can xu ly ngay</div>
              <h2 className="mt-2 font-inter text-2xl font-semibold tracking-[-0.02em] text-[#111827]">
                Priority queue
              </h2>
            </div>

            <div className="mt-6 space-y-3">
              {adminPriorityQueue.map((item) => (
                <button
                  key={item.id}
                  onClick={() => navigate(item.id === 'queue-2' ? '/admin/taxonomy' : '/admin/system')}
                  className="w-full rounded-2xl border border-[#EEF2F7] bg-[#FBFCFE] p-4 text-left transition hover:border-[#D9DEF2] hover:bg-white"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="text-sm font-medium leading-6 text-[#111827]">{item.title}</div>
                    <span className="rounded-full bg-[#EEF2FF] px-2.5 py-1 text-[11px] font-semibold text-lh-blue">
                      {item.badge}
                    </span>
                  </div>
                  <div className="mt-2 text-xs font-medium text-[#6B7280]">{item.meta}</div>
                </button>
              ))}
            </div>
          </div>
        </div>

        <div className="grid gap-6 xl:grid-cols-[minmax(0,1.3fr)_minmax(0,0.9fr)]">
          <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <div className="text-sm font-medium text-[#6B7280]">Hang doi nguoi dung</div>
                <h2 className="mt-2 font-inter text-2xl font-semibold tracking-[-0.02em] text-[#111827]">
                  Ho so can review hoac can can thiep
                </h2>
              </div>
              <button
                onClick={() => navigate('/admin/users')}
                className="inline-flex items-center gap-2 text-sm font-medium text-lh-blue"
              >
                Mo quan ly nguoi dung
                <ArrowUpRight className="h-4 w-4" />
              </button>
            </div>

            <div className="mt-6 overflow-x-auto">
              <table className="min-w-full divide-y divide-[#EEF2F7]">
                <thead>
                  <tr className="text-left text-xs font-semibold text-[#6B7280]">
                    <th className="pb-3 pr-4">Nguoi dung</th>
                    <th className="pb-3 pr-4">Vai tro</th>
                    <th className="pb-3 pr-4">Trang thai</th>
                    <th className="pb-3">Rui ro</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#F3F4F6]">
                  {adminUsers.slice(0, 4).map((row) => (
                    <tr key={row.id} className="text-sm text-[#111827]">
                      <td className="py-4 pr-4">
                        <div className="font-medium">{row.name}</div>
                        <div className="mt-1 text-xs text-[#6B7280]">{row.email}</div>
                      </td>
                      <td className="py-4 pr-4 text-[#4B5563]">{row.role}</td>
                      <td className="py-4 pr-4">
                        <span className="rounded-full bg-[#F4F2F8] px-2.5 py-1 text-xs font-medium text-lh-purple">
                          {row.status}
                        </span>
                      </td>
                      <td className="py-4 font-medium">{row.risk}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
            <div className="text-sm font-medium text-[#6B7280]">Timeline</div>
            <h2 className="mt-2 font-inter text-2xl font-semibold tracking-[-0.02em] text-[#111827]">
              Hoat dong gan day
            </h2>

            <div className="mt-6 space-y-4">
              {adminActivityFeed.map((activity) => (
                <div key={activity.id} className="rounded-2xl border border-[#EEF2F7] bg-[#FBFCFE] p-4">
                  <div className="text-sm font-medium leading-6 text-[#111827]">{activity.title}</div>
                  <div className="mt-2 text-xs font-medium text-[#6B7280]">{activity.meta}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </AdminWorkspaceLayout>
  );
};

export default AdminDashboardPage;
