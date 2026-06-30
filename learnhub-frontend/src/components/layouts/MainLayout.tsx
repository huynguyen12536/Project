import { useEffect, useMemo, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  Bell,
  ChevronDown,
  Compass,
  GraduationCap,
  LogOut,
  LucideIcon,
  Menu,
  BookOpen,
  LayoutGrid,
  Map,
  Search,
  ShoppingCart,
  UserCircle2,
  UsersRound,
} from 'lucide-react';
import apiClient from '../../lib/api';
import { cn } from '../../lib/cn';
import { useAuthStore } from '../../stores/authStore';
import { Avatar } from '../../ui-kit/Avatar';

interface MainLayoutProps {
  children: React.ReactNode;
}

type Language = 'vi' | 'en';

type NavItem = {
  label: string;
  path: string;
  exact?: boolean;
  icon?: LucideIcon;
};

const guestNavItems: NavItem[] = [
  { label: 'Khoa hoc', path: '/courses' },
  { label: 'Lo trinh', path: '/courses#roadmap' },
  { label: 'Hoc phi', path: '/courses#pricing' },
  { label: 'Giang day', path: '/courses#teach' },
];

const authedNavItems: NavItem[] = [
  { label: 'Kham pha khoa hoc', path: '/courses', icon: Compass },
  { label: 'Bang dieu khien', path: '/dashboard', exact: true, icon: LayoutGrid },
  { label: 'Khoa hoc cua toi', path: '/dashboard#my-courses', icon: BookOpen },
  { label: 'Lo trinh', path: '/dashboard#roadmap', icon: Map },
  { label: 'Cong dong', path: '/dashboard#community', icon: UsersRound },
];

const searchSuggestions = [
  {
    id: 7,
    title: 'He thong thiet ke cho SaaS Product',
    meta: 'Thiet ke · Le Thu Ha',
    price: '329.000d',
    tag: 'New',
  },
  {
    id: 8,
    title: 'Data Storytelling cho Product Analyst',
    meta: 'Du lieu · Pham Duc Anh',
    price: '389.000d',
    tag: 'Moi cap nhat',
  },
  {
    id: 9,
    title: 'DevOps can ban cho backend engineer',
    meta: 'Backend · Nguyen Hoang',
    price: '449.000d',
    tag: 'New',
  },
  {
    id: 10,
    title: 'Khoa hoc AI Prompting thuc chien',
    meta: 'AI · Tran Gia Bao',
    price: '279.000d',
    tag: 'Moi',
  },
];

const copy = {
  vi: {
    categories: 'Danh muc',
    search: 'Tim khoa hoc, ky nang, bai hoc...',
    login: 'Dang nhap',
    signup: 'Dang ky',
    footerAbout:
      'LearnHub la nen tang hoc truc tuyen giup ban lam chu ky nang moi voi cac khoa hoc video tu chuyen gia hang dau.',
    footerExplore: 'Kham pha',
    footerCompany: 'Cong ty',
    footerSupport: 'Ho tro',
    footerAboutLabel: 'Ve chung toi',
    footerContact: 'Lien he',
    footerHelp: 'Trung tam tro giup',
    footerTerms: 'Dieu khoan',
    footerPrivacy: 'Bao mat',
    footerRights: 'Bao luu moi quyen.',
    account: 'Tai khoan',
    profile: 'Trang ca nhan',
    logout: 'Dang xuat',
  },
  en: {
    categories: 'Categories',
    search: 'Search courses, skills, lessons...',
    login: 'Log in',
    signup: 'Sign up',
    footerAbout:
      'LearnHub is an online learning platform that helps you master new skills with video courses from top experts.',
    footerExplore: 'Explore',
    footerCompany: 'Company',
    footerSupport: 'Support',
    footerAboutLabel: 'About us',
    footerContact: 'Contact',
    footerHelp: 'Help center',
    footerTerms: 'Terms',
    footerPrivacy: 'Privacy',
    footerRights: 'All rights reserved.',
    account: 'Account',
    profile: 'Profile',
    logout: 'Log out',
  },
} as const;

function normalizeRoute(route: string): { pathname: string; hash: string } {
  const [pathname, hash] = route.split('#');
  return { pathname, hash: hash ? `#${hash}` : '' };
}

