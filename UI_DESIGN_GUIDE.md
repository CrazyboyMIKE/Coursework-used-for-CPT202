# ConsultBridge UI Design Guide

This document defines the UI direction and development rules for the ConsultBridge consultancy booking platform. Use it when adding new pages, extending existing workflows, or reviewing UI changes.

## Design Direction

The product should feel like a polished consultancy operations platform: warm, calm, professional, and organized. The visual language is based on soft beige backgrounds, glass-like cards, deep green text, warm copper actions, and rounded interface elements.

Core principles:

- Keep pages calm and readable instead of dense or dashboard-heavy.
- Use card-based sections for all major content blocks.
- Use rounded pill actions for navigation and buttons.
- Preserve the warm background atmosphere across all pages.
- Keep all visible UI copy in English.
- Prefer consistent reusable classes from `styles.css` instead of creating page-specific one-off styles.

## Source Files

Primary UI files:

- `src/main/resources/static/styles.css`
- `src/main/resources/static/index.html`
- `src/main/resources/static/login.html`
- `src/main/resources/static/register.html`
- `src/main/resources/static/register-customer.html`
- `src/main/resources/static/register-specialist.html`
- `src/main/resources/static/dashboard.html`
- `src/main/resources/static/admin.html`
- `src/main/resources/static/specialist-detail.html`

Shared frontend behavior:

- `src/main/resources/static/common.js`
- `src/main/resources/static/auth.js`
- `src/main/resources/static/dashboard.js`
- `src/main/resources/static/admin.js`
- `src/main/resources/static/specialist-detail.js`

## Page Structure

Every public or workspace page should follow this structure:

```html
<body class="site-body">
<div class="bg-orb orb-left"></div>
<div class="bg-orb orb-right"></div>

<header class="site-header">
    <a class="brand" href="/">
        <span class="brand-mark">CB</span>
        <span class="brand-copy">
            <strong>ConsultBridge</strong>
            <small>Consultancy Booking Platform</small>
        </span>
    </a>

    <nav class="site-nav">
        <!-- Page links -->
    </nav>
</header>

<main>
    <!-- Page content -->
</main>
</body>
```

Workspace pages should use `dashboard-header` and `dashboard-main`:

```html
<header class="dashboard-header">
    <!-- Brand, navigation, account panel -->
</header>

<main class="dashboard-main">
    <section class="dashboard-banner">
        <!-- Page heading and metrics -->
    </section>
</main>
```

## Color System

Use the CSS variables already defined in `:root`. Do not hard-code new colors unless a repeated pattern needs a new variable.

| Token | Value | Usage |
| --- | --- | --- |
| `--bg` | `#f2ebde` | Main warm page background |
| `--bg-deep` | `#e8dcc6` | Lower gradient background |
| `--surface` | `rgba(255, 251, 245, 0.84)` | Glass panels and major sections |
| `--surface-strong` | `#fffdfa` | Cards and high-contrast content surfaces |
| `--surface-accent` | `#f8f1e6` | Subtle supporting surfaces |
| `--ink` | `#17332c` | Primary text |
| `--muted` | `#61746d` | Secondary text, notes, descriptions |
| `--line` | `rgba(23, 51, 44, 0.12)` | Standard borders |
| `--line-strong` | `rgba(23, 51, 44, 0.2)` | Strong borders |
| `--brand` | `#b65b38` | Primary actions and brand accents |
| `--brand-deep` | `#8f3d23` | Dark brand gradient and important labels |
| `--forest` | `#1f7051` | Positive and active states |
| `--gold` | `#c9982e` | Warning or pending accents |
| `--danger` | `#b5483a` | Errors, rejected, cancelled, inactive |

Color usage rules:

- Primary actions use `linear-gradient(135deg, var(--brand), var(--brand-deep))`.
- Positive states use `var(--forest)`.
- Pending or reserved states use warm gold/brown tones.
- Destructive, inactive, rejected, or cancelled states use `var(--danger)`.
- Body copy should use `var(--ink)`.
- Supporting descriptions should use `var(--muted)`.

