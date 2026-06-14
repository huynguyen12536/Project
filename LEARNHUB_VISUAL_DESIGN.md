# LearnHub LMS — Visual Design Specification

> **Design System v1.0** | Figma-to-Code Reference
> Targeting Coursera / Udemy / Linear / Notion quality standards.
> All measurements in pixels. All colors from the established token set.

---

## GLOBAL DESIGN TOKENS

```
COLORS
  primary-600:        #30525C    (Deep teal — primary actions, hero bg)
  primary-500/400:    #4C848D    (Teal hover, light teal accents)
  primary-700:        #243E46    (Darker teal — pattern overlay)
  primary-100:        #D6E4E8    (Light teal — hero subtitle, muted text on dark)
  primary-200:        #ADC9CF    (Soft teal — secondary links on dark)
  primary-300:        #85AFB7    (Mid teal — trust bar text)
  primary-50:         #E9F2F4    (Success banner bg)

  secondary-500:      #C35627    (Warm orange — accents, errors, destructive)
  secondary-600:      #A3461F    (Darker orange — error text)
  secondary-400:      #D6794D    (Orange hover)
  secondary-50:       #FBE8E0    (Orange tint — error banner bg)

  neutral-50:         #FAFAFA    (Page background)
  neutral-100:        #F5F5F5    (Subtle surface)
  neutral-200:        #E5E5E5    (Borders, dividers)
  neutral-300:        #D4D4D4    ("or" divider lines)
  neutral-400:        #A3A3A3    (Disabled text, placeholder)
  neutral-500:        #737373    (Body text, secondary descriptions)
  neutral-600:        #525252    (Muted headings)
  neutral-700:        #404040    (Sub-headings)
  neutral-900:        #171717    (Primary text, headings)

  border:             #BFB9B5    (Card borders, input borders)
  surface:            #FFFFFF    (Card backgrounds, nav bg)
  error:              #C35627    (Validation error — same as secondary-500)
  success:            #16A34A    (Success green — check icons, verified states)

RADII
  card:      16px    (Feature cards, form cards, info cards)
  button:    8px     (All buttons, inputs, badges)
  pill:      9999px  (Fully rounded pills/badges)
  circle:    50%     (Icon circles, avatars)

SHADOWS
  sm:        0 1px 2px rgba(23, 23, 23, 0.06)
  md:        0 4px 6px -1px rgba(23, 23, 23, 0.07), 0 2px 4px -2px rgba(23, 23, 23, 0.05)
  lg:        0 10px 15px -3px rgba(23, 23, 23, 0.08), 0 4px 6px -4px rgba(23, 23, 23, 0.04)
  xl:        0 20px 25px -5px rgba(23, 23, 23, 0.1), 0 8px 10px -6px rgba(23, 23, 23, 0.04)
  focus:     0 0 0 3px rgba(76, 132, 141, 0.35)

TYPOGRAPHY
  Font:      Inter (400, 500, 600, 700)
  Display:   56px / 60px  weight 700  letter-spacing: -0.02em
  H1:        40px / 48px  weight 700  letter-spacing: -0.02em
  H2:        36px / 44px  weight 700  letter-spacing: -0.015em
  H3:        24px / 32px  weight 700  letter-spacing: -0.01em
  H4:        20px / 28px  weight 600  letter-spacing: 0
  Body L:    18px / 28px  weight 400
  Body:      16px / 24px  weight 400
  Body S:    14px / 20px  weight 400
  Caption:   12px / 16px  weight 400

SPACING
  0:    0      4:    4px     8:    8px      12:   12px
  16:   16px   20:   20px    24:   24px     32:   32px
  40:   40px   48:   48px    56:   56px     64:   64px
  72:   72px   80:   80px    96:   96px

BREAKPOINTS
  sm:    640px    (Mobile)
  md:    768px    (Tablet portrait)
  lg:    1024px   (Tablet landscape / small desktop)
  xl:    1280px   (Desktop)

MOTION
  DEFAULT:         150ms ease-out
  FAST:            100ms ease-in
  SLOW:            250ms ease-out
  BOUNCE:          400ms cubic-bezier(0.68, -0.55, 0.265, 1.55)
  FOCUS:           200ms ease-out
  SPINNER:         800ms linear infinite
```

---

## PAGE 1: LANDING PAGE  `/`

### A. NAVIGATION BAR

```
┌─────────────────────────────────────────────────────────────────────┐
│  (surface bg, border-b border-border, fixed top-0, z-50, h-72px)   │
│                                                                     │
│  [LearnHub]                              [Sign In]  [Get Started]   │
│  primary-600                             ghost      primary-600     │
│  24px, 700                               outline     filled         │
│  tracking-tight                          neutral-    white text     │
│                                          500 border                  │
│                                                                     │
│  After scroll: bg changes to surface/90% opacity + backdrop-blur(8px)│
└─────────────────────────────────────────────────────────────────────┘

DETAIL:

Left side — Logo:
  - Position: left-24 (24px from container edge)
  - Typography: "LearnHub" in Inter 700, 24px, color primary-600 (#30525C)
  - Letter-spacing: -0.01em (tracking-tight)
  - Vertical alignment: centered within the 72px bar

Right side — Two buttons, stacked horizontally, gap-12 (12px):
  - Position: right-24

  Button 1: "Sign In" (Ghost / Outline variant)
    - Border: 1px solid neutral-300 (#D4D4D4)
    - Background: transparent → hover neutral-50 (#FAFAFA)
    - Text: neutral-600 (#525252), 14px, weight 500
    - Padding: horizontal 16px, vertical 8px
    - Border-radius: 8px (button token)
    - Height: 40px
    - Hover: bg neutral-50, border neutral-400
    - Press: scale 0.98, 100ms ease-in

  Button 2: "Get Started" (Primary Filled variant)
    - Background: primary-600 (#30525C)
    - Text: surface (#FFFFFF), 14px, weight 600
    - Padding: horizontal 20px, vertical 8px
    - Border-radius: 8px
    - Height: 40px
    - Hover: background primary-500 (#4C848D), scale 1.02, shadow increases to md, 150ms ease-out
    - Press: scale 0.98, 100ms ease-in

Scroll Behavior:
  - At page top (scrollY = 0): fully opaque surface background
  - After scrolling past 48px: background transitions to surface with 90% opacity
  - Backdrop-filter: blur(8px) fades in over 200ms
  - A bottom border (1px, border #BFB9B5) remains visible at all times
```

### B. HERO SECTION

