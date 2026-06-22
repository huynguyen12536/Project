/**
 * CourseListing Component
 *
 * Public page displaying all courses with search, filters, and pagination.
 */

import React, { useState, useEffect } from 'react';

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
  mentorName: string;
}

type FilterKey = 'category' | 'level' | 'price' | 'rating';

export const CourseListing: React.FC = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [filters, setFilters] = useState<Record<FilterKey, string[]>>({
    category: [],
    level: [],
    price: [],
    rating: [],
  });
  const [currentPage, setCurrentPage] = useState(1);
  const [isLoading, setIsLoading] = useState(false);

  // Mock courses data
  const allCourses: Course[] = [
    {
      id: '1',
      title: 'React Fundamentals',
      description: 'Master React from scratch with hands-on projects',
      category: 'Web Development',
      level: 'Beginner',
      price: 49,
      rating: 4.8,
      enrolledCount: 5240,
      imageUrl: 'https://via.placeholder.com/300x200?text=React',
      mentorName: 'Sarah Chen',
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
      imageUrl: 'https://via.placeholder.com/300x200?text=TypeScript',
      mentorName: 'Mike Johnson',
    },
    {
      id: '3',
      title: 'Data Science Essentials',
      description: 'Learn Python, pandas, and ML basics',
      category: 'Data Science',
      level: 'Beginner',
      price: 59,
      rating: 4.7,
      enrolledCount: 2890,
      imageUrl: 'https://via.placeholder.com/300x200?text=Data+Science',
      mentorName: 'Priya Patel',
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
      imageUrl: 'https://via.placeholder.com/300x200?text=UI+UX',
      mentorName: 'Sarah Chen',
    },
    {
      id: '5',
      title: 'Python for Beginners',
      description: 'Start your programming journey with Python',
      category: 'Web Development',
      level: 'Beginner',
      price: 0,
      rating: 4.5,
      enrolledCount: 8920,
      imageUrl: 'https://via.placeholder.com/300x200?text=Python',
      mentorName: 'Mike Johnson',
    },
    {
      id: '6',
      title: 'Machine Learning Advanced',
      description: 'Deep dive into ML algorithms and techniques',
      category: 'Data Science',
      level: 'Advanced',
      price: 99,
      rating: 4.9,
      enrolledCount: 1200,
      imageUrl: 'https://via.placeholder.com/300x200?text=ML+Advanced',
      mentorName: 'Priya Patel',
    },
    {
      id: '7',
      title: 'UI Design Principles',
      description: 'Learn fundamental principles of UI design',
      category: 'Design',
      level: 'Beginner',
      price: 39,
      rating: 4.6,
      enrolledCount: 3450,
      imageUrl: 'https://via.placeholder.com/300x200?text=UI+Principles',
      mentorName: 'Sarah Chen',
    },
    {
      id: '8',
      title: 'Full Stack Web Development',
      description: 'Build complete web applications',
      category: 'Web Development',
      level: 'Intermediate',
      price: 89,
      rating: 4.8,
      enrolledCount: 2100,
      imageUrl: 'https://via.placeholder.com/300x200?text=Full+Stack',
      mentorName: 'Mike Johnson',
    },
    {
      id: '9',
      title: 'Data Visualization',
      description: 'Create impactful data visualizations',
      category: 'Data Science',
      level: 'Intermediate',
      price: 54,
      rating: 4.7,
      enrolledCount: 1850,
      imageUrl: 'https://via.placeholder.com/300x200?text=Data+Viz',
      mentorName: 'Priya Patel',
    },
    {
      id: '10',
      title: 'UX Research Methods',
      description: 'Master user research techniques',
      category: 'Design',
      level: 'Intermediate',
      price: 64,
      rating: 4.5,
      enrolledCount: 890,
      imageUrl: 'https://via.placeholder.com/300x200?text=UX+Research',
      mentorName: 'Sarah Chen',
    },
    {
      id: '11',
      title: 'Node.js Backend',
      description: 'Build scalable backend services',
      category: 'Web Development',
      level: 'Intermediate',
      price: 74,
      rating: 4.7,
      enrolledCount: 2340,
      imageUrl: 'https://via.placeholder.com/300x200?text=Node.js',
      mentorName: 'Mike Johnson',
    },
    {
      id: '12',
      title: 'Advanced Data Analysis',
      description: 'Analyze complex datasets efficiently',
      category: 'Data Science',
      level: 'Advanced',
      price: 94,
      rating: 4.8,
      enrolledCount: 1100,
      imageUrl: 'https://via.placeholder.com/300x200?text=Data+Analysis',
      mentorName: 'Priya Patel',
    },
  ];

  // Filter courses
  const filteredCourses = allCourses.filter((course) => {
    const matchesSearch =
      course.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      course.description.toLowerCase().includes(searchQuery.toLowerCase());

    const matchesCategory = filters.category.length === 0 || filters.category.includes(course.category);
    const matchesLevel = filters.level.length === 0 || filters.level.includes(course.level);
    const matchesRating =
      filters.rating.length === 0 ||
      filters.rating.some((rating) => {
        const ratingValue = parseFloat(rating);
        return course.rating >= ratingValue;
      });

    const matchesPrice =
      filters.price.length === 0 ||
      filters.price.some((priceRange) => {
        if (priceRange === 'free') return course.price === 0;
        if (priceRange === 'under50') return course.price > 0 && course.price < 50;
        if (priceRange === '50-100') return course.price >= 50 && course.price <= 100;
        if (priceRange === 'over100') return course.price > 100;
        return true;
      });

    return matchesSearch && matchesCategory && matchesLevel && matchesRating && matchesPrice;
  });

  // Pagination
  const itemsPerPage = 12;
  const totalPages = Math.ceil(filteredCourses.length / itemsPerPage);
  const startIdx = (currentPage - 1) * itemsPerPage;
  const paginatedCourses = filteredCourses.slice(startIdx, startIdx + itemsPerPage);

  const handleFilterChange = (filterKey: FilterKey, value: string) => {
    setFilters((prev) => {
      const current = prev[filterKey];
      const updated = current.includes(value) ? current.filter((v) => v !== value) : [...current, value];
      return { ...prev, [filterKey]: updated };
    });
    setCurrentPage(1);
  };

  const resetFilters = () => {
    setFilters({ category: [], level: [], price: [], rating: [] });
    setSearchQuery('');
    setCurrentPage(1);
  };

  return (
    <div className="bg-gray-50 min-h-screen">
      {/* Page Header */}
      <div className="bg-white border-b border-gray-200 p-6">
        <div className="max-w-7xl mx-auto">
          <h1 className="text-3xl font-bold text-gray-900 mb-4">Explore Courses</h1>
          {/* Search Bar */}
          <div className="relative max-w-xl">
            <input
              type="text"
              placeholder="Search courses..."
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setCurrentPage(1);
              }}
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            />
            <span className="absolute right-3 top-3 text-gray-400">🔍</span>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
          {/* Sidebar Filters */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200">
              <h3 className="text-lg font-bold text-gray-900 mb-4">Filters</h3>

              {/* Category Filter */}
              <div className="mb-6">
                <h4 className="font-semibold text-gray-900 mb-3">Category</h4>
                {['Web Development', 'Data Science', 'Design'].map((cat) => (
                  <label key={cat} className="flex items-center gap-2 mb-2 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={filters.category.includes(cat)}
                      onChange={() => handleFilterChange('category', cat)}
                      className="w-4 h-4 rounded border-gray-300 text-primary-600"
                    />
                    <span className="text-gray-700">{cat}</span>
                  </label>
                ))}
              </div>

              {/* Level Filter */}
              <div className="mb-6 pb-6 border-b">
                <h4 className="font-semibold text-gray-900 mb-3">Level</h4>
                {['Beginner', 'Intermediate', 'Advanced'].map((level) => (
                  <label key={level} className="flex items-center gap-2 mb-2 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={filters.level.includes(level)}
                      onChange={() => handleFilterChange('level', level)}
                      className="w-4 h-4 rounded border-gray-300 text-primary-600"
                    />
                    <span className="text-gray-700">{level}</span>
                  </label>
                ))}
              </div>

              {/* Price Filter */}
              <div className="mb-6 pb-6 border-b">
                <h4 className="font-semibold text-gray-900 mb-3">Price</h4>
                {[
                  { label: 'Free', value: 'free' },
                  { label: 'Under $50', value: 'under50' },
                  { label: '$50 - $100', value: '50-100' },
                  { label: 'Over $100', value: 'over100' },
                ].map(({ label, value }) => (
                  <label key={value} className="flex items-center gap-2 mb-2 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={filters.price.includes(value)}
                      onChange={() => handleFilterChange('price', value)}
                      className="w-4 h-4 rounded border-gray-300 text-primary-600"
                    />
                    <span className="text-gray-700">{label}</span>
                  </label>
                ))}
              </div>

              {/* Rating Filter */}
              <div className="mb-6">
                <h4 className="font-semibold text-gray-900 mb-3">Rating</h4>
                {[
                  { label: '4.5+ ★★★★★', value: '4.5' },
                  { label: '4.0+ ★★★★☆', value: '4' },
                  { label: '3.5+ ★★★☆☆', value: '3.5' },
                ].map(({ label, value }) => (
                  <label key={value} className="flex items-center gap-2 mb-2 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={filters.rating.includes(value)}
                      onChange={() => handleFilterChange('rating', value)}
                      className="w-4 h-4 rounded border-gray-300 text-primary-600"
                    />
                    <span className="text-gray-700">{label}</span>
                  </label>
                ))}
              </div>

              <button
                onClick={resetFilters}
                className="w-full px-4 py-2 bg-gray-100 text-gray-900 rounded-lg hover:bg-gray-200 transition font-semibold"
              >
                Reset Filters
              </button>
            </div>
          </div>

          {/* Course Grid */}
          <div className="lg:col-span-3">
            {isLoading ? (
              <div className="text-center py-12">Loading courses...</div>
            ) : paginatedCourses.length === 0 ? (
              <div className="text-center py-12 bg-white rounded-lg border border-gray-200">
                <p className="text-gray-600 text-lg">No courses found. Try adjusting your filters.</p>
              </div>
            ) : (
              <>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
                  {paginatedCourses.map((course) => (
                    <div
                      key={course.id}
                      className="bg-white rounded-lg overflow-hidden shadow-sm border border-gray-200 hover:shadow-md transition cursor-pointer"
                    >
                      <img src={course.imageUrl} alt={course.title} className="w-full h-40 object-cover" />
                      <div className="p-4">
                        <span className="text-xs font-semibold text-primary-600 bg-primary-50 px-2 py-1 rounded">
                          {course.category}
                        </span>
                        <h3 className="font-bold text-gray-900 mt-2 line-clamp-2">{course.title}</h3>
                        <p className="text-sm text-gray-600 mt-2 line-clamp-2">{course.description}</p>
                        <div className="flex justify-between items-center mt-4">
                          <span className="text-lg font-bold text-gray-900">
                            {course.price === 0 ? 'Free' : `$${course.price}`}
                          </span>
                          <div className="flex items-center gap-1">
                            <span className="text-yellow-500">★</span>
                            <span className="text-sm font-semibold text-gray-700">{course.rating}</span>
                          </div>
                        </div>
                        <p className="text-xs text-gray-500 mt-2">{course.enrolledCount} students</p>
                        <button className="mt-4 w-full px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition font-semibold text-sm">
                          View Course
                        </button>
                      </div>
                    </div>
                  ))}
                </div>

                {/* Pagination */}
                <div className="flex justify-center items-center gap-2">
                  <button
                    onClick={() => setCurrentPage(Math.max(1, currentPage - 1))}
                    disabled={currentPage === 1}
                    className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-100 transition disabled:opacity-50"
                  >
                    ← Previous
                  </button>
                  <span className="px-4 py-2 text-gray-700 font-semibold">
                    Page {currentPage} of {totalPages}
                  </span>
                  <button
                    onClick={() => setCurrentPage(Math.min(totalPages, currentPage + 1))}
                    disabled={currentPage === totalPages}
                    className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-100 transition disabled:opacity-50"
                  >
                    Next →
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default CourseListing;
