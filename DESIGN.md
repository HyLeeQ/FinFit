# Design System Document

## 1. Overview & Creative North Star
**Creative North Star: The Luminescent Observer**
This design system moves away from the clinical, rigid nature of traditional health apps toward a "Luminescent Observer"—an interface that feels less like a database and more like a quiet, premium sanctuary for personal growth. 

To break the "template" look, we employ **Editorial Asymmetry**. By utilizing high-contrast typography scales (the authoritative 'Manrope' paired with the functional 'Inter') and intentional breathing room, we create an environment where data is curated, not just displayed. The experience is defined by soft, glowing accents against a deep, ink-like canvas, creating a sense of sophisticated calm and nocturnal focus.

---

## 2. Colors
Our palette is rooted in a "Deep Space" philosophy, using tonal shifts to define boundaries rather than structural lines.

- **Primary & Tonal Palette:** 
    - `background`: #0e0e0e (The anchor)
    - `surface-container`: #1a1a1a (Primary card surface)
    - `primary`: #64b5f6 (Water / Vitality)
    - `secondary`: #ea73fb (Sleep / Recovery)
    - `tertiary`: #bbffb3 (Activity / Growth)

- **The "No-Line" Rule:** 1px solid borders are strictly prohibited for sectioning. Boundaries must be defined solely through background color shifts. A `surface-container-low` (#131313) card should sit on a `background` (#0e0e0e) to create a natural, soft edge.
- **Surface Hierarchy & Nesting:** Treat the UI as physical layers of frosted obsidian. Use `surface-container-highest` (#262626) for interactive elements within a `surface-container` (#1a1a1a) card to create "inner depth" without shadows.
- **The "Glass & Gradient" Rule:** For floating Action Buttons or High-Impact Hero sections, use semi-transparent `surface` colors with a 20px Backdrop Blur. 
- **Signature Textures:** Main CTAs should utilize a subtle linear gradient from `primary` (#64b5f6) to `primary_container` (#54a7e7) at a 135° angle to provide a "lit from within" glow that flat colors cannot achieve.

---

## 3. Typography
The typography system uses a dual-typeface approach to balance editorial authority with high-density legibility.

- **Display & Headlines (Manrope):** These are the "Voices" of the system. Large, bold, and authoritative. Use `display-lg` (3.5rem) for milestone achievements to create an emotional "moment" of celebration.
- **Titles & Body (Inter):** The "Workhorse." Inter is used for all functional data points. Its high x-height ensures that even `body-sm` (0.75rem) metrics are crystal clear against dark backgrounds.
- **Hierarchy Logic:** Use `headline-md` for section headers (e.g., "Health Analysis") to establish a clear architectural anchor, while `label-md` is reserved for metadata, always set in `on_surface_variant` (#adaaaa) to reduce visual noise.

---

## 4. Elevation & Depth
We abandon the "drop shadow" of the early web in favor of **Tonal Layering**.

- **The Layering Principle:** Depth is achieved by "stacking." A card using `surface-container-high` (#20201f) floating over a `surface-dim` (#0e0e0e) background provides sufficient contrast for the eye to perceive elevation.
- **Ambient Shadows:** When a modal or floating menu requires absolute separation, use an "Ambient Glow": a shadow with a 40px blur, 0px offset, and 6% opacity using the `primary` token color. This mimics the light of a screen reflecting off a dark surface.
- **The "Ghost Border" Fallback:** If accessibility requires a container edge, use the `outline_variant` (#484847) at 15% opacity. It should be felt, not seen.
- **Glassmorphism:** Apply a 10% opacity white overlay on top of `surface_container_highest` for elements that need to feel "closer" to the user, like a navigation bar or a top-level alert.

---

## 5. Components

- **Buttons:**
    - *Primary:* Gradient fill (`primary` to `primary_dim`), `xl` (1.5rem) roundedness. No border.
    - *Secondary:* `surface_container_highest` fill, `on_surface` text.
- **Chips:**
    - Use `full` (9999px) roundedness. Selected states should use a soft glow (0.1 opacity of the accent color) rather than a heavy solid fill.
- **Circular Progress Indicators:**
    - For activity tracking, use a stroke width of 12-16px. The track should be `surface_variant` (#262626) and the progress should be the specific accent color (Blue, Green, or Purple).
- **Cards:**
    - Use `lg` (1rem) or `xl` (1.5rem) corner radius. Vertical whitespace (using a 16px/24px/32px scale) is the only allowed separator. **Never use divider lines.**
- **Input Fields:**
    - Background: `surface_container_low`. Bottom-border only (Ghost Border style) during focus state to maintain an editorial feel.
- **Health-Specific Components:**
    - *The Metric Pulse:* A small, animated glow behind the primary daily metric to indicate "live" data.
    - *The Summary Card:* A large `surface-container` block that uses `display-sm` for the primary number, creating a "dashboard" focal point.

---

## 6. Do's and Don'ts

### Do:
- **Use "Optical Alignment":** Sometimes icons need to be nudged 1-2px to feel centered in their circular containers.
- **Embrace Asymmetry:** Place high-level summaries on the left and supporting icons on the right to lead the eye.
- **Prioritize Breathing Room:** If in doubt, add 8px more padding. The system relies on space to feel premium.

### Don't:
- **Don't use pure black (#000000):** Except for the `surface_container_lowest` tier. Pure black can cause "smearing" on OLED screens during scrolling.
- **Don't use high-contrast borders:** They break the "Luminescent Observer" vibe and make the app feel like a spreadsheet.
- **Don't crowd icons:** Line icons require significant padding (at least 12px) to remain legible and sophisticated.