```
┌──────────────────────────────────────────────────────────────────────┐
│  bg: primary-600 (#30525C)                                           │
│  min-height: calc(100vh - 72px)                                      │
│  position: relative                                                   │
│                                                                       │
│  ┌── Geometric Pattern Overlay ──────────────────────────────────┐   │
│  │  Diagonal lines at 45° angle, 40px spacing                     │   │
│  │  Color: primary-700 (#243E46) at 5% opacity                     │   │
│  │  Line weight: 1px                                               │   │
│  │  Position: absolute inset-0, pointer-events-none, z-0           │   │
│  └────────────────────────────────────────────────────────────────┘   │
│                                                                       │
│  ┌── Content (centered, z-10, flex-col, items-center) ──────────┐   │
│  │                                                                │   │
│  │  ┌──────────────────────────────────────┐                     │   │
│  │  │ 🚀  Join 500,000+ learners worldwide  │  Pill Badge        │   │
│  │  │  bg: primary-400/20 + primary-400 text│                     │   │
│  │  │  rounded-full, py-4 px-16, 14px, 500  │                    │   │
│  │  └──────────────────────────────────────┘                     │   │
│  │                                                                │   │
│  │  gap: 32px                                                     │   │
│  │                                                                │   │
│  │  Learn Without Limits                                          │   │
│  │  H1: 56px/60px, 700, white, tracking-tight                    │   │
│  │  max-width: 700px, text-center                                │   │
│  │                                                                │   │
│  │  Mast er new skills with expert-led courses.                   │   │
│  │  Start your journey today — free.                              │   │
│  │  Body L: 18px/28px, 400, primary-100 (#D6E4E8)                │   │
│  │  max-width: 550px, text-center                                │   │
│  │                                                                │   │
│  │  ┌──────────────────────────┐                                  │   │
│  │  │  Start Learning Free      │  CTA Button                    │   │
│  │  │  bg: surface (#FFFFFF)    │                                 │   │
│  │  │  text: primary-600        │                                 │   │
│  │  │  16px, weight 600         │                                 │   │
│  │  │  px-32 py-12 rounded-lg   │                                 │   │
│  │  │  Hover: scale(1.05),      │                                 │   │
│  │  │  shadow-lg, 150ms ease-out│                                 │   │
│  │  └──────────────────────────┘                                  │   │
│  │                                                                │   │
│  │  Already have an account? Sign in                               │   │
│  │  Body S: 14px, primary-200 (#ADC9CF), underline on hover      │   │
│  │                                                                │   │
│  │  ──────────────────────────────────────────                    │   │
│  │  500K+ Learners  ·  2000+ Courses  ·  98% Satisfaction         │   │
│  │  Body S: 14px, primary-300 (#85AFB7), letter-spacing: 0.05em  │   │
│  │  Dots as separator in primary-400 at 30% opacity               │   │
│  └────────────────────────────────────────────────────────────────┘   │
│                                                                       │
└──────────────────────────────────────────────────────────────────────┘

DETAIL:

Hero Block:
  - Full width, min-height: viewport height minus 72px navigation
  - Background: solid primary-600 (#30525C)
  - Flex display: column, centered both axes, gap: 32px between elements
  - Padding: vertical 80px, horizontal 24px (safe area)

Geometric Pattern:
  - Positioned absolutely, covering entire hero area
  - Repeating diagonal lines at 45 degrees
  - Each line: 1px stroke, primary-700 at exactly 5% opacity
  - Line spacing: 40px
  - The pattern should be visually perceptible but extremely subtle — adds texture without distraction
  - z-index: 0 (behind content, above background)

Pill Badge:
  - Inline-flex container, items centered
  - Background: a subtle tint — mix primary-400 (#4C848D) at ~20% opacity over the primary-600 background
  - Border-radius: 9999px (fully rounded)
  - Padding: vertical 4px, horizontal 16px
  - Text: "🚀  Join 500,000+ learners worldwide" (includes rocket emoji)
  - Typography: Inter 500, 14px, color primary-400 (#4C848D) for the label text
  - The emoji sits at 16px, vertically aligned with the text
  - Subtle glow: box-shadow 0 0 20px rgba(76, 132, 141, 0.15)

H1 Headline:
  - "Learn Without Limits"
  - Typography: Inter 700, 56px, line-height 60px (1.07 ratio for tight leading)
  - Color: surface (#FFFFFF)
  - Letter-spacing: -0.02em (tracking-tight)
  - Max-width: 700px
  - Text-align: center
  - Text-shadow: 0 2px 4px rgba(36, 62, 70, 0.2) — extremely subtle depth

Subtitle:
  - "Master new skills with expert-led courses. Start your journey today — free."
  - Typography: Inter 400, 18px, line-height 28px (Body L)
  - Color: primary-100 (#D6E4E8) — light teal, excellent contrast ratio of ~7:1 on the dark teal background
  - Max-width: 550px
  - Text-align: center

CTA Button:
  - "Start Learning Free"
  - Background: surface (#FFFFFF) — the white button on dark teal creates strong visual hierarchy
  - Text: primary-600 (#30525C), Inter 600, 16px
  - Padding: horizontal 32px, vertical 12px (tight but comfortable touch target at 48px total height)
  - Border-radius: 8px (button token)
  - Hover state:
    - Transform: scale(1.05) — 5% scale up
    - Box-shadow: transition from sm to lg
    - Duration: 150ms, easing: ease-out
    - Background remains white
  - Press state:
    - Transform: scale(0.98)
    - Duration: 100ms, easing: ease-in
  - Focus ring: 3px solid primary-400 at 35% opacity, ring-offset 2px

Secondary Link:
  - "Already have an account? Sign in" — "Sign in" is the link portion
  - Typography: Inter 400, 14px (Body S)
  - Color: primary-200 (#ADC9CF) — lighter than subtitle, indicating secondary action
  - Underline appears only on hover
  - Hover color: surface (#FFFFFF) — brightens to attract click
  - Transition: color 150ms ease-out

Statistical Trust Bar:
  - "500K+ Learners · 2000+ Courses · 98% Satisfaction"
  - Typography: Inter 400, 14px, letter-spacing: 0.05em (expanded tracking for stat readability)
  - Color: primary-300 (#85AFB7)
  - Middle-dot separators (·) at primary-400 (#4C848D) with 30% opacity
  - Positioned below the secondary link with a 24px gap
  - A subtle horizontal rule line (1px, primary-400 at 15% opacity, max-width 400px) appears above the stats

Mobile Adaptation (< 640px):
  - Hero min-height: calc(100vh - 64px) (nav shrinks to 64px)
  - Padding: vertical 48px, horizontal 16px
  - H1: 36px / 40px (scaled down)
  - Subtitle: 16px / 24px
  - Pill badge: 12px text
  - CTA button: full-width (minus 32px gutters), text 14px
  - Trust bar: stack metrics vertically or use smaller text (12px)
  - Gap between elements reduces to 24px
```

### C. FEATURES GRID

```
┌──────────────────────────────────────────────────────────────────────┐
│  bg: neutral-50 (#FAFAFA)                                            │
│  section-padding: vertical 96px, horizontal 24px                     │
│  max-width container: 1280px, centered                                │
│                                                                       │
│  ┌── Section Header (centered) ──────────────────────────────────┐   │
│  │  Why LearnHub?                                                 │   │
│  │  H1: 40px/48px, 700, neutral-900, center                       │   │
│  │                                                                │   │
│  │  Everything you need to advance your career                     │   │
│  │  Body L: 18px/28px, 400, neutral-500, center                   │   │
│  │  margin-top: 16px, margin-bottom: 64px                          │   │
│  └────────────────────────────────────────────────────────────────┘   │
│                                                                       │
│  ┌── 4-Column Grid (gap: 32px)  ─────────────────────────────────┐   │
│  │                                                                │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐     │   │
│  │  │  (icon)   │  │  (icon)   │  │  (icon)   │  │  (icon)   │     │   │
│  │  │  circle   │  │  circle   │  │  circle   │  │  circle   │     │   │
│  │  │           │  │           │  │           │  │           │     │   │
│  │  │ Expert    │  │ Flexible  │  │ Practical │  │ Career    │     │   │
│  │  │Instructors│  │ Learning  │  │ Projects  │  │ Support   │     │   │
│  │  │           │  │           │  │           │  │           │     │   │
│  │  │ Learn from│  │ Study at  │  │ Build real│  │ Get resume│     │   │
│  │  │ industry  │  │ your own  │  │ projects  │  │ reviews,  │     │   │
│  │  │ pros with │  │ pace, on  │  │ and earn  │  │ interview │     │   │
│  │  │ real-     │  │ any device│  │ profes-   │  │ prep, and │     │   │
│  │  │ world exp.│  │ anytime   │  │ certs     │  │ placement │     │   │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘     │   │
│  └────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────┘

DETAIL:

Section:
  - Background: neutral-50 (#FAFAFA) — off-white gives warmth and separates from hero
  - Container: max-width 1280px, margin: 0 auto (centered)
  - Vertical padding: 96px top, 96px bottom
  - Horizontal padding: 24px (safe area for smaller screens)

Section Header:
  - "Why LearnHub?"
    Typography: Inter 700, 40px, line-height 48px
    Color: neutral-900 (#171717)
    Text-align: center
  - Subtitle below with 16px gap:
    "Everything you need to advance your career"
    Typography: Inter 400, 18px, line-height 28px (Body L)
    Color: neutral-500 (#737373)
    Text-align: center
  - Bottom margin: 64px (separates header from grid)

Feature Card (each of 4):
  - Container: bg surface (#FFFFFF), border 1px solid border (#BFB9B5)
  - Border-radius: 16px (card token)
  - Padding: 32px (p-8) on all sides
  - Box-shadow: sm (0 1px 2px rgba(23, 23, 23, 0.06)) — very subtle
  - Flex direction: column, items-start (left-aligned content)
  - Gap between elements: 20px

  Icon Circle (top of each card):
    - Width/Height: 48px
    - Border-radius: 50% (fully circular)
    - Background: primary-100 (#D6E4E8) — light teal circle
    - Contains an SVG/icon in primary-600 (#30525C), centered, 24px × 24px
    - Icons should be 2px stroke weight, matching the system's clean aesthetic
    - Icons:
      1. Expert Instructors → graduation cap or academic hat icon
      2. Flexible Learning → clock or calendar with check icon
      3. Practical Projects → code brackets or briefcase icon
      4. Career Support → briefcase with star or compass icon

  Title:
    - Typography: Inter 600, 20px, line-height 28px (H4)
    - Color: neutral-900 (#171717)

  Description:
    - Typography: Inter 400, 16px, line-height 24px (Body)
    - Color: neutral-500 (#737373)
    - Max-width: unconstrained (fills card width)

  Hover State:
    - Transform: translateY(-4px) — lifts card up
    - Box-shadow: transition from sm to lg
    - Duration: 200ms, easing: ease-out
    - Border color subtly shifts to primary-400 (#4C848D) at 30% opacity
    - The icon circle slightly scales to 1.05

Grid Layout:
  - Desktop (> 1024px): 4 columns, equal width (1fr each), gap 32px
  - Tablet (640–1024px): 2 columns, gap 24px
  - Mobile (< 640px): 1 column, gap 20px, cards full-width

  Each card has equal height (stretch to tallest in row via CSS Grid align-items: stretch)

Between Section Transition:
  - A subtle gradient fade at the top edge: linear-gradient(to bottom, transparent, neutral-50) over 40px
  - This creates a soft blending effect from the hero's primary-600 into the neutral-50 section
```

