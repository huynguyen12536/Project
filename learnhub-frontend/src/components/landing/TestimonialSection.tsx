import React from 'react';

interface Testimonial {
  id: number;
  name: string;
  role: string;
  content: string;
  avatar: string;
  rating: number;
  color: string;
}

const testimonials: Testimonial[] = [
  {
    id: 1,
    name: 'Sarah Chen',
    role: 'Product Designer',
    content: 'LearnHub transformed how I approach design. The courses are engaging, interactive, and the community support is incredible. I landed my dream job thanks to the skills I learned!',
    avatar: '👩‍💼',
    rating: 5,
    color: 'from-vibrant-pink to-rose-400',
  },
  {
    id: 2,
    name: 'Marcus Johnson',
    role: 'Full Stack Developer',
    content: 'The depth of content and real-world projects on LearnHub are unmatched. The instructors genuinely care about your progress. Highly recommended for anyone serious about learning to code.',
    avatar: '👨‍💻',
    rating: 5,
    color: 'from-vibrant-cyan to-blue-400',
  },
  {
    id: 3,
    name: 'Emma Rodriguez',
    role: 'Data Analyst',
    content: 'I was intimidated by data science, but LearnHub broke everything down into digestible lessons. Now I\'m confidently analyzing datasets at my new role!',
    avatar: '👩‍🔬',
    rating: 5,
    color: 'from-vibrant-purple to-indigo-400',
  },
  {
    id: 4,
    name: 'David Kim',
    role: 'UX Researcher',
    content: 'The progress tracking and personalized learning path kept me motivated throughout. Best investment I\'ve made in myself. Worth every penny!',
    avatar: '👨‍🎓',
    rating: 5,
    color: 'from-vibrant-orange to-amber-400',
  },
];

export const TestimonialSection: React.FC = () => {
  return (
    <section className="py-20 px-4">
      <div className="max-w-6xl mx-auto">
        <div className="text-center mb-16">
          <h2 className="text-4xl md:text-5xl font-bold text-slate-900 mb-4">
            Loved by <span className="text-transparent bg-clip-text bg-gradient-to-r from-vibrant-orange to-vibrant-pink">50,000+ Learners</span>
          </h2>
          <p className="text-slate-600 text-lg">
            See what students around the world are saying about their LearnHub experience
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
          {testimonials.map((testimonial) => (
            <div
              key={testimonial.id}
              className="group relative rounded-3xl shadow-clay hover:shadow-clay-lg transition-all duration-300 transform hover:-translate-y-1 p-8 bg-white overflow-hidden"
            >
              <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-slate-100 to-slate-50 rounded-bl-3xl opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>

              <div className="relative z-10">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex gap-1">
                    {[...Array(testimonial.rating)].map((_, i) => (
                      <span key={i} className="text-lg">⭐</span>
                    ))}
                  </div>
                  <span className="text-4xl">{testimonial.avatar}</span>
                </div>

                <p className="text-slate-700 text-lg leading-relaxed mb-6 font-medium">
                  "{testimonial.content}"
                </p>

                <div className="flex items-center gap-3 pt-4 border-t border-slate-200">
                  <div className="w-12 h-12 rounded-full bg-gradient-to-br from-slate-200 to-slate-300 flex items-center justify-center text-xl">
                    {testimonial.avatar}
                  </div>
                  <div>
                    <h4 className="font-bold text-slate-900">{testimonial.name}</h4>
                    <p className="text-sm text-slate-500">{testimonial.role}</p>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>

        <div className="bg-gradient-to-r from-vibrant-purple/10 via-vibrant-pink/10 to-vibrant-orange/10 rounded-3xl shadow-clay p-8 md:p-12 text-center">
          <h3 className="text-3xl font-bold text-slate-900 mb-4">Join Our Learning Community</h3>
          <p className="text-slate-600 text-lg mb-6 max-w-2xl mx-auto">
            Be part of a global community of learners achieving their goals every single day
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center items-center flex-wrap">
            <div className="text-center">
              <p className="text-3xl font-bold text-vibrant-purple">50K+</p>
              <p className="text-slate-600">Active Learners</p>
            </div>
            <div className="text-slate-300">•</div>
            <div className="text-center">
              <p className="text-3xl font-bold text-vibrant-pink">200+</p>
              <p className="text-slate-600">Courses Available</p>
            </div>
            <div className="text-slate-300">•</div>
            <div className="text-center">
              <p className="text-3xl font-bold text-vibrant-orange">150+</p>
              <p className="text-slate-600">Expert Instructors</p>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
