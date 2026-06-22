/**
 * Dashboard Component
 *
 * Protected page showing user's enrolled courses, recommendations, and mentor connections.
 * Only accessible to logged-in users.
 */

import React, { useEffect, useState } from 'react';
import { useUser } from '../context/UserContext';
import { Navigate } from 'react-router-dom';

interface Course {
  id: string;
  title: string;
  progress: number;
  mentor: { name: string; id: string };
}

interface Mentor {
  id: string;
  name: string;
  specialty: string;
  rating: number;
  avatarUrl: string;
}

export const Dashboard: React.FC = () => {
  const { user, isLoading: userLoading } = useUser();
  const [enrolledCourses, setEnrolledCourses] = useState<Course[]>([]);
  const [recommendedCourses, setRecommendedCourses] = useState<Course[]>([]);
  const [mentors, setMentors] = useState<Mentor[]>([]);
  const [stats, setStats] = useState({ courses: 0, mentors: 0, points: 0 });

  // Redirect if not logged in
  if (!user && !userLoading) {
    return <Navigate to="/" />;
  }

  useEffect(() => {
    // Mock data
    setStats({ courses: 5, mentors: 8, points: 2350 });
    setEnrolledCourses([
      {
        id: '1',
        title: 'React Fundamentals',
        progress: 60,
        mentor: { name: 'Sarah Chen', id: '1' },
      },
      {
        id: '2',
        title: 'Advanced TypeScript',
        progress: 30,
        mentor: { name: 'Mike Johnson', id: '2' },
      },
    ]);
    setRecommendedCourses([
      {
        id: '3',
        title: 'Data Science Essentials',
        progress: 0,
        mentor: { name: 'Priya Patel', id: '3' },
      },
      {
        id: '4',
        title: 'UI/UX Design',
        progress: 0,
        mentor: { name: 'Sarah Chen', id: '1' },
      },
    ]);
    setMentors([
      {
        id: '1',
        name: 'Sarah Chen',
        specialty: 'React & Frontend',
        rating: 4.9,
        avatarUrl: 'https://via.placeholder.com/100?text=Sarah',
      },
      {
        id: '2',
        name: 'Mike Johnson',
        specialty: 'Full Stack',
        rating: 4.8,
        avatarUrl: 'https://via.placeholder.com/100?text=Mike',
      },
    ]);
  }, []);

  if (userLoading) {
    return <div className="text-center py-12">Loading dashboard...</div>;
  }

  return (
    <div className="bg-gray-50 min-h-screen">
      {/* Welcome Header */}
      <div className="bg-white border-b border-gray-200 p-6">
        <div className="max-w-7xl mx-auto">
          <h1 className="text-3xl font-bold text-gray-900">
            Welcome back, {user?.firstName}! 👋
          </h1>
          <p className="text-gray-600 mt-2">
            You've completed {stats.courses} courses • {stats.points} points earned this week
          </p>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-12">
          <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200 text-center">
            <div className="text-3xl font-bold text-primary-600">{stats.courses}</div>
            <p className="text-gray-600 mt-2">Courses Enrolled</p>
          </div>
          <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200 text-center">
            <div className="text-3xl font-bold text-vibrant-purple">{stats.mentors}</div>
            <p className="text-gray-600 mt-2">Mentors Following</p>
          </div>
          <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200 text-center">
            <div className="text-3xl font-bold text-vibrant-orange">{stats.points}</div>
            <p className="text-gray-600 mt-2">Points Earned</p>
          </div>
        </div>

        {/* Enrolled Courses */}
        <div className="mb-12">
          <h2 className="text-2xl font-bold text-gray-900 mb-6">My Active Courses</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {enrolledCourses.map((course) => (
              <div key={course.id} className="bg-white rounded-lg overflow-hidden shadow-sm border border-gray-200">
                <div className="p-6">
                  <h3 className="text-lg font-bold text-gray-900">{course.title}</h3>
                  <p className="text-sm text-gray-600 mt-2">Instructor: {course.mentor.name}</p>
                  <div className="mt-4">
                    <div className="flex justify-between items-center mb-2">
                      <span className="text-sm font-semibold text-gray-700">Progress</span>
                      <span className="text-sm font-bold text-primary-600">{course.progress}%</span>
                    </div>
                    <div className="w-full bg-gray-200 rounded-full h-2">
                      <div
                        className="bg-primary-600 h-2 rounded-full transition-all"
                        style={{ width: `${course.progress}%` }}
                      ></div>
                    </div>
                  </div>
                  <button className="mt-4 w-full px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition font-semibold">
                    Continue →
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Recommended Courses */}
        <div className="mb-12">
          <h2 className="text-2xl font-bold text-gray-900 mb-6">Recommended For You</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {recommendedCourses.map((course) => (
              <div key={course.id} className="bg-white rounded-lg overflow-hidden shadow-sm border border-gray-200 hover:shadow-md transition cursor-pointer">
                <div className="h-32 bg-gradient-to-r from-primary-500 to-blue-500"></div>
                <div className="p-4">
                  <h3 className="font-bold text-gray-900">{course.title}</h3>
                  <p className="text-sm text-gray-600 mt-2">by {course.mentor.name}</p>
                  <button className="mt-4 w-full px-3 py-2 bg-gray-100 text-gray-900 rounded-lg hover:bg-gray-200 transition font-semibold text-sm">
                    Enroll Now
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Mentor Connections */}
        <div>
          <h2 className="text-2xl font-bold text-gray-900 mb-6">Your Mentor Connections</h2>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
            {mentors.map((mentor) => (
              <div key={mentor.id} className="bg-white rounded-lg p-4 shadow-sm border border-gray-200 text-center">
                <img
                  src={mentor.avatarUrl}
                  alt={mentor.name}
                  className="w-16 h-16 rounded-full mx-auto mb-4 object-cover"
                />
                <h3 className="font-bold text-gray-900">{mentor.name}</h3>
                <p className="text-xs text-primary-600 font-semibold mt-1">{mentor.specialty}</p>
                <div className="flex justify-center items-center gap-1 mt-2">
                  <span className="text-yellow-500">★</span>
                  <span className="text-sm font-semibold text-gray-700">{mentor.rating}</span>
                </div>
                <button className="mt-4 w-full px-3 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition font-semibold text-sm">
                  Message
                </button>
              </div>
            ))}
            <div className="bg-gray-50 rounded-lg p-4 border-2 border-dashed border-gray-300 flex items-center justify-center text-center min-h-48">
              <div>
                <p className="text-gray-600 font-semibold mb-3">Find More Mentors</p>
                <button className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition font-semibold text-sm">
                  Browse Mentors
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