---

## PAGE 2: REGISTER PAGE  `/register`

```
┌──────────────────────┬──────────────────────────────────────────────┐
│  LEFT PANEL (50%)    │  RIGHT PANEL (50%)                            │
│  Desktop only        │  Always visible                               │
│                      │                                               │
│  bg: primary-600     │  bg: neutral-50 (#FAFAFA)                     │
│  full height (100vh) │  full height (100vh)                          │
│  flex-col,           │  flex, items-center, justify-center           │
│  items-center,       │                                               │
│  justify-center      │  ┌────────────────────────────┐              │
│                      │  │  SURFACE CARD              │              │
│  ┌──────────────┐    │  │  max-w: 440px, w-full      │              │
│  │  LearnHub    │    │  │  bg: surface (#FFFFFF)     │              │
│  │  (logo,white)│    │  │  border: border           │              │
│  │              │    │  │  rounded-card (16px)       │              │
│  │ Start Your   │    │  │  shadow-sm                │              │
│  │ Learning     │    │  │  p-8 (32px)               │              │
│  │ Journey      │    │  │                            │              │
│  │              │    │  │  ┌ Create Your Account ──┐ │              │
│  │ Description  │    │  │  │ H2: 24px, 700,        │ │              │
│  │ text here    │    │  │  │ neutral-900           │ │              │
│  │              │    │  │  │ mb-32                  │ │              │
│  │ ✓ 2000+      │    │  │  └───────────────────────┘ │              │
│  │   courses    │    │  │                            │ │              │
│  │ ✓ Self-paced │    │  │  ┌── FORM FIELDS ────────┐ │ │              │
│  │ ✓ Certificates│   │  │  │ 1. Full Name          │ │ │              │
│  │              │    │  │  │ 2. Username           │ │ │              │
│  │ [illustration│    │  │  │ 3. Email              │ │ │              │
│  │  placeholder]│    │  │  │ 4. Password           │ │ │              │
│  │  200×200px   │    │  │  │ 5. Confirm Password   │ │ │              │
│  │  primary-500 │    │  │  └───────────────────────┘ │ │              │
│  │  /30%        │    │  │                            │ │              │
│  └──────────────┘    │  │  [   Create Account   ]    │ │              │
│                      │  │  primary-600, full-width   │ │              │
│                      │  │                            │ │              │
│                      │  │  ──────── or ────────      │ │              │
│                      │  │                            │ │              │
│                      │  │  Already have an account?  │ │              │
│                      │  │  Sign in →                 │ │              │
│                      │  └────────────────────────────┘              │
└──────────────────────┴──────────────────────────────────────────────┘

DETAIL — LEFT PANEL:

  Background: primary-600 (#30525C) solid fill
  Width: 50% of viewport (hidden on screens < 768px)
  Height: 100vh (fixed, scrolls independently on the right on desktop via overflow-y: auto)
  Display: flex, direction column, align-items center, justify-content center
  Padding: 64px

  Content Layout (top to bottom, gap 32px):

    1. Logo:
       - "LearnHub" in Inter 700, 28px, color surface (#FFFFFF)
       - May include a small 32×32px icon mark to its left
       - Letter-spacing: -0.01em

    2. Headline:
       - "Start Your Learning Journey"
       - Typography: Inter 700, 36px, line-height 44px (H2)
       - Color: surface (#FFFFFF)
       - Max-width: 380px

    3. Description:
       - "Join a global community of learners and unlock your potential with expert-led courses."
       - Typography: Inter 400, 18px, line-height 28px (Body L)
       - Color: primary-100 (#D6E4E8)
       - Max-width: 450px

    4. Benefit Bullets (stacked, gap 16px):
       Each bullet is a flex row:
       - Left: 20×20px check-circle icon, color primary-200 (#ADC9CF), filled with a white checkmark
       - Right: text in Inter 400, 16px, color primary-100 (#D6E4E8)
       Items:
         a. "Access to 2000+ courses"
         b. "Learn at your own pace"
         c. "Earn recognized certificates"
       - Each bullet has an 8px gap between icon and text
       - Icons should animate in on page load: fade-in + slide-right (8px), staggered 100ms each

    5. Illustration Placeholder:
       - Width: 200px, Height: 200px
       - Border-radius: 16px (card token)
       - Background: primary-500 (#4C848D) at 30% opacity
       - Center contains a subtle icon or illustration hint (e.g., a graduation cap line art at 40px, primary-200)
       - Used as a placeholder for a future SVG illustration
       - Margin-top: 16px

  Mobile (< 768px):
    - Left panel is hidden entirely (display: none)
    - Right panel expands to 100% width
    - The card retains its styling but gains horizontal padding: 16px

DETAIL — RIGHT PANEL:

  Background: neutral-50 (#FAFAFA)
  Width: 50% of viewport (100% on mobile)
  Height: 100vh
  Display: flex, align-items center, justify-content center
  Padding: 24px (safe gutter)

  Surface Card:
    - Background: surface (#FFFFFF)
    - Border: 1px solid border (#BFB9B5)
    - Border-radius: 16px (card token)
    - Box-shadow: sm
    - Max-width: 440px
    - Width: 100%
    - Padding: 32px on all sides
    - On focus of any child input, box-shadow subtly shifts to include primary-focus ring on the card

  Card Header:
    - "Create Your Account"
    - Typography: Inter 700, 24px, line-height 32px (H3)
    - Color: neutral-900 (#171717)
    - Margin-bottom: 32px

  FORM FIELDS (×5):

  Shared Input Styling (default state):
    - Container: relative, width 100%
    - Label: position absolute (or above), color neutral-700, 14px, weight 500, mb-4
    - Input wrapper: relative, flex, items-center
    - Left icon: position absolute, left-12, top 50%, translateY(-50%)
      - Icon: 18px × 18px, color neutral-400 (#A3A3A3)
      - Stroke: 1.5px
    - Input field:
      - Width: 100%
      - Height: 48px
      - Padding: left 40px (to clear icon), right 40px (for validation icons), vertical 12px
      - Typography: Inter 400, 16px, color neutral-900
      - Background: surface (#FFFFFF)
      - Border: 1px solid border (#BFB9B5)
      - Border-radius: 8px (button token)
      - Outline: none (custom focus visible)
    - Placeholder: color neutral-400 (#A3A3A3), 16px

  Focus State (all fields):
    - Border color: primary-500 (#4C848D)
    - Box-shadow: 0 0 0 3px rgba(76, 132, 141, 0.35) — focus ring
    - Left icon color changes to primary-500 (#4C848D)
    - Transition: border-color 200ms ease-out, box-shadow 200ms ease-out
    - Label color shifts to primary-600 (#30525C)

  FIELD 1: Full Name
    - Label: "Full Name" (visible above input, not placeholder-only)
    - Placeholder inside input: "John Doe"
    - Left icon: User icon (circle outline with head + shoulders)

  FIELD 2: Username
    - Label: "Username"
    - Placeholder: "johndoe"
    - Left icon: @ (at-sign) icon
    - Below input, hint text:
      "3-50 characters, lowercase letters, numbers, dots, underscores"
      Typography: Inter 400, 12px, line-height 16px (Caption)
      Color: neutral-500 (#737373)
      Margin-top: 4px
    - Visual: the hint text is indented 4px to align with the input left edge

  FIELD 3: Email
    - Label: "Email Address"
    - Placeholder: "john@example.com"
    - Left icon: Mail/envelope icon
    - Type: email (browser validation)
    - Extra right icon: on valid blur, a green check appears (see validation states)

  FIELD 4: Password
    - Label: "Password"
    - Placeholder: "••••••••" (8 bullet dots, rendered as masked text)
    - Left icon: Lock icon (padlock closed)
    - Right icon: Eye/Eye-off toggle (click to show/hide password)
      - Default: eye-off icon, color neutral-400
      - Active (password visible): eye icon, color primary-500
      - 24px × 24px clickable area, positioned right-12, vertically centered
    - Below input, hint text:
      "At least 12 characters with uppercase, lowercase, number, and special character"
      Typography: Inter 400, 12px, color neutral-500
      Margin-top: 4px
    - Type attribute toggles between 'password' and 'text' on eye click

  FIELD 5: Confirm Password
    - Label: "Confirm Password"
    - Placeholder: "••••••••"
    - Left icon: Lock icon (padlock closed)
    - Extra validation: compared to Password field value
    - If mismatched on blur: red border + "Passwords do not match" error below

  Spacing between fields: 20px (margin-bottom on each field container)

VALIDATION STATES (applied to any field):

  Valid State (on blur, after passing validation):
    - Border color: success (#16A34A)
    - Right icon: 20×20px green circle with white checkmark
      - Position: absolute, right-12, top 50%, translateY(-50%)
      - Checkmark appears with scale 0→1 bounce animation (400ms cubic-bezier(0.68,-0.55,0.265,1.55))
    - Left icon remains primary-500
    - Box-shadow: 0 0 0 1px rgba(22, 163, 74, 0.3)

  Invalid State (on blur, validation fails):
    - Border color: secondary-500 (#C35627)
    - Right icon: 20×20px secondary-500 circle with white X/exclamation
      - Position: absolute, right-12, top 50%, translateY(-50%)
      - Appears with fade-in 200ms
    - Below the input, error text appears:
      - Typography: Inter 400, 12px (Caption)
      - Color: secondary-600 (#A3461F)
      - Margin-top: 4px
      - Fade-in + slide-down (8px), 250ms ease-out
    - aria-describedby links the input to the error message element
    - Screen reader announces: "[Field Name] — error: [message]"

  Validating State (during async validation, e.g., username check):
    - Border color: primary-300 (#85AFB7)
    - Right: a 16px spinner (primary-500, 2px stroke) with continuous rotation (800ms linear infinite)
    - Input background: subtle pulse animation — opacity varies between 1.0 and 0.95 over 1.2s
    - Left icon remains neutral-400
    - All input interactions are still permitted during validation

  Error-Specific Messages per Field:
    - Full Name: "Name is required" (empty), "Name must be at least 2 characters" (too short)
    - Username: "Username is required", "Must be 3-50 characters", "Only lowercase letters, numbers, dots, underscores", "Username already taken" (server)
    - Email: "Valid email is required", "Email already registered" (server)
    - Password: "Password is required", "Must be at least 12 characters", "Must include uppercase, lowercase, number, and special character"
    - Confirm Password: "Passwords do not match"

SUBMIT BUTTON:

  Default State:
    - Text: "Create Account"
    - Background: primary-600 (#30525C)
    - Text color: surface (#FFFFFF)
    - Typography: Inter 600, 16px
    - Width: 100%
    - Padding: vertical 12px (height 48px total)
    - Border-radius: 8px (button token)
    - Border: none
    - Cursor: pointer
    - Box-shadow: sm
    - Transition: all 150ms ease-out

  Hover State:
    - Background: primary-500 (#4C848D)
    - Transform: scale(1.02)
    - Box-shadow: md
    - Duration: 150ms, easing: ease-out

  Press State:
    - Transform: scale(0.98)
    - Duration: 100ms, easing: ease-in

  Focus State:
    - Box-shadow: 0 0 0 3px rgba(76, 132, 141, 0.35)
    - Outline: none

  Margin-top: 8px from last field

DIVIDER:

  - Text: "or" centered between two horizontal lines
  - Line dimensions: 1px height, flex-grow 1, color neutral-300 (#D4D4D4)
  - Text: Inter 400, 14px, color neutral-400 (#A3A3A3), padding horizontal 12px
  - Margin: vertical 24px

FOOTER LINK:

  - "Already have an account? Sign in"
  - Typography: Inter 400, 14px (Body S)
  - "Already have an account?" → neutral-500 (#737373)
  - "Sign in" → primary-600 (#30525C), weight 500, with underline on hover
  - Entire text block is a link to /login
  - Centered below the divider

  Mobile Note:
    - If screen height is small (< 700px), the card gains overflow-y: auto and the body loses its fixed 100vh constraint

LOADING STATE (after form submission):

  The card transitions to a loading state:

  Card Overlay:
    - A semi-transparent white overlay (surface at 50% opacity)
    - Covers the entire card surface, prevents all interaction
    - Fades in over 200ms ease-out

  Button:
    - Text changes to "Creating account…" with an inline spinner
    - Spinner: 18px circular spinner, 2px stroke, primary-200 track, primary-600 arc
    - 800ms linear infinite rotation
    - Button is disabled (cursor: not-allowed, opacity remains 100% to preserve legibility)

  All Inputs:
    - Disabled state (opacity: 0.6, cursor: not-allowed, pointer-events: none)
    - Border color transitions to neutral-200

  Background:
    - Card background unchanged (surface)
    - The 50% overlay gives a visual "frozen" feeling

ERROR STATE (server returns error):

  Error Banner:
    - Appears at the very top of the card (above the "Create Your Account" header)
    - Background: secondary-50 (#FBE8E0) — warm orange tint
    - Text: secondary-600 (#A3461F) — darker orange for readability
    - Border: 1px solid secondary-500 at 20% opacity
    - Border-radius: 8px
    - Padding: 12px horizontal, 16px vertical
    - Typography: Inter 400, 14px, line-height 20px (Body S)
    - Margin-bottom: 24px (pushes form content down)
    - Icon: 18px exclamation-circle icon in secondary-500 to the left of the text
    - Animation: fade-in + slide-down (from -8px to 0), 250ms ease-out

  Content layout:
    - Icon (flex-shrink: 0) + text (flex-grow: 1), gap: 8px

  Example messages:
    - "Username already taken. Please choose another."
    - "Email already registered. Try signing in instead."
    - "An unexpected error occurred. Please try again."

  Form Behavior:
    - All fields remain filled with user data (no data loss)
    - The form is NOT disabled — user can edit and resubmit
    - The submit button re-enables and returns to "Create Account" text

  Dismissal:
    - Banner dismisses when user begins typing in any field
    - Or on manual close (X button, 20×20px, top-right of banner)

SUCCESS TRANSITION:

  On successful registration:
    1. A brief success pulse on the submit button (background turns success green for 400ms)
    2. The card fades out (opacity 1 → 0, 300ms ease-out)
    3. Browser navigates to /login?registered=true
    4. The URL parameter triggers a success banner on the LoginPage (see Page 4)

MOBILE ADAPTATION (< 768px):

  Layout:
    - Left panel: display: none
    - Right panel: width 100%, flex items-center justify-center
    - Card: max-width 100% (no 440px constraint), border-radius: 0 on very small screens
    - Padding: 24px horizontal, 32px vertical inside the card

  Typography adjustments:
    - "Create Your Account": 20px (H4)
    - Labels: 13px
    - Hint text: 11px

  Spacing:
    - Field gap reduces to 16px
    - Card header margin-bottom: 24px

  Form:
    - Full-width inputs remain full-width (100%)
    - Button remains full-width
    - No change to validation behavior

TABLET BREAKPOINT (768–1024px):

  Left panel: 40% width
  Right panel: 60% width
  Card max-width: 448px (max-w-md)
  Left panel headline: 28px
  Left panel description: 16px
```