## Typography

Base font:

```css
font-family: "Avenir Next", "Segoe UI", "Helvetica Neue", sans-serif;
```

Heading font:

```css
font-family: "Palatino Linotype", "Book Antiqua", Georgia, serif;
```

Typography rules:

- Use serif typography for major headings and selected high-impact values.
- Use the base sans-serif font for body copy, labels, forms, buttons, and operational content.
- Use `.eyebrow` for small uppercase section labels.
- Keep `h1` large and expressive on landing, auth, and dashboard banner sections.
- Avoid adding new font families unless the full UI direction is intentionally updated.

Common heading hierarchy:

- Page hero heading: `h1` inside `.hero-primary`, `.auth-story`, or `.dashboard-banner`.
- Panel heading: `h2` inside `.panel-head`.
- Card heading: `h3` inside `.content-card`, `.feature-card`, or `.specialist-card`.
- Section label: `<p class="eyebrow">...</p>`.

## Layout System

Use these width and spacing patterns:

- Main page width: `width: min(1220px, calc(100% - 32px))`.
- Auth page width: `width: min(1120px, calc(100% - 32px))`.
- Standard large gap: `22px`.
- Standard grid gap: `18px`.
- Compact card gap: `14px`.
- Major section padding: `24px` to `42px`, depending on hierarchy.

Primary layout classes:

- `.landing-main`
- `.auth-shell`
- `.dashboard-main`
- `.landing-hero`
- `.dashboard-banner`
- `.workspace-frame`
- `.two-column-panel`
- `.three-column-panel`
- `.catalog-grid`
- `.management-grid`
- `.feature-grid`
- `.capability-grid`
- `.stats-grid`
- `.banner-metrics`

Use CSS grid for page sections and multi-column content. Use flex only for navigation, action rows, and horizontal toolbars.

## Surfaces And Cards

Major surfaces use glass styling:

```css
background: var(--surface);
border: 1px solid rgba(255, 255, 255, 0.64);
backdrop-filter: blur(18px);
box-shadow: var(--shadow-lg);
```

Reusable card classes:

- `.content-card`
- `.specialist-card`
- `.slot-item`
- `.booking-card`
- `.stat-card`
- `.category-chip`
- `.metric-card`
- `.choice-card`
- `.management-card`

Card rules:

- Use `var(--surface-strong)` for cards inside panels.
- Use `var(--radius-lg)` for cards.
- Use `var(--shadow-md)` for standard cards.
- Use `var(--shadow-lg)` only for major page-level surfaces.
- Keep card content in a simple grid with `gap: 14px` to `20px`.

## Navigation

Brand block:

- Use `.brand`, `.brand-mark`, and `.brand-copy`.
- Keep the brand text as `ConsultBridge`.
- Keep the product subtitle as `Consultancy Booking Platform`.

Navigation rules:

- Landing page main actions use `.nav-pill`.
- Secondary page navigation can use plain `.site-nav a` links.
- Workspace and admin pages use `.dashboard-nav`.
- Use pill-shaped links and buttons for all primary navigation.

Landing page role buttons:

- Sign in: `.nav-pill-signin`
- Customer registration: `.nav-pill-customer`
- Specialist registration: `.nav-pill-specialist`
- Admin portal: `.nav-pill-admin`

## Buttons And Links

Button hierarchy:

- Primary: `button`, `.primary-link`, `.button-link`
- Secondary: `.secondary-link`
- Tertiary or low-emphasis: `.ghost-button`

Rules:

- Primary actions should use the copper brand gradient.
- Secondary and ghost actions should use light translucent surfaces.
- Buttons should remain pill-shaped with `border-radius: 999px`.
- Hover states should use subtle lift: `transform: translateY(-1px)`.
- Disabled buttons should reduce opacity and remove the strong shadow.

