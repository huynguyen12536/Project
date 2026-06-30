import React from 'react';
import { Bell, Globe, ShieldCheck } from 'lucide-react';
import InstructorWorkspaceLayout from '../../components/layouts/InstructorWorkspaceLayout';

const settingsCards = [
  {
    icon: ShieldCheck,
    title: 'Thong tin studio',
    description: 'Cap nhat ten hien thi, mo ta ngan va thong tin thanh toan de hoc vien tin tuong hon.',
    action: 'Cap nhat ho so',
  },
  {
    icon: Bell,
    title: 'Thong bao va doi soat',
    description: 'Chon cach nhan thong bao khi co don hang moi, danh gia moi hoac khi den ky doi soat.',
    action: 'Quan ly thong bao',
  },
  {
    icon: Globe,
    title: 'Chinh sach xuat ban',
    description: 'Kiem soat cach khoa hoc duoc cong bo, hien thi ngoai catalog va quy tac mo uu dai.',
    action: 'Mo cai dat',
  },
];

const InstructorSettingsPage: React.FC = () => {
  return (
    <InstructorWorkspaceLayout
      title="Cai dat"
      description="Tap trung vao nhung cai dat can thiet nhat de giang vien quan ly studio ma khong bi ngop boi qua nhieu bieu mau."
    >
      <div className="grid gap-6 xl:grid-cols-3">
        {settingsCards.map((card) => {
          const Icon = card.icon;
          return (
            <div key={card.title} className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
              <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[#EEF2FF] text-lh-blue">
                <Icon className="h-5 w-5" />
              </div>
              <h2 className="mt-5 font-inter text-xl font-semibold tracking-[-0.02em] text-[#111827]">{card.title}</h2>
              <p className="mt-3 text-sm leading-6 text-[#6B7280]">{card.description}</p>
              <button className="mt-6 inline-flex h-10 items-center rounded-xl border border-[#D9DEF2] px-4 text-sm font-semibold text-lh-blue transition hover:bg-[#F8FAFF]">
                {card.action}
              </button>
            </div>
          );
        })}
      </div>
    </InstructorWorkspaceLayout>
  );
};

export default InstructorSettingsPage;
