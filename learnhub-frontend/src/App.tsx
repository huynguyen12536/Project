import React, { Suspense, lazy } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { UserProvider } from './context/UserContext';
import { MainLayout } from './components/layouts/MainLayout';
import { PageTransition } from './components/layouts/PageTransition';
import { RouteSkeleton } from './components/layouts/RouteSkeleton';
import { queryClient } from './lib/queryClient';
import { getRoleHomePath } from './lib/roleRouting';
import { useAuthStore } from './stores/authStore';
import { ToastContainer } from './ui-kit/Toast';
import RequireRole from './components/auth/RequireRole';

const HomePage = lazy(() => import('./pages/HomePage'));
const DashboardPage = lazy(() => import('./pages/DashboardPage'));
const OrdersPage = lazy(() => import('./pages/OrdersPage'));
const CoursePlayerPage = lazy(() => import('./pages/CoursePlayerPage'));
const CourseCatalogPage = lazy(() => import('./pages/CourseCatalogPage'));
const CourseDetailPage = lazy(() => import('./pages/CourseDetailPage'));
const InstructorDashboardPage = lazy(() => import('./pages/instructor/InstructorDashboardPage'));
const InstructorCoursesPage = lazy(() => import('./pages/instructor/InstructorCoursesPage'));
const InstructorLessonsPage = lazy(() => import('./pages/instructor/InstructorLessonsPage'));
const InstructorCourseWizardPage = lazy(() => import('./pages/instructor/InstructorCourseWizardPage'));
const InstructorStudentsPage = lazy(() => import('./pages/instructor/InstructorStudentsPage'));
const InstructorRevenuePage = lazy(() => import('./pages/instructor/InstructorRevenuePage'));
const InstructorSettingsPage = lazy(() => import('./pages/instructor/InstructorSettingsPage'));
const AdminDashboardPage = lazy(() => import('./pages/admin/AdminDashboardPage'));
const AdminUsersPage = lazy(() => import('./pages/admin/AdminUsersPage'));
const AdminCoursesPage = lazy(() => import('./pages/admin/AdminCoursesPage'));
const AdminOrdersPage = lazy(() => import('./pages/admin/AdminOrdersPage'));
const AdminSystemPage = lazy(() => import('./pages/admin/AdminSystemPage'));
const AdminTaxonomyPage = lazy(() => import('./pages/admin/AdminTaxonomyPage'));
const LoginPage = lazy(() => import('./pages/auth/AuthPages').then((mod) => ({ default: mod.LoginPage })));
const RegisterPage = lazy(() => import('./pages/auth/AuthPages').then((mod) => ({ default: mod.RegisterPage })));
const VerifyEmailPage = lazy(() => import('./pages/auth/AuthPages').then((mod) => ({ default: mod.VerifyEmailPage })));
const ForgotPasswordPage = lazy(() => import('./pages/auth/AuthPages').then((mod) => ({ default: mod.ForgotPasswordPage })));
const ResetPasswordPage = lazy(() => import('./pages/auth/AuthPages').then((mod) => ({ default: mod.ResetPasswordPage })));
const ProfilePage = lazy(() => import('./pages/ProfilePage').then((mod) => ({ default: mod.ProfilePage })));

const RequireAuth: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const location = useLocation();

  if (!isAuthenticated) {
    const redirect = `${location.pathname}${location.search}${location.hash}`;
    return <Navigate to={`/login?redirect=${encodeURIComponent(redirect)}`} replace />;
  }

  return <>{children}</>;
};

const RoleAwareDashboard: React.FC = () => {
  const role = useAuthStore((state) => state.role);
  const homePath = getRoleHomePath(role);

  if (homePath !== '/dashboard') {
    return <Navigate to={homePath} replace />;
  }

  return <DashboardPage />;
};

const RoleAwareHome: React.FC = () => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const role = useAuthStore((state) => state.role);

  if (isAuthenticated && role === 'admin') {
    return <Navigate to="/admin" replace />;
  }

  return <HomePage />;
};

const AdminSafePublicPage: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const role = useAuthStore((state) => state.role);

  if (isAuthenticated && role === 'admin') {
    return <Navigate to="/admin" replace />;
  }

  return <>{children}</>;
};

