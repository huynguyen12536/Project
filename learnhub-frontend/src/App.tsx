import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { UserProvider } from './context/UserContext';
import { MainLayout } from './components/layouts/MainLayout';
import { ProfilePage } from './pages/ProfilePage';
import HomePage from './pages/HomePage';
import CourseCatalogPage from './pages/CourseCatalogPage';
import CourseDetailPage from './pages/CourseDetailPage';
import { LoginPage, RegisterPage, VerifyEmailPage } from './pages/auth/AuthPages';
import { queryClient } from './lib/queryClient';

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <UserProvider>
        <Router>
          <MainLayout>
            <Routes>
              <Route path="/" element={<HomePage />} />
              <Route path="/courses" element={<CourseCatalogPage />} />
              <Route path="/courses/:id" element={<CourseDetailPage />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/signup" element={<RegisterPage />} />
              <Route path="/verify-email" element={<VerifyEmailPage />} />
              <Route path="/profile/:userId" element={<ProfilePage />} />
              <Route path="/profile" element={<Navigate to="/profile/me" />} />
              <Route path="*" element={<Navigate to="/" />} />
            </Routes>
          </MainLayout>
        </Router>
      </UserProvider>
    </QueryClientProvider>
  );
};

export default App;
