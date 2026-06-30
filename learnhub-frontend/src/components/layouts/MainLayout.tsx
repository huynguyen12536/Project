import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

interface MainLayoutProps {
  children: React.ReactNode;
}

export const MainLayout = ({ children }: MainLayoutProps) => {
  const navigate = useNavigate();
  const [langOpen, setLangOpen] = useState(false);
  const [lang, setLang] = useState<'vi' | 'en'>('vi');

  const t = {
    vi: {
      categories: 'Danh mục',
      search_ph: 'Tìm khoá học, kỹ năng...',
      nav_courses: 'Khoá học',
      nav_roadmap: 'Lộ trình',
      nav_pricing: 'Học phí',
      nav_teach: 'Giảng dạy',
      login: 'Đăng nhập',
      signup: 'Đăng ký',
      footer_about: 'LearnHub là nền tảng học trực tuyến giúp bạn làm chủ kỹ năng mới với các khoá học video từ chuyên gia hàng đầu.',
      footer_explore: 'Khám phá',
      footer_company: 'Công ty',
      footer_support: 'Hỗ trợ',
      footer_about_l: 'Về chúng tôi',
      footer_contact: 'Liên hệ',
      footer_help: 'Trung tâm trợ giúp',
      footer_terms: 'Điều khoản',
      footer_privacy: 'Bảo mật',
      footer_rights: 'Bảo lưu mọi quyền.',
    },
    en: {
      categories: 'Categories',
      search_ph: 'Search courses, skills...',
      nav_courses: 'Courses',
      nav_roadmap: 'Roadmap',
      nav_pricing: 'Pricing',
      nav_teach: 'Teach',
      login: 'Log in',
      signup: 'Sign up',
      footer_about: 'LearnHub is an online learning platform that helps you master new skills with video courses from top experts.',
      footer_explore: 'Explore',
      footer_company: 'Company',
      footer_support: 'Support',
      footer_about_l: 'About us',
      footer_contact: 'Contact',
      footer_help: 'Help center',
      footer_terms: 'Terms',
      footer_privacy: 'Privacy',
      footer_rights: 'All rights reserved.',
    },
  };

  const strings = t[lang];

  return (
    <div className="font-jakarta">
      {/* Sticky Header */}
      <header className="sticky top-0 z-50 flex items-center gap-4 h-[66px] px-6 bg-white border-b border-lh-border">
        {/* Logo */}
        <button onClick={() => navigate('/')} className="flex items-center gap-3 cursor-pointer flex-none hover:opacity-80">
          <div className="w-[34px] h-[34px] bg-lh-navy flex items-center justify-center">
            <div className="w-0 h-0 border-t-[7px] border-b-[7px] border-l-[11px] border-t-transparent border-b-transparent border-l-lh-pink ml-1"></div>
          </div>
          <span className="font-black text-xl text-lh-navy tracking-[-0.03em]">LearnHub</span>
        </button>

        {/* Categories Button */}
        <button className="flex-none flex items-center gap-2 h-10 px-3.5 bg-[#F4F5FB] border-none text-sm font-bold text-lh-navy rounded cursor-pointer hover:bg-[#E9EBF5]">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M3 6h18M3 12h18M3 18h18" />
          </svg>
          {strings.categories}
        </button>

        {/* Search Bar */}
        <div className="flex-1 min-w-0 flex items-center gap-2.5 h-11 px-4 border border-lh-input rounded max-w-[440px]">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#9498A8" strokeWidth="2" className="flex-none">
            <circle cx="11" cy="11" r="7" />
            <path d="M21 21l-4-4" />
          </svg>
          <input placeholder={strings.search_ph} className="flex-1 border-none outline-none font-inherit text-sm text-lh-dark bg-transparent" />
        </div>

        {/* Nav Links */}
        <nav className="flex-none flex items-center gap-4">
          <button onClick={() => navigate('/courses')} className="text-sm font-bold text-lh-navy cursor-pointer hover:text-lh-blue">
            {strings.nav_courses}
          </button>
          <button className="text-sm font-bold text-lh-navy cursor-pointer hover:text-lh-blue">
            {strings.nav_roadmap}
          </button>
          <button className="text-sm font-bold text-lh-navy cursor-pointer hover:text-lh-blue">
            {strings.nav_pricing}
          </button>
          <button className="text-sm font-bold text-lh-navy cursor-pointer hover:text-lh-blue">
            {strings.nav_teach}
          </button>
        </nav>

        {/* Language Toggle */}
        <div className="relative flex-none">
          <button
            onClick={() => setLangOpen(!langOpen)}
            className="flex items-center gap-2 h-10 px-3.5 bg-white border border-lh-border text-lh-navy text-sm font-bold rounded cursor-pointer hover:bg-[#F4F5FB]"
          >
            {lang === 'vi' ? 'Tiếng Việt' : 'English'}
            <span className="text-[8px] opacity-60">▼</span>
          </button>
          {langOpen && (
            <div className="absolute top-[46px] right-0 bg-white border border-lh-border min-w-[150px] z-60 shadow-lg">
              <button
                onClick={() => { setLang('vi'); setLangOpen(false); }}
                className="block w-full text-left px-4 py-3 text-sm font-bold text-lh-dark bg-transparent border-none cursor-pointer hover:bg-[#F4F5FB]"
              >
                Tiếng Việt
              </button>
              <button
                onClick={() => { setLang('en'); setLangOpen(false); }}
                className="block w-full text-left px-4 py-3 text-sm font-bold text-lh-dark bg-transparent border-t border-[#EFF1F7] border-none cursor-pointer hover:bg-[#F4F5FB]"
              >
                English
              </button>
            </div>
          )}
        </div>

        {/* Auth Buttons */}
        <button onClick={() => navigate('/login')} className="flex-none h-[42px] px-4.5 bg-white text-lh-navy border border-lh-navy font-bold text-sm rounded cursor-pointer hover:bg-[#F4F5FB]">
          {strings.login}
        </button>
        <button onClick={() => navigate('/signup')} className="flex-none h-[42px] px-4.5 bg-lh-pink text-white border-none font-bold text-sm rounded cursor-pointer hover:bg-lh-pink-dark">
          {strings.signup}
        </button>
      </header>

      {/* Main Content */}
      <main>{children}</main>

      {/* Footer */}
      <footer className="bg-lh-dark text-white px-6 py-16">
        <div className="max-w-6xl mx-auto grid grid-cols-4 gap-10 pb-9 border-b border-white/12 mb-6">
          {/* Brand */}
          <div>
            <div className="flex items-center gap-3 mb-4">
              <div className="w-8 h-8 bg-lh-pink flex items-center justify-center rounded">
                <div className="w-0 h-0 border-t-[6px] border-b-[6px] border-l-[10px] border-t-transparent border-b-transparent border-l-white ml-1"></div>
              </div>
              <span className="font-black text-base">LearnHub</span>
            </div>
            <p className="text-sm text-[#9A9CB8] leading-relaxed max-w-[280px]">{strings.footer_about}</p>
          </div>

          {/* Explore */}
          <div>
            <div className="text-xs font-bold uppercase tracking-wider text-[#6E70A0] mb-4">{strings.footer_explore}</div>
            <div className="flex flex-col gap-3">
              <button className="text-sm text-[#C7C9E4] hover:text-white cursor-pointer">{strings.nav_courses}</button>
              <button className="text-sm text-[#C7C9E4] hover:text-white cursor-pointer">{strings.categories}</button>
              <button className="text-sm text-[#C7C9E4] hover:text-white cursor-pointer">{strings.nav_pricing}</button>
            </div>
          </div>

          {/* Company */}
          <div>
            <div className="text-xs font-bold uppercase tracking-wider text-[#6E70A0] mb-4">{strings.footer_company}</div>
            <div className="flex flex-col gap-3">
              <button className="text-sm text-[#C7C9E4] hover:text-white cursor-pointer">{strings.footer_about_l}</button>
              <button className="text-sm text-[#C7C9E4] hover:text-white cursor-pointer">{strings.footer_contact}</button>
              <button className="text-sm text-[#C7C9E4] hover:text-white cursor-pointer">FAQ</button>
            </div>
          </div>

          {/* Support */}
          <div>
            <div className="text-xs font-bold uppercase tracking-wider text-[#6E70A0] mb-4">{strings.footer_support}</div>
            <div className="flex flex-col gap-3">
              <button className="text-sm text-[#C7C9E4] hover:text-white cursor-pointer">{strings.footer_help}</button>
              <button className="text-sm text-[#C7C9E4] hover:text-white cursor-pointer">{strings.footer_terms}</button>
              <button className="text-sm text-[#C7C9E4] hover:text-white cursor-pointer">{strings.footer_privacy}</button>
            </div>
          </div>
        </div>

        {/* Copyright */}
        <div className="max-w-6xl mx-auto text-sm text-[#6E70A0]">
          © 2026 LearnHub. {strings.footer_rights}
        </div>
      </footer>
    </div>
  );
};
