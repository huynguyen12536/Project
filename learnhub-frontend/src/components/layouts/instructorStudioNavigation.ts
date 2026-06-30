import {
  BookOpen,
  ChartNoAxesColumn,
  LayoutDashboard,
  Settings,
  Users,
} from 'lucide-react';

export type InstructorStudioNavItem = {
  label: string;
  path: string;
  icon: typeof LayoutDashboard;
};

export const instructorStudioNavItems: InstructorStudioNavItem[] = [
  { label: 'Tong quan', path: '/instructor/dashboard', icon: LayoutDashboard },
  { label: 'Khoa hoc cua toi', path: '/instructor/courses', icon: BookOpen },
  { label: 'Hoc vien', path: '/instructor/students', icon: Users },
  { label: 'Doanh thu', path: '/instructor/revenue', icon: ChartNoAxesColumn },
  { label: 'Cai dat', path: '/instructor/settings', icon: Settings },
];

export function getInstructorStudioLabel(pathname: string): string {
  const matchedItem = instructorStudioNavItems.find((item) => pathname.startsWith(item.path));
  if (matchedItem) {
    return matchedItem.label;
  }

  if (pathname.startsWith('/instructor/lessons')) {
    return 'Bai giang';
  }

  return 'Studio';
}