---

## PAGE 3: VERIFY EMAIL PAGE  `/verify-email`

```
┌──────────────────────────────────────────────────────────────────────┐
│  Entire page background: neutral-50 (#FAFAFA)                        │
│  min-height: 100vh                                                    │
│  display: flex, items-center, justify-center                          │
│  padding: 24px (safe gutter)                                          │
│                                                                       │
│  ┌──────────────────────────────────────────────────┐                │
│  │  CENTERED SURFACE CARD                            │                │
│  │  max-width: 440px, width: 100%                    │                │
│  │  bg: surface (#FFFFFF)                            │                │
│  │  border: 1px solid border (#BFB9B5)               │                │
│  │  border-radius: 16px (card token)                 │                │
│  │  box-shadow: sm                                   │                │
│  │  padding: 48px 40px                               │                │
│  │  display: flex, flex-col, items-center,           │                │
│  │  text-align: center                               │                │
│  │  gap: 24px (between elements)                      │                │
│  └──────────────────────────────────────────────────┘                │
│                                                                       │
│  (Card content varies by state — see below)                           │
└──────────────────────────────────────────────────────────────────────┘

SHARED CARD WRAPPER DETAILS:
  - No split-screen, single centered card
  - Card vertical padding: 48px (top) + 48px (bottom)
  - Card horizontal padding: 40px
  - All content is centered (text-align: center, align-items: center)
  - Gap between every direct child: 24px
  - Page background: neutral-50, covers entire viewport
  - A small "LearnHub" logo wordmark at the very top of the card (above the state content):
    - Inter 700, 18px, color primary-600
    - Centers above the state content with a 32px gap
    - Acts as a subtle brand anchor and "you're in the right place" signal

TRANSITION BETWEEN STATES:
  - All state changes use a cross-fade: old content fades out (200ms) → new content fades in (200ms)
  - The card dimensions should remain stable — use min-height to prevent layout shift
  - Card min-height: 400px (prevents jarring resizing between states)
```

