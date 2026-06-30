import React, { useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import {
  ArrowUpRight,
  BadgeCheck,
  Clock3,
  Compass,
  CreditCard,
  PackageCheck,
  ShoppingCart,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { cn } from '../lib/cn';
import { DataTable } from '../ui-kit/DataTable';
import type { Column, SelectOption } from '../ui-kit';
import { SearchInput, Select } from '../ui-kit';

type OrderStatus = 'PAID' | 'PROCESSING' | 'PENDING_ACCESS';

type OrderRecord = {
  orderId: string;
  courseId: number;
  courseTitle: string;
  instructor: string;
  createdAt: string;
  amount: number;
  status: OrderStatus;
  accessLabel: string;
};

const orderRows: OrderRecord[] = [
  {
    orderId: 'LH-2026-1042',
    courseId: 1,
    courseTitle: 'Lap trinh Web Full-Stack 2026',
    instructor: 'Tran Minh Quan',
    createdAt: '2026-06-28',
    amount: 499000,
    status: 'PAID',
    accessLabel: 'Dang hoc',
  },
  {
    orderId: 'LH-2026-1031',
    courseId: 3,
    courseTitle: 'Phan tich du lieu voi Python',
    instructor: 'Pham Duc Anh',
    createdAt: '2026-06-21',
    amount: 599000,
    status: 'PAID',
    accessLabel: 'Da mo quyen',
  },
  {
    orderId: 'LH-2026-1054',
    courseId: 7,
    courseTitle: 'He thong thiet ke cho SaaS Product',
    instructor: 'Le Thu Ha',
    createdAt: '2026-06-30',
    amount: 329000,
    status: 'PROCESSING',
    accessLabel: 'Dang xu ly thanh toan',
  },
  {
    orderId: 'LH-2026-1056',
    courseId: 9,
    courseTitle: 'DevOps can ban cho backend engineer',
    instructor: 'Nguyen Hoang',
    createdAt: '2026-06-30',
    amount: 449000,
    status: 'PENDING_ACCESS',
    accessLabel: 'Cho cap quyen hoc',
  },
];

const statusOptions: SelectOption[] = [
  { value: 'ALL', label: 'Tat ca trang thai' },
  { value: 'PAID', label: 'Da thanh toan' },
  { value: 'PROCESSING', label: 'Dang xu ly' },
  { value: 'PENDING_ACCESS', label: 'Cho kich hoat' },
];

const statusTone: Record<OrderStatus, string> = {
  PAID: 'bg-[#EEF8F2] text-[#1F7A45]',
  PROCESSING: 'bg-[#EEF2FF] text-[#3758C8]',
  PENDING_ACCESS: 'bg-[#FFF5E8] text-[#B76E14]',
};

const statusLabel: Record<OrderStatus, string> = {
  PAID: 'Da thanh toan',
  PROCESSING: 'Dang xu ly',
  PENDING_ACCESS: 'Cho kich hoat',
};

const cardMotion = {
  initial: { opacity: 0, y: 18 },
  animate: { opacity: 1, y: 0 },
};

const statCards = [
  {
    label: 'Don da thanh toan',
    value: orderRows.filter((row) => row.status === 'PAID').length,
    note: 'Khoa hoc co the vao hoc ngay',
    icon: BadgeCheck,
    tone: 'bg-[#EEF8F2] text-[#1F7A45]',
  },
  {
    label: 'Don dang xu ly',
    value: orderRows.filter((row) => row.status === 'PROCESSING').length,
    note: 'Dang doi xac nhan thanh toan',
    icon: Clock3,
    tone: 'bg-[#EEF2FF] text-[#3758C8]',
  },
  {
    label: 'Cho kich hoat',
    value: orderRows.filter((row) => row.status === 'PENDING_ACCESS').length,
    note: 'Se mo quyen sau khi he thong dong bo',
    icon: PackageCheck,
    tone: 'bg-[#FFF5E8] text-[#B76E14]',
  },
];

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(value);
}

const OrdersPage: React.FC = () => {
  const navigate = useNavigate();
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [query, setQuery] = useState('');

  const filteredOrders = useMemo(() => {
    return orderRows.filter((row) => {
      const matchesStatus = statusFilter === 'ALL' || row.status === statusFilter;
      const normalizedQuery = query.trim().toLowerCase();
      const matchesQuery =
        normalizedQuery.length === 0 ||
        row.orderId.toLowerCase().includes(normalizedQuery) ||
        row.courseTitle.toLowerCase().includes(normalizedQuery) ||
        String(row.courseId).includes(normalizedQuery);

      return matchesStatus && matchesQuery;
    });
  }, [query, statusFilter]);

  const columns: Column<OrderRecord>[] = [
    {
      key: 'orderId',
      header: 'Ma don',
      sortable: true,
      accessor: (row) => <span className="font-semibold text-[#111827]">{row.orderId}</span>,
    },
    {
      key: 'course',
      header: 'Khoa hoc',
      sortable: true,
      sortValue: (row) => row.courseTitle,
      accessor: (row) => (
        <div>
          <div className="font-semibold text-[#111827]">{row.courseTitle}</div>
          <div className="mt-1 text-xs text-[#6B7280]">
            ID khoa hoc: {row.courseId} · {row.instructor}
          </div>
        </div>
      ),
    },
    {
      key: 'createdAt',
      header: 'Ngay mua',
      sortable: true,
      accessor: (row) => <span className="text-[#4B5563]">{row.createdAt}</span>,
    },
    {
      key: 'amount',
      header: 'Gia tri',
      sortable: true,
      sortValue: (row) => row.amount,
      accessor: (row) => <span className="font-semibold text-[#111827]">{formatCurrency(row.amount)}</span>,
      className: 'whitespace-nowrap',
    },
    {
      key: 'status',
      header: 'Trang thai',
      sortable: true,
      sortValue: (row) => statusLabel[row.status],
      accessor: (row) => (
        <span className={cn('inline-flex rounded-full px-2.5 py-1 text-xs font-semibold', statusTone[row.status])}>
          {statusLabel[row.status]}
        </span>
      ),
    },
    {
      key: 'accessLabel',
      header: 'Tinh trang truy cap',
      accessor: (row) => <span className="text-[#4B5563]">{row.accessLabel}</span>,
    },
    {
      key: 'action',
      header: '',
      accessor: (row) => (
        <button
          onClick={() =>
            navigate(row.status === 'PAID' ? `/learn/courses/${row.courseId}` : `/courses/${row.courseId}`)
          }
          className="inline-flex items-center gap-2 rounded-lg border border-[#D9DEF2] px-3 py-2 text-xs font-semibold text-lh-blue transition hover:bg-[#F8FAFF]"
        >
          {row.status === 'PAID' ? 'Vao hoc' : 'Xem khoa hoc'}
          <ArrowUpRight className="h-3.5 w-3.5" />
        </button>
      ),
      className: 'whitespace-nowrap text-right',
    },
  ];

  return (
    <div className="min-h-full bg-[#F9FAFB]">
      <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
        <motion.section
          variants={cardMotion}
          initial="initial"
          animate="animate"
          transition={{ duration: 0.22, ease: 'easeOut' }}
          className="mb-6"
        >
          <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <div className="inline-flex items-center gap-2 rounded-full border border-[#E5E7EB] bg-white px-3 py-1 text-xs font-semibold text-[#6B7280]">
                <ShoppingCart className="h-3.5 w-3.5 text-lh-blue" />
                Don hang va quyen truy cap
              </div>
              <h1 className="mt-4 font-inter text-[28px] font-semibold tracking-[-0.02em] text-[#111827] sm:text-[32px]">
                Theo doi khoa hoc da mua
              </h1>
              <p className="mt-2 max-w-3xl text-sm leading-6 text-[#6B7280]">
                Trang nay chi hien thi cac don hang cua rieng ban, bao gom khoa hoc da thanh toan, dang xu ly va dang cho mo quyen.
              </p>
            </div>
            <button
              onClick={() => navigate('/courses')}
              className="inline-flex h-11 items-center justify-center gap-2 rounded-xl border border-[#D9DEF2] bg-white px-5 text-sm font-semibold text-lh-blue transition hover:bg-[#F8FAFF]"
            >
              <Compass className="h-4 w-4" />
              Kham pha them khoa hoc
            </button>
          </div>
        </motion.section>

        <motion.section
          variants={cardMotion}
          initial="initial"
          animate="animate"
          transition={{ duration: 0.24, delay: 0.04, ease: 'easeOut' }}
          className="grid gap-4 sm:grid-cols-3"
        >
          {statCards.map((stat) => {
            const Icon = stat.icon;
            return (
              <div
                key={stat.label}
                className="rounded-2xl border border-[#E9ECF3] bg-white p-6 shadow-[0_1px_2px_rgba(16,24,40,0.02),0_10px_24px_rgba(16,24,40,0.04)]"
              >
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <div className="text-sm font-medium text-[#6B7280]">{stat.label}</div>
                    <div className="mt-3 font-inter text-[30px] font-semibold tracking-[-0.02em] text-[#111827]">
                      {stat.value}
                    </div>
                  </div>
                  <div className={cn('rounded-xl p-3', stat.tone)}>
                    <Icon className="h-5 w-5" />
                  </div>
                </div>
                <div className="mt-4 text-sm leading-6 text-[#6B7280]">{stat.note}</div>
              </div>
            );
          })}
        </motion.section>

        <motion.section
          variants={cardMotion}
          initial="initial"
          animate="animate"
          transition={{ duration: 0.28, delay: 0.08, ease: 'easeOut' }}
          className="mt-6 rounded-2xl border border-[#E9ECF3] bg-white p-6 shadow-[0_1px_2px_rgba(16,24,40,0.02),0_10px_24px_rgba(16,24,40,0.04)]"
        >
          <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <div className="text-sm font-medium text-[#6B7280]">Danh sach don hang</div>
              <h2 className="mt-2 font-inter text-2xl font-semibold tracking-[-0.02em] text-[#111827]">
                Don mua khoa hoc cua ban
              </h2>
            </div>
            <div className="grid gap-3 sm:grid-cols-[minmax(0,280px)_220px]">
              <SearchInput
                placeholder="Tim theo ma don, ID khoa hoc, ten khoa hoc..."
                debounceMs={200}
                onSearch={setQuery}
              />
              <Select
                value={statusFilter}
                onChange={(value) => setStatusFilter(value)}
                options={statusOptions}
                placeholder="Loc trang thai"
                searchable={false}
              />
            </div>
          </div>

          <div className="mt-5 flex flex-wrap items-center gap-2 text-xs text-[#6B7280]">
            <div className="inline-flex items-center gap-2 rounded-full border border-[#E5E7EB] bg-[#F9FAFB] px-3 py-1.5">
              <CreditCard className="h-3.5 w-3.5 text-lh-blue" />
              {filteredOrders.length} don dang hien thi
            </div>
            <div className="inline-flex items-center gap-2 rounded-full border border-[#E5E7EB] bg-[#F9FAFB] px-3 py-1.5">
              <PackageCheck className="h-3.5 w-3.5 text-[#1F7A45]" />
              {orderRows.filter((row) => row.status === 'PAID').length} khoa hoc co the vao hoc ngay
            </div>
          </div>

          <div className="mt-6">
            <DataTable
              data={filteredOrders}
              columns={columns}
              rowKey={(row) => row.orderId}
              emptyMessage="Khong tim thay don hang phu hop voi bo loc hien tai."
            />
          </div>
        </motion.section>
      </div>
    </div>
  );
};

export default OrdersPage;
