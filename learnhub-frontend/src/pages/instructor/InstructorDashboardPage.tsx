import React from 'react';
import {
  ArrowUpRight,
  BookOpen,
  CreditCard,
  Plus,
  Star,
  Users,
  Wallet,
} from 'lucide-react';
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { useNavigate } from 'react-router-dom';
import InstructorWorkspaceLayout from '../../components/layouts/InstructorWorkspaceLayout';
import InstructorStatCard from '../../components/layouts/InstructorStatCard';

const revenueData = [
  { label: 'T2', revenue: 42, payouts: 28 },
  { label: 'T3', revenue: 48, payouts: 31 },
  { label: 'T4', revenue: 51, payouts: 34 },
  { label: 'T5', revenue: 63, payouts: 39 },
  { label: 'T6', revenue: 76, payouts: 47 },
  { label: 'T7', revenue: 88, payouts: 54 },
];

const recentActivities = [
  {
    id: 'activity-1',
    title: 'Nguyen Van A vua mua Lap trinh Web Full-Stack 2026',
    meta: '5 phut truoc',
  },
  {
    id: 'activity-2',
    title: 'Tran Thi B de lai danh gia 5 sao cho He thong thiet ke cho SaaS Product',
    meta: '18 phut truoc',
  },
  {
    id: 'activity-3',
    title: 'Le Thi D da hoan tat 3 bai hoc moi trong khoa AI Prompting',
    meta: '1 gio truoc',
  },
];

const studentRows = [
  {
    id: 'student-1',
    name: 'Nguyen Van A',
    course: 'Lap trinh Web Full-Stack 2026',
    status: 'Dang hoc',
    amount: '499.000d',
  },
  {
    id: 'student-2',
    name: 'Tran Thi B',
    course: 'He thong thiet ke cho SaaS Product',
    status: 'Moi dang ky',
    amount: '399.000d',
  },
  {
    id: 'student-3',
    name: 'Le Thi D',
    course: 'AI Prompting thuc chien cho team van hanh',
    status: 'Tien do 72%',
    amount: '279.000d',
  },
];

