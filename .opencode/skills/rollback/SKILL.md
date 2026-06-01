---
name: rollback
description: "Safely reverts all changes made by the current pipeline run. Restores files to their pre-pipeline state. Use when: undo changes, revert pipeline, rollback, cancel changes, start over, something went wrong."
argument-hint: "e.g. 'rollback all changes' or 'revert the pipeline changes'"
---

# Rollback Skill

Safely revert all changes made by the current pipeline run.

## Purpose

When a pipeline run produces incorrect results, or the user wants to start over, this skill cleanly reverts all changes without losing track of what was attempted.

## Workflow

### Step 1: Identify Changes to Revert

```bash
# In the target repo
git status                    # See what's changed
git diff --stat               # Summary of changes
git diff --name-only          # List of files
```

### Step 2: Categorize Changes

| Category | Action |
|----------|--------|
| Modified files (tracked) | `git checkout -- <file>` |
| New files (untracked) | `rm <file>` (with confirmation) |
| Staged changes | `git reset HEAD -- <file>` then checkout |
| New test files | `rm <file>` (with confirmation) |

### Step 3: Confirm Before Reverting

Present to user:
```
🔄 Rollback Summary

Files to revert (modifications):
  • src/main/java/com/example/Feature.java (+45/-3)
  • src/main/java/com/example/Service.java (+12/-0)

Files to delete (newly created):
  • src/test/java/com/example/FeatureTest.java (new)

Total: 2 files reverted, 1 file deleted

⚠️  This cannot be undone. Proceed?
  ✅ Yes, rollback all
  📋 Let me pick which files to keep
  ⏹️ Cancel
```

### Step 4: Execute Rollback

```bash
# Reset staged changes
git reset HEAD -- .

# Revert modified tracked files
git checkout -- <file1> <file2> ...

# Remove new untracked files created by pipeline
rm <new_file1> <new_file2> ...
```

### Step 5: Verify Clean State

```bash
git status  # Should show clean working tree (or back to pre-pipeline state)
git diff    # Should show no changes
```

### Step 6: Archive the Attempt (Optional)

Before rolling back, optionally save what was attempted:

```bash
# Create a patch file for reference
git diff > .opencode/rollbacks/attempt-$(date +%Y%m%d-%H%M%S).patch

# Or create a stash
git stash push -m "Pipeline attempt: [feature name] - rolled back"
```

## Output Format

```markdown
# Rollback Report

## Status: ✅ Complete / ❌ Failed

## Changes Reverted
| File | Action | Status |
|------|--------|--------|
| `path/to/file` | Reverted to HEAD | ✅ |
| `path/to/new-file` | Deleted | ✅ |

## Archive
- Patch saved: `.opencode/rollbacks/attempt-20260531-100000.patch`
- (OR) Stash created: `stash@{0}: Pipeline attempt: [feature]`

## Post-Rollback State
- Working tree: Clean / Has pre-existing changes
- Branch: [current branch]
- Last commit: [hash] [message]

## Recommendations
- [Why the pipeline failed / what to try differently]
- [Suggested adjustments for retry]
```

## Safety Rules

- ALWAYS show the user what will be reverted before doing it
- NEVER force-delete files without confirmation
- NEVER run `git reset --hard` (too destructive)
- ALWAYS offer to archive the attempt first (stash or patch)
- If there were pre-existing changes before the pipeline, identify and preserve them
- If unsure what the pipeline changed vs. pre-existing → ask the user

## Smart Rollback (Partial)

If the user wants to keep some changes:
```
Which changes would you like to keep?
  1. ☐ src/main/java/Feature.java (new feature code)
  2. ☐ src/main/java/Service.java (service changes)
  3. ☐ src/test/java/FeatureTest.java (new tests)

Select files to KEEP (everything else will be reverted):
```
