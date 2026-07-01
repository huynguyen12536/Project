import {
  BookCopy,
  LayoutDashboard,
  ReceiptText,
  ServerCog,
  ShieldUser,
  Tags,
} from 'lucide-react';

export type AdminWorkspaceNavItem = {
  label: string;
  path: string;
  icon: typeof LayoutDashboard;
};

export const adminWorkspaceNavItems: AdminWorkspaceNavItem[] = [
  { label: 'Tong quan', path: '/admin', icon: LayoutDashboard },
  { label: 'Nguoi dung', path: '/admin/users', icon: ShieldUser },
  { label: 'Khoa hoc', path: '/admin/courses', icon: BookCopy },
  { label: 'Taxonomy', path: '/admin/taxonomy', icon: Tags },
  { label: 'Don hang', path: '/admin/orders', icon: ReceiptText },
  { label: 'He thong', path: '/admin/system', icon: ServerCog },
];

export function getAdminWorkspaceLabel(pathname: string): string {
  const matchedItem = adminWorkspaceNavItems.find((item) =>
    item.path === '/admin' ? pathname === '/admin' : pathname.startsWith(item.path)
  );

  return matchedItem?.label ?? 'Admin Console';
}