const AppRoutes = () => {
  const location = useLocation();
  const isAuthRoute =
    location.pathname.startsWith('/login') ||
    location.pathname.startsWith('/register') ||
    location.pathname.startsWith('/signup') ||
    location.pathname.startsWith('/verify-email') ||
    location.pathname.startsWith('/forgot-password') ||
    location.pathname.startsWith('/reset-password');
  const isLearningRoute = location.pathname.startsWith('/learn/');
  const isInstructorRoute = location.pathname.startsWith('/instructor/');
  const isAdminRoute = location.pathname.startsWith('/admin');

  const routes = (
    <PageTransition compact={isAuthRoute || isLearningRoute || isInstructorRoute || isAdminRoute}>
      <Suspense
        fallback={<RouteSkeleton path={location.pathname} compact={isAuthRoute || isLearningRoute || isInstructorRoute || isAdminRoute} />}
      >
        <Routes>
          <Route path="/" element={<RoleAwareHome />} />
          <Route path="/dashboard" element={<RequireAuth><RoleAwareDashboard /></RequireAuth>} />
          <Route path="/orders" element={<RequireAuth><OrdersPage /></RequireAuth>} />
          <Route path="/learn/courses/:id" element={<RequireAuth><CoursePlayerPage /></RequireAuth>} />
          <Route path="/admin" element={<RequireRole roles={['admin']}><AdminDashboardPage /></RequireRole>} />
          <Route path="/admin/users" element={<RequireRole roles={['admin']}><AdminUsersPage /></RequireRole>} />
          <Route path="/admin/courses" element={<RequireRole roles={['admin']}><AdminCoursesPage /></RequireRole>} />
          <Route path="/admin/taxonomy" element={<RequireRole roles={['admin']}><AdminTaxonomyPage /></RequireRole>} />
          <Route path="/admin/orders" element={<RequireRole roles={['admin']}><AdminOrdersPage /></RequireRole>} />
          <Route path="/admin/system" element={<RequireRole roles={['admin']}><AdminSystemPage /></RequireRole>} />
          <Route path="/instructor/dashboard" element={<RequireRole roles={['instructor', 'admin']}><InstructorDashboardPage /></RequireRole>} />
          <Route path="/instructor/courses" element={<RequireRole roles={['instructor', 'admin']}><InstructorCoursesPage /></RequireRole>} />
          <Route path="/instructor/courses/new" element={<RequireRole roles={['instructor', 'admin']}><InstructorCourseWizardPage /></RequireRole>} />
          <Route path="/instructor/courses/:courseId" element={<RequireRole roles={['instructor', 'admin']}><InstructorCourseWizardPage /></RequireRole>} />
          <Route path="/instructor/lessons" element={<RequireRole roles={['instructor', 'admin']}><InstructorLessonsPage /></RequireRole>} />
          <Route path="/instructor/students" element={<RequireRole roles={['instructor', 'admin']}><InstructorStudentsPage /></RequireRole>} />
          <Route path="/instructor/revenue" element={<RequireRole roles={['instructor', 'admin']}><InstructorRevenuePage /></RequireRole>} />
          <Route path="/instructor/settings" element={<RequireRole roles={['instructor', 'admin']}><InstructorSettingsPage /></RequireRole>} />
          <Route path="/instructor" element={<Navigate to="/instructor/dashboard" replace />} />
          <Route path="/courses" element={<AdminSafePublicPage><CourseCatalogPage /></AdminSafePublicPage>} />
          <Route path="/courses/:id" element={<AdminSafePublicPage><CourseDetailPage /></AdminSafePublicPage>} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/signup" element={<RegisterPage />} />
          <Route path="/verify-email" element={<VerifyEmailPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route path="/profile/:userId" element={<RequireAuth><ProfilePage /></RequireAuth>} />
          <Route path="/profile" element={<Navigate to="/profile/me" />} />
          <Route path="*" element={<Navigate to="/" />} />
        </Routes>
      </Suspense>
    </PageTransition>
  );

  if (isLearningRoute || isInstructorRoute || isAdminRoute) {
    return routes;
  }

  return <MainLayout>{routes}</MainLayout>;
};

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <UserProvider>
        <Router>
          <AppRoutes />
          <ToastContainer />
        </Router>
      </UserProvider>
    </QueryClientProvider>
  );
};

export default App;
