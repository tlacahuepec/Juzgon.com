# Code Review Checklist & Rubric

This checklist is used by human reviewers and AI agents during pull request review to enforce the **Engineering Constitution**.

---

## 1. Architecture & SOLID Compliance

- [ ] **Single Responsibility**: Does each new/modified class, module, and function have one clear responsibility?
- [ ] **Open/Closed**: Are new behaviors added via extension (interfaces, adapters, strategies) rather than mutating existing tested logic?
- [ ] **Liskov Substitution**: Do subtypes fully honor base contracts without throwing unexpected exceptions?
- [ ] **Interface Segregation**: Are interfaces thin and cohesive? No client forced to depend on unused methods.
- [ ] **Dependency Inversion**: Do high-level modules depend on abstractions/interfaces rather than concrete implementations?
- [ ] **No Circular Dependencies**: Are component boundaries clean without circular imports?

---

## 2. Code Readability & Writing Standards

- [ ] **Function Length**: Are functions focused and concise (target ≤ 30 lines, max 50)?
- [ ] **File Length**: Are files modular (target ≤ 300 lines, max 500)?
- [ ] **Cyclomatic Complexity**: Is complexity ≤ 10? Are deeply nested conditionals refactored into guard clauses?
- [ ] **Nesting Depth**: Are indentation levels ≤ 3?
- [ ] **Naming**: Are variables, functions, and classes semantic and descriptive? No abbreviations or single-letter names (except short loop counters).
- [ ] **Self-Documenting**: Do comments explain *why*, not *what*?
- [ ] **Public Documentation**: Do all public APIs, methods, and classes have docstrings?
- [ ] **Zero Suppressions**: Are there **NO** suppression comments (`# noqa`, `@SuppressWarnings`, `// noinspection`, etc.)?

---

## 3. Verification & Testing

- [ ] **Verification-First Evidence**: For executable code, did tests fail first? For artifacts, was validation defined before modification?
- [ ] **Behavioral Coverage**: Do tests verify actual behavior, contracts, and edge cases (not just line coverage)?
- [ ] **Coverage Threshold**: Does test coverage meet or exceed the repository minimum (≥ 80%)?
- [ ] **Isolation**: Are unit tests fast, deterministic, and free of external network/disk dependencies?
- [ ] **Mocking Boundaries**: Are internal domain models real objects? (Only external services / I/O should be mocked).

---

## 4. Security & Sensitive Data

- [ ] **Zero Secrets**: No API keys, passwords, bearer tokens, or `.env` files committed.
- [ ] **Input Validation**: Is untrusted input validated at the controller/gateway boundary before reaching domain logic?
- [ ] **No Injection**: Are database queries parameterized and commands sanitized?
- [ ] **Forbidden Files**: No large binary assets, model checkpoints, or generated output batches committed.

---

## 5. Error Handling & Observability

- [ ] **No Swallowed Exceptions**: Are errors handled explicitly? No empty `catch` blocks or bare `except:`.
- [ ] **Domain Errors**: Are meaningful domain-specific errors returned?
- [ ] **Structured Logging**: Are logs semantic (DEBUG/INFO/WARN/ERROR) without logging sensitive PII or credentials?
