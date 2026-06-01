---
name: context-compressor
description: "Compresses pipeline stage output into minimal, structured inter-agent payloads. Reduces token waste by 50-70% between stages while preserving all actionable information. Use when: between stages, reduce tokens, compress context, stage handoff, agent payload, token budget, optimize pipeline."
argument-hint: "e.g. 'compress discovery report for implementation agent' or 'extract contract from report'"
---

# Context Compressor Skill

Compress full pipeline reports into minimal, structured payloads for the next agent.

## Purpose

Each pipeline stage produces a detailed, human-readable report (~4-10K tokens). But the next agent only needs ~1-3K tokens of actionable information from it. This skill defines **what to extract** and **what to drop** for each stage transition, reducing total pipeline token usage by 50-70%.

## The Problem

```
Without compression:
  Discovery Report (8K) ──────────────────────────▶ Implementation Agent
  + Agent instructions (3K)                          Total input: ~15-25K tokens
  + code-rag results (5K)                            Wasted: ~60% is irrelevant prose
  + report template (2K)

With compression:
  Discovery Report (8K) ──▶ [COMPRESS] ──▶ Contract (2K) ──▶ Implementation Agent
                                                              Total input: ~8K tokens
                                                              Savings: ~50-70%
```

## Compression Rules by Transition

### Transition 1: Discovery → Implementation

**KEEP (structured contract):**
```json
{
  "must_do": [
    {"id": 1, "action": "Add column header", "file": "path/to/file.html", "lines": "289-307", "details": "Use bb-translate directive"}
  ],
  "must_not": [
    {"id": 1, "constraint": "Do NOT modify backend API", "reason": "Field already exists"}
  ],
  "files_to_modify": [
    {"path": "path/to/file.html", "action": "modify", "target_lines": "289-310, 349-353"}
  ],
  "api_contract": {
    "field": "lastAccessDate",
    "type": "Date|null",
    "source": "Already in GET /api/v1/courses/{id}/users response",
    "frontend_property": "membership.lastAccessDate"
  },
  "patterns_to_follow": [
    {"pattern": "bb-date directive for date formatting", "example_file": "course-grades-student.html:209"}
  ],
  "null_handling": "Display localized 'Never' when null",
  "guard_rails": ["hide-for-small on mobile", "No additional API calls", "No model changes"]
}
```

**DROP:**
- Discovery journey/methodology narrative
- code-rag search queries performed
- Alternative approaches considered
- Rationale for why other patterns weren't chosen
- Full code snippets (just reference file:line)
- Architecture diagrams
- Risk assessment prose (keep only if severity ≥ High)
- Open questions that were already resolved

**Token savings: ~60-75%** (8K → 2K)

---

### Transition 2: Implementation → Test Agent

**KEEP:**
```json
{
  "files_changed": [
    {
      "path": "path/to/file.html",
      "changes": "Added column header (L305-307) + data cell (L356-361)",
      "net_lines": "+9"
    }
  ],
  "methods_added": [],
  "behavior_added": [
    "Column shows formatted date when lastAccessDate is present",
    "Column shows 'Never' when lastAccessDate is null",
    "Column hidden on small screens (hide-for-small)"
  ],
  "edge_cases": [
    "null lastAccessDate",
    "undefined lastAccessDate",
    "Very old date",
    "Today's date"
  ],
  "test_patterns": {
    "framework": "Karma + Jasmine",
    "existing_test_file": "course-roster-controller_test.ts",
    "mock_setup": "membership.ui.lastAccessDate"
  },
  "acceptance_criteria": [
    "Date displays in Short format when present",
    "Never displays when null",
    "Column hidden on mobile breakpoints"
  ]
}
```

