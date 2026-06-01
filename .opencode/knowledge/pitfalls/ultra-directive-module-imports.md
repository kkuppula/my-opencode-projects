# Pitfall: AngularJS Custom Directives Not Available Without Module Import

## What Happens
Custom directives like `<bb-date>`, `<bb-datetime>`, `<bb-usercard>` render their inner text as **literal text** instead of being compiled as directives. The screen shows the raw expression (e.g., `membership.user.lastLoginDate`) instead of a formatted value.

## Why It Happens
In AngularJS, custom directives only work if their module is declared as a dependency of the component's module. Unlike built-in directives (`ng-if`, `ng-repeat`), custom directives are modular and must be explicitly imported.

If a module definition in `component.ts` doesn't include the directive's `moduleName` in its dependency array, AngularJS treats the element as an unknown HTML tag and renders its text content literally.

## How to Avoid
1. **Check module dependencies first**: Before using any `bb-*` directive in a template, verify it's in the component's module dependency list (the `.module(name, [...])` array in the component's `.ts` file)
2. **Prefer built-in filters for simple cases**: For date formatting, `{{ expr | date:'short' }}` always works without imports
3. **Look at existing directives in the template**: If the template already uses `bb-translate` but not `bb-date`, that's a signal `bb-date` isn't imported

## Safe Alternative
Instead of `<bb-date>`, use AngularJS built-in date filter:
```html
<!-- UNRELIABLE (requires module import) -->
<bb-date format="Short">membership.user.lastLoginDate</bb-date>

<!-- RELIABLE (always available) -->
{{ membership.user.lastLoginDate | date:'short' }}
```

### AngularJS date filter formats:
| Format | Output Example |
|--------|---------------|
| `'short'` | 5/26/26, 11:15 PM |
| `'shortDate'` | 5/26/26 |
| `'medium'` | May 26, 2026, 11:15:30 PM |
| `'mediumDate'` | May 26, 2026 |
| `'longDate'` | May 26, 2026 |
| `'fullDate'` | Monday, May 26, 2026 |

## Detection
- Visual: Screen shows a property path as text (e.g., `membership.user.lastLoginDate`)
- Code review: Check if the directive module is in the component's `.module()` dependency array

## Affected Area
- Repo: Ultra (frontend)
- Path pattern: `apps/ultra-ui/app/features/**/` (any component using custom directives)
- Module files: `*.ts` files with `angular.module(moduleName, [...])` pattern

## Key Module Names (for reference)
| Directive | Module Name | Import Path |
|-----------|-------------|-------------|
| `bb-date` | `ultra.directives.bbDate` | `directives/date/date-directive` |
| `bb-datetime` | (check directive file) | `directives/datetime/datetime-directive` |
| `bb-translate` | (usually inherited) | `directives/translate/...` |

## Occurrences
| Date | Pipeline | Caught At | Component |
|------|----------|-----------|-----------|
| 2026-05-31 | Roster Last Access | Post-deploy (user reported) | course-roster |
