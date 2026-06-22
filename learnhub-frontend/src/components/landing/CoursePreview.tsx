import React from 'react';

interface Course {
  id: number;
  title: string;
  category: string;
  icon: string;
  students: number;
  rating: number;
  color: string;
  lightColor: string;
  accentColor: string;
}

const courses: Course[] = [
  {
    id: 1,
    title: 'Web Development Masterclass',
    category: 'Web Design',
    icon: '💻',
    students: 12500,
    rating: 4.9,
    color: 'vibrant-cyan',
    lightColor: 'cyan-50',
    accentColor: 'cyan-100',
  },
  {
    id: 2,
    title: 'Data Science Essentials',
    category: 'Analytics',
    icon: '📊',
    students: 8900,
    rating: 4.8,
    color: 'vibrant-purple',
    lightColor: 'purple-50',
    accentColor: 'purple-100',
  },
  {
    id: 3,
    title: 'Creative Design Bootcamp',
    category: 'Design',
    icon: '🎨',
    students: 10200,
    rating: 4.9,
    color: 'vibrant-pink',
    lightColor: 'pink-50',
    accentColor: 'pink-100',
  },
  {
    id: 4,
    title: 'Mobile App Development',
    category: 'Mobile',
    icon: '📱',
    students: 7600,
    rating: 4.7,
    color: 'vibrant-orange',
    lightColor: 'orange-50',
    accentColor: 'orange-100',
  },
];

export const CoursePreview: React.FC = () => {
  return (
    <section className="py-20 px-4">
      <div className="max-w-6xl mx-auto">
        <div className="text-center mb-16">
          <h2 className="text-4xl md:text-5xl font-bold text-slate-900 mb-4">
            Explore Our <span className="text-transparent bg-clip-text bg-gradient-to-r from-vibrant-purple to-vibrant-pink">Course Catalog</span>
          </h2>
          <p className="text-slate-600 text-lg max-w-2xl mx-auto">
            Discover hundreds of courses taught by industry experts. From beginner to advanced, find your perfect learning path.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {courses.map((course) => (
            <div
              key={course.id}
              className="group relative rounded-3xl shadow-clay hover:shadow-clay-lg transition-all duration-300 transform hover:-translate-y-2 cursor-pointer p-6 bg-white overflow-hidden"
            >
              <div className={`absolute inset-0 bg-gradient-to-br from-${course.lightColor} to-${course.accentColor} opacity-0 group-hover:opacity-100 transition-opacity duration-300`}></div>

              <div className="relative z-10">
                <div className="text-5xl mb-4">{course.icon}</div>

                <div className="mb-3">
                  <p className={`text-sm font-bold text-${course.color} uppercase tracking-widest`}>
                    {course.category}
                  </p>
                </div>

                <h3 className="text-xl font-bold text-slate-900 mb-4 leading-snug group-hover:text-slate-800 transition-colors">
                  {course.title}
                </h3>

                <div className="space-y-3 mb-6">
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-slate-600">👥 {course.students.toLocaleString()} students</span>
                    <span className="text-sm font-bold text-slate-900">⭐ {course.rating}</span>
                  </div>
                </div>

                <button className={`w-full py-2 px-4 rounded-xl font-bold text-white bg-gradient-to-r from-${course.color} to-${course.accentColor} opacity-0 group-hover:opacity-100 transform translate-y-4 group-hover:translate-y-0 transition-all duration-300`}>
                  View Course
                </button>
              </div>
            </div>
          ))}
        </div>

        <div className="mt-12 text-center">
          <button className="px-8 py-3 bg-slate-900 text-white font-bold rounded-xl hover:bg-slate-800 transition-colors">
            View All Courses →
          </button>
        </div>
      </div>
    </section>
  );
};
