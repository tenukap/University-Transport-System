---
name: Kinetic Logistics
colors:
  surface: '#f8f9ff'
  surface-dim: '#cbdbf5'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4ff'
  surface-container: '#e5eeff'
  surface-container-high: '#dce9ff'
  surface-container-highest: '#d3e4fe'
  on-surface: '#0b1c30'
  on-surface-variant: '#44474c'
  inverse-surface: '#213145'
  inverse-on-surface: '#eaf1ff'
  outline: '#74777d'
  outline-variant: '#c4c6cd'
  surface-tint: '#4f6073'
  primary: '#041627'
  on-primary: '#ffffff'
  primary-container: '#1a2b3c'
  on-primary-container: '#8192a7'
  inverse-primary: '#b7c8de'
  secondary: '#0058bc'
  on-secondary: '#ffffff'
  secondary-container: '#0070eb'
  on-secondary-container: '#fefcff'
  tertiary: '#121618'
  on-tertiary: '#ffffff'
  tertiary-container: '#272a2d'
  on-tertiary-container: '#8e9194'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d2e4fb'
  primary-fixed-dim: '#b7c8de'
  on-primary-fixed: '#0b1d2d'
  on-primary-fixed-variant: '#38485a'
  secondary-fixed: '#d8e2ff'
  secondary-fixed-dim: '#adc6ff'
  on-secondary-fixed: '#001a41'
  on-secondary-fixed-variant: '#004493'
  tertiary-fixed: '#e0e3e6'
  tertiary-fixed-dim: '#c4c7ca'
  on-tertiary-fixed: '#191c1e'
  on-tertiary-fixed-variant: '#44474a'
  background: '#f8f9ff'
  on-background: '#0b1c30'
  surface-variant: '#d3e4fe'
typography:
  headline-xl:
    fontFamily: Inter
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 34px
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-bold:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.05em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  gutter: 24px
  margin-mobile: 16px
  margin-desktop: 64px
  container-max: 1280px
---

## Brand & Style

The design system is rooted in the principles of **Modern Corporate** efficiency and **Minimalist** clarity. It targets B2B stakeholders and logistics managers who prioritize reliability, speed, and precision. The visual language avoids decorative fluff in favor of a highly structured, functional interface that evokes a sense of "moving parts working in perfect harmony."

The emotional response should be one of absolute trust and systematic competence. By utilizing heavy whitespace, a disciplined color application, and a focus on data legibility, the UI communicates that the user’s cargo and data are in professional hands.

## Colors

The palette is anchored by **Deep Navy (#1A2B3C)**, used for primary headings, navigation bars, and heavy structural elements to establish authority. **Safety Blue (#007AFF)** serves as the high-visibility action color, reserved strictly for interactive elements like primary buttons, active states, and progress indicators.

**Light Grey (#F5F7FA)** provides a clean, low-fatigue background for complex data tables and dashboards. Neutral tones (Slate/Grey) are used for secondary text and borders to maintain a soft but clear hierarchy. Success, warning, and error states should follow standard utility conventions but with slightly desaturated tones to match the professional aesthetic.

## Typography

This design system utilizes **Inter** across all levels to take advantage of its exceptional legibility and systematic "tall" x-height, which is ideal for data-heavy logistics interfaces. 

Headlines use a tighter letter-spacing and heavier weights to feel impactful and grounded. Body text is optimized for readability with generous line heights. The `label-bold` style is specifically designed for small metadata, such as tracking numbers or status tags, using uppercase styling to differentiate from standard prose.

## Layout & Spacing

The layout follows a **12-column fluid grid** for desktop and a **4-column grid** for mobile. A strict **8px spacing scale** governs all margins and paddings, ensuring vertical rhythm across modular components.

- **Desktop:** 64px outer margins with 24px gutters. Content is centered in a max-width container to prevent line-lengths from becoming unreadable on ultra-wide monitors.
- **Tablet:** 32px outer margins; cards typically stack into 2 columns.
- **Mobile:** 16px outer margins; all primary content containers span the full width (12 columns).

Large sections (Hero, Feature blocks) should utilize "Section Padding" of 80px to 120px to allow the design to breathe and emphasize the premium nature of the service.

## Elevation & Depth

Hierarchy is established through **Ambient Shadows** and **Tonal Layering**. Surfaces are categorized into three levels:
1.  **Floor (Level 0):** The Light Grey (#F5F7FA) background.
2.  **Card/Surface (Level 1):** White (#FFFFFF) surfaces with a subtle, highly diffused shadow (Y: 2px, Blur: 4px, 5% Opacity Black).
3.  **Overlay/Floating (Level 2):** Used for tooltips or modals, utilizing a more pronounced shadow (Y: 8px, Blur: 16px, 10% Opacity Black).

Avoid heavy borders; use 1px strokes in a light grey (#E2E8F0) only when elements need to be separated on a white background.

## Shapes

The shape language is **Rounded (Level 2)** to soften the corporate aesthetic and make the technology feel accessible. Standard components like buttons and input fields use an 8px (0.5rem) radius. Larger containers, such as dashboard cards or hero image masks, use 16px (1rem) to create a clear container-nested-within-container relationship. Icons should follow a consistent 2px stroke weight with slightly rounded terminals to match the UI.

## Components

- **Buttons:** Primary buttons use Safety Blue with white text. Hover states shift the background to a slightly darker shade of blue. Secondary buttons use a Deep Navy outline with a transparent background.
- **Input Fields:** Use white backgrounds with 1px light grey borders. On focus, the border transitions to Safety Blue with a 2px soft outer glow (halo).
- **Status Chips:** Small, pill-shaped indicators for shipment statuses (e.g., "In Transit," "Delivered"). Use subtle background tints of Green, Amber, or Blue with high-contrast text.
- **Data Cards:** The primary vehicle for shipment info. Cards should have a white background, Level 1 elevation, and an 8px corner radius.
- **Tracking Bar:** A custom component featuring a horizontal line with circular nodes. The completed portion of the line and nodes are Safety Blue; upcoming steps are light grey.
- **Lists:** Use "Zebra-striping" or subtle bottom borders to separate rows in data-heavy views, ensuring row height is at least 48px for touch targets.