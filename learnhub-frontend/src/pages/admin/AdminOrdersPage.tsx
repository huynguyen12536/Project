import React, { useMemo, useState } from 'react';
import { CreditCard, Filter, ReceiptText, RotateCcw } from 'lucide-react';
import AdminWorkspaceLayout from '../../components/layouts/AdminWorkspaceLayout';
import { adminOrders } from '../../data/adminOperations';
import { cn } from '../../lib/cn';

const orderFilters = ['Tat ca', 'Da thanh toan', 'Cho xac nhan', 'Hoan tien', 'Tranh chap'] as const;

const AdminOrdersPage: React.FC = () => {
  const [activeFilter, setActiveFilter] = useState<(typeof orderFilters)[number]>('Tat ca');

  const rows = useMemo(() => {
    return adminOrders.filter((row) => activeFilter === 'Tat ca' || row.status === activeFilter);
  }, [activeFilter]);

  return (
    <AdminWorkspaceLayout
      title="Van hanh don hang"
      description="Theo doi lifecycle giao dich, xu ly refund/dispute va giu lane doi soat thong suot cho toan he thong."
      actions={
        <>
          <button className="inline-flex h-11 items-center justify-center gap-2 rounded-xl border border-[#D9DEF2] bg-white px-4 text-sm font-semibold text-lh-blue transition hover:bg-[#F8FAFF]">
            <RotateCcw className="h-4 w-4" />
            Replay webhook
          </button>
          <button className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-lh-blue px-4 text-sm font-semibold text-white transition hover:bg-lh-navy">
            <CreditCard className="h-4 w-4" />
            Xem settlement lane
          </button>
        </>
      }
    >
      <div className="space-y-6">
        <div className="grid gap-4 md:grid-cols-3">
          <div className="rounded-2xl border border-[#E5E7EB] bg-white p-5 shadow-sm">
            <div className="text-sm font-medium text-[#6B7280]">Giao dich dang mo</div>
            <div className="mt-3 text-3xl font-semibold tracking-[-0.03em] text-[#111827]">48</div>
          </div>
          <div className="rounded-2xl border border-[#E5E7EB] bg-white p-5 shadow-sm">
            <div className="text-sm font-medium text-[#6B7280]">Hoan tien can phe duyet</div>
            <div className="mt-3 text-3xl font-semibold tracking-[-0.03em] text-[#111827]">08</div>
          </div>
          <div className="rounded-2xl border border-[#E5E7EB] bg-white p-5 shadow-sm">
            <div className="text-sm font-medium text-[#6B7280]">Dispute lane</div>
            <div className="mt-3 text-3xl font-semibold tracking-[-0.03em] text-[#111827]">03</div>
          </div>
        </div>

        <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
          <div className="flex flex-wrap items-center gap-2">
            <Filter className="h-4 w-4 text-[#6B7280]" />
            {orderFilters.map((filter) => (
              <button
                key={filter}
                onClick={() => setActiveFilter(filter)}
                className={cn(
                  'rounded-full px-4 py-2 text-sm font-medium transition',
                  activeFilter === filter ? 'bg-[#111827] text-white' : 'bg-[#F3F4F6] text-[#4B5563] hover:bg-[#E5E7EB]'
                )}
              >
                {filter}
              </button>
            ))}
          </div>

          <div className="mt-6 overflow-x-auto">
            <table className="min-w-full divide-y divide-[#EEF2F7]">
              <thead>
                <tr className="text-left text-xs font-semibold text-[#6B7280]">
                  <th className="pb-3 pr-4">Ma don</th>
                  <th className="pb-3 pr-4">Nguoi mua</th>
                  <th className="pb-3 pr-4">Khoa hoc</th>
                  <th className="pb-3 pr-4">So tien</th>
                  <th className="pb-3 pr-4">Kenh thanh toan</th>
                  <th className="pb-3 pr-4">Trang thai</th>
                  <th className="pb-3">Settlement</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#F3F4F6]">
                {rows.map((row) => (
                  <tr key={row.id} className="text-sm text-[#111827]">
                    <td className="py-4 pr-4 font-medium">{row.id}</td>
                    <td className="py-4 pr-4">{row.user}</td>
                    <td className="py-4 pr-4 text-[#4B5563]">{row.course}</td>
                    <td className="py-4 pr-4 font-medium">{row.amount}</td>
                    <td className="py-4 pr-4">{row.payment}</td>
                    <td className="py-4 pr-4">
                      <span className={cn(
                        'rounded-full px-2.5 py-1 text-xs font-medium',
                        row.status === 'Da thanh toan' && 'bg-[#EEF8F2] text-[#1F7A45]',
                        row.status === 'Cho xac nhan' && 'bg-[#EEF2FF] text-lh-blue',
                        row.status === 'Hoan tien' && 'bg-[#FFF5E8] text-[#B76E14]',
                        row.status === 'Tranh chap' && 'bg-[#FDE7EC] text-lh-pink'
                      )}>
                        {row.status}
                      </span>
                    </td>
                    <td className="py-4">
                      <div className="inline-flex items-center gap-2 rounded-full bg-[#F3F4F6] px-2.5 py-1 text-xs font-medium text-[#4B5563]">
                        <ReceiptText className="h-3.5 w-3.5" />
                        {row.settlement}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </AdminWorkspaceLayout>
  );
};

export default AdminOrdersPage;