function isActiveRoute(currentPath: string, currentHash: string, item: NavItem): boolean {
  const target = normalizeRoute(item.path);

  if (item.exact) {
    return currentPath === target.pathname && currentHash === target.hash;
  }

  if (target.hash) {
    return currentPath === target.pathname && currentHash === target.hash;
  }

  return currentPath.startsWith(target.pathname);
}

const SearchBar = ({
  placeholder,
  onSelectCourse,
  onOpenCatalog,
}: {
  placeholder: string;
  onSelectCourse: (courseId: number) => void;
  onOpenCatalog: () => void;
}) => {
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const visibleSuggestions = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    if (!normalized) {
      return searchSuggestions.slice(0, 4);
    }

    return searchSuggestions.filter((item) => {
      return item.title.toLowerCase().includes(normalized) || item.meta.toLowerCase().includes(normalized);
    });
  }, [query]);

  return (
    <div ref={containerRef} className="relative">
      <div className="flex h-11 min-w-0 items-center gap-2.5 rounded-2xl border border-lh-input bg-white px-4 shadow-[0_8px_24px_rgba(21,22,46,0.04)] transition focus-within:border-[#D9DEF2] focus-within:ring-4 focus-within:ring-[#EEF2FF]">
        <Search className="h-4 w-4 flex-none text-lh-muted" />
        <input
          value={query}
          onFocus={() => setOpen(true)}
          onChange={(event) => {
            setQuery(event.target.value);
            setOpen(true);
          }}
          onKeyDown={(event) => {
            if (event.key === 'Enter' && visibleSuggestions[0]) {
              onSelectCourse(visibleSuggestions[0].id);
              setOpen(false);
              setQuery('');
            }
          }}
          placeholder={placeholder}
          className="flex-1 border-none bg-transparent text-sm text-lh-dark outline-none placeholder:text-lh-muted"
        />
      </div>

      {open ? (
        <div className="absolute left-0 right-0 top-[52px] z-40 rounded-2xl border border-[#E7EAF0] bg-white p-2 shadow-[0_18px_35px_rgba(21,22,46,0.10)]">
          <div className="px-3 py-2 text-xs font-semibold uppercase tracking-[0.14em] text-[#9CA3AF]">
            Goi y khoa hoc
          </div>
          <div className="space-y-1">
            {visibleSuggestions.length > 0 ? (
              visibleSuggestions.map((item) => (
                <button
                  key={item.id}
                  onClick={() => {
                    onSelectCourse(item.id);
                    setOpen(false);
                    setQuery('');
                  }}
                  className="flex w-full items-start justify-between gap-3 rounded-2xl px-3 py-3 text-left transition hover:bg-[#F8FAFF]"
                >
                  <div className="min-w-0">
                    <div className="truncate text-sm font-semibold text-[#111827]">{item.title}</div>
                    <div className="mt-1 truncate text-xs text-[#6B7280]">{item.meta}</div>
                  </div>
                  <div className="flex flex-none flex-col items-end gap-1">
                    <span className="rounded-full bg-[#EEF2FF] px-2.5 py-1 text-[11px] font-semibold text-lh-blue">
                      {item.tag}
                    </span>
                    <span className="text-xs font-semibold text-[#111827]">{item.price}</span>
                  </div>
                </button>
              ))
            ) : (
              <div className="rounded-2xl px-3 py-5 text-center text-sm text-[#6B7280]">
                Khong co khoa hoc phu hop voi tu khoa nay.
              </div>
            )}
          </div>
          <button
            onClick={() => {
              onOpenCatalog();
              setOpen(false);
            }}
            className="mt-2 flex w-full items-center justify-between rounded-2xl border border-[#E7EAF0] px-3 py-3 text-sm font-semibold text-lh-blue transition hover:bg-[#F8FAFF]"
          >
            Xem toan bo catalog
            <Compass className="h-4 w-4" />
          </button>
        </div>
      ) : null}
    </div>
  );
};

const IconActionButton = ({
  onClick,
  children,
  badge,
  active = false,
}: {
  onClick?: () => void;
  children: React.ReactNode;
  badge?: string;
  active?: boolean;
}) => (
  <button
    onClick={onClick}
    className={cn(
      'relative inline-flex h-10 w-10 items-center justify-center rounded-xl border transition',
      active
        ? 'border-[#D9DEF2] bg-[#F8FAFF] text-lh-blue'
        : 'border-[#E7EAF0] bg-white text-lh-navy hover:bg-[#F9FAFB]'
    )}
  >
    {children}
    {badge ? (
      <span className="absolute right-2 top-2 inline-flex min-h-[18px] min-w-[18px] items-center justify-center rounded-full bg-lh-pink px-1 text-[10px] font-black text-white">
        {badge}
      </span>
    ) : null}
  </button>
);