const InstructorDashboardPage: React.FC = () => {
  const navigate = useNavigate();

  return (
    <InstructorWorkspaceLayout
      title="Tong quan"
      description="Tong hop nhanh cac chi so kinh doanh, nhung bien dong moi nhat va cac hanh dong can xu ly trong Instructor Studio."
      actions={
        <button
          onClick={() => navigate('/instructor/courses')}
          className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-lh-blue px-4 text-sm font-semibold text-white transition hover:bg-lh-navy"
        >
          <Plus className="h-4 w-4" />
          Tao khoa hoc moi
        </button>
      }
    >
      <div className="space-y-6">
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <InstructorStatCard
            label="Doanh thu thang nay"
            value="287.1tr"
            note="+12.4tr so voi thang truoc"
            icon={Wallet}
            toneClassName="bg-[#EEF2FF] text-lh-blue"
          />
          <InstructorStatCard
            label="Hoc vien dang hoc"
            value="13.5K"
            note="1.238 hoc vien quay lai trong 7 ngay"
            icon={Users}
            toneClassName="bg-[#FFF1F5] text-lh-pink"
          />
          <InstructorStatCard
            label="Don hang moi"
            value="84"
            note="23 giao dich trong 24 gio qua"
            icon={CreditCard}
            toneClassName="bg-[#F4F2F8] text-lh-purple"
          />
          <InstructorStatCard
            label="Danh gia trung binh"
            value="4.83"
            note="Tu 1.248 danh gia hoc vien"
            icon={Star}
            toneClassName="bg-[#FFF5E8] text-[#B76E14]"
          />
        </div>

        <div className="grid gap-6 xl:grid-cols-[minmax(0,1.55fr)_380px]">
          <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <div className="text-sm font-medium text-[#6B7280]">Doanh thu</div>
                <h2 className="mt-2 font-inter text-2xl font-semibold tracking-[-0.02em] text-[#111827]">
                  Duong tang truong 6 thang gan nhat
                </h2>
              </div>
              <button
                onClick={() => navigate('/instructor/revenue')}
                className="inline-flex items-center gap-2 text-sm font-medium text-lh-blue"
              >
                Xem chi tiet
                <ArrowUpRight className="h-4 w-4" />
              </button>
            </div>

            <div className="mt-6 h-[320px]">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={revenueData} margin={{ left: -18, right: 8, top: 12, bottom: 0 }}>
                  <defs>
                    <linearGradient id="overviewRevenue" x1="0" x2="0" y1="0" y2="1">
                      <stop offset="5%" stopColor="#4F46E5" stopOpacity={0.28} />
                      <stop offset="95%" stopColor="#4F46E5" stopOpacity={0.02} />
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
                  <Area
                    type="monotone"
                    dataKey="revenue"
                    stroke="#4F46E5"
                    strokeWidth={2.5}
                    fill="url(#overviewRevenue)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
            <div className="flex items-center justify-between gap-4">
              <div>
                <div className="text-sm font-medium text-[#6B7280]">Gan day</div>
                <h2 className="mt-2 font-inter text-2xl font-semibold tracking-[-0.02em] text-[#111827]">
                  Hoat dong moi nhat
                </h2>
              </div>
            </div>

            <div className="mt-6 space-y-4">
              {recentActivities.map((activity) => (
                <div key={activity.id} className="rounded-2xl border border-[#EEF2F7] bg-[#FBFCFE] p-4">
                  <div className="text-sm font-medium leading-6 text-[#111827]">{activity.title}</div>
                  <div className="mt-2 text-xs font-medium text-[#6B7280]">{activity.meta}</div>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="grid gap-6 xl:grid-cols-[minmax(0,1.3fr)_minmax(0,0.9fr)]">
          <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <div className="text-sm font-medium text-[#6B7280]">Hoc vien va don hang</div>
                <h2 className="mt-2 font-inter text-2xl font-semibold tracking-[-0.02em] text-[#111827]">
                  Nhung giao dich can theo doi
                </h2>
              </div>
              <button
                onClick={() => navigate('/instructor/students')}
                className="inline-flex items-center gap-2 text-sm font-medium text-lh-blue"
              >
                Mo trang hoc vien
                <ArrowUpRight className="h-4 w-4" />
              </button>
            </div>

            <div className="mt-6 overflow-x-auto">
              <table className="min-w-full divide-y divide-[#EEF2F7]">
                <thead>
                  <tr className="text-left text-xs font-semibold text-[#6B7280]">
                    <th className="pb-3 pr-4">Hoc vien</th>
                    <th className="pb-3 pr-4">Khoa hoc</th>
                    <th className="pb-3 pr-4">Trang thai</th>
                    <th className="pb-3">Gia tri</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#F3F4F6]">
                  {studentRows.map((row) => (
                    <tr key={row.id} className="text-sm text-[#111827]">
                      <td className="py-4 pr-4 font-medium">{row.name}</td>
                      <td className="py-4 pr-4 text-[#4B5563]">{row.course}</td>
                      <td className="py-4 pr-4">
                        <span className="rounded-full bg-[#EEF2FF] px-2.5 py-1 text-xs font-medium text-lh-blue">
                          {row.status}
                        </span>
                      </td>
                      <td className="py-4 font-medium">{row.amount}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="space-y-6">
            <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
              <div className="text-sm font-medium text-[#6B7280]">Tiep theo</div>
              <h2 className="mt-2 font-inter text-2xl font-semibold tracking-[-0.02em] text-[#111827]">
                Dieu huong nhanh
              </h2>
              <div className="mt-5 space-y-3">
                <button
                  onClick={() => navigate('/instructor/courses')}
                  className="flex w-full items-center justify-between rounded-2xl border border-[#E5E7EB] bg-[#FBFCFE] px-4 py-4 text-left transition hover:border-[#D9DEF2] hover:bg-white"
                >
                  <div className="flex items-center gap-3">
                    <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#EEF2FF] text-lh-blue">
                      <BookOpen className="h-4.5 w-4.5" />
                    </div>
                    <div>
                      <div className="font-medium text-[#111827]">Quan ly khoa hoc</div>
                      <div className="mt-1 text-sm text-[#6B7280]">Sua noi dung va curriculum</div>
                    </div>
                  </div>
                  <ArrowUpRight className="h-4 w-4 text-[#6B7280]" />
                </button>
                <button
                  onClick={() => navigate('/instructor/lessons')}
                  className="flex w-full items-center justify-between rounded-2xl border border-[#E5E7EB] bg-[#FBFCFE] px-4 py-4 text-left transition hover:border-[#D9DEF2] hover:bg-white"
                >
                  <div className="flex items-center gap-3">
                    <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#FFF1F5] text-lh-pink">
                      <BookOpen className="h-4.5 w-4.5" />
                    </div>
                    <div>
                      <div className="font-medium text-[#111827]">Mo danh sach bai giang</div>
                      <div className="mt-1 text-sm text-[#6B7280]">Tim nhanh video, bai viet, quiz</div>
                    </div>
                  </div>
                  <ArrowUpRight className="h-4 w-4 text-[#6B7280]" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </InstructorWorkspaceLayout>
  );
};

export default InstructorDashboardPage;
