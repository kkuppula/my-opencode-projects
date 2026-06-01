# Pattern: Ultra Roster List View Column Addition

## Where Used
- `apps/ultra-ui/app/features/course/roster/course-roster.html` (list view section, lines ~262-420)
- `apps/ultra-ui/app/features/course/roster/course-roster.ts` (module definition)
- `apps/ultra-ui/app/locales/en/features/course/roster/course-roster.yaml` (i18n keys)

## Structure

### 1. Column Header (in header row)
```html
<div class="hide-for-small medium-N columns column-title">
  <span bb-translate>course.roster.listView.YOUR_KEY</span>
</div>
```

### 2. Data Cell (in ng-repeat row)
```html
<div class="medium-N columns hide-for-small">
  <span ng-if="membership.YOUR_FIELD">
    {{ membership.YOUR_FIELD | date:'short' }}
  </span>
  <span ng-if="!membership.YOUR_FIELD">&mdash;</span>
</div>
```

### 3. Module Import (if using custom directives)
```typescript
// course-roster.ts
import { moduleName as yourDirectiveModuleName } from 'directives/your-directive/your-directive';

angular.module(moduleName, [
  yourDirectiveModuleName,  // ← Add here
  ...
])
```

### 4. Translation Key
```yaml
# course-roster.yaml
listView:
  yourKey: "Column Header Text"
```

## Column Width Rules
- Foundation 12-column grid
- Current layout: Name(4) + Role(3) + LastAccess(2) + Accommodations(2) + Actions(1) = 12
- Use `hide-for-small` for non-essential columns (hidden on mobile)
- Match `medium-N` in BOTH header and data row

## When to Apply
- Adding any new column to the course roster list view
- Displaying membership or nested user data

## Common Mistakes
1. **Using bb-date without module import** → shows literal text (use `| date:'short'` filter instead)
2. **Wrong property path** → check `membership.FIELD` vs `membership.user.FIELD`
3. **Grid columns don't sum to 12** → layout breaks
4. **Forgetting hide-for-small on data cell** → mobile overflow
5. **Only adding header, not data cell** → misaligned rows

## Date Formatting
- **DO**: `{{ membership.user.lastLoginDate | date:'short' }}` (built-in, always works)
- **DON'T**: `<bb-date>` unless module is imported
- Formats: `'short'` = "5/26/26, 11:15 PM", `'shortDate'` = "5/26/26"

## First Recorded
- Pipeline: roster-last-access-20260531
- Feature: Add Last Access column to roster
- Date: 2026-05-31
