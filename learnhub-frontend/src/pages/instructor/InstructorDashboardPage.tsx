import React, { useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import {
    BarChart3,
    BookOpen,
    ChevronRight,
    TrendingUp,
    Users,
    Star,
    Wallet,
    Clock,
    ShoppingCart,
} from 'lucide-react';
import { useNavigate, useLocation } from 'react-router-dom';
import { cn } from '../../lib/cn';
import { instructorCoursesSeed } from '../../data/courseCatalog';

type RevenueData = {
    month: string;
    revenue: number;
};

type RecentActivity = {
    id: string;
    type: 'purchase' | 'enrollment' | 'review';
    user: string;
    course: string;
    time: string;
    amount?: string;
    rating?: number;
};

type CourseBuyer = {
    id: string;
    name: string;
    email: string;
    avatar?: string;
    course: string;
    purchaseDate: string;
    amount: string;
    status: 'active' | 'completed';
};

const sidebarItems: Array<{
    label: string;
    icon: typeof BookOpen;
    path: string;
}> = [
        { label: 'Tong quan', icon: BarChart3, path: '/instructor/dashboard' },
        { label: 'Khoa hoc cua toi', icon: BookOpen, path: '/instructor/courses' },
        { label: 'Bai hoc', icon: Clock, path: '/instructor/lessons' },
        { label: 'Hoc vien', icon: Users, path: '/instructor/students' },
        { label: 'Danh gia', icon: Star, path: '/instructor/reviews' },
    ];

const revenueData: RevenueData[] = [
    { month: 'T1', revenue: 45 },
    { month: 'T2', revenue: 52 },
    { month: 'T3', revenue: 48 },
    { month: 'T4', revenue: 65 },
    { month: 'T5', revenue: 78 },
    { month: 'T6', revenue: 92 },
];

const recentActivities: RecentActivity[] = [
    { id: 'act-1', type: 'purchase', user: 'Nguyen Van A', course: 'Lap trinh Web Full-Stack 2026', time: '5 phut truoc', amount: '499.000d' },
    { id: 'act-2', type: 'enrollment', user: 'Tran Thi B', course: 'He thong thiet ke cho SaaS Product', time: '12 phut truoc' },
    { id: 'act-3', type: 'review', user: 'Pham Van C', course: 'Lap trinh Web Full-Stack 2026', time: '1 gio truoc', rating: 5 },
    { id: 'act-4', type: 'purchase', user: 'Le Thi D', course: 'Lap trinh Web Full-Stack 2026', time: '2 gio truoc', amount: '499.000d' },
    { id: 'act-5', type: 'enrollment', user: 'Hoang Van E', course: 'He thong thiet ke cho SaaS Product', time: '3 gio truoc' },
];

const courseBuyers: CourseBuyer[] = [
    { id: 'buyer-1', name: 'Nguyen Van A', email: 'nguyenvana@email.com', course: 'Lap trinh Web Full-Stack 2026', purchaseDate: '2026-06-30', amount: '499.000d', status: 'active' },
    { id: 'buyer-2', name: 'Tran Thi B', email: 'tranthib@email.com', course: 'He thong thiet ke cho SaaS Product', purchaseDate: '2026-06-30', amount: '399.000d', status: 'active' },
    { id: 'buyer-3', name: 'Pham Van C', email: 'phamvanc@email.com', course: 'Lap trinh Web Full-Stack 2026', purchaseDate: '2026-06-29', amount: '499.000d', status: 'completed' },
    { id: 'buyer-4', name: 'Le Thi D', email: 'lethid@email.com', course: 'Lap trinh Web Full-Stack 2026', purchaseDate: '2026-06-29', amount: '499.000d', status: 'active' },
    { id: 'buyer-5', name: 'Hoang Van E', email: 'hoangvane@email.com', course: 'He thong thiet ke cho SaaS Product', purchaseDate: '2026-06-28', amount: '399.000d', status: 'active' },
];

const InstructorDashboardPage: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();

    const stats = useMemo(() => {
        return {
            revenue: '287.1tr',
            students: '13.5K',
            courses: '3',
            rating: '4.83',
        };
    }, []);

    const maxRevenue = Math.max(...revenueData.map(d => d.revenue));

    return (
        <div className="min-h-screen bg-[#F7F8FC]">
            <div className="mx-auto max-w-[1600px] px-6 py-6 xl:px-8">
                <div className="mb-6 flex items-center justify-between gap-4 rounded-[28px] border border-[#E7E9F2] bg-white px-6 py-5 shadow-[0_12px_32px_rgba(21,22,46,0.05)]">
                    <div className="flex items-center gap-4">
                        <button
                            onClick={() => navigate('/dashboard')}
                            className="inline-flex items-center gap-2 rounded-xl border border-[#E5E7EB] px-3.5 py-2 text-sm font-semibold text-[#374151] transition hover:bg-[#F9FAFB]"
                        >
                            <ChevronRight className="h-4 w-4 rotate-180" />
                            Quay lai Dashboard
                        </button>
                        <div>
                            <div className="text-xs font-bold uppercase tracking-[0.16em] text-lh-muted">Instructor studio</div>
                            <h1 className="mt-1 font-inter text-3xl font-semibold tracking-[-0.03em] text-[#111827]">
                                Tong quan doanh thu va hoat dong
                            </h1>
                        </div>
                    </div>
                </div>

                <div className="grid gap-6 xl:grid-cols-[260px_minmax(0,1fr)]">
                    <aside className="rounded-[28px] border border-[#E7E9F2] bg-white p-4 shadow-[0_12px_32px_rgba(21,22,46,0.04)]">
                        <div className="mb-4 px-3 py-2">
                            <div className="text-xs font-bold uppercase tracking-[0.16em] text-lh-muted">Giang vien</div>
                            <div className="mt-2 font-inter text-xl font-semibold text-[#111827]">Studio LearnHub</div>
                        </div>
                        <div className="space-y-1.5">
                            {sidebarItems.map((item) => {
                                const Icon = item.icon;
                                const isActive = location.pathname === item.path;
                                return (
                                    <button
                                        key={item.label}
                                        onClick={() => navigate(item.path)}
                                        className={cn(
                                            'flex w-full items-center gap-3 rounded-2xl px-4 py-3 text-left text-sm font-semibold transition',
                                            isActive
                                                ? 'bg-[#111827] text-white shadow-[0_10px_20px_rgba(17,24,39,0.12)]'
                                                : 'text-[#4B5563] hover:bg-[#F8FAFF] hover:text-[#111827]'
                                        )}
                                    >
                                        <Icon className="h-4.5 w-4.5" />
                                        {item.label}
                                    </button>
                                );
                            })}
                        </div>
                    </aside>

                    <div className="min-w-0 space-y-6">
                        <div className="grid gap-4 md:grid-cols-4">
                            {[
                                {
                                    label: 'Tong doanh thu',
                                    value: stats.revenue,
                                    note: '+12.4tr trong 30 ngay qua',
                                    icon: Wallet,
                                    tone: 'bg-[#EEF2FF] text-lh-blue',
                                    trend: '+15.2%',
                                },
                                {
                                    label: 'Tong hoc vien',
                                    value: stats.students,
                                    note: '1.238 hoc vien dang hoc tuan nay',
                                    icon: Users,
                                    tone: 'bg-[#FFF1F5] text-lh-pink',
                                    trend: '+8.7%',
                                },
                                {
                                    label: 'So khoa hoc',
                                    value: stats.courses,
                                    note: '2 khoa hoc da xuat ban',
                                    icon: BookOpen,
                                    tone: 'bg-[#F4F2F8] text-lh-purple',
                                    trend: '+1',
                                },
                                {
                                    label: 'Xep hang trung binh',
                                    value: stats.rating,
                                    note: 'Duoc tinh tren toan bo khoa hoc',
                                    icon: Star,
                                    tone: 'bg-[#FFF5E8] text-[#B76E14]',
                                    trend: '+0.2',
                                },
                            ].map((stat) => {
                                const Icon = stat.icon;
                                return (
                                    <div
                                        key={stat.label}
                                        className="rounded-[24px] border border-[#E7E9F2] bg-white p-6 shadow-[0_12px_32px_rgba(21,22,46,0.04)]"
                                    >
                                        <div className="flex items-start justify-between gap-4">
                                            <div>
                                                <div className="text-sm font-medium text-[#6B7280]">{stat.label}</div>
                                                <div className="mt-3 font-inter text-[32px] font-semibold tracking-[-0.03em] text-[#111827]">
                                                    {stat.value}
                                                </div>
                                            </div>
                                            <div className={cn('rounded-2xl p-3', stat.tone)}>
                                                <Icon className="h-5 w-5" />
                                            </div>
                                        </div>
                                        <div className="mt-4 flex items-center justify-between">
                                            <div className="text-sm leading-6 text-[#6B7280]">{stat.note}</div>
                                            <div className="inline-flex items-center gap-1 rounded-full bg-[#EEF8F2] px-2 py-1 text-[11px] font-semibold text-[#1F7A45]">
                                                <TrendingUp className="h-3 w-3" />
                                                {stat.trend}
                                            </div>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>

                        <div className="grid gap-6 lg:grid-cols-3">
                            <div className="lg:col-span-2 rounded-[28px] border border-[#E7E9F2] bg-white p-6 shadow-[0_12px_32px_rgba(21,22,46,0.04)]">
                                <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                                    <div>
                                        <div className="text-sm font-medium text-[#6B7280]">Doanh thu</div>
                                        <h2 className="mt-2 font-inter text-2xl font-semibold tracking-[-0.02em] text-[#111827]">
                                            Bieu do doanh thu 6 thang qua
                                        </h2>
                                    </div>
                                </div>
                                <div className="mt-6 h-64">
                                    <div className="flex h-full items-end gap-3 px-2">
                                        {revenueData.map((data, index) => (
                                            <motion.div
                                                key={data.month}
                                                initial={{ height: 0 }}
                                                animate={{ height: `${(data.revenue / maxRevenue) * 100}%` }}
                                                transition={{ duration: 0.5, delay: index * 0.05 }}
                                                className="flex-1 flex flex-col items-center gap-2"
                                            >
                                                <div className="w-full rounded-t-xl bg-gradient-to-t from-lh-blue to-[#6B6BF0]" />
                                                <div className="text-xs font-semibold text-[#6B7280]">{data.month}</div>
                                            </motion.div>
                                        ))}
                                    </div>
                                </div>
                            </div>

                            <div className="rounded-[28px] border border-[#E7E9F2] bg-white p-6 shadow-[0_12px_32px_rgba(21,22,46,0.04)]">
                                <div>
                                    <div className="text-sm font-medium text-[#6B7280]">Hoat dong gan day</div>
                                    <h2 className="mt-2 font-inter text-2xl font-semibold tracking-[-0.02em] text-[#111827]">
                                        Cac hoat dong moi nhat
                                    </h2>
                                </div>
                                <div className="mt-6 space-y-4">
                                    {recentActivities.map((activity) => {
                                        let icon, tone;
                                        if (activity.type === 'purchase') {
                                            icon = ShoppingCart;
                                            tone = 'bg-[#EEF8F2] text-[#1F7A45]';
                                        } else if (activity.type === 'enrollment') {
                                            icon = Users;
                                            tone = 'bg-[#EEF2FF] text-lh-blue';
                                        } else {
                                            icon = Star;
                                            tone = 'bg-[#FFF5E8] text-[#B76E14]';
                                        }
                                        const Icon = icon;
                                        return (
                                            <div key={activity.id} className="flex items-start gap-3">
                                                <div className={cn('mt-0.5 flex h-8 w-8 items-center justify-center rounded-xl', tone)}>
                                                    <Icon className="h-4 w-4" />
                                                </div>
                                                <div className="flex-1 min-w-0">
                                                    <div className="text-sm font-semibold text-[#111827] truncate">
                                                        {activity.user}
                                                    </div>
                                                    <div className="text-xs text-[#6B7280] truncate">
                                                        {activity.type === 'purchase'
                                                            ? `Da mua ${activity.course}`
                                                            : activity.type === 'enrollment'
                                                                ? `Da dang ky ${activity.course}`
                                                                : `Da danh gia ${activity.course}`
                                                        }
                                                        {activity.amount && ` - ${activity.amount}`}
                                                        {activity.rating && ` (${activity.rating}★)`}
                                                    </div>
                                                    <div className="mt-1 text-[11px] text-[#9CA3AF]">{activity.time}</div>
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            </div>
                        </div>

                        <div className="rounded-[28px] border border-[#E7E9F2] bg-white p-6 shadow-[0_12px_32px_rgba(21,22,46,0.04)]">
                            <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                                <div>
                                    <div className="text-sm font-medium text-[#6B7280]">Hoc vien</div>
                                    <h2 className="mt-2 font-inter text-2xl font-semibold tracking-[-0.02em] text-[#111827]">
                                        Danh sach nguoi da mua khoa hoc
                                    </h2>
                                </div>
                            </div>
                            <div className="mt-6 overflow-x-auto">
                                <table className="w-full">
                                    <thead>
                                        <tr className="border-b border-[#ECEFF5]">
                                            <th className="pb-4 text-left text-xs font-semibold uppercase tracking-[0.14em] text-[#9CA3AF]">Hoc vien</th>
                                            <th className="pb-4 text-left text-xs font-semibold uppercase tracking-[0.14em] text-[#9CA3AF]">Khoa hoc</th>
                                            <th className="pb-4 text-left text-xs font-semibold uppercase tracking-[0.14em] text-[#9CA3AF]">Ngay mua</th>
                                            <th className="pb-4 text-left text-xs font-semibold uppercase tracking-[0.14em] text-[#9CA3AF]">So tien</th>
                                            <th className="pb-4 text-left text-xs font-semibold uppercase tracking-[0.14em] text-[#9CA3AF]">Trang thai</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-[#ECEFF5]">
                                        {courseBuyers.map((buyer) => (
                                            <tr key={buyer.id} className="transition hover:bg-[#F8FAFF]">
                                                <td className="py-4">
                                                    <div className="flex items-center gap-3">
                                                        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[#EEF2FF] text-lh-blue font-semibold">
                                                            {buyer.name.charAt(0)}
                                                        </div>
                                                        <div>
                                                            <div className="text-sm font-semibold text-[#111827]">{buyer.name}</div>
                                                            <div className="text-xs text-[#6B7280]">{buyer.email}</div>
                                                        </div>
                                                    </div>
                                                </td>
                                                <td className="py-4 text-sm text-[#111827]">{buyer.course}</td>
                                                <td className="py-4 text-sm text-[#6B7280]">{buyer.purchaseDate}</td>
                                                <td className="py-4 text-sm font-semibold text-[#111827]">{buyer.amount}</td>
                                                <td className="py-4">
                                                    <span className={cn(
                                                        'inline-flex rounded-full px-3 py-1 text-[11px] font-semibold',
                                                        buyer.status === 'active'
                                                            ? 'bg-[#EEF8F2] text-[#1F7A45]'
                                                            : 'bg-[#F4F2F8] text-lh-purple'
                                                    )}>
                                                        {buyer.status === 'active' ? 'Dang hoc' : 'Hoan thanh'}
                                                    </span>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default InstructorDashboardPage;