### STATE 1 — LOADING (Default, no user interaction)

```
┌──────────────────────────────────────────┐
│                                          │
│          [LearnHub]                      │
│          (logo wordmark)                  │
│                                          │
│               ⟳                          │
│          (spinner)                       │
│          48px primary-600               │
│          3px stroke                     │
│          track: neutral-200              │
│          arc: primary-600                │
│          rotate: 800ms linear ∞         │
│                                          │
│      Verifying your email address…       │
│       + animated bouncing dots          │
│      Inter 400, 16px, neutral-700       │
│                                          │
└──────────────────────────────────────────┘

VISUAL DETAIL:

  Spinner (CircularProgress):
    - Width/Height: 48px
    - SVG circle with stroke: 3px
    - Track (background arc): neutral-200 (#E5E5E5), 270° arc
    - Indicator (spinning arc): primary-600 (#30525C), 90° arc
    - Animation: continuous clockwise rotation, 800ms per full rotation, linear timing
    - Position: centered, margin: 0 auto
    - The spinner itself has a subtle drop-shadow: 0 0 12px rgba(48, 82, 92, 0.15)

  Text:
    - "Verifying your email address…" (with ellipsis)
    - Typography: Inter 400, 16px, line-height 24px (Body)
    - Color: neutral-700 (#404040)
    - Spacing below spinner: follows the 24px gap

  Animated Dots:
    - Three dots after the text, each 4px × 4px, border-radius: 50%
    - Color: primary-500 (#4C848D)
    - Bounce animation: each dot translates up by -4px and returns, staggered
    - Dot 1 delay: 0ms
    - Dot 2 delay: 200ms
    - Dot 3 delay: 400ms
    - Duration: 600ms per cycle, easing: ease-in-out, loops infinitely
    - Gap between dots: 4px

  Background Behavior:
    - The card sits perfectly centered
    - No additional overlays or dimming
    - This is the initial state when the user arrives from the email link
    - The API call to verify the token fires immediately on mount
    - A subtle pulsing glow emanates from the spinner: radial-gradient at 50% 50%, primary-600 at 5% opacity fading to transparent over 80px
```

### STATE 2 — SUCCESS

```
┌──────────────────────────────────────────┐
│                                          │
│          [LearnHub]                      │
│                                          │
│            ┌──────┐                      │
│            │  ✓   │                      │
│            │      │  Green circle        │
│            └──────┘  48px, success green │
│                      (16A34A)            │
│                      White checkmark     │
│                      inside, 24px        │
│                                          │
│        Email Verified!                    │
│        Inter 700, 24px, neutral-900      │
│                                          │
│     Your account is now active.           │
│     Redirecting you to sign in…          │
│     Inter 400, 16px, neutral-500         │
│                                          │
│     ┌──────────────────────────┐         │
│     │ ████████░░░░░░░░░░░░░░░░ │  Bar   │
│     └──────────────────────────┘         │
│     3-second countdown bar               │
│                                          │
│     ┌──────────────────────┐             │
│     │    Sign In Now       │  Button    │
│     │  primary-600 filled  │  (fallback) │
│     └──────────────────────┘             │
│                                          │
└──────────────────────────────────────────┘

VISUAL DETAIL:

  Success Icon (Animated Checkmark):
    - Container: 48px × 48px circle
    - Background: success green (#16A34A)
    - Border-radius: 50%
    - Center: white checkmark SVG, 24px × 24px, 3px stroke weight
    - Animation sequence:
      1. Circle scales from 0 → 1.15 (150ms, cubic-bezier(0.68,-0.55,0.265,1.55))
      2. Circle returns to 1.0 (150ms, ease-out)
      3. Total: 300ms, then the checkmark fades in over 100ms
    - After animation settles: a subtle continuous pulse glow
      - Box-shadow: 0 0 20px rgba(22, 163, 74, 0.25) pulsing between 0.2 and 0.35 opacity over 2s

  Heading:
    - "Email Verified!"
    - Typography: Inter 700, 24px, line-height 32px (H3)
    - Color: neutral-900 (#171717)
    - Fade-in animation: starts at 300ms (after icon animation), 300ms duration, ease-out

  Description:
    - "Your account is now active. Redirecting you to sign in…"
    - Typography: Inter 400, 16px, line-height 24px (Body)
    - Color: neutral-500 (#737373)
    - Fade-in animation: starts at 500ms, 300ms duration

  Countdown Progress Bar:
    - Width: 100% (matches card content width)
    - Height: 4px
    - Background: neutral-200 (#E5E5E5)
    - Border-radius: 2px (fully rounded ends)
    - Fill: primary-500 (#4C848D)
    - Animation: width shrinks from 50% → 0% over the remaining time (up to 3 seconds)
    - The bar auto-starts at 50% width and animates to 0% over 3s with linear timing
    - At 0%, the redirect fires (or if user clicks "Sign In Now" manually)
    - Margin: 8px above and below

  Fallback Button:
    - Text: "Sign In Now"
    - Background: primary-600 (#30525C)
    - Text color: surface (#FFFFFF)
    - Typography: Inter 600, 16px
    - Width: auto (not full-width), padding horizontal 32px, vertical 12px
    - Border-radius: 8px (button token)
    - Hover: background primary-500, scale 1.02, shadow md, 150ms ease-out
    - Press: scale 0.98, 100ms ease-in
    - The button is always available even during countdown — provides a manual fallback

  Redirect Logic:
    - Auto-redirect to /login?verified=true after 3 seconds
    - The countdown provides visual feedback so the user isn't surprised
    - If the user clicks "Sign In Now", redirect immediately

  Screen Reader:
    - Announce "Email verified successfully. Redirecting to sign in." on state entry
    - Countdown is announced as "Redirecting in 3 seconds… 2 seconds… 1 second…"
```

### STATE 3 — INVALID TOKEN

```
┌──────────────────────────────────────────┐
│                                          │
│          [LearnHub]                      │
│                                          │
│            ┌──────┐                      │
│            │  ✕   │                      │
│            │      │  Red/orange circle   │
│            └──────┘  48px, secondary-500 │
│                      White X, 24px       │
│                                          │
│        Invalid Verification Link          │
│        Inter 700, 24px, neutral-900      │
│                                          │
│     This link is invalid or has already   │
│     been used. Please request a new       │
│     verification email.                   │
│     Inter 400, 16px, neutral-500         │
│                                          │
│     ┌──────────────────────────┐         │
│     │     Resend Email         │         │
│     │  secondary-500 outline   │         │
│     └──────────────────────────┘         │
│                                          │
│         Back to Sign In →                │
│         primary-600, 14px                │
│                                          │
└──────────────────────────────────────────┘

VISUAL DETAIL:

  Error Icon (X Circle):
    - Container: 48px × 48px circle
    - Background: secondary-500 (#C35627)
    - Border-radius: 50%
    - Center: white X (close/cross icon), 24px × 24px, 2.5px stroke weight
    - Animation: scale 0 → 1.0 with ease-out over 250ms (no bounce — feels more serious/somber)

  Heading:
    - "Invalid Verification Link"
    - Typography: Inter 700, 24px, line-height 32px (H3)
    - Color: neutral-900 (#171717)

  Description:
    - "This link is invalid or has already been used. Please request a new verification email."
    - Typography: Inter 400, 16px, line-height 24px (Body)
    - Color: neutral-500 (#737373)
    - Max-width: 340px (keeps line length readable)

  Resend Email Button:
    - Text: "Resend Email"
    - Background: transparent
    - Border: 1.5px solid secondary-500 (#C35627)
    - Text color: secondary-500 (#C35627)
    - Typography: Inter 600, 16px
    - Width: auto (not full-width), padding horizontal 24px, vertical 12px
    - Border-radius: 8px (button token)
    - Hover: background secondary-50 (#FBE8E0), border secondary-600, text secondary-600, 150ms ease-out
    - Press: scale 0.98, 100ms ease-in

  Secondary Link:
    - "Back to Sign In" with arrow icon (→)
    - Typography: Inter 500, 14px
    - Color: primary-600 (#30525C)
    - Underline appears on hover
    - Links to /login
    - Gap above: 16px
```

### STATE 4 — EXPIRED TOKEN

