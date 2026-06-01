# Pitfall: Nested Object Property Path in Ultra Membership Model

## What Happens
Template binds to the wrong property path for data that lives in a nested/embedded object. The column shows "—" (null fallback) even though the API response clearly contains the data.

## Why It Happens
The Learn API's membership endpoint embeds related objects (user, course) as nested objects. The Ultra frontend model reflects this nesting:
- `membership.lastAccessDate` → top-level field (course access date, may be empty)
- `membership.user.lastLoginDate` → nested user object field (system login date, populated)

When discovery identifies multiple similar fields, it's easy to bind to the wrong one.

## How to Avoid
1. **Always check the Network tab** during development to see the exact JSON structure
2. **Verify which field is populated** — just because a field exists in the model doesn't mean it's populated in the API response
3. **Match the API request URL**: The `?fields=` parameter on the API call determines which fields are returned. Check what's being requested.
4. **Distinguish between**:
   - `lastAccessDate` = when user last accessed THIS COURSE (from course_users table)
   - `user.lastLoginDate` = when user last logged into THE SYSTEM (from users table)

## Detection
- Column shows null/fallback values while Network tab shows data is present
- Data appears in API response under a nested object (e.g., `results[].user.lastLoginDate`) but template references top-level path

## Affected Area
- Repo: Ultra (frontend)
- Path pattern: `apps/ultra-ui/app/features/course/roster/`
- Model: `components/models/course/membership/course-membership-model.ts`

## API Response Structure (for reference)
```json
{
  "results": [
    {
      "id": "_123_1",
      "userId": "_456_1",
      "courseRoleId": "Student",
      "lastAccessDate": null,          // ← top-level, often empty
      "user": {
        "userName": "jsmith",
        "lastLoginDate": "2026-06-01T03:34:19.517Z"  // ← nested, populated
      }
    }
  ]
}
```

## Correct Template Binding
```html
<!-- WRONG (top-level, often null) -->
<span>{{ membership.lastAccessDate | date:'short' }}</span>

<!-- CORRECT (nested in user object) -->
<span>{{ membership.user.lastLoginDate | date:'short' }}</span>
```

## Occurrences
| Date | Pipeline | Caught At | Field |
|------|----------|-----------|-------|
| 2026-05-31 | Roster Last Access | Post-deploy (user reported) | lastLoginDate vs lastAccessDate |
