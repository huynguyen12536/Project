import {
  AlertTriangle,
  BookCopy,
  CreditCard,
  ServerCog,
  ShieldCheck,
  UserRoundCheck,
} from 'lucide-react';

export const adminOverviewStats = [
  {
    label: 'Nguoi dung hoat dong',
    value: '28.4K',
    note: '+1.2K trong 7 ngay qua',
    icon: UserRoundCheck,
    toneClassName: 'bg-[#EEF2FF] text-lh-blue',
  },
  {
    label: 'GMV 30 ngay',
    value: '1.84 ty',
    note: '42 don cho doi doi soat',
    icon: CreditCard,
    toneClassName: 'bg-[#F4F2F8] text-lh-purple',
  },
  {
    label: 'Khoa hoc can duyet',
    value: '19',
    note: '5 khoa hoc qua SLA 24h',
    icon: BookCopy,
    toneClassName: 'bg-[#FFF5E8] text-[#B76E14]',
  },
  {
    label: 'Canh bao he thong',
    value: '03',
    note: '1 service dang suy giam',
    icon: AlertTriangle,
    toneClassName: 'bg-[#FFF1F5] text-lh-pink',
  },
] as const;

export const adminGrowthData = [
  { label: 'T2', signups: 420, paidOrders: 188, refunds: 12 },
  { label: 'T3', signups: 468, paidOrders: 214, refunds: 16 },
  { label: 'T4', signups: 512, paidOrders: 239, refunds: 18 },
  { label: 'T5', signups: 575, paidOrders: 274, refunds: 22 },
  { label: 'T6', signups: 621, paidOrders: 302, refunds: 19 },
  { label: 'T7', signups: 688, paidOrders: 327, refunds: 24 },
];

export const adminPriorityQueue = [
  {
    id: 'queue-1',
    title: '8 yeu cau hoan tien can phe duyet',
    meta: 'Thanh toan · SLA 2 gio',
    badge: 'Cao',
  },
  {
    id: 'queue-2',
    title: '5 ho so giang vien moi chua review',
    meta: 'Nguoi dung · SLA 24 gio',
    badge: 'Moi',
  },
  {
    id: 'queue-3',
    title: 'Hang doi OTP co 17 job retry',
    meta: 'Notification · Retry lane',
    badge: 'Theo doi',
  },
  {
    id: 'queue-4',
    title: 'Search index tre 12 phut so voi ingest',
    meta: 'He thong · Can dong bo',
    badge: 'Canh bao',
  },
] as const;

export const adminActivityFeed = [
  { id: 'activity-1', title: 'Instructor Nguyen Minh da gui khoa hoc "Data cho Product Team" len review.', meta: '7 phut truoc' },
  { id: 'activity-2', title: 'Don hang #ORD-2048 duoc dispute do giao dich that bai lan 2.', meta: '14 phut truoc' },
  { id: 'activity-3', title: 'User support@learnhub.vn da mo lai 23 tai khoan OTP pending.', meta: '26 phut truoc' },
  { id: 'activity-4', title: 'Redis queue tro lai binh thuong sau khi xu ly batch resend OTP.', meta: '41 phut truoc' },
] as const;

export const adminUsers = [
  {
    id: 'usr-1001',
    name: 'Nguyen Van A',
    email: 'nguyenvana@example.com',
    role: 'Hoc vien',
    segment: 'student',
    status: 'Hoat dong',
    spending: '1.240.000d',
    joinedAt: '2026-06-28',
    risk: 'Thap',
  },
  {
    id: 'usr-1002',
    name: 'Tran Thi B',
    email: 'tranthib@example.com',
    role: 'Giang vien',
    segment: 'instructor',
    status: 'Cho duyet',
    spending: '0d',
    joinedAt: '2026-06-29',
    risk: 'Trung binh',
  },
  {
    id: 'usr-1003',
    name: 'Le Hoang C',
    email: 'lehoangc@example.com',
    role: 'Hoc vien',
    segment: 'student',
    status: 'OTP pending',
    spending: '329.000d',
    joinedAt: '2026-06-30',
    risk: 'Thap',
  },
  {
    id: 'usr-1004',
    name: 'Pham My D',
    email: 'phammyd@example.com',
    role: 'Giang vien',
    segment: 'instructor',
    status: 'Tam khoa',
    spending: '0d',
    joinedAt: '2026-06-27',
    risk: 'Cao',
  },
  {
    id: 'usr-1005',
    name: 'Vu Quoc E',
    email: 'vuquoce@example.com',
    role: 'Hoc vien',
    segment: 'student',
    status: 'Hoat dong',
    spending: '879.000d',
    joinedAt: '2026-06-25',
    risk: 'Thap',
  },
] as const;

export const adminCourseReviews = [
  {
    id: 'course-901',
    title: 'Data cho Product Team',
    instructor: 'Nguyen Minh',
    status: 'Cho duyet',
    category: 'Data',
    updatedAt: '12 phut truoc',
    issues: ['Can check mo ta bai 3', 'Thumbnail chua dung guideline'],
  },
  {
    id: 'course-902',
    title: 'UX Writing cho SaaS',
    instructor: 'Le Thu Ha',
    status: 'Da xuat ban',
    category: 'Design',
    updatedAt: '48 phut truoc',
    issues: ['Ty le completion giam 8%'],
  },
  {
    id: 'course-903',
    title: 'AI Prompting cho Support Ops',
    instructor: 'Tran Gia Bao',
    status: 'Can sua',
    category: 'AI',
    updatedAt: '1 gio truoc',
    issues: ['Transcript video 2 loi', 'Quiz chapter 1 trung dap an'],
  },
  {
    id: 'course-904',
    title: 'Spring Security cho Enterprise',
    instructor: 'Pham Duc Anh',
    status: 'Ban nhap',
    category: 'Backend',
    updatedAt: '3 gio truoc',
    issues: ['Chua co lesson preview'],
  },
] as const;

export const adminOrders = [
  {
    id: 'ORD-2048',
    user: 'Nguyen Van A',
    course: 'Lap trinh Web Full-Stack 2026',
    amount: '499.000d',
    payment: 'Visa',
    status: 'Da thanh toan',
    settlement: 'Da doi soat',
  },
  {
    id: 'ORD-2052',
    user: 'Tran Thi B',
    course: 'UX Writing cho SaaS',
    amount: '389.000d',
    payment: 'Momo',
    status: 'Cho xac nhan',
    settlement: 'Dang xu ly',
  },
  {
    id: 'ORD-2059',
    user: 'Le Hoang C',
    course: 'AI Prompting cho Support Ops',
    amount: '279.000d',
    payment: 'Visa',
    status: 'Hoan tien',
    settlement: 'Can phe duyet',
  },
  {
    id: 'ORD-2064',
    user: 'Vu Quoc E',
    course: 'Data cho Product Team',
    amount: '459.000d',
    payment: 'Banking',
    status: 'Tranh chap',
    settlement: 'Can dieu tra',
  },
] as const;

export const adminSystemHealth = [
  {
    id: 'svc-api',
    label: 'API gateway',
    status: 'Healthy',
    latency: '148ms',
    note: '99.98% uptime 7 ngay',
    icon: ShieldCheck,
    toneClassName: 'bg-[#EEF8F2] text-[#1F7A45]',
  },
  {
    id: 'svc-queue',
    label: 'Notification queue',
    status: 'Degraded',
    latency: '1.8m retry lag',
    note: '17 OTP jobs dang retry',
    icon: ServerCog,
    toneClassName: 'bg-[#FFF5E8] text-[#B76E14]',
  },
  {
    id: 'svc-payment',
    label: 'Payment webhook',
    status: 'Healthy',
    latency: '312ms',
    note: '2 event can replay',
    icon: CreditCard,
    toneClassName: 'bg-[#EEF2FF] text-lh-blue',
  },
] as const;

export const adminAuditLogs = [
  { id: 'log-1', actor: 'admin@learnhub.local', action: 'Approve instructor profile', target: 'usr-1002', timestamp: '2026-07-01 08:14' },
  { id: 'log-2', actor: 'ops@learnhub.local', action: 'Replay payment webhook', target: 'ORD-2052', timestamp: '2026-07-01 08:03' },
  { id: 'log-3', actor: 'system', action: 'Retry OTP notification batch', target: 'notification-lane', timestamp: '2026-07-01 07:56' },
  { id: 'log-4', actor: 'admin@learnhub.local', action: 'Suspend account', target: 'usr-1004', timestamp: '2026-07-01 07:42' },
] as const;