```
┌──────────────────────────────────────────┐
│                                          │
│          [LearnHub]                      │
│                                          │
│            ┌──────┐                      │
│            │  🕐   │                      │
│            │      │  Clock icon           │
│            └──────┘  48px circle         │
│                      bg: secondary-50    │
│                      icon: secondary-500 │
│                      (clock face)        │
│                                          │
│        Link Expired                       │
│        Inter 700, 24px, neutral-900      │
│                                          │
│     This verification link has expired.   │
│     Verification links are valid for     │
│     24 hours.                             │
│     Inter 400, 16px, neutral-500         │
│                                          │
│     ┌──────────────────────────┐         │
│     │     Resend Email         │         │
│     │  secondary-500 outline   │         │
│     └──────────────────────────┘         │
│                                          │
│         Back to Sign In →                │
│                                          │
└──────────────────────────────────────────┘

VISUAL DETAIL:

  Clock Icon:
    - Container: 48px × 48px circle
    - Background: secondary-50 (#FBE8E0) — soft orange tint
    - Border: 2px solid secondary-500 at 25% opacity (for visual interest)
      - Actually: just the bg is sufficient, the border adds definition
    - Center: clock icon SVG, 24px × 24px, color secondary-500 (#C35627), 2px stroke
    - Clock hands point to approximately 11:55 (almost expired position)
    - Animation: subtle rotation oscillation (±3°) over 3s, easing ease-in-out — gives a "ticking/time passing" feel
    - This is a milder visual than the X circle (State 3) — the user hasn't done anything wrong, the link simply expired

  Heading:
    - "Link Expired"
    - Same typography as State 3: Inter 700, 24px, neutral-900

  Description:
    - "This verification link has expired. Verification links are valid for 24 hours."
    - Same typography as State 3: Inter 400, 16px, neutral-500
    - Max-width: 340px

  Resend Email Button:
    - Identical styling to State 3 (secondary-500 outline)
    - Click triggers an API call to resend the verification email
    - Button transitions to loading state: text becomes "Sending…" with inline spinner
    - On success: button changes to "Email Sent!" (success green, 2 seconds) then reverts

  Back to Sign In link:
    - Identical to State 3
```

### STATE 5 — NO TOKEN PROVIDED

```
┌──────────────────────────────────────────┐
│                                          │
│          [LearnHub]                      │
│                                          │
│            ┌──────┐                      │
│            │  ⚠   │                      │
│            │      │  Warning triangle     │
│            └──────┘  48px, amber/yellow  │
│                      bg: #FEF3C7 (amber  │
│                      50 tint)            │
│                      icon: #D97706       │
│                      (amber 600)         │
│                                          │
│     No Verification Token                 │
│     Inter 700, 24px, neutral-900         │
│                                          │
│     Please check your email and click     │
│     the verification link, or try         │
│     copying the full link from your       │
│     email.                                │
│     Inter 400, 16px, neutral-500         │
│                                          │
│          Back to Sign In →               │
│                                          │
└──────────────────────────────────────────┘

VISUAL DETAIL:

  Warning Icon:
    - Container: 48px × 48px circle
    - Background: amber-50 (#FEF3C7) — warm yellow tint
    - Center: exclamation triangle icon, 24px × 24px, color amber-600 (#D97706), 2px stroke
    - Animation: fade-in + subtle scale from 0.9 → 1.0, 250ms ease-out
    - Note: this uses amber/yellow (not secondary/orange) because it's informative, not an error

  Heading:
    - "No Verification Token"
    - Typography: Inter 700, 24px, neutral-900

  Description:
    - "Please check your email and click the verification link, or try copying the full link from your email."
    - Typography: Inter 400, 16px, neutral-500
    - Max-width: 340px

  Action:
    - Only "Back to Sign In" link (no resend — there's no token to associate the request with)
    - Typography and styling identical to State 3/4 secondary links
    - Prominent and clear — this is the only path forward for this state
```

### STATE 6 — NETWORK / SERVER ERROR

```
┌──────────────────────────────────────────┐
│                                          │
│          [LearnHub]                      │
│                                          │
│            ┌──────┐                      │
│            │  ☁️✕  │                      │
│            │      │  Cloud-off icon       │
│            └──────┘  48px circle         │
│                      bg: neutral-100     │
│                      icon: neutral-400   │
│                                          │
│        Connection Error                   │
│        Inter 700, 24px, neutral-900      │
│                                          │
│     We couldn't verify your email right   │
│     now. Please check your internet       │
│     connection and try again.             │
│     Inter 400, 16px, neutral-500         │
│                                          │
│     ┌──────────────────────────┐         │
│     │        Retry             │         │
│     │   primary-600 filled     │         │
│     └──────────────────────────┘         │
│                                          │
│         Back to Sign In →                │
│                                          │
└──────────────────────────────────────────┘

VISUAL DETAIL:

  Cloud-Off Icon:
    - Container: 48px × 48px circle
    - Background: neutral-100 (#F5F5F5) — the most neutral/muted background of all states
    - Center: cloud-off icon (cloud outline with a slash through it), 24px × 24px
    - Color: neutral-400 (#A3A3A3) — muted, not alarming, but clearly indicates unavailability
    - Stroke: 2px
    - Animation: fade-in, 250ms ease-out

  Heading:
    - "Connection Error"
    - Typography: Inter 700, 24px, neutral-900

  Description:
    - "We couldn't verify your email right now. Please check your internet connection and try again."
    - Typography: Inter 400, 16px, neutral-500
    - Max-width: 340px

  Retry Button:
    - Text: "Retry"
    - Background: primary-600 (#30525C)
    - Text color: surface (#FFFFFF)
    - Typography: Inter 600, 16px
    - Width: auto, padding horizontal 32px, vertical 12px
    - Border-radius: 8px
    - Hover: background primary-500, scale 1.02, shadow md, 150ms ease-out
    - Press: scale 0.98, 100ms ease-in
    - Click action: re-fires the verification API call with the same token
    - On click, transitions to State 1 (Loading)

  Back to Sign In link:
    - Identical styling to other states
    - Provides escape for users who don't want to retry immediately

VISUAL SUMMARY — ALL 6 STATES AT A GLANCE:

  | State   | Icon          | Circle BG        | Feeling                |
  |---------|---------------|------------------|------------------------|
  | Loading | Spinner       | N/A (no circle)  | Anticipation, neutral  |
  | Success | ✓ checkmark  | success green    | Joy, relief, positive  |
  | Invalid | ✕ cross      | secondary-500    | Concern, serious       |
  | Expired | 🕐 clock     | secondary-50     | Mild disappointment    |
  | NoToken | ⚠ triangle   | amber-50         | Informative, helpful   |
  | Error   | ☁️✕ cloud-off| neutral-100      | Neutral, practical     |

  The icon colors follow a severity spectrum:
    Green (success) → Amber (warning/info) → Neutral (unavailable) → Orange (error/expired)
```

---

## PAGE 4: LOGIN PAGE — POST-REGISTRATION STATES  `/login`

```
These are enhancements to the EXISTING LoginPage split-screen layout.
The page structure remains unchanged — only banners are added conditionally.

EXISTING LOGIN PAGE STRUCTURE (reference):
  ┌──────────────────┬──────────────────────────────────┐
  │  LEFT PANEL      │  RIGHT PANEL                     │
  │  primary-600 bg  │  neutral-50 bg                   │
  │  brand content   │  ┌────────────────────────────┐  │
  │                  │  │  SURFACE CARD              │  │
  │                  │  │                            │  │
  │                  │  │  [BANNER SLOT — NEW]       │  │
  │                  │  │                            │  │
  │                  │  │  Welcome Back              │  │
  │                  │  │  (H2, neutral-900)         │  │
  │                  │  │                            │  │
  │                  │  │  [Email field]             │  │
  │                  │  │  [Password field]          │  │
  │                  │  │  [Sign In button]          │  │
  │                  │  │                            │  │
  │                  │  │  ───── or ─────            │  │
  │                  │  │  Don't have an account?    │  │
  │                  │  │  Sign up →                 │  │
  │                  │  └────────────────────────────┘  │
  └──────────────────┴──────────────────────────────────┘
```

### BANNER:  `?registered=true`

