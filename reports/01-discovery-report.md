# Discovery Report: "Last Login" / Last Access Date Column on User Roster

## Metadata
- **Pipeline Mode**: full-feature
- **Target Repos**: Learn (Java/Gradle), Ultra (TypeScript/Angular)
- **Timestamp**: 2026-06-01
- **Confidence**: 92%

## Requirement Summary

Add a "Last Login" column to the course roster page in Ultra, displaying the last access date for each enrolled user. The backend already exposes `lastAccessed` on the membership API — the frontend model already has `lastAccessDate` mapped but it is **not rendered** in the roster list view.

## Existing Architecture

### Relevant Code Paths

| File | Purpose | Key Lines |
|------|---------|-----------|
| **Learn** `publicapi/v1/memberships/CourseMembershipsRestServicePubV1.java` | REST endpoint `GET /public/v1/courses/{courseId}/users` | L41-100 |
| **Learn** `publicapi/v1/memberships/MembershipTOPubV1.java` | DTO - already has `getLastAccessed()` returning `ISODate` | L332-353 |
| **Learn** `publicapi/v1/memberships/params/MembershipsSearchCriteriaPubV1.java` | Search/filter by `lastAccessed` already supported | L121-180 |
| **Learn** `persist/course/CourseMembershipDbPersister.java` | `persistLastAccess()` / `persistLastAccessBatch()` | L121-180 |
| **Learn** DB column | `course_users.last_access_date` | CourseMembershipDbLoaderImpl L1057 |
| **Ultra** `features/course/roster/course-roster-controller.ts` | Roster controller — manages list/grid views | L126-205 |
| **Ultra** `features/course/roster/course-roster.html` | Template — list view has Name, Role, Accommodations columns | L262-370 |
| **Ultra** `models/course/membership/course-membership-model.ts` | Model — `lastAccessDate` field exists (L57, L182) | L49-84, L170-199 |

### Data Flow

```
DB: course_users.last_access_date
  → CourseMembership entity (getLastAccessDate())
  → CourseMembershipTORest → MembershipTOPubV1.getLastAccessed()
  → JSON response field: "lastAccessed" (ISO 8601 date string)
  → Ultra model: membership.lastAccessDate (Date object, already deserialized)
  → NOT currently rendered in roster template
```

## Implementation Contract

### Must Do

1. **Ultra: Add "Last Access" column header** to `course-roster.html` list view header row (~L289-307) — add a new `<div class="medium-2 columns">` with `bb-translate` key
2. **Ultra: Add "Last Access" data cell** in the `<li>` repeater section (~L349-353) — display `membership.lastAccessDate` formatted as a date
3. **Ultra: Adjust column widths** — current layout is 5+3+3=11 columns; need to rebalance (e.g., 4+2+2+3 or 5+2+2+2) to fit 4 columns
4. **Ultra: Add localization key** — `course.roster.listView.lastAccessDate` (and corresponding Learn bundle entry)
5. **Ultra: Add date formatting** — use existing date pipe/filter pattern (check `enrollmentDate` usage for pattern)
6. **Learn: Add localization bundle entry** — for the new column header key

### Must NOT Do

1. **Do NOT modify the backend API** — `lastAccessed` is already exposed on `MembershipTOPubV1` since version 3300.9.0
2. **Do NOT add a new API call** — the data is already included in the membership response
3. **Do NOT change the grid view** — only the list view shows tabular columns; grid view shows cards
4. **Do NOT modify the CourseMembership model** — `lastAccessDate` already exists in the Ultra model
5. **Do NOT add sorting by last access** unless explicitly requested (scope creep)

### Guard Rails

- **Performance**: No additional DB queries needed — `last_access_date` is already loaded with the membership and included in the API response
- **Backward compatibility**: Pure UI addition; no API or data model changes
- **Null handling**: `lastAccessDate` can be null (user never accessed the course) — display empty or "Never"
- **Responsive**: The column should be `hide-for-small` (following the Role and Accommodations column pattern)

### Files to Modify

| File | Change |
|------|--------|
| `ultra/apps/ultra-ui/app/features/course/roster/course-roster.html` | Add column header + data cell |
| `ultra/apps/ultra-ui/app/features/course/roster/course-roster.scss` | Adjust column widths if needed |
| Learn language bundle (TBD — need to locate exact bundle file for `course.roster.*` keys) | Add `course.roster.listView.lastAccessColumn` key |
| `ultra/apps/ultra-ui/app/features/course/roster/course-roster-controller_test.ts` | Update tests if column assertions exist |

### API Contract

- **Field name** (JSON): `lastAccessed`
- **Type**: ISO 8601 date string (nullable)
- **Source**: Already present in `GET /learn/api/public/v1/courses/{courseId}/users` response
- **Frontend model property**: `membership.lastAccessDate` (Date object, already deserialized)

## Risks & Edge Cases

| Risk | Severity | Mitigation |
|------|----------|------------|
| Column layout overflow on medium screens | Low | Use `hide-for-small` and test responsive breakpoints |
| Null `lastAccessDate` for users who never accessed | Low | Display "—" or localized "Never" string |
| Localization bundle location unknown | Med | Need to find where `course.roster.*` keys are defined in Learn |
| Async batch update delay (up to 5 min) | Low | Document in UI tooltip; matches existing API docs |

## Open Questions

1. Where is the localization bundle for `course.roster.*` keys stored in Learn?
2. Should the column be sortable? (Recommend: no — keep scope tight)
3. Date format: relative time ("3 days ago") or absolute date ("May 28, 2026")?
