/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        jakarta: ['Plus Jakarta Sans', 'sans-serif'],
        inter: ['Inter', 'system-ui', 'sans-serif'],
      },
      colors: {
        lh: {
          dark: '#15162E',
          navy: '#242582',
          blue: '#2F2FA2',
          purple: '#553D67',
          mauve: '#99738E',
          pink: '#F64C72',
          'pink-dark': '#E23B61',
          muted: '#6A6E84',
          border: '#E7E9F2',
          surface: '#F6F7FB',
          input: '#E1E4ED',
        },
        primary: {
          50: '#f0f9ff',
          100: '#e0f2fe',
          500: '#0ea5e9',
          600: '#0284c7',
          700: '#0369a1',
        },
        success: {
          50: '#f0fdf4',
          500: '#22c55e',
          600: '#16a34a',
        },
        error: {
          50: '#fef2f2',
          500: '#ef4444',
          600: '#dc2626',
        },
        vibrant: {
          orange: '#FF6B35',
          purple: '#A855F7',
          pink: '#EC4899',
          lime: '#84CC16',
          cyan: '#06B6D4',
          amber: '#F59E0B',
        },
      },
      spacing: {
        '128': '32rem',
      },
      borderRadius: {
        'lg': '0.5rem',
        'xl': '0.75rem',
        '3xl': '1.5rem',
      },
      boxShadow: {
        'clay': '8px 8px 16px rgba(0, 0, 0, 0.1), -8px -8px 16px rgba(255, 255, 255, 0.7)',
        'clay-lg': '12px 12px 24px rgba(0, 0, 0, 0.12), -12px -12px 24px rgba(255, 255, 255, 0.8)',
        'clay-md': '6px 6px 12px rgba(0, 0, 0, 0.08), -6px -6px 12px rgba(255, 255, 255, 0.6)',
      },
    },
  },
  plugins: [],
}