```
┌────────────────────────────────────────────┐
│ ✓  Account created! Please check your      │
│    email to verify your account before      │
│    signing in.                              │
│                                             │
│  bg: primary-50 (#E9F2F4)                   │
│  border: none                               │
│  border-radius: 8px                         │
│  padding: 12px horizontal, 16px vertical    │
│  margin-bottom: 24px (pushes form down)     │
│  typography: Inter 400, 14px/20px (Body S)  │
│  color: primary-700 (#243E46)               │
│                                             │
│  Left icon: 18px green check-circle (16A34A)│
│  flex row, gap-8                            │
│                                             │
│  Auto-dismiss: after 8 seconds              │
│  OR on user interaction (typing, click)     │
│  Dismiss animation: fade-out + slide-up     │
│  250ms ease-out                             │
└────────────────────────────────────────────┘

DETAIL:

Background & Border:
  - Background: primary-50 (#E9F2F4) — the lightest teal tint, creates a soft but noticeable banner
  - No border (the background tint provides enough distinction from the white card)
  - Border-radius: 8px (button token — slightly tighter radius for internal elements)

Layout:
  - Flex row, align-items: flex-start (icon stays top-aligned if text wraps)
  - Gap between icon and text: 8px

Icon:
  - Green check-circle (success #16A34A), 18px × 18px
  - SVG: circle outline with a checkmark inside
  - Flex-shrink: 0 (prevents squishing)

Text:
  - "✓ Account created! Please check your email to verify your account before signing in."
  - Typography: Inter 400, 14px, line-height 20px (Body S)
  - Color: primary-700 (#243E46) — dark teal for strong contrast on the light tint
  - The text may wrap to 2-3 lines on narrow cards

Animation (Appear):
  - Fade-in + slide-down from -8px to 0, 250ms ease-out
  - Triggers immediately on page load if the query param is present

Auto-Dismiss:
  - Starts an 8-second timer on mount
  - At 7 seconds: banner opacity begins to decrease (fade from 1 → 0 over 500ms)
  - Also translates up by -8px during the fade
  - Total dismiss animation: 500ms ease-out
  - After dismissal: the banner is removed from DOM (or display: none + height: 0 with transition)

Manual Dismiss:
  - Dismisses immediately if the user:
    a. Starts typing in any form field
    b. Clicks anywhere on the form card
    c. Clicks a small × button (20×20px, neutral-400, top-right of banner)
  - Manual dismiss uses the same fade+slide-up animation but at 250ms

Screen Reader:
  - On page load: "Success: Account created. Please check your email to verify your account before signing in."
  - The banner has role="alert" for automatic announcement
```

### BANNER:  `?verified=true`

```
┌────────────────────────────────────────────┐
│ ✓  Email verified! You can now sign in.    │
│                                             │
│  bg: primary-50 (#E9F2F4)                   │
│  border: none                               │
│  border-radius: 8px                         │
│  padding: 12px horizontal, 16px vertical    │
│  margin-bottom: 24px                        │
│  typography: Inter 400, 14px/20px           │
│  color: primary-700 (#243E46)               │
│                                             │
│  Left icon: 18px green check-circle (16A34A)│
│  flex row, gap-8                            │
│                                             │
│  Auto-dismiss: 8 seconds                    │
└────────────────────────────────────────────┘

DETAIL:

  Identical styling to the registered banner, but with shorter text:
  "✓ Email verified! You can now sign in."

  This banner appears when the user is redirected from the Verify Email page
  after successful verification (via /verify-email success state's auto-redirect).

  The text is friendlier and more direct — the user has completed the flow
  and just needs to sign in.

  All animation, dismiss, and accessibility behavior is identical to the
  registered banner.
```

### EDGE CASE: BOTH PARAMS PRESENT

  If both `?registered=true&verified=true` appear (unlikely but possible):
  - Show only the "Email verified!" banner (the higher-priority success message)
  - The registered banner is redundant once the email is verified

### EDGE CASE: INVALID PARAM VALUES

  If `?registered=false` or `?verified=false`:
  - No banner is shown
  - The param is silently ignored
  - No error is displayed (not a user-facing error condition)
```

---

## RESPONSIVE DESIGN SUMMARY

```
BREAKPOINT OVERVIEW:

  ┌──────────────┬─────────────────┬─────────────────┬──────────────────┐
  │              │  MOBILE         │  TABLET          │  DESKTOP         │
  │              │  < 640px        │  640–1024px      │  > 1024px        │
  ├──────────────┼─────────────────┼─────────────────┼──────────────────┤
  │ LANDING NAV  │ 64px h,        │ 72px h,          │ 72px h,          │
  │              │ logo 20px      │ logo 24px        │ logo 24px        │
  │              │ buttons: 13px  │ buttons: 14px    │ buttons: 14px    │
  ├──────────────┼─────────────────┼─────────────────┼──────────────────┤
  │ LANDING HERO │ H1: 36px/40px  │ H1: 48px/52px   │ H1: 56px/60px   │
  │              │ pad: 48px 16px │ pad: 64px 24px   │ pad: 80px 24px   │
  │              │ CTA: full-w    │ CTA: auto-w      │ CTA: auto-w      │
  │              │ stats stacked  │ stats inline     │ stats inline     │
  ├──────────────┼─────────────────┼─────────────────┼──────────────────┤
  │ FEATURES     │ 1 column       │ 2×2 grid        │ 4 columns        │
  │              │ gap: 20px      │ gap: 24px        │ gap: 32px        │
  │              │ pad: 48px 16px │ pad: 64px 24px   │ pad: 96px 24px   │
  ├──────────────┼─────────────────┼─────────────────┼──────────────────┤
  │ REGISTER     │ left: hidden   │ left: 40%       │ left: 50%        │
  │              │ right: 100%    │ right: 60%      │ right: 50%       │
  │              │ card: full-w   │ card: max-w-md  │ card: max-w-sm   │
  │              │ form: 16px pad │ (448px)         │ (384px)          │
  ├──────────────┼─────────────────┼─────────────────┼──────────────────┤
  │ VERIFY EMAIL │ card: full-w   │ card: 440px     │ card: 440px      │
  │              │ pad: 32px 20px │ pad: 48px 40px  │ pad: 48px 40px   │
  │              │ (no left/rt)   │                 │                  │
  └──────────────┴─────────────────┴─────────────────┴──────────────────┘
```

---

## ACCESSIBILITY SPECIFICATION

```
SKIP TO CONTENT:
  - Element: <a href="#main-content"> at the very top of the page
  - Text: "Skip to content"
  - Position: absolute, top: -100% (hidden until focused via Tab key)
  - On focus: top: 0, left: 50%, transform: translateX(-50%)
  - Styling: bg primary-600, text white, 14px, weight 500, px-16 py-8, rounded-b-lg
  - z-index: 9999 (above everything)

COLOR CONTRAST (WCAG AA compliance):

  | Element                    | Background | Foreground  | Ratio  | Pass? |
  |----------------------------|------------|-------------|--------|-------|
  | H1 on hero                 | #30525C    | #FFFFFF     | 7.26:1 | AAA   |
  | Subtitle on hero           | #30525C    | #D6E4E8     | 5.52:1 | AA    |
  | Body text on white         | #FFFFFF    | #737373     | 4.64:1 | AA    |
  | Headings on white          | #FFFFFF    | #171717     | 16.7:1 | AAA   |
  | Button text on primary-600 | #30525C    | #FFFFFF     | 7.26:1 | AAA   |
  | Ghost button on white      | #FFFFFF    | #525252     | 6.02:1 | AA    |
  | Error text on card         | #FFFFFF    | #A3461F     | 4.77:1 | AA    |
  | Error banner text          | #FBE8E0    | #A3461F     | 4.39:1 | AA    |
  | Success banner text        | #E9F2F4    | #243E46     | 7.18:1 | AAA   |
  | Hint text on card          | #FFFFFF    | #737373     | 4.64:1 | AA    |
  | Primary button on white bg | #30525C    | #FFFFFF     | 7.26:1 | AAA   |
  | Orange outline on white    | #FFFFFF    | #C35627     | 4.16:1 | AA    |
  | Placeholder text           | #FFFFFF    | #A3A3A3     | 3.08:1 | FAIL  |
  | → Placeholder is decorative only; labels are always visible above inputs |

