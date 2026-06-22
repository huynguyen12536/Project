/**
 * Main App Component
 *
 * Root component with routing setup and global providers.
 * Integrates:
 * - React Router for navigation
 * - UserProvider for global state
 * - Layout wrapper with header/footer
 */

import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { UserProvider } from './context/UserContext';
import { MainLayout } from './components/layouts/MainLayout';
import { ProfilePage } from './pages/ProfilePage';
import { LandingPage } from './pages/LandingPage';

/**
 * App Component
 */
export const App: React.FC = () => {
  return (
    <UserProvider>
      <Router>
        <MainLayout>
          <Routes>
            {/* Landing Page */}
            <Route path="/" element={<LandingPage />} />

            {/* Profile Management Routes */}
            <Route path="/profile/:userId" element={<ProfilePage />} />
            <Route path="/profile" element={<Navigate to="/profile/me" />} />

            {/* Fallback */}
            <Route path="*" element={<Navigate to="/" />} />
          </Routes>
        </MainLayout>
      </Router>
    </UserProvider>
  );
};

export default App;
