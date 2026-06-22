/**
 * HomePage Component
 *
 * Landing page with hero section, featured courses, mentors, and CTAs.
 * Public page - accessible to all users (logged in or not).
 */

import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useUser } from '../context/UserContext';

interface Course {
  id: string;
  title: string;
  description: string;
  category: string;
  level: string;
  price: number;
  rating: number;
  enrolledCount: number;
  imageUrl: string;
}

interface Mentor {
  id: string;
  name: string;
  specialty: string;
  bio: string;
  hourlyRate: number;
  rating: number;
  reviewCount: number;
  avatarUrl: string;
}

export const HomePage: React.FC = () => {
  const { user } = useUser();
  const navigate = useNavigate();
  const [featuredCourses, setFeaturedCourses] = useState<Course[]>([]);
  const [featuredMentors, setFeaturedMentors] = useState<Mentor[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Mock data for featured courses
  const mockCourses: Course[] = [
    {
      id: '1',
      title: 'React Fundamentals',
      description: 'Master React from scratch with hands-on projects',
      category: 'Web Development',
      level: 'Beginner',
      price: 49,
      rating: 4.8,
      enrolledCount: 5240,
      imageUrl: 'https://via.placeholder.com/300x200?text=React+Fundamentals',
    },
    {
      id: '2',
      title: 'Advanced TypeScript',
      description: 'Build type-safe applications with TypeScript',
      category: 'Web Development',
      level: 'Advanced',
      price: 79,
      rating: 4.9,
      enrolledCount: 3100,
      imageUrl: 'https://via.placeholder.com/300x200?text=Advanced+TypeScript',
    },
    {
      id: '3',
      title: 'Data Science Essentials',
      description: 'Learn Python, pandas, and machine learning basics',
      category: 'Data Science',
      level: 'Beginner',
      price: 59,
      rating: 4.7,
      enrolledCount: 2890,
      imageUrl: 'https://via.placeholder.com/300x200?text=Data+Science',
    },
    {
      id: '4',
      title: 'UI/UX Design Masterclass',
      description: 'Create stunning user interfaces and experiences',
      category: 'Design',
      level: 'Intermediate',
      price: 69,
      rating: 4.6,
      enrolledCount: 1950,
      imageUrl: 'https://via.placeholder.com/300x200?text=UI+UX+Design',
    },
  ];

  // Mock data for featured mentors
  const mockMentors: Mentor[] = [
    {
      id: '1',
      name: 'Sarah Chen',
      specialty: 'React & Frontend',
      bio: 'Senior Engineer at TechCorp with 8 years of experience',
      hourlyRate: 45,
      rating: 4.9,
      reviewCount: 342,
      avatarUrl: 'https://via.placeholder.com/150?text=Sarah+Chen',
    },
    {
      id: '2',
      name: 'Mike Johnson',
      specialty: 'Full Stack Development',
      bio: 'Startup founder and passionate mentor',
      hourlyRate: 50,
      rating: 4.8,
      reviewCount: 286,
      avatarUrl: 'https://via.placeholder.com/150?text=Mike+Johnson',
    },
    {
      id: '3',
      name: 'Priya Patel',
      specialty: 'Data Science & ML',
      bio: 'PhD in Computer Science, ML researcher',
      hourlyRate: 55,
      rating: 4.7,
      reviewCount: 198,
      avatarUrl: 'https://via.placeholder.com/150?text=Priya+Patel',
    },
  ];

  useEffect(() => {
    // Simulate loading
    setIsLoading(true);
    setTimeout(() => {
      setFeaturedCourses(mockCourses);
      setFeaturedMentors(mockMentors);
      setIsLoading(false);
    }, 500);
  }, []);

  return (
    <div className="bg-white">
      {/* Hero Section */}
      <section className="relative bg-gradient-to-r from-primary-600 via-primary-500 to-blue-600 text-white py-20 md:py-32">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-12 items-center">
            <div>
              <h1 className="text-4xl md:text-5xl font-bold mb-6 leading-tight">
                Learn From Industry Experts
              </h1>
              <p className="text-xl mb-8 text-white/90">
                Master skills with personalized mentor guidance and hands-on projects. Get mentored by top professionals.
              </p>
              <div className="flex gap-4">
                <button
                  onClick={() => (user ? navigate('/dashboard') : navigate('/signup'))}
                  className="px-8 py-3 bg-white text-primary-600 font-bold rounded-lg hover:bg-gray-100 transition shadow-lg"
                >
                  {user ? 'Go to Dashboard' : 'Get Started Free'}
                </button>
                <button
                  onClick={() => navigate('/courses')}
                  className="px-8 py-3 border-2 border-white text-white font-bold rounded-lg hover:bg-white/10 transition"
                >
                  Explore Courses
                </button>
              </div>
            </div>
            <div className="hidden md:block">
              <div className="bg-white/20 rounded-lg p-12 backdrop-blur-sm">
                <div className="space-y-4">
                  <div className="flex items-center gap-3">
                    <div className="w-12 h-12 bg-white/30 rounded-full"></div>
                    <div className="h-4 bg-white/30 rounded w-48"></div>
                  </div>
                  <div className="flex items-center gap-3">
                    <div className="w-12 h-12 bg-white/30 rounded-full"></div>
                    <div className="h-4 bg-white/30 rounded w-40"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Feature Highlights */}
      <section className="py-16 md:py-24 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 className="text-3xl font-bold text-center mb-16">Why Choose LearnHub?</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            {[
              {
                icon: '👨‍🏫',
                title: 'Expert Mentors',
                description: 'Connect with vetted professionals in your field',
              },
              {
                icon: '📚',
                title: 'Live Projects',
                description: 'Build real-world projects for your portfolio',
              },
              {
                icon: '⏰',
                title: 'Flexible Learning',
                description: 'Learn at your own pace, anytime, anywhere',
              },
            ].map((feature, idx) => (
              <div key={idx} className="bg-white rounded-lg p-8 text-center shadow-sm border border-gray-200">
                <div className="text-4xl mb-4">{feature.icon}</div>
                <h3 className="text-xl font-bold mb-3 text-gray-900">{feature.title}</h3>
                <p className="text-gray-600">{feature.description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Featured Courses */}
      <section className="py-16 md:py-24 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center mb-12">
            <h2 className="text-3xl font-bold text-gray-900">Featured Courses</h2>
            <a href="/courses" className="text-primary-600 font-semibold hover:text-primary-700 flex items-center gap-2">
              View All Courses <span>→</span>
            </a>
          </div>

          {isLoading ? (
            <div className="text-center py-12">Loading courses...</div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
              {featuredCourses.map((course) => (
                <div
                  key={course.id}
                  className="bg-white rounded-lg overflow-hidden shadow-sm border border-gray-200 hover:shadow-md transition cursor-pointer"
                  onClick={() => navigate(`/courses/${course.id}`)}
                >
                  <img src={course.imageUrl} alt={course.title} className="w-full h-40 object-cover" />
                  <div className="p-4">
                    <span className="text-xs font-semibold text-primary-600 bg-primary-50 px-2 py-1 rounded">
                      {course.category}
                    </span>
                    <h3 className="font-bold text-gray-900 mt-2 line-clamp-2">{course.title}</h3>
                    <p className="text-sm text-gray-600 mt-2 line-clamp-2">{course.description}</p>
                    <div className="flex justify-between items-center mt-4">
                      <span className="text-lg font-bold text-gray-900">${course.price}</span>
                      <div className="flex items-center gap-1">
                        <span className="text-yellow-500">★</span>
                        <span className="text-sm font-semibold text-gray-700">{course.rating}</span>
                      </div>
                    </div>
                    <p className="text-xs text-gray-500 mt-2">{course.enrolledCount} students</p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* Featured Mentors */}
      <section className="py-16 md:py-24 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center mb-12">
            <h2 className="text-3xl font-bold text-gray-900">Top Mentors</h2>
            <a href="/mentors" className="text-primary-600 font-semibold hover:text-primary-700 flex items-center gap-2">
              View All Mentors <span>→</span>
            </a>
          </div>

          {isLoading ? (
            <div className="text-center py-12">Loading mentors...</div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {featuredMentors.map((mentor) => (
                <div
                  key={mentor.id}
                  className="bg-white rounded-lg p-6 shadow-sm border border-gray-200 hover:shadow-md transition text-center"
                >
                  <img
                    src={mentor.avatarUrl}
                    alt={mentor.name}
                    className="w-16 h-16 rounded-full mx-auto mb-4 object-cover"
                  />
                  <h3 className="text-lg font-bold text-gray-900">{mentor.name}</h3>
                  <p className="text-sm text-primary-600 font-semibold">{mentor.specialty}</p>
                  <p className="text-sm text-gray-600 mt-2 line-clamp-2">{mentor.bio}</p>
                  <div className="flex justify-center items-center gap-3 mt-4">
                    <div className="flex items-center">
                      <span className="text-yellow-500">★</span>
                      <span className="text-sm font-semibold ml-1">{mentor.rating}</span>
                      <span className="text-xs text-gray-500 ml-1">({mentor.reviewCount})</span>
                    </div>
                    <span className="text-gray-700 font-bold">${mentor.hourlyRate}/hr</span>
                  </div>
                  <button className="mt-4 w-full px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition font-semibold">
                    View Profile
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* CTA Section */}
      <section className="bg-gradient-to-r from-primary-600 to-blue-600 text-white py-16 md:py-24">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <h2 className="text-3xl md:text-4xl font-bold mb-6">Join Thousands of Learners Today</h2>
          <p className="text-lg mb-8 text-white/90">
            Start your learning journey and transform your career with expert guidance
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <button
              onClick={() => (user ? navigate('/courses') : navigate('/signup'))}
              className="px-8 py-3 bg-white text-primary-600 font-bold rounded-lg hover:bg-gray-100 transition"
            >
              {user ? 'Browse Courses' : 'Sign Up Now'}
            </button>
            <button
              onClick={() => navigate('/mentors')}
              className="px-8 py-3 border-2 border-white text-white font-bold rounded-lg hover:bg-white/10 transition"
            >
              Find a Mentor
            </button>
          </div>
        </div>
      </section>
    </div>
  );
};

export default HomePage;
