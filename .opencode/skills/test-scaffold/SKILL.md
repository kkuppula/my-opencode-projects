---
name: test-scaffold
description: "Generates test boilerplate and structure based on implementation report. Pre-generates imports, mocking setup, test class skeleton, and test method stubs. Speeds up the test agent by ~40%. Use when: generate test boilerplate, scaffold tests, create test skeleton, test setup, mock setup."
argument-hint: "e.g. 'scaffold tests for CourseSettingsController' or 'generate test boilerplate for the AI toggle feature'"
---

# Test Scaffold Skill

Pre-generate test boilerplate to speed up the Test Agent.

## Purpose

The Test Agent spends ~40% of its time on boilerplate: figuring out imports, mocking patterns, test class setup, and naming conventions. This skill front-loads that work by generating a ready-to-fill skeleton.

## Workflow

### Input Required
- Implementation Report (from Implementation Agent)
- Target repo stack (Java/Gradle or TypeScript/Angular)
- Existing test patterns (from code-rag or discovery)

### Step 1: Determine Test Framework

| Stack | Framework | Runner | Mocking |
|-------|-----------|--------|---------|
| Java/Gradle (Learn) | JUnit 4 | `@RunWith(MockitoJUnitRunner.class)` | Mockito + MockedStatic |
| TypeScript/Angular (Ultra) | Jest | `describe/it` | jest.mock / TestBed |

### Step 2: Generate Skeleton Per Class Under Test

For each class in the implementation report's "What's Left for Test Agent" section:

#### Java Test Skeleton:

```java
package [same.package.as.source];

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

// [Additional imports based on dependencies]

@RunWith(MockitoJUnitRunner.class)
public class [ClassName]Test {

    // === Mocks ===
    @Mock
    private [DependencyType] [dependencyName];
    // [One @Mock per dependency from implementation report]

    // === Subject Under Test ===
    @InjectMocks
    private [ClassName] subject;

    // === Test Data ===
    // [Common test fixtures]

    @Before
    public void setUp() {
        // [Common setup based on patterns]
    }

    // === Happy Path Tests ===

    @Test
    public void should[Scenario]() {
        // GIVEN
        // [setup]

        // WHEN
        // [action]

        // THEN
        // [assertions]
    }

    // === Guard Clause Tests ===

    @Test
    public void should[GuardScenario]() {
        // TODO: implement
    }

    // === Edge Case Tests ===

    @Test
    public void should[EdgeCase]() {
        // TODO: implement
    }
}
```

#### TypeScript Test Skeleton:

```typescript
import { TestBed } from '@angular/core/testing';
import { [ClassName] } from './[file-name]';
// [Additional imports]

describe('[ClassName]', () => {
  let component: [ClassName];
  // let mockDependency: jest.Mocked<[DependencyType]>;

  beforeEach(() => {
    // TestBed setup or manual instantiation
  });

  describe('[methodName]', () => {
    it('should [happy path scenario]', () => {
      // GIVEN

      // WHEN

      // THEN
    });

    it('should [guard clause scenario]', () => {
      // TODO: implement
    });

    it('should [edge case scenario]', () => {
      // TODO: implement
    });
  });
});
```

### Step 3: Pre-fill Known Information

From the Implementation Report, fill in:
- Exact class names and method signatures
- Known dependencies to mock
- Known scenarios from "What's Left for Test Agent"
- Known mocking requirements

### Step 4: Output

Produce the scaffolded test file(s) with:
- All imports resolved
- All mocks declared
- Method stubs with `// TODO: implement` for the Test Agent to fill
- Comments indicating what each test should verify

## Rules

- Follow the EXACT test naming convention of the repo (no "test" prefix for Java)
- Match the mocking style used in existing tests
- Include both `@Test` method stubs AND `// TODO` markers
- The Test Agent should be able to fill in the TODOs without redoing the setup
- Don't generate assertions — that's the Test Agent's job
- DO generate the GIVEN/WHEN/THEN structure as comments
