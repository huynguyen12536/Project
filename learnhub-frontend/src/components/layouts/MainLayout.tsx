/**
 * Main Layout Component
 *
 * Wraps all pages with header, footer, and navigation.
 * Provides consistent layout structure across the application.
 */

import React, { ReactNode } from 'react';
import { useLocation } from 'react-router-dom';

interface MainLayoutProps {
  children: ReactNode;
}

export const MainLayout: React.FC<MainLayoutProps> = ({ children }) => {
  const location = useLocation();
  const isLandingPage = location.pathname === '/';

  return (
    <div className="flex flex-col min-h-screen">
      {/* Header */}
      <header className={`${isLandingPage ? 'bg-transparent border-b border-transparent' : 'bg-white shadow-sm border-b border-gray-200'} sticky top-0 z-40 backdrop-blur-sm`}>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <a href="/" className={`text-2xl font-bold ${isLandingPage ? 'text-white drop-shadow-lg' : 'text-primary-600'}`}>
                LearnHub
              </a>
              <nav className="hidden md:flex gap-6">
                <a
                  href="/"
                  className={`transition ${isLandingPage ? 'text-white/80 hover:text-white' : 'text-gray-600 hover:text-primary-600'}`}
                >
                  Home
                </a>
                <a
                  href="/profile"
                  className={`transition ${isLandingPage ? 'text-white/80 hover:text-white' : 'text-gray-600 hover:text-primary-600'}`}
                >
                  My Profile
                </a>
              </nav>
            </div>

            {/* User Menu */}
            <div className="flex items-center gap-4">
              <button className={`px-4 py-2 transition ${isLandingPage ? 'text-white/80 hover:text-white' : 'text-gray-600 hover:text-primary-600'}`}>
                Sign In
              </button>
              <button className={`px-4 py-2 rounded-lg transition ${isLandingPage ? 'bg-white text-primary-600 hover:bg-gray-100' : 'bg-error-500 text-white hover:bg-error-600'}`}>
                {isLandingPage ? 'Get Started' : 'Logout'}
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className={`flex-1 ${!isLandingPage ? 'bg-gray-50' : ''}`}>
        {isLandingPage ? children : <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">{children}</div>}
      </main>

      {/* Footer */}
      <footer className="bg-slate-900 text-white border-t border-slate-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-8 mb-8">
            <div>
              <h3 className="text-lg font-bold mb-4">LearnHub</h3>
              <p className="text-gray-400">Empowering learners worldwide with quality education.</p>
            </div>
            <div>
              <h4 className="font-bold mb-4">Courses</h4>
              <ul className="space-y-2 text-gray-400 text-sm">
                <li><a href="#" className="hover:text-white transition">Web Development</a></li>
                <li><a href="#" className="hover:text-white transition">Data Science</a></li>
                <li><a href="#" className="hover:text-white transition">Design</a></li>
              </ul>
            </div>
            <div>
              <h4 className="font-bold mb-4">Company</h4>
              <ul className="space-y-2 text-gray-400 text-sm">
                <li><a href="#" className="hover:text-white transition">About Us</a></li>
                <li><a href="#" className="hover:text-white transition">Blog</a></li>
                <li><a href="#" className="hover:text-white transition">Careers</a></li>
              </ul>
            </div>
            <div>
              <h4 className="font-bold mb-4">Legal</h4>
              <ul className="space-y-2 text-gray-400 text-sm">
                <li><a href="#" className="hover:text-white transition">Privacy</a></li>
                <li><a href="#" className="hover:text-white transition">Terms</a></li>
                <li><a href="#" className="hover:text-white transition">Contact</a></li>
              </ul>
            </div>
          </div>
          <div className="border-t border-slate-800 pt-8 text-center text-gray-400 text-sm">
            <p>© 2026 LearnHub. All rights reserved.</p>
          </div>
        </div>
      </footer>
    </div>
  );
};
