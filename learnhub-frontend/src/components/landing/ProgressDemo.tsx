import React from 'react';

interface ProgressItem {
  title: string;
  progress: number;
  color: string;
  icon: string;
}

const progressItems: ProgressItem[] = [
  { title: 'Web Development', progress: 85, color: 'from-vibrant-cyan to-blue-500', icon: '💻' },
  { title: 'UI/UX Design', progress: 60, color: 'from-vibrant-pink to-rose-500', icon: '🎨' },
  { title: 'Data Analytics', progress: 45, color: 'from-vibrant-purple to-indigo-500', icon: '📊' },
  { title: 'Mobile Dev', progress: 92, color: 'from-vibrant-orange to-yellow-500', icon: '📱' },
];

export const ProgressDemo: React.FC = () => {
  return (
    <section className="py-20 px-4 bg-white/50">
      <div className="max-w-6xl mx-auto">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
          <div>
            <h2 className="text-4xl md:text-5xl font-bold text-slate-900 mb-6">
              Track Your <span className="text-transparent bg-clip-text bg-gradient-to-r from-vibrant-purple to-vibrant-pink">Progress</span> in Real-Time
            </h2>
            <p className="text-slate-600 text-lg mb-8">
              See your learning journey unfold with detailed progress analytics, skill assessments, and personalized recommendations based on your pace and goals.
            </p>

            <div className="space-y-4">
              <div className="flex items-start gap-4">
                <span className="text-3xl">📈</span>
                <div>
                  <h3 className="font-bold text-slate-900">Smart Analytics</h3>
                  <p className="text-slate-600">Visualize your learning patterns and improvement areas</p>
                </div>
              </div>
              <div className="flex items-start gap-4">
                <span className="text-3xl">🎯</span>
                <div>
                  <h3 className="font-bold text-slate-900">Personalized Goals</h3>
                  <p className="text-slate-600">Set milestones and get achievements along the way</p>
                </div>
              </div>
              <div className="flex items-start gap-4">
                <span className="text-3xl">🏆</span>
                <div>
                  <h3 className="font-bold text-slate-900">Badges & Certificates</h3>
                  <p className="text-slate-600">Earn recognition for completing courses and milestones</p>
                </div>
              </div>
            </div>
          </div>

          <div className="space-y-6">
            {progressItems.map((item, index) => (
              <div
                key={index}
                className="bg-white rounded-2xl shadow-clay p-6 hover:shadow-clay-lg transition-shadow duration-300"
              >
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-3">
                    <span className="text-3xl">{item.icon}</span>
                    <h3 className="font-bold text-slate-900">{item.title}</h3>
                  </div>
                  <span className="text-2xl font-bold text-transparent bg-clip-text bg-gradient-to-r" style={{ backgroundImage: `linear-gradient(to right, var(--color-${item.color.split(' ')[0].split('-')[2]}), var(--color-${item.color.split(' ')[1].split('-')[2]}))` }}>
                    {item.progress}%
                  </span>
                </div>

                <div className="w-full bg-slate-200 rounded-full h-3 overflow-hidden">
                  <div
                    className={`h-full bg-gradient-to-r ${item.color} rounded-full transition-all duration-500 ease-out`}
                    style={{ width: `${item.progress}%` }}
                  ></div>
                </div>

                <div className="flex justify-between items-center mt-3">
                  <span className="text-sm text-slate-500">{item.progress === 100 ? '✓ Completed' : `${100 - item.progress}% to go`}</span>
                  <span className="text-xs text-slate-400">Next lesson available</span>
                </div>
              </div>
            ))}

            <div className="bg-gradient-to-br from-vibrant-lime/20 to-vibrant-cyan/20 rounded-2xl shadow-clay p-6 border-2 border-vibrant-lime/30">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="font-bold text-slate-900 mb-1">Learning Streak</h3>
                  <p className="text-slate-600 text-sm">Keep up the momentum!</p>
                </div>
                <p className="text-4xl font-bold text-vibrant-lime">🔥 14</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