export const MainLayout = ({ children }: MainLayoutProps) => {
  const navigate = useNavigate();
  const location = useLocation();
  const [lang, setLang] = useState<Language>('vi');
  const [langOpen, setLangOpen] = useState(false);
  const [accountOpen, setAccountOpen] = useState(false);
  const user = useAuthStore((state) => state.user);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const logout = useAuthStore((state) => state.logout);

  const strings = copy[lang];
  const displayName = useMemo(() => {
    const fullName = [user?.firstName, user?.lastName].filter(Boolean).join(' ').trim();
    return fullName || user?.email || strings.account;
  }, [strings.account, user?.email, user?.firstName, user?.lastName]);
  const profilePath = user?.id ? `/profile/${user.id}` : '/profile';

  const handleLogout = async () => {
    setAccountOpen(false);
    try {
      await apiClient.post('/v1/auth/logout');
    } catch {
      /* ignore logout API errors and clear client state anyway */
    }
    logout();
    navigate('/login');
  };

  const renderLogo = () => (
    <button onClick={() => navigate(isAuthenticated ? '/dashboard' : '/')} className="flex items-center gap-3">
      <div className="flex h-[36px] w-[36px] items-center justify-center rounded-xl bg-lh-navy shadow-[0_10px_20px_rgba(36,37,130,0.18)]">
        <div className="ml-1 h-0 w-0 border-b-[7px] border-l-[11px] border-t-[7px] border-b-transparent border-l-lh-pink border-t-transparent" />
      </div>
      <span className="font-inter text-xl font-black tracking-[-0.03em] text-lh-navy">LearnHub</span>
    </button>
  );

  const renderAuthAccountMenu = () => (
    <div className="relative">
      <button
        onClick={() => setAccountOpen((current) => !current)}
        className="flex h-11 items-center gap-3 rounded-2xl border border-lh-border bg-white px-3 text-lh-navy transition hover:bg-[#F4F5FB]"
      >
        <Avatar src={user?.avatarUrl} name={displayName} size="sm" className="bg-[#EEF0FB] text-lh-blue" />
        <div className="hidden min-w-0 text-left lg:block">
          <div className="max-w-[140px] truncate text-sm font-bold">{displayName}</div>
          <div className="max-w-[140px] truncate text-xs text-lh-muted">{user?.email}</div>
        </div>
        <ChevronDown className="h-4 w-4 opacity-60" />
      </button>

      {accountOpen ? (
        <div className="absolute right-0 top-[52px] z-60 w-[260px] rounded-[22px] border border-lh-border bg-white p-2 shadow-[0_20px_35px_rgba(21,22,46,0.12)]">
          <button
            onClick={() => {
              setAccountOpen(false);
              navigate('/instructor/courses');
            }}
            className="flex w-full items-center gap-3 rounded-2xl px-3 py-3 text-left transition hover:bg-[#F4F5FB]"
          >
            <GraduationCap className="h-5 w-5 text-lh-blue" />
            <div className="min-w-0">
              <div className="truncate text-sm font-bold text-lh-navy">Studio giang day</div>
              <div className="truncate text-xs text-lh-muted">Quan ly khoa hoc va bai giang</div>
            </div>
          </button>
          <button
            onClick={() => {
              setAccountOpen(false);
              navigate(profilePath);
            }}
            className="mt-1 flex w-full items-center gap-3 rounded-2xl px-3 py-3 text-left transition hover:bg-[#F4F5FB]"
          >
            <UserCircle2 className="h-5 w-5 text-lh-blue" />
            <div className="min-w-0">
              <div className="truncate text-sm font-bold text-lh-navy">{displayName}</div>
              <div className="truncate text-xs text-lh-muted">{strings.profile}</div>
            </div>
          </button>
          <button
            onClick={handleLogout}
            className="mt-1 flex w-full items-center gap-3 rounded-2xl px-3 py-3 text-left transition hover:bg-[#FDE7EC]"
          >
            <LogOut className="h-5 w-5 text-lh-pink" />
            <div className="text-sm font-bold text-lh-navy">{strings.logout}</div>
          </button>
        </div>
      ) : null}
    </div>
  );

  return (
    <div className="flex min-h-screen flex-col font-jakarta">
      <header className="sticky top-0 z-50 border-b border-[#ECEFF4] bg-white/95 backdrop-blur-sm">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          {isAuthenticated ? (
            <>
              <div className="flex min-h-[64px] items-center gap-3 py-3">
                <div className="flex flex-none items-center">{renderLogo()}</div>
                <div className="hidden min-w-0 max-w-[420px] flex-1 lg:block">
                  <SearchBar
                    placeholder={strings.search}
                    onSelectCourse={(courseId) => navigate(`/courses/${courseId}`)}
                    onOpenCatalog={() => navigate('/courses')}
                  />
                </div>
                <div className="ml-auto flex items-center gap-2">
                  <IconActionButton badge="3">
                    <Bell className="h-4.5 w-4.5" />
                  </IconActionButton>
                  <IconActionButton
                    badge="2"
                    active={location.pathname.startsWith('/orders')}
                    onClick={() => navigate('/orders')}
                  >
                    <ShoppingCart className="h-4.5 w-4.5" />
                  </IconActionButton>
                  {renderAuthAccountMenu()}
                </div>
              </div>

              <div className="pb-3 lg:hidden">
                <SearchBar
                  placeholder={strings.search}
                  onSelectCourse={(courseId) => navigate(`/courses/${courseId}`)}
                  onOpenCatalog={() => navigate('/courses')}
                />
              </div>

              <div className="flex gap-2 overflow-x-auto pb-3 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
                {authedNavItems.map((item) => {
                  const active = isActiveRoute(location.pathname, location.hash, item);
                  const Icon = item.icon;
                  return (
                    <button
                      key={item.label}
                      onClick={() => navigate(item.path)}
                      className={cn(
                        'inline-flex items-center gap-2 whitespace-nowrap rounded-lg px-3.5 py-2 text-sm font-medium transition',
                        active
                          ? 'bg-[#111827] text-white shadow-[0_8px_16px_rgba(17,24,39,0.12)]'
                          : 'bg-transparent text-[#6B7280] hover:bg-[#F3F4F6] hover:text-[#111827]'
                      )}
                    >
                      {Icon ? <Icon className="h-4 w-4" /> : null}
                      {item.label}
                    </button>
                  );
                })}
              </div>
            </>
          ) : (
            <>
              <div className="flex min-h-[68px] items-center gap-3 py-3">
                <div className="flex flex-none items-center">{renderLogo()}</div>
                <button className="hidden h-10 flex-none items-center gap-2 rounded-xl bg-[#F4F5FB] px-3.5 text-sm font-semibold text-lh-navy transition hover:bg-[#E9EBF5] md:inline-flex">
                  <Menu className="h-4 w-4" />
                  {strings.categories}
                </button>
                <div className="hidden min-w-0 max-w-[420px] flex-1 lg:block">
                  <SearchBar
                    placeholder={strings.search}
                    onSelectCourse={(courseId) => navigate(`/courses/${courseId}`)}
                    onOpenCatalog={() => navigate('/courses')}
                  />
                </div>
                <div className="ml-auto hidden items-center gap-3 xl:flex">
                  {guestNavItems.map((item) => (
                    <button
                      key={item.label}
                      onClick={() => navigate(item.path)}
                      className="text-sm font-semibold text-lh-navy transition hover:text-lh-blue"
                    >
                      {item.label}
                    </button>
                  ))}
                </div>
                <div className="relative ml-auto">
                  <button
                    onClick={() => setLangOpen((current) => !current)}
                    className="flex h-10 items-center gap-2 rounded-xl border border-[#E7EAF0] bg-white px-3.5 text-sm font-semibold text-lh-navy transition hover:bg-[#F9FAFB]"
                  >
                    {lang === 'vi' ? 'Tieng Viet' : 'English'}
                    <ChevronDown className="h-4 w-4 opacity-60" />
                  </button>
                  {langOpen ? (
                    <div className="absolute right-0 top-[52px] z-60 min-w-[150px] rounded-[22px] border border-lh-border bg-white p-2 shadow-[0_18px_30px_rgba(21,22,46,0.12)]">
                      <button
                        onClick={() => {
                          setLang('vi');
                          setLangOpen(false);
                        }}
                        className="block w-full rounded-2xl px-4 py-3 text-left text-sm font-bold text-lh-dark transition hover:bg-[#F4F5FB]"
                      >
                        Tieng Viet
                      </button>
                      <button
                        onClick={() => {
                          setLang('en');
                          setLangOpen(false);
                        }}
                        className="mt-1 block w-full rounded-2xl px-4 py-3 text-left text-sm font-bold text-lh-dark transition hover:bg-[#F4F5FB]"
                      >
                        English
                      </button>
                    </div>
                  ) : null}
                </div>
                <button
                  onClick={() => navigate('/login')}
                  className="hidden h-10 rounded-xl border border-lh-navy bg-white px-4 text-sm font-semibold text-lh-navy transition hover:bg-[#F4F5FB] sm:inline-flex sm:items-center"
                >
                  {strings.login}
                </button>
                <button
                  onClick={() => navigate('/register')}
                  className="hidden h-10 rounded-xl bg-lh-pink px-4 text-sm font-semibold text-white transition hover:bg-lh-pink-dark sm:inline-flex sm:items-center"
                >
                  {strings.signup}
                </button>
              </div>

              <div className="pb-3 lg:hidden">
                <SearchBar
                  placeholder={strings.search}
                  onSelectCourse={(courseId) => navigate(`/courses/${courseId}`)}
                  onOpenCatalog={() => navigate('/courses')}
                />
              </div>

              <div className="flex gap-2 overflow-x-auto pb-4 xl:hidden [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
                {guestNavItems.map((item) => (
                  <button
                    key={item.label}
                    onClick={() => navigate(item.path)}
                    className="whitespace-nowrap rounded-full bg-[#F4F5FB] px-4 py-2.5 text-sm font-bold text-lh-navy transition hover:bg-[#E9EBF5]"
                  >
                    {item.label}
                  </button>
                ))}
              </div>
            </>
          )}
        </div>
      </header>

      <main className="relative flex-1 overflow-hidden">{children}</main>

      <footer className="bg-lh-dark px-6 py-16 text-white">
        <div className="mx-auto mb-6 grid max-w-6xl gap-10 border-b border-white/12 pb-9 md:grid-cols-2 xl:grid-cols-4">
          <div>
            <div className="mb-4 flex items-center gap-3">
              <div className="flex h-8 w-8 items-center justify-center rounded bg-lh-pink">
                <div className="ml-1 h-0 w-0 border-b-[6px] border-l-[10px] border-t-[6px] border-b-transparent border-l-white border-t-transparent" />
              </div>
              <span className="font-black text-base">LearnHub</span>
            </div>
            <p className="max-w-[280px] text-sm leading-relaxed text-[#9A9CB8]">{strings.footerAbout}</p>
          </div>

          <div>
            <div className="mb-4 text-xs font-bold uppercase tracking-wider text-[#6E70A0]">{strings.footerExplore}</div>
            <div className="flex flex-col gap-3">
              <button className="text-left text-sm text-[#C7C9E4] transition hover:text-white">Khoa hoc</button>
              <button className="text-left text-sm text-[#C7C9E4] transition hover:text-white">{strings.categories}</button>
              <button className="text-left text-sm text-[#C7C9E4] transition hover:text-white">Lo trinh</button>
            </div>
          </div>

          <div>
            <div className="mb-4 text-xs font-bold uppercase tracking-wider text-[#6E70A0]">{strings.footerCompany}</div>
            <div className="flex flex-col gap-3">
              <button className="text-left text-sm text-[#C7C9E4] transition hover:text-white">{strings.footerAboutLabel}</button>
              <button className="text-left text-sm text-[#C7C9E4] transition hover:text-white">{strings.footerContact}</button>
              <button className="text-left text-sm text-[#C7C9E4] transition hover:text-white">FAQ</button>
            </div>
          </div>

          <div>
            <div className="mb-4 text-xs font-bold uppercase tracking-wider text-[#6E70A0]">{strings.footerSupport}</div>
            <div className="flex flex-col gap-3">
              <button className="text-left text-sm text-[#C7C9E4] transition hover:text-white">{strings.footerHelp}</button>
              <button className="text-left text-sm text-[#C7C9E4] transition hover:text-white">{strings.footerTerms}</button>
              <button className="text-left text-sm text-[#C7C9E4] transition hover:text-white">{strings.footerPrivacy}</button>
            </div>
          </div>
        </div>

        <div className="mx-auto max-w-6xl text-sm text-[#6E70A0]">(c) 2026 LearnHub. {strings.footerRights}</div>
      </footer>
    </div>
  );
};