Example:

```html
<button type="submit">Submit Booking</button>
<a class="secondary-link" href="/register.html">Choose Registration</a>
<button class="ghost-button" type="button">Refresh Data</button>
```

## Forms

Use `.stack-form` for vertical forms and `.filter-form` for search/filter forms.

Standard form pattern:

```html
<form class="stack-form">
    <div class="form-error hidden" data-form-error></div>

    <label>
        <span>Email</span>
        <input name="email" type="email" required>
        <small class="field-error" data-error-for="email"></small>
    </label>

    <button type="submit">Save Changes</button>
</form>
```

Rules:

- Always wrap each input with a `label`.
- Use a visible `<span>` for the field label.
- Use `.field-error` for field-level messages.
- Use `.form-error` for form-level errors.
- Use `.input-error` when the field has an error.
- Preserve focus styling for accessibility.
- Keep placeholders short and descriptive.

## Status And Role Pills

Use `.role-pill` and `.status-pill` for roles and lifecycle states.

Supported role classes:

- `.role-pill.ADMIN`
- `.role-pill.SPECIALIST`
- `.role-pill.CUSTOMER`

Supported status classes:

- `.status-pill.ACTIVE`
- `.status-pill.AVAILABLE`
- `.status-pill.CONFIRMED`
- `.status-pill.COMPLETED`
- `.status-pill.PENDING`
- `.status-pill.RESERVED`
- `.status-pill.CANCELLED`
- `.status-pill.REJECTED`
- `.status-pill.INACTIVE`
- `.status-pill.CLOSED`

Rules:

- Use uppercase status text to match backend enum values.
- Do not invent new colors for status states.
- If a new status is added, map it into one of the existing semantic color groups first.

## Metrics And Data Values

Use `.banner-metrics` for top-level metrics in dashboard and admin pages.

Metric card pattern:

```html
<article class="metric-card">
    <span>Total Bookings</span>
    <strong id="metric-bookings">0</strong>
</article>
```

For user profile metrics in the dashboard, use:

- `.metric-value`
- `.metric-value-identity`
- `.metric-value-contact`
- `.metric-value-count`
- `.metric-value-mono`
- `.metric-value-numeric`

Rules:

- Long account values should wrap instead of overflowing.
- Use `.metric-value-contact` for email and phone values.
- Use `.metric-value-count` for short numeric totals.
- Use `.metric-value-mono` only where consistent character spacing is needed.

## Page Types

### Landing Page

Use:

- `.site-header`
- `.landing-main`
- `.landing-hero`
- `.hero-primary`
- `.hero-secondary`
- `.section-block`
- `.feature-grid`
- `.capability-grid`

Landing page should explain the product, roles, and functional scope. It should not show temporary demo notes.

### Authentication Pages

Use:

- `.auth-page`
- `.auth-shell`
- `.auth-story`
- `.auth-card`
- `.story-card`
- `.auth-footer`

Authentication pages should have one clear purpose per page. Keep sign-in, registration, password reset, and password change as separate pages.

### Workspace Page

Use:

- `.dashboard-header`
- `.dashboard-main`
- `.dashboard-banner`
- `.workspace-frame`
- `.workspace-sidebar`
- `.workspace-main`
- `.panel`

Workspace pages should use anchor navigation for major modules and preserve role-based visibility.

### Admin Portal

Use:

- `.dashboard-header`
- `.dashboard-main`
- `.dashboard-banner`
- `.panel`
- `.management-grid`
- `.management-list`
- `.management-card`
- `.editor-card`

Admin pages should separate list selection from edit forms. Avoid putting too many unrelated forms into one card.

### Specialist Detail Page

Use:

- `.specialist-detail-shell`
- `.detail-grid`
- `.detail-card`
- `.slot-list`

Detail pages should show profile information first, then available booking slots or related actions.