**DROP:**
- Discovery context entirely
- Implementation decisions rationale
- Contract compliance self-check (already validated at gate)
- Column width math explanation
- Why bb-date was chosen over Angular pipe
- Assumptions narrative
- Verification checklist (that's for verification agent)

**Token savings: ~65-80%** (6K → 1.5K)

---

### Transition 3: Implementation → Verification Agent

**KEEP:**
```json
{
  "requirement_summary": "Add Last Access Date column to course roster list view",
  "contract_checklist": [
    {"item": "Add column header with bb-translate", "status": "done"},
    {"item": "Add data cell with bb-date", "status": "done"},
    {"item": "Adjust column widths", "status": "done"},
    {"item": "Null handling", "status": "done"},
    {"item": "Localization bundle keys", "status": "pending_dependency"}
  ],
  "must_not_checklist": [
    "No API changes",
    "No model changes",
    "No grid view changes",
    "No sorting added"
  ],
  "files_changed": ["path/to/file.html (+9 lines)"],
  "compile_command": "npm run build",
  "test_command": "npm test -- --include course-roster",
  "diff_ref": "HEAD~1",
  "known_issues": ["Localization keys not yet in Learn bundle"],
  "confidence": 85
}
```

**DROP:**
- Full discovery report
- Implementation journey
- code-rag results
- Pattern explanations
- All prose/narrative

**Token savings: ~70-80%** (10K → 2K)

---

### Transition 4: Test → Verification Agent

**KEEP:**
```json
{
  "tests_created": [
    {"file": "path/to/test.ts", "tests": 4, "passing": 4, "failing": 0}
  ],
  "coverage": {
    "scenarios_covered": ["date present", "date null", "responsive hidden"],
    "scenarios_not_covered": ["cross-browser", "accessibility"],
    "reason": "Require manual/E2E testing"
  },
  "test_run_result": "PASS (4/4)",
  "regressions_found": []
}
```

**DROP:**
- Full test code (verification can read the file if needed)
- Mock setup details
- Framework configuration
- Test rationale

**Token savings: ~75%** (4K → 1K)

---

## Orchestrator Integration

The orchestrator uses this skill **between every stage transition**:

```python
# In orchestrator logic (pseudo-code)

# Stage 1 complete
discovery_report = await run_agent("discovery-agent", full_prompt)
save_report("reports/01-discovery-report.md", discovery_report)  # Human-readable

# COMPRESSION STEP (no LLM call — rule-based extraction)
impl_context = compress(
    source=discovery_report,
    target_agent="implementation",
    transition="discovery→implementation"
)

# Stage 2 with compressed context
implementation_report = await run_agent("implementation-agent", impl_context)
```

### Token Budget Per Agent

| Agent | Max Context Budget | Breakdown |
|-------|-------------------|-----------|
| **Discovery** | 6K tokens | Requirement (1K) + Agent instructions (3K) + Repo context (2K) |
| **Implementation** | 8K tokens | Compressed contract (2K) + Agent instructions (3K) + Code context (3K) |
| **Test** | 6K tokens | Compressed impl (1.5K) + Agent instructions (3K) + Test patterns (1.5K) |
| **Verification** | 6K tokens | Compressed state (2K) + Agent instructions (3K) + Diff (1K) |

**Total pipeline: ~26K tokens** (vs. ~100K+ without compression)

---

## Compression Techniques

### 1. Schema Enforcement (Primary)

Convert prose → structured JSON. Fixed schema = predictable token count.

```
❌ "The discovery agent found that the lastAccessed field is already exposed 
   in the MembershipTOPubV1 DTO since version 3300.9.0. It's an ISO 8601 
   date string that can be null when the user has never accessed the course.
   The frontend model already maps this as lastAccessDate (line 57 of 
   course-membership-model.ts) with serialize: 'date' annotation..."

✅ {"field": "lastAccessDate", "type": "Date|null", "source": "API already exposes", "model_line": 57}
```

### 2. Reference-Only (No Inline Code)

Never pass full code blocks between agents. Use file:line references.

```
❌ "Here's the existing column pattern:
   <div class='medium-3 columns column-title hide-for-small'>
     <button id='sortRole' class='active clear ellipse'...
   </div>"

✅ {"pattern_ref": "course-roster.html:289-307", "note": "Follow this column structure"}
```

### 3. Deduplication

If information appears in both discovery and implementation reports, only pass it once.

### 4. Relevance Filtering

Drop anything the target agent won't act on:

| Content Type | Discovery Agent Needs? | Implementation Needs? | Test Needs? | Verification Needs? |
|-------------|:---:|:---:|:---:|:---:|
| Requirement text | ✅ | ❌ (has contract) | ❌ | ❌ |
| Must Do items | — | ✅ | ❌ | ✅ (checklist only) |
| Code examples | — | ✅ (as refs) | ❌ | ❌ |
| Files changed | — | — | ✅ | ✅ |
| Test results | — | — | — | ✅ |
| Risk narrative | — | ❌ | ❌ | ✅ (if High) |

---

## Compression Quality Checks

After compression, verify:

1. **Completeness:** Every Must Do item is present in the contract
2. **Actionability:** Every item has enough info to act on (file + line + what to do)
3. **No ambiguity:** No items where the agent would need to ask "what does this mean?"
4. **Budget compliance:** Output is within the target token budget

If compression drops something critical, the next agent will:
- Report lower confidence
- Flag "insufficient context" in their report
- Trigger a re-run with expanded context (fallback)

---

## Fallback: Expand on Demand

If a sub-agent reports confidence < 60% or flags missing context:

```
Agent: "I need more context about the API contract shape"

Orchestrator: Re-send with expanded context for that specific gap
  → Only expand the relevant section, not the entire report
  → Track which expansions were needed (feedback for improving compression rules)
```

This "compress first, expand if needed" strategy is more efficient than "send everything always" because most runs (>80%) won't need expansion.

---

## Metrics to Track

| Metric | Target | How to Measure |
|--------|--------|---------------|
| Tokens per pipeline run | < 30K total | Sum all agent inputs |
| Compression ratio | > 60% | (original - compressed) / original |
| Expansion rate | < 20% | How often agents request more context |
| Re-do rate | < 5% | How often compression caused wrong output |
| Pipeline duration | -30% improvement | Time from start to final report |

Store in `.opencode/knowledge/metrics/compression-metrics.json`.

---

## Rules

- NEVER drop Must Do items — they are always passed through
- NEVER drop file paths — agents need to know WHERE to work
- NEVER drop API contract fields — type mismatches are critical
- ALWAYS preserve null handling instructions
- ALWAYS preserve guard rails
- Prose/rationale is ALWAYS droppable (it's for humans, not agents)
- If unsure whether to keep something, keep it (safe default)
- Compression is DETERMINISTIC — same input always produces same output (no LLM needed)
- Full reports are ALWAYS saved for humans — compression only affects inter-agent payload
