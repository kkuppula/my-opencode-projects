# Pattern: Learn Locale-Layered Resource Bundles

## Where Used
- `workspace/base/base-locale/<locale>/messages/*.properties`
- Locales: `en_US`, `en_US_k12`, `en_US_pro`, `en_GB`, `en_AU`, and others

## Structure

```
workspace/base/base-locale/
├── en_US/messages/              ← Base English (default)
│   ├── course.properties
│   ├── organization.properties
│   └── ...
├── en_US_k12/messages/          ← K12 overrides (uses "Class" terminology)
│   ├── course.properties
│   └── organization.properties
├── en_US_pro/messages/          ← Professional overrides
└── en_GB/messages/              ← British English overrides
```

### Key Mechanism
- **Layered override**: `en_US_k12` overrides `en_US` for K12-specific terminology
- **Same keys, different values**: The property key (e.g., `banner.course.complete`) is the same across locales — only the value differs
- **Renderer is locale-agnostic**: Java code loads the key; the bundle resolution picks the right locale file at runtime

## When to Apply
- Changing any user-facing text in Learn Original
- Renaming terminology (e.g., "Class" → "Course")
- Adding locale-specific messages
- Checking if a text change is needed in one locale or all

## Common Mistakes
- **Changing all locales when only one needs it**: Check if the text difference is locale-specific (K12 uses "Class" while others use "Course")
- **Modifying Java code for text changes**: The renderer already handles context (course vs org) — only properties need updating
- **Forgetting the subtitle text**: Banner messages often have a bold title AND a subtitle portion in the same property value

## Key Renderer
`workspace/projects/apis/taglib-api/src/main/java/blackboard/servlet/renderer/ngui/BreadcrumbBarRenderer.java`
- Lines 509-528: Course vs Organization conditional banner selection
- Uses `course.isCourse()` / `course.isOrganization()` to pick the right resource key
- Course key: `banner.course.complete` (from `course` bundle)
- Org key: `banner.organization.complete` (from `organization` bundle)

## Example

**Property value with HTML (typical pattern):**
```properties
banner.course.complete=<strong>Course is complete!</strong> You can no longer make edits to this course or its content.
```

## First Recorded
- Pipeline: banner-rename-20260531
- Feature: Rename "Class is Complete" banner
- Date: 2026-05-31
