# Requirement Template

Use this template when submitting feature requests to the multi-agent pipeline.
Structured requirements reduce discovery errors and eliminate back-and-forth clarification.

---

## Template

```
## Feature: [Short title]

### What
[1-2 sentences: what to build and WHERE it goes in the system.
 Reference existing code/patterns if you know them.]

### Who
[Audience — who sees it, who does NOT]
- ✅ [users who SHOULD see/use this]
- ❌ [users who must NOT]

### When / Conditions
[Visibility rules, date logic, feature flags, environment constraints]
- Show when: [condition]
- Hide when: [condition]
- Feature flag: [yes/no, name if known]

### Content / Behavior
[What it says/does, links, interactions, error states]

### Scope
[Which repo(s), which layer, what patterns to follow]
- Repo: Backend (learn) | Frontend (ultra) | Both
- Layer: [REST API / Service / UI Component / Server-rendered / DB]
- Pattern to follow: [existing similar feature or code path]

### Reference
[Existing code to look at, similar patterns, screenshots, ADO ticket]
```

---

## Example (Good)

```
## Feature: Read-Only Warning Banner for Original Courses

### What
Add a new warning banner inside the **Original (Classic) course view**
(same location as the existing "Course is complete!" banner in the
breadcrumb bar area) that informs non-students about the upcoming
forced read-only (course completion) date.

### Who
- ✅ Instructors, TAs, Course Builders, Admins (non-students)
- ❌ Students must NOT see this banner
- ❌ Ultra courses must NOT show this banner
- ❌ Organizations must NOT show this banner

### When / Conditions
- Show ONLY on or after: August 1, 2026
- Show UNTIL: January 1, 2027 (when forced read-only takes effect)
- Date comparison must be part of the code logic
- Deploy code NOW, but banner auto-activates on Aug 1, 2026
- Feature flag: yes (for safe rollback)

### Content / Behavior
- Inform: "This Original course will be set to read-only on Jan 1, 2027"
- Include: Link to external documentation (URL TBD — use placeholder)
- Not dismissible (always visible when conditions met)

### Scope
- Repo: Backend only (Learn)
- Layer: Server-rendered (Java → Velocity template)
- Pattern to follow: existing "Course is complete!" banner in BreadcrumbBarRenderer

### Reference
- Existing banner: setCourseOrOrganizationCompletionBanner() in BreadcrumbBarRenderer.java
- Template: breadcrumbBar.vm
- Locale: course.properties → banner.course.complete
```

---

## Anti-Patterns (Avoid These)

| ❌ Vague | ✅ Clear |
|----------|----------|
| "Add a banner" | "Add a banner inside the Original course breadcrumb bar" |
| "only certain users" | "✅ Instructors ❌ Students ❌ Guests" |
| "show it later" | "Show on/after Aug 1, 2026 (date check in code)" |
| "same place as the other one" | "Same location as 'Course is complete!' banner in BreadcrumbBarRenderer" |
| "in the backend" | "Backend (Learn repo), server-rendered layer, Velocity template" |
| "refer to the screenshot" | "Mirror pattern: setCourseOrOrganizationCompletionBanner()" |

---

## Tips

1. **Name the existing pattern** — if you know the class/method, mention it
2. **List exclusions explicitly** — "NOT students, NOT Ultra, NOT orgs"
3. **Specify dates as absolutes** — "Aug 1, 2026" not "around 6 months from now"
4. **State the repo** — even if obvious, it saves a routing step
5. **Mention feature flag needs** — agents will add it automatically if you say "gate behind flag"
6. **TBD items are fine** — just mark them clearly: "(URL TBD — use placeholder)"
