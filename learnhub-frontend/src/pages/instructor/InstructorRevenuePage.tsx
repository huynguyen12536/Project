import React from 'react';
import { ArrowDownToLine, CreditCard, Receipt, TrendingUp, Wallet } from 'lucide-react';
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import InstructorWorkspaceLayout from '../../components/layouts/InstructorWorkspaceLayout';
import InstructorStatCard from '../../components/layouts/InstructorStatCard';

const revenueData = [
  { label: 'T2', revenue: 42, refunds: 2.4 },
  { label: 'T3', revenue: 48, refunds: 2.1 },
  { label: 'T4', revenue: 51, refunds: 1.9 },
  { label: 'T5', revenue: 63, refunds: 2.8 },
  { label: 'T6', revenue: 76, refunds: 3.2 },
  { label: 'T7', revenue: 88, refunds: 3.6 },
];

const payouts = [
  { id: 'pay-1', period: '01/06 - 15/06', status: 'Da doi soat', amount: '126.500.000d' },
  { id: 'pay-2', period: '16/06 - 30/06', status: 'Dang xu ly', amount: '160.600.000d' },
  { id: 'pay-3', period: '01/07 - 15/07', status: 'Tam tinh', amount: '92.800.000d' },
];

const InstructorRevenuePage: React.FC = () => {
  return (
    <InstructorWorkspaceLayout
      title="Doanh thu"
      description="Phan tach luong doanh thu, hoan tien va cac dot doi soat de giang vien co duoc buc tranh tai chinh ro rang hon."
      actions={
        <button className="inline-flex h-11 items-center justify-center gap-2 rounded-xl border border-[#E5E7EB] bg-white px-4 text-sm font-semibold text-[#374151] transition hover:bg-[#F9FAFB]">
          <ArrowDownToLine className="h-4 w-4" />
          Tai bao cao
        </button>
      }
    >
      <div className="space-y-6">
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <InstructorStatCard label="Tong doanh thu" value="1.24 ty" note="+18.2% theo thang" icon={Wallet} />
          <InstructorStatCard
            label="Thuc nhan thang nay"
            value="287.1tr"
            note="Sau phi nen tang va doi soat"
            icon={TrendingUp}
            toneClassName="bg-[#FFF1F5] text-lh-pink"
          />
          <InstructorStatCard
            label="Hoan tien"
            value="3.6tr"
            note="Ty le hoan tien 1.8%"
            icon={Receipt}
            toneClassName="bg-[#F4F2F8] text-lh-purple"
          />
          <InstructorStatCard
            label="Don thanh toan"
            value="842"
            note="Gia tri trung binh 426.000d"
            icon={CreditCard}
            toneClassName="bg-[#FFF5E8] text-[#B76E14]"
          />
        </div>

        <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
          <div>
            <div className="text-sm font-medium text-[#6B7280]">Bieu do</div>
            <h2 className="mt-2 font-inter text-2xl font-semibold tracking-[-0.02em] text-[#111827]">
              Dong doanh thu va hoan tien
            </h2>
          </div>
          <div className="mt-6 h-[360px]">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={revenueData} margin={{ left: -18, right: 8, top: 12, bottom: 0 }}>
                <defs>
                  <linearGradient id="revenueFill" x1="0" x2="0" y1="0" y2="1">
                    <stop offset="5%" stopColor="#4F46E5" stopOpacity={0.28} />
                    <stop offset="95%" stopColor="#4F46E5" stopOpacity={0.02} />
                  </linearGradient>
                  <linearGradient id="refundFill" x1="0" x2="0" y1="0" y2="1">
                    <stop offset="5%" stopColor="#EC4899" stopOpacity={0.18} />
                    <stop offset="95%" stopColor="#EC4899" stopOpacity={0.02} />
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
                <Area type="monotone" dataKey="revenue" stroke="#4F46E5" strokeWidth={2.5} fill="url(#revenueFill)" />
                <Area type="monotone" dataKey="refunds" stroke="#EC4899" strokeWidth={2} fill="url(#refundFill)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
          <div>
            <div className="text-sm font-medium text-[#6B7280]">Doi soat</div>
            <h2 className="mt-2 font-inter text-2xl font-semibold tracking-[-0.02em] text-[#111827]">
              Lich su dot thanh toan
            </h2>
          </div>
          <div className="mt-6 overflow-x-auto">
            <table className="min-w-full divide-y divide-[#EEF2F7]">
              <thead>
                <tr className="text-left text-xs font-semibold text-[#6B7280]">
                  <th className="pb-3 pr-4">Ky doi soat</th>
                  <th className="pb-3 pr-4">Trang thai</th>
                  <th className="pb-3">So tien</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#F3F4F6]">
                {payouts.map((payout) => (
                  <tr key={payout.id} className="text-sm text-[#111827]">
                    <td className="py-4 pr-4 font-medium">{payout.period}</td>
                    <td className="py-4 pr-4">
                      <span className="rounded-full bg-[#EEF2FF] px-2.5 py-1 text-xs font-medium text-lh-blue">
                        {payout.status}
                      </span>
                    </td>
                    <td className="py-4 font-medium">{payout.amount}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </InstructorWorkspaceLayout>
  );
};

export default InstructorRevenuePage;
