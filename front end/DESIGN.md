---
name: Fleet Precision
colors:
  surface: '#faf8ff'
  surface-dim: '#d9d9e5'
  surface-bright: '#faf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f3fe'
  surface-container: '#ededf9'
  surface-container-high: '#e7e7f3'
  surface-container-highest: '#e1e2ed'
  on-surface: '#191b23'
  on-surface-variant: '#434655'
  inverse-surface: '#2e3039'
  inverse-on-surface: '#f0f0fb'
  outline: '#737686'
  outline-variant: '#c3c6d7'
  surface-tint: '#0053db'
  primary: '#004ac6'
  on-primary: '#ffffff'
  primary-container: '#2563eb'
  on-primary-container: '#eeefff'
  inverse-primary: '#b4c5ff'
  secondary: '#565e74'
  on-secondary: '#ffffff'
  secondary-container: '#dae2fd'
  on-secondary-container: '#5c647a'
  tertiary: '#943700'
  on-tertiary: '#ffffff'
  tertiary-container: '#bc4800'
  on-tertiary-container: '#ffede6'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dbe1ff'
  primary-fixed-dim: '#b4c5ff'
  on-primary-fixed: '#00174b'
  on-primary-fixed-variant: '#003ea8'
  secondary-fixed: '#dae2fd'
  secondary-fixed-dim: '#bec6e0'
  on-secondary-fixed: '#131b2e'
  on-secondary-fixed-variant: '#3f465c'
  tertiary-fixed: '#ffdbcd'
  tertiary-fixed-dim: '#ffb596'
  on-tertiary-fixed: '#360f00'
  on-tertiary-fixed-variant: '#7d2d00'
  background: '#faf8ff'
  on-background: '#191b23'
  surface-variant: '#e1e2ed'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.05em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
  headline-md-mobile:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  sidebar-width: 280px
  container-padding: 32px
  gutter: 24px
  card-gap: 20px
---

## Brand & Style

This design system is engineered for high-performance logistics and fleet management, where clarity and rapid data processing are paramount. The aesthetic follows a **Corporate / Modern** direction with a hybrid structural approach: a high-contrast, immersive dark sidebar for focused navigation paired with a clinical, expansive light workspace for data density.

The brand personality is authoritative yet approachable, evoking a sense of reliability and technical sophistication. We utilize deep navy and crisp white to create a professional environment that minimizes cognitive load while highlighting critical operational alerts.

## Colors

The palette is bifurcated to distinguish between global navigation and task-oriented workspaces.
- **Primary Blue (#2563eb):** Used for primary actions, active navigation states, and key interactive elements.
- **Surface Tones:** The sidebar uses a deep navy (#0f172a) to recede from the user's focus, while the main content area utilizes a cool-toned white (#f8fafc) to provide maximum legibility for data tables and charts.
- **Semantic Logic:** Success, Warning, and Error colors are used strictly for status indicators, trend badges, and critical alerts to ensure they "pop" against the neutral background.

## Typography

This design system relies on **Inter** to deliver a systematic, utilitarian feel suitable for complex SaaS environments. 
- **Headlines:** Use tighter letter-spacing and heavier weights to maintain a strong hierarchy.
- **Data Tables:** Use `body-md` for standard row content to maximize information density without sacrificing legibility.
- **Labels:** Small caps or bolded uppercase labels are used for table headers and section overviews to differentiate structural text from user data.

## Layout & Spacing

The design system utilizes a **Fluid Grid** model for the main content area, anchored by a fixed-width sidebar. 
- **Desktop:** A 12-column grid with 24px gutters. Content is housed in a flexible container that expands to a max-width of 1600px.
- **Tablet:** Sidebar collapses to an icon-only rail (72px). Margins reduce to 24px.
- **Mobile:** Sidebar transitions to a hidden off-canvas drawer. Layout reflows to a single column with 16px margins.
- **Rhythm:** All margins and paddings follow an 8px base unit to ensure visual consistency across complex component groupings.

## Elevation & Depth

Hierarchy is established through **Tonal Layers** and **Ambient Shadows**:
- **Level 0 (Floor):** The main background (#f8fafc).
- **Level 1 (Cards):** Pure white (#ffffff) surfaces with a subtle, highly diffused shadow (Y: 4px, Blur: 12px, 4% Opacity Black). These are used for metric cards and data tables.
- **Level 2 (Modals/Dropdowns):** Pure white surfaces with a more pronounced shadow (Y: 10px, Blur: 24px, 10% Opacity Black) to provide clear separation from the workspace.
- **The Sidebar:** Uses depth through color contrast rather than shadow, acting as the structural foundation.

## Shapes

The shape language is modern and approachable, utilizing `rounded-xl` (1.5rem) for major containers like cards and modals to soften the data-heavy interface.
- **Buttons & Inputs:** Use `rounded-lg` (1rem) for a cohesive but slightly more precise feel.
- **Status Badges:** Use a full pill-shape (9999px) to distinguish them clearly from interactive buttons.

## Components

### Buttons
- **Primary:** High-contrast Blue (#2563eb) or Black (#0f172a) with white text. 16px horizontal padding, 10px vertical.
- **Secondary:** Transparent background with a thin border matching the secondary color.

### Metric Cards
- **Structure:** Large `headline-md` for the primary metric, with a small pill-shaped trend indicator (Success Green or Error Red) positioned in the top right.
- **Background:** Solid white with the defined Level 1 shadow.

### Data Tables
- **Header:** `label-md` text with a subtle bottom border (#e2e8f0).
- **Rows:** Alternating subtle hover state (#f1f5f9). No vertical grid lines.
- **Actions:** Use icon-only buttons or "three-dot" menus for row-level actions to keep the view clean.

### Status Badges
- **Style:** Pill-shaped with a low-opacity background of the semantic color (e.g., 10% Green) and a high-opacity text color (100% Green) for accessibility.

### Inputs & Modals
- **Inputs:** White background, 1px border (#cbd5e1), focusing to Primary Blue with a 2px outer glow.
- **Modals:** Centered overlay, semi-transparent backdrop blur (4px), using `rounded-xl` corners. The "Add User" modal should utilize a single-column stack for clarity.