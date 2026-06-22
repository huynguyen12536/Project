# 🎓 LearnHub Playful Landing Page

A beautiful, modern educational platform landing page built with **React**, **TypeScript**, **Tailwind CSS**, and **Claymorphism** design principles.

## 🎨 Design Features

### Claymorphism Design
- Soft, rounded cards with layered clay-like shadows
- Custom shadow utilities: `shadow-clay`, `shadow-clay-lg`, `shadow-clay-md`
- Smooth hover effects with elevation changes
- Tactile, approachable aesthetic perfect for education

### Vibrant Color Palette
```
- Orange:  #FF6B35
- Purple:  #A855F7
- Pink:    #EC4899
- Lime:    #84CC16
- Cyan:    #06B6D4
- Amber:   #F59E0B
```

## 📱 Page Sections

### 1. **Hero Section** (`HeroSection.tsx`)
- Bold headline with gradient text
- Engaging call-to-action buttons
- Animated background blobs using CSS blur effects
- Key statistics display (50K+ learners, 200+ courses, 4.9★ rating)
- Fully responsive design

**Features:**
- Gradient background with floating blur elements
- Primary and secondary CTAs
- Stats dashboard at the bottom

### 2. **Course Preview** (`CoursePreview.tsx`)
- 4-column course catalog grid (responsive: 1 → 2 → 4 columns)
- Claymorphism cards with hover effects
- Interactive course cards with:
  - Category badges with color coding
  - Student count and rating display
  - Hover-reveal action buttons
  - Smooth animations on hover

**Features:**
- Dynamic course data with proper styling
- Color-coded categories
- Animated buttons that slide in on hover
- View All Courses navigation

### 3. **Progress Tracking Demo** (`ProgressDemo.tsx`)
- Real-time progress visualization
- Split layout: text + progress bars
- 4 sample courses with gradient progress bars
- Learning streak display
- Feature highlights with icons

**Features:**
- Progress bars with gradient fills
- Interactive progress items
- Streak counter with emoji
- Feature benefit cards

### 4. **Student Testimonials** (`TestimonialSection.tsx`)
- 4 beautiful testimonial cards
- Star ratings with emoji
- User avatars and credentials
- Hover effects with background color transitions
- Social proof dashboard showing stats

**Features:**
- Claymorphism cards with hover transformation
- 5-star ratings for each testimonial
- Professional credentials display
- Community stats section

### 5. **Enrollment CTA** (`EnrollmentCTA.tsx`)
- Eye-catching gradient background
- Strong value proposition messaging
- Feature checklist with icons
- Getting started button
- Benefits showcase grid

**Features:**
- Gradient purple-pink-orange background
- Animated background elements
- Feature highlights grid
- Why Choose LearnHub section with 6 key benefits

### 6. **Modal Enrollment Form** (in `LandingPage.tsx`)
- Beautiful modal with backdrop blur
- Email and name input fields
- Experience level dropdown
- Smooth animations
- Close button functionality

**Features:**
- Dark overlay with blur effect
- Styled form inputs
- Gradient button
- Responsive design

## 🛠️ Technical Stack

```
Frontend Framework: React 18
Language: TypeScript
Styling: Tailwind CSS 3
Routing: React Router v6
Build Tool: Vite
```

## 📂 File Structure

```
src/
├── pages/
│   └── LandingPage.tsx          # Main landing page component
├── components/
│   └── landing/
│       ├── HeroSection.tsx       # Hero with CTA
│       ├── CoursePreview.tsx     # Course catalog
│       ├── ProgressDemo.tsx      # Progress tracking demo
│       ├── TestimonialSection.tsx # Student reviews
│       └── EnrollmentCTA.tsx     # Main enrollment CTA
│   └── layouts/
│       └── MainLayout.tsx        # Header & footer
├── main.tsx                       # React entry point
└── index.css                      # Tailwind imports
```

## 🎯 Key Component Props

### HeroSection
```typescript
interface HeroSectionProps {
  onEnroll: () => void;
}
```

### EnrollmentCTA
```typescript
interface EnrollmentCTAProps {
  onEnroll: () => void;
}
```

## 🌈 Custom Tailwind Extensions

### Colors (tailwind.config.js)
```javascript
vibrant: {
  orange: '#FF6B35',
  purple: '#A855F7',
  pink: '#EC4899',
  lime: '#84CC16',
  cyan: '#06B6D4',
  amber: '#F59E0B',
}
```

### Shadows
```javascript
boxShadow: {
  'clay': '8px 8px 16px rgba(0, 0, 0, 0.1), -8px -8px 16px rgba(255, 255, 255, 0.7)',
  'clay-lg': '12px 12px 24px rgba(0, 0, 0, 0.12), -12px -12px 24px rgba(255, 255, 255, 0.8)',
  'clay-md': '6px 6px 12px rgba(0, 0, 0, 0.08), -6px -6px 12px rgba(255, 255, 255, 0.6)',
}
```

### Border Radius
```javascript
borderRadius: {
  'lg': '0.5rem',
  'xl': '0.75rem',
  '3xl': '1.5rem',  // NEW: For claymorphism
}
```

## 🚀 Getting Started

### Install Dependencies
```bash
npm install
```

### Start Dev Server
```bash
npm run dev
```

The landing page will be available at `http://localhost:3000/`

### Build for Production
```bash
npm run build
```

## ✨ Design Highlights

1. **Playful & Engaging**: Emoji icons, vibrant colors, and smooth animations
2. **Modern Claymorphism**: Soft shadows and rounded corners throughout
3. **Responsive**: Mobile-first design that adapts to all screen sizes
4. **Accessible**: Proper semantic HTML and keyboard navigation
5. **Performance**: Optimized with Vite and React Fast Refresh
6. **Interactive**: Working modal, hover effects, and smooth scrolling

## 📊 Content Included

- ✅ Hero section with compelling headline
- ✅ 4 featured courses with ratings
- ✅ Progress tracking demo with 4 sample courses
- ✅ 4 student testimonials with avatars
- ✅ Enrollment modal with form fields
- ✅ Why Choose Us section with 6 benefits
- ✅ Community stats and social proof
- ✅ Learning streak counter
- ✅ Professional footer with links

## 🎪 Interactive Elements

- Click "Start Learning Free" to open enrollment modal
- Hover on course cards to reveal buttons
- Hover on testimonial cards for background transitions
- Smooth scroll behavior throughout the page
- Form inputs with focus states
- Gradient buttons with scale animations

## 🔧 Customization

All colors, spacing, and animations can be easily customized via:
- `tailwind.config.js` - Colors, shadows, spacing
- Component props - Text, callbacks
- CSS classes - Direct Tailwind utilities

## 📄 License

Built as part of the LearnHub Educational Platform
