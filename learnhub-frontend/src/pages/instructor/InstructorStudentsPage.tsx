import React, { useMemo, useState } from 'react';
import { Download, Mail, Search, Users, UserPlus, Wallet } from 'lucide-react';
import InstructorWorkspaceLayout from '../../components/layouts/InstructorWorkspaceLayout';
import InstructorStatCard from '../../components/layouts/InstructorStatCard';

const students = [
  {
    id: 'student-1',
    name: 'Nguyen Van A',
    email: 'nguyenvana@email.com',
    course: 'Lap trinh Web Full-Stack 2026',
    joinedAt: '30/06/2026',
    progress: '42%',
    spend: '499.000d',
  },
  {
    id: 'student-2',
    name: 'Tran Thi B',
    email: 'tranthib@email.com',
    course: 'He thong thiet ke cho SaaS Product',
    joinedAt: '30/06/2026',
    progress: '12%',
    spend: '399.000d',
  },
  {
    id: 'student-3',
    name: 'Le Thi D',
    email: 'lethid@email.com',
    course: 'AI Prompting thuc chien cho team van hanh',
    joinedAt: '29/06/2026',
    progress: '72%',
    spend: '279.000d',
  },
  {
    id: 'student-4',
    name: 'Vo Thi F',
    email: 'vothif@email.com',
    course: 'Lap trinh Web Full-Stack 2026',
    joinedAt: '28/06/2026',
    progress: '100%',
    spend: '499.000d',
  },
];

const InstructorStudentsPage: React.FC = () => {
  const [query, setQuery] = useState('');

  const filteredStudents = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    return students.filter((student) => {
      if (!normalized) return true;
      return (
        student.name.toLowerCase().includes(normalized) ||
        student.email.toLowerCase().includes(normalized) ||
        student.course.toLowerCase().includes(normalized)
      );
    });
  }, [query]);

  return (
    <InstructorWorkspaceLayout
      title="Hoc vien"
      description="Theo doi danh sach hoc vien, tien do hoc tap va gia tri don hang de uu tien cac can thiep dung luc."
      actions={
        <button className="inline-flex h-11 items-center justify-center gap-2 rounded-xl border border-[#E5E7EB] bg-white px-4 text-sm font-semibold text-[#374151] transition hover:bg-[#F9FAFB]">
          <Download className="h-4 w-4" />
          Xuat CSV
        </button>
      }
    >
      <div className="space-y-6">
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <InstructorStatCard label="Tong hoc vien" value="13.5K" note="Tang 6.2% tuan nay" icon={Users} />
          <InstructorStatCard
            label="Hoc vien moi"
            value="128"
            note="84 hoc vien den tu chien dich hien tai"
            icon={UserPlus}
            toneClassName="bg-[#FFF1F5] text-lh-pink"
          />
          <InstructorStatCard
            label="Ty le quay lai"
            value="67%"
            note="Tuan nay co 1.238 hoc vien hoc tiep"
            icon={Mail}
            toneClassName="bg-[#F4F2F8] text-lh-purple"
          />
          <InstructorStatCard
            label="Gia tri trung binh"
            value="421.000d"
            note="Moi hoc vien trong 30 ngay"
            icon={Wallet}
            toneClassName="bg-[#FFF5E8] text-[#B76E14]"
          />
        </div>

        <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <div className="text-sm font-medium text-[#6B7280]">Danh sach</div>
              <h2 className="mt-2 font-inter text-2xl font-semibold tracking-[-0.02em] text-[#111827]">
                Hoc vien gan day va tien do hien tai
              </h2>
            </div>
            <div className="relative w-full sm:w-[320px]">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[#9CA3AF]" />
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Tim theo ten, email hoac khoa hoc..."
                className="h-11 w-full rounded-xl border border-[#E5E7EB] bg-white pl-9 pr-3 text-sm text-[#111827] outline-none transition placeholder:text-[#9CA3AF] focus:border-[#D9DEF2] focus:ring-4 focus:ring-[#EEF2FF]"
              />
            </div>
          </div>

          <div className="mt-6 overflow-x-auto">
            <table className="min-w-full divide-y divide-[#EEF2F7]">
              <thead>
                <tr className="text-left text-xs font-semibold text-[#6B7280]">
                  <th className="pb-3 pr-4">Hoc vien</th>
                  <th className="pb-3 pr-4">Khoa hoc</th>
                  <th className="pb-3 pr-4">Ngay mua</th>
                  <th className="pb-3 pr-4">Tien do</th>
                  <th className="pb-3">Gia tri</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#F3F4F6]">
                {filteredStudents.map((student) => (
                  <tr key={student.id} className="text-sm text-[#111827]">
                    <td className="py-4 pr-4">
                      <div className="font-medium">{student.name}</div>
                      <div className="mt-1 text-xs text-[#6B7280]">{student.email}</div>
                    </td>
                    <td className="py-4 pr-4 text-[#4B5563]">{student.course}</td>
                    <td className="py-4 pr-4 text-[#4B5563]">{student.joinedAt}</td>
                    <td className="py-4 pr-4">
                      <span className="rounded-full bg-[#EEF2FF] px-2.5 py-1 text-xs font-medium text-lh-blue">
                        {student.progress}
                      </span>
                    </td>
                    <td className="py-4 font-medium">{student.spend}</td>
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

export default InstructorStudentsPage;
