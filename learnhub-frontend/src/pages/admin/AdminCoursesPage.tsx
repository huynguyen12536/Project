import React, { useMemo, useState } from 'react';
import { ArrowUpRight, BookCopy, CircleDashed, ShieldCheck, Sparkles } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import AdminWorkspaceLayout from '../../components/layouts/AdminWorkspaceLayout';
import { adminCourseReviews } from '../../data/adminOperations';
import { cn } from '../../lib/cn';

const courseTabs = ['Tat ca', 'Cho duyet', 'Can sua', 'Da xuat ban', 'Ban nhap'] as const;

const AdminCoursesPage: React.FC = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<(typeof courseTabs)[number]>('Tat ca');

  const visibleCourses = useMemo(() => {
    return adminCourseReviews.filter((course) => activeTab === 'Tat ca' || course.status === activeTab);
  }, [activeTab]);

  return (
    <AdminWorkspaceLayout
      title="Dieu phoi noi dung khoa hoc"
      description="Review queue xuat ban, theo doi cac khoa hoc can sua va giu chat luong catalog thong nhat truoc khi mo ban."
      actions={
        <button
          onClick={() => navigate('/admin/taxonomy')}
          className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-lh-blue px-4 text-sm font-semibold text-white transition hover:bg-lh-navy"
        >
          <ShieldCheck className="h-4 w-4" />
          Mo taxonomy editor
        </button>
      }
    >
      <div className="space-y-6">
        <div className="flex flex-wrap gap-2">
          {courseTabs.map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={cn(
                'rounded-full px-4 py-2 text-sm font-medium transition',
                activeTab === tab ? 'bg-[#111827] text-white' : 'bg-[#F3F4F6] text-[#4B5563] hover:bg-[#E5E7EB]'
              )}
            >
              {tab}
            </button>
          ))}
        </div>

        <div className="grid gap-6 xl:grid-cols-[minmax(0,1.2fr)_340px]">
          <div className="space-y-4">
            {visibleCourses.map((course) => (
              <div key={course.id} className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
                <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="rounded-full bg-[#EEF2FF] px-2.5 py-1 text-[11px] font-semibold text-lh-blue">
                        {course.status}
                      </span>
                      <span className="rounded-full bg-[#F3F4F6] px-2.5 py-1 text-[11px] font-semibold text-[#4B5563]">
                        {course.category}
                      </span>
                    </div>
                    <h2 className="mt-3 text-xl font-semibold tracking-[-0.02em] text-[#111827]">{course.title}</h2>
                    <div className="mt-2 text-sm text-[#6B7280]">Giang vien: {course.instructor} · Cap nhat {course.updatedAt}</div>
                    <div className="mt-4 flex flex-wrap gap-2">
                      {course.issues.map((issue) => (
                        <span key={issue} className="rounded-full bg-[#FFF5E8] px-3 py-1 text-xs font-medium text-[#B76E14]">
                          {issue}
                        </span>
                      ))}
                    </div>
                  </div>

                  <button className="inline-flex h-10 items-center gap-2 rounded-xl border border-[#D9DEF2] bg-white px-3.5 text-sm font-semibold text-lh-blue transition hover:bg-[#F8FAFF]">
                    Mo chi tiet
                    <ArrowUpRight className="h-4 w-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>

          <div className="space-y-4">
            <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
              <div className="flex items-center gap-3 text-sm font-medium text-[#6B7280]">
                <BookCopy className="h-4 w-4 text-lh-blue" />
                Moderation policy
              </div>
              <div className="mt-4 space-y-3 text-sm leading-6 text-[#4B5563]">
                <p>1. Uu tien review khoa hoc moi trong 24 gio.</p>
                <p>2. Bat buoc co preview, thumbnail dung guideline va 1 lesson mo ta ro ket qua dau ra.</p>
                <p>3. Danh sach "Can sua" phai co comment cu the de giang vien xu ly trong 1 vong lap.</p>
              </div>
            </div>

            <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
              <div className="flex items-center gap-3 text-sm font-medium text-[#6B7280]">
                <Sparkles className="h-4 w-4 text-lh-pink" />
                Queue summary
              </div>
              <div className="mt-4 space-y-3">
                <div className="flex items-center justify-between rounded-2xl bg-[#FBFCFE] px-4 py-3">
                  <span className="text-sm text-[#4B5563]">Cho duyet</span>
                  <span className="text-sm font-semibold text-[#111827]">07</span>
                </div>
                <div className="flex items-center justify-between rounded-2xl bg-[#FBFCFE] px-4 py-3">
                  <span className="text-sm text-[#4B5563]">Can sua</span>
                  <span className="text-sm font-semibold text-[#111827]">05</span>
                </div>
                <div className="flex items-center justify-between rounded-2xl bg-[#FBFCFE] px-4 py-3">
                  <span className="text-sm text-[#4B5563]">Qua SLA</span>
                  <span className="text-sm font-semibold text-[#111827]">02</span>
                </div>
              </div>
            </div>

            <div className="rounded-2xl border border-dashed border-[#D9DEF2] bg-[#EEF2FF]/40 p-6 shadow-sm">
              <div className="flex items-center gap-3 text-sm font-medium text-lh-blue">
                <CircleDashed className="h-4 w-4" />
                Ghi chu van hanh
              </div>
              <p className="mt-3 text-sm leading-6 text-[#4B5563]">
                Khi chat luong catalog khong dong deu, completion va conversion se giam rat nhanh. Queue moderation nay can duoc du tri nhu mot lane rieng.
              </p>
            </div>
          </div>
        </div>
      </div>
    </AdminWorkspaceLayout>
  );
};

export default AdminCoursesPage;
