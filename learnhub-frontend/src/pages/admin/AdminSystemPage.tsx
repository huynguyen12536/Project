import React from 'react';
import { Activity, BellRing, DatabaseZap, ShieldCheck } from 'lucide-react';
import AdminWorkspaceLayout from '../../components/layouts/AdminWorkspaceLayout';
import InstructorStatCard from '../../components/layouts/InstructorStatCard';
import { adminAuditLogs, adminSystemHealth } from '../../data/adminOperations';

const AdminSystemPage: React.FC = () => {
  return (
    <AdminWorkspaceLayout
      title="He thong va audit"
      description="Theo doi service health, queue retry, audit trail va nhung lane can intervention de giu van hanh on dinh."
      actions={
        <button className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-lh-blue px-4 text-sm font-semibold text-white transition hover:bg-lh-navy">
          <ShieldCheck className="h-4 w-4" />
          Chay health review
        </button>
      }
    >
      <div className="space-y-6">
        <div className="grid gap-4 md:grid-cols-3">
          {adminSystemHealth.map((item) => (
            <InstructorStatCard
              key={item.id}
              label={item.label}
              value={item.status}
              note={item.note}
              icon={item.icon}
              toneClassName={item.toneClassName}
            />
          ))}
        </div>

        <div className="grid gap-6 xl:grid-cols-[minmax(0,1.1fr)_minmax(0,0.9fr)]">
          <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
            <div className="flex items-center gap-3 text-sm font-medium text-[#6B7280]">
              <Activity className="h-4 w-4 text-lh-blue" />
              Audit log
            </div>
            <div className="mt-5 overflow-x-auto">
              <table className="min-w-full divide-y divide-[#EEF2F7]">
                <thead>
                  <tr className="text-left text-xs font-semibold text-[#6B7280]">
                    <th className="pb-3 pr-4">Actor</th>
                    <th className="pb-3 pr-4">Action</th>
                    <th className="pb-3 pr-4">Target</th>
                    <th className="pb-3">Timestamp</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#F3F4F6]">
                  {adminAuditLogs.map((log) => (
                    <tr key={log.id} className="text-sm text-[#111827]">
                      <td className="py-4 pr-4 font-medium">{log.actor}</td>
                      <td className="py-4 pr-4">{log.action}</td>
                      <td className="py-4 pr-4 text-[#4B5563]">{log.target}</td>
                      <td className="py-4 text-[#4B5563]">{log.timestamp}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="space-y-6">
            <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
              <div className="flex items-center gap-3 text-sm font-medium text-[#6B7280]">
                <BellRing className="h-4 w-4 text-lh-pink" />
                Queue va retry lane
              </div>
              <div className="mt-4 space-y-3">
                <div className="rounded-2xl bg-[#FBFCFE] px-4 py-4">
                  <div className="text-sm font-medium text-[#111827]">Email OTP retry</div>
                  <div className="mt-1 text-sm text-[#6B7280]">17 job · backoff toi da 10 phut</div>
                </div>
                <div className="rounded-2xl bg-[#FBFCFE] px-4 py-4">
                  <div className="text-sm font-medium text-[#111827]">Payment replay lane</div>
                  <div className="mt-1 text-sm text-[#6B7280]">2 event cho replay sau khi provider on dinh</div>
                </div>
              </div>
            </div>

            <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
              <div className="flex items-center gap-3 text-sm font-medium text-[#6B7280]">
                <DatabaseZap className="h-4 w-4 text-lh-blue" />
                Ghi chu he thong
              </div>
              <div className="mt-4 space-y-3 text-sm leading-6 text-[#4B5563]">
                <p>- MinIO dang phuc vu anh dai dien qua public bucket avatars.</p>
                <p>- Queue OTP can tiep tuc theo doi khi Gmail SMTP rate limit tang.</p>
                <p>- Flow profile da chuyen sang PATCH va da verify upload avatar toi object storage that.</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </AdminWorkspaceLayout>
  );
};

export default AdminSystemPage;
