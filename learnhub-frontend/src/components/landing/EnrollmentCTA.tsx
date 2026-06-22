import React from 'react';

interface EnrollmentCTAProps {
  onEnroll: () => void;
}

export const EnrollmentCTA: React.FC<EnrollmentCTAProps> = ({ onEnroll }) => {
  return (
    <section className="py-20 px-4">
      <div className="max-w-5xl mx-auto">
        <div className="relative rounded-3xl overflow-hidden">
          <div className="absolute inset-0 bg-gradient-to-br from-vibrant-purple via-vibrant-pink to-vibrant-orange opacity-100"></div>

          <div className="absolute inset-0 opacity-20">
            <div className="absolute top-10 left-10 w-40 h-40 bg-white rounded-full filter blur-3xl"></div>
            <div className="absolute bottom-10 right-10 w-40 h-40 bg-white rounded-full filter blur-3xl"></div>
          </div>

          <div className="relative z-10 px-6 md:px-12 py-16 md:py-20">
            <div className="text-center">
              <div className="inline-block mb-6">
                <span className="text-6xl md:text-7xl">🚀</span>
              </div>

              <h2 className="text-4xl md:text-5xl font-bold text-white mb-4">
                Ready to Transform Your Future?
              </h2>

              <p className="text-xl text-white/90 max-w-2xl mx-auto mb-8">
                Start your learning journey today with our free trial. No credit card required. Access all beginner courses and track your progress instantly.
              </p>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8 max-w-2xl mx-auto text-white">
                <div className="flex items-center gap-2 justify-center">
                  <span className="text-2xl">✓</span>
                  <span className="font-semibold">Lifetime Access</span>
                </div>
                <div className="flex items-center gap-2 justify-center">
                  <span className="text-2xl">✓</span>
                  <span className="font-semibold">24/7 Support</span>
                </div>
                <div className="flex items-center gap-2 justify-center">
                  <span className="text-2xl">✓</span>
                  <span className="font-semibold">Certificates</span>
                </div>
              </div>

              <button
                onClick={onEnroll}
                className="px-10 py-4 bg-white text-vibrant-purple font-bold text-lg rounded-2xl shadow-clay-lg hover:shadow-clay-lg hover:scale-105 transition-all duration-300 transform inline-block mb-4"
              >
                Get Started Free →
              </button>

              <p className="text-white/70 text-sm">
                Join 50,000+ learners already making progress. Your next chapter starts here.
              </p>
            </div>

            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-16 pt-12 border-t border-white/20">
              <div className="text-center text-white">
                <p className="text-3xl font-bold mb-1">99%</p>
                <p className="text-sm opacity-75">Would Recommend</p>
              </div>
              <div className="text-center text-white">
                <p className="text-3xl font-bold mb-1">4.9★</p>
                <p className="text-sm opacity-75">Average Rating</p>
              </div>
              <div className="text-center text-white">
                <p className="text-3xl font-bold mb-1">35hr</p>
                <p className="text-sm opacity-75">Avg Time to Cert</p>
              </div>
              <div className="text-center text-white">
                <p className="text-3xl font-bold mb-1">95%</p>
                <p className="text-sm opacity-75">Completion Rate</p>
              </div>
            </div>
          </div>
        </div>

        <div className="mt-16 p-8 rounded-2xl shadow-clay bg-white">
          <h3 className="text-2xl font-bold text-slate-900 mb-6 text-center">
            Why Choose LearnHub?
          </h3>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="flex gap-4">
              <span className="text-4xl flex-shrink-0">🎯</span>
              <div>
                <h4 className="font-bold text-slate-900 mb-2">Personalized Learning</h4>
                <p className="text-slate-600">Adaptive curriculum that adjusts to your pace and learning style</p>
              </div>
            </div>

            <div className="flex gap-4">
              <span className="text-4xl flex-shrink-0">👥</span>
              <div>
                <h4 className="font-bold text-slate-900 mb-2">Expert Instructors</h4>
                <p className="text-slate-600">Learn from industry professionals with 10+ years of experience</p>
              </div>
            </div>

            <div className="flex gap-4">
              <span className="text-4xl flex-shrink-0">📊</span>
              <div>
                <h4 className="font-bold text-slate-900 mb-2">Real-Time Analytics</h4>
                <p className="text-slate-600">Track progress with detailed insights and performance metrics</p>
              </div>
            </div>

            <div className="flex gap-4">
              <span className="text-4xl flex-shrink-0">🏆</span>
              <div>
                <h4 className="font-bold text-slate-900 mb-2">Industry Recognized</h4>
                <p className="text-slate-600">Earn certificates valued by top companies worldwide</p>
              </div>
            </div>

            <div className="flex gap-4">
              <span className="text-4xl flex-shrink-0">💬</span>
              <div>
                <h4 className="font-bold text-slate-900 mb-2">Community Support</h4>
                <p className="text-slate-600">Connect with peers, join study groups, and get help 24/7</p>
              </div>
            </div>

            <div className="flex gap-4">
              <span className="text-4xl flex-shrink-0">⏱️</span>
              <div>
                <h4 className="font-bold text-slate-900 mb-2">Learn at Your Pace</h4>
                <p className="text-slate-600">Flexible schedules with lifetime access to course materials</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