FOCUS MANAGEMENT:
  - All interactive elements have a visible focus indicator
  - Focus ring: 3px solid primary-500 (#4C848D) at 35% opacity,
    with a 2px ring-offset (gap between element and ring)
  - Focus ring adapts to dark backgrounds (hero): uses 3px solid surface at 50% opacity
  - No elements use outline: none without a replacement focus indicator
  - Focus order follows DOM order (logical tab sequence)

FORM ACCESSIBILITY:
  - Every input has a visible <label> (not placeholder-only)
  - Labels are associated with inputs via htmlFor/id pairing
  - Error messages use aria-describedby linking the input to the error span
  - Validation errors are announced via aria-live="polite" region
  - Password visibility toggle has aria-label: "Show password" / "Hide password"
  - Eye icon button has a minimum 44×44px touch target (larger than visual icon)

SCREEN READER ANNOUNCEMENTS:
  - Page title on route change
  - Form validation errors on blur
  - Loading states: "Verifying your email address. Please wait."
  - Success states: "Email verified successfully."
  - Error states: the exact error message
  - Auto-redirect: countdown announced as it progresses

FONT LOADING:
  - Inter loaded with font-display: swap (text remains visible during font load)
  - Fallback font stack: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif
  - Preload the Inter 400 and 700 weight files for critical above-the-fold text

REDUCED MOTION:
  - All animations respect prefers-reduced-motion: reduce
  - When active: animations are replaced with instant (0ms) transitions
  - The bounce checkmark becomes a simple opacity fade
  - Countdown progress bar eliminates animation (shows static fill)
  - Spinner rotation disabled (show static icon instead)
  - Auto-dismiss banners still dismiss but without animation

KEYBOARD NAVIGATION:
  - All buttons, links, inputs reachable via Tab
  - Enter/Space activates buttons
  - Escape dismisses banners
  - Arrow keys do not change behavior (no custom keyboard traps)
```

---

## MICRO-INTERACTION REFERENCE TABLE

```
┌──────────────────────────────┬──────────────────────┬──────────┬──────────────────────────┐
│ ELEMENT                      │ INTERACTION          │ DURATION │ EASING                   │
├──────────────────────────────┼──────────────────────┼──────────┼──────────────────────────┤
│ Button (hover)               │ scale(1.02)          │ 150ms    │ ease-out                 │
│                              │ shadow sm→md         │          │                          │
├──────────────────────────────┼──────────────────────┼──────────┼──────────────────────────┤
│ Button (press)               │ scale(0.98)          │ 100ms    │ ease-in                  │
├──────────────────────────────┼──────────────────────┼──────────┼──────────────────────────┤
│ Input (focus)                │ ring expand          │ 200ms    │ ease-out                 │
│                              │ border primary-500   │          │                          │
├──────────────────────────────┼──────────────────────┼──────────┼──────────────────────────┤
│ Success checkmark            │ scale 0→1.15→1.0     │ 400ms    │ cubic-bezier             │
│                              │ (bounce)             │          │ (0.68,-0.55,0.265,1.55)  │
├──────────────────────────────┼──────────────────────┼──────────┼──────────────────────────┤
│ Error banner                 │ fade-in +            │ 250ms    │ ease-out                 │
│                              │ slide-down (-8px→0)  │          │                          │
├──────────────────────────────┼──────────────────────┼──────────┼──────────────────────────┤
│ Feature card (hover)         │ translateY(-4px)     │ 200ms    │ ease-out                 │
│                              │ shadow sm→lg         │          │                          │
│                              │ icon scale 1.05      │          │                          │
├──────────────────────────────┼──────────────────────┼──────────┼──────────────────────────┤
│ Spinner                      │ rotate continuously  │ 800ms    │ linear (infinite)        │
├──────────────────────────────┼──────────────────────┼──────────┼──────────────────────────┤
│ Animated dots (loading text) │ Y bounce, staggered  │ 600ms    │ ease-in-out (infinite)   │
├──────────────────────────────┼──────────────────────┼──────────┼──────────────────────────┤
│ Success banner (appear)      │ fade-in +            │ 250ms    │ ease-out                 │
│                              │ slide-down (-8px→0)  │          │                          │
├──────────────────────────────┼──────────────────────┼──────────┼──────────────────────────┤
│ Success banner (dismiss)     │ fade-out +           │ 500ms    │ ease-out                 │
│                              │ slide-up (0→-8px)    │          │ (auto-dismiss)           │
├──────────────────────────────┼──────────────────────┼──────────┼──────────────────────────┤
│ Nav scroll blur              │ backdrop-blur(8px)   │ 200ms    │ ease-out                 │
│                              │ bg opacity 1→0.9     │          │                          │
├──────────────────────────────┼──────────────────────┼──────────┼──────────────────────────┤
│ Left panel bullets (stagger) │ fade-in +            │ 250ms    │ ease-out                 │
│                              │ slide-right (8px)    │ each     │ (100ms stagger)          │
├──────────────────────────────┼──────────────────────┼──────────┼──────────────────────────┤
│ Countdown bar                │ width 50%→0%         │ 3000ms   │ linear                   │
├──────────────────────────────┼──────────────────────┼──────────┼──────────────────────────┤
│ Verify state transition      │ cross-fade old→new   │ 200ms    │ ease-out                 │
│                              │                      │ each way │                          │
├──────────────────────────────┼──────────────────────┼──────────┼──────────────────────────┤
│ Loading overlay (register)   │ opacity 0→0.5        │ 200ms    │ ease-out                 │
├──────────────────────────────┼──────────────────────┼──────────┼──────────────────────────┤
│ Clock icon (expired)         │ rotation oscillation │ 3000ms   │ ease-in-out (infinite)   │
│                              │ ±3°                  │          │                          │
├──────────────────────────────┼──────────────────────┼──────────┼──────────────────────────┤
│ Valid field checkmark        │ scale 0→1 bounce     │ 400ms    │ cubic-bezier             │
│                              │                      │          │ (0.68,-0.55,0.265,1.55)  │
├──────────────────────────────┼──────────────────────┼──────────┼──────────────────────────┤
│ Invalid field error text     │ fade-in +            │ 250ms    │ ease-out                 │
│                              │ slide-down (-8px→0)  │          │                          │
└──────────────────────────────┴──────────────────────┴──────────┴──────────────────────────┘
```

---

## COMPONENT INVENTORY

```
REUSABLE COMPONENTS (needed across pages):

  1.  Button
      Variants: primary-filled, primary-ghost, secondary-outline, white-cta
      Sizes: sm (14px, h-36), md (16px, h-44), lg (16px, h-52)
      States: default, hover, press, focus, disabled, loading

  2.  Input Field
      Variants: default, with-icon-left, with-icon-right, with-hint
      States: default, focus, valid, invalid, disabled, validating

  3.  Surface Card
      Variants: form-card (max-w-440), info-card (max-w-440), feature-card
      States: default, hover (feature cards only)

  4.  Banner
      Variants: success, error, warning
      Props: message, dismissible, autoDismissTimeout
      Animations: appear (slide-down), dismiss (fade+slide-up)

  5.  Spinner
      Sizes: sm (18px), md (24px), lg (48px)
      Colors: primary (default), white (on dark bg)

  6.  Status Icon
      Types: success-check, error-x, warning-triangle, clock, cloud-off
      Sizes: sm (18px), md (24px), lg (48px)
      All include the colored circle container

  7.  Divider
      Orientation: horizontal only
      Variant: with-text ("or"), plain (no text)

  8.  NavBar
      Props: transparent/opaque, blurred/not
      Content slots: left (logo), right (actions)

  9.  SkipToContent
      Always present, always the first focusable element

  10. PillBadge
      Used in hero section
      Variant: primary-400 tint, rounded-full
```

---

## FILE STRUCTURE (recommended)

```
src/
├── pages/
│   ├── LandingPage.tsx        → Page 1
│   ├── RegisterPage.tsx       → Page 2
│   ├── VerifyEmailPage.tsx    → Page 3 (all 6 states)
│   └── LoginPage.tsx          → Page 4 (enhanced)
│
├── components/
│   ├── ui/
│   │   ├── Button.tsx
│   │   ├── Input.tsx
│   │   ├── Spinner.tsx
│   │   ├── Divider.tsx
│   │   ├── Banner.tsx
│   │   └── StatusIcon.tsx
│   │
│   ├── layout/
│   │   ├── NavBar.tsx
│   │   ├── SplitScreen.tsx
│   │   ├── SurfaceCard.tsx
│   │   └── SkipToContent.tsx
│   │
│   └── features/
│       ├── landing/
│       │   ├── HeroSection.tsx
│       │   ├── FeaturesGrid.tsx
│       │   ├── FeatureCard.tsx
│       │   └── PillBadge.tsx
│       │
│       ├── register/
│       │   ├── RegisterForm.tsx
│       │   ├── LeftPanel.tsx
│       │   └── RightPanel.tsx
│       │
│       ├── verify-email/
│       │   ├── VerifyEmailCard.tsx
│       │   └── states/
│       │       ├── LoadingState.tsx
│       │       ├── SuccessState.tsx
│       │       ├── InvalidTokenState.tsx
│       │       ├── ExpiredTokenState.tsx
│       │       ├── NoTokenState.tsx
│       │       └── NetworkErrorState.tsx
│       │
│       └── login/
│           └── PostRegistrationBanner.tsx
│
├── hooks/
│   ├── useCountdown.ts
│   ├── useAutoDismiss.ts
│   └── useReducedMotion.ts
│
├── styles/
│   ├── tokens.css            → CSS custom properties matching design tokens
│   ├── globals.css           → reset, font-face, base styles
│   └── animations.css        → keyframe definitions for all micro-interactions
│
└── utils/
    ├── validation.ts         → client-side form validation rules
    └── contrast.ts           → WCAG contrast calculation utility
```
