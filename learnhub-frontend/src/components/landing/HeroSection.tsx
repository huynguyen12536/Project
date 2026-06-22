import React from 'react';

interface HeroSectionProps {
  onEnroll: () => void;
}

export const HeroSection: React.FC<HeroSectionProps> = ({ onEnroll }) => {
  return (
    <section className="relative min-h-screen flex items-center justify-center px-4 py-20 overflow-hidden">
      <div className="absolute inset-0 overflow-hidden">
        <div className="absolute top-20 left-10 w-72 h-72 bg-vibrant-orange/10 rounded-full blur-3xl"></div>
        <div className="absolute bottom-20 right-10 w-72 h-72 bg-vibrant-purple/10 rounded-full blur-3xl"></div>
        <div className="absolute top-1/2 left-1/2 w-72 h-72 bg-vibrant-pink/5 rounded-full blur-3xl"></div>
      </div>

      <div className="relative z-10 max-w-4xl mx-auto text-center">
        <div className="mb-6 inline-block">
          <span className="bg-gradient-to-r from-vibrant-orange to-vibrant-amber px-4 py-2 rounded-full text-sm font-bold text-white shadow-clay-md">
            🎓 Welcome to LearnHub
          </span>
        </div>

        <h1 className="text-5xl md:text-7xl font-bold text-slate-900 mb-6 leading-tight">
          Learn Anything,
          <span className="bg-gradient-to-r from-vibrant-purple via-vibrant-pink to-vibrant-orange bg-clip-text text-transparent"> Anytime</span>
        </h1>

        <p className="text-lg md:text-xl text-slate-600 max-w-2xl mx-auto mb-8">
          Master new skills with our interactive courses, personalized learning paths, and real-time progress tracking. Join a community of learners transforming their futures.
        </p>

        <div className="flex flex-col sm:flex-row gap-4 justify-center mb-12">
          <button
            onClick={onEnroll}
            className="px-8 py-4 bg-gradient-to-r from-vibrant-purple to-vibrant-pink text-white font-bold rounded-2xl shadow-clay-lg hover:shadow-clay-lg hover:scale-105 transition-all duration-300 transform"
          >
            Start Learning Free
          </button>
          <button className="px-8 py-4 bg-white text-slate-900 font-bold rounded-2xl shadow-clay hover:shadow-clay-lg transition-all duration-300 border-2 border-slate-200">
            Explore Courses
          </button>
        </div>

        <div className="grid grid-cols-3 gap-4 md:gap-8 max-w-2xl mx-auto">
          <div className="py-4">
            <p className="text-3xl md:text-4xl font-bold text-vibrant-purple">50K+</p>
            <p className="text-slate-600 text-sm md:text-base">Active Learners</p>
          </div>
          <div className="py-4">
            <p className="text-3xl md:text-4xl font-bold text-vibrant-orange">200+</p>
            <p className="text-slate-600 text-sm md:text-base">Expert Courses</p>
          </div>
          <div className="py-4">
            <p className="text-3xl md:text-4xl font-bold text-vibrant-pink">4.9/5</p>
            <p className="text-slate-600 text-sm md:text-base">Avg Rating</p>
          </div>
        </div>
      </div>
    </section>
  );
};