## Responsive Rules

Existing breakpoints:

- `1080px`: collapse major page grids into one column and reduce complex grids.
- `820px`: stack headers, navigation, account panels, metric grids, forms, and two-column layouts.
- `560px`: reduce hero heading size and stack action rows.

Rules:

- Any new multi-column grid must have a responsive fallback at `1080px` or `820px`.
- Do not rely on fixed widths for text-heavy cards.
- Use `minmax(0, 1fr)` in grid columns to prevent overflow.
- Long values should use `overflow-wrap: anywhere` when needed.

## Interaction Feedback

Use the existing toast system:

```html
<div class="toast hidden" id="toast"></div>
```

Toast classes:

- `.toast`
- `.toast.success`
- `.toast.error`

Rules:

- Success messages should be short and direct.
- Error messages should explain what the user can fix.
- Do not replace field-level validation with toast-only feedback.
- For authentication/session failures, redirect through the shared logic in `common.js`.

## Copywriting Rules

All UI text must be English.

Tone:

- Professional
- Clear
- Action-oriented
- Not overly casual

Examples:

- Use `Sign In`, not `Login` if the page already uses sign-in language.
- Use `Register Customer` and `Register Specialist` for role-specific actions.
- Use `Refresh Portal Data`, `Save Changes`, and `Submit Booking` for clear operational actions.
- Use `No specialist selected yet` for empty states.

Avoid:

- Demo-only text on production-facing pages.
- Long paragraphs inside cards.
- Mixed Chinese and English in static pages.
- Inconsistent role names such as `consultant` and `specialist` for the same concept.

## Accessibility And Usability

Minimum expectations:

- Every form input has a visible label.
- Buttons and links have clear action text.
- Focus states must remain visible.
- Error messages must be rendered near the relevant field.
- Color should not be the only way to understand state.
- Click targets should remain comfortable on mobile.
- Avoid hiding critical information behind hover-only interactions.

## Development Checklist

Before submitting a UI change, check:

- The page uses the shared brand/header pattern.
- The page imports `/styles.css`.
- All visible text is English.
- New colors are either existing CSS variables or justified new tokens.
- New sections use existing layout classes where possible.
- Forms include `.field-error` and `.form-error` where validation can occur.
- Buttons use the established primary, secondary, or ghost styles.
- New grids collapse correctly at existing breakpoints.
- The page works on desktop and mobile widths.
- JavaScript passes `node --check` if a script file changed.

## Preferred Reuse Map

Use this map when deciding which class to reuse:

| Need | Preferred Classes |
| --- | --- |
| Page shell | `.site-body`, `.landing-main`, `.dashboard-main`, `.auth-shell` |
| Header | `.site-header`, `.dashboard-header`, `.brand`, `.site-nav` |
| Hero/banner | `.landing-hero`, `.hero-primary`, `.dashboard-banner` |
| Main content section | `.section-block`, `.panel` |
| Cards | `.content-card`, `.feature-card`, `.specialist-card`, `.booking-card` |
| Metrics | `.banner-metrics`, `.metric-card`, `.stats-grid`, `.stat-card` |
| Forms | `.stack-form`, `.filter-form`, `.field-error`, `.form-error` |
| Actions | `button`, `.primary-link`, `.secondary-link`, `.ghost-button` |
| Status | `.status-pill`, `.role-pill`, `.badge` |
| Lists/editors | `.management-grid`, `.management-list`, `.management-card`, `.editor-card` |

## Do Not Do

- Do not add inline styles to HTML pages.
- Do not create another global CSS file for a single page.
- Do not use default browser buttons without the shared button styling.
- Do not add new purple-themed UI elements.
- Do not replace the warm background with a flat white or dark-only design.
- Do not remove responsive breakpoints when adding new layouts.
- Do not use screenshots or image text for essential UI content.
- Do not add Chinese UI copy to static pages or JavaScript-rendered content.

