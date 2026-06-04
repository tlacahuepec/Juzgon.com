# Story: Evaluate whether LangChain or LangGraph benefits Juzgon

## Goal
Investigate whether adopting LangChain, LangGraph, or a JVM-native alternative would materially improve Juzgon's AI enrichment roadmap while giving the team a focused opportunity to learn LangChain/LangGraph through a small, reversible spike.

## User Story
As a Juzgon developer,
I want to run a timeboxed investigation into LangChain and LangGraph,
so that we can decide whether either framework is beneficial for future AI enrichment work and intentionally capture learning value before adding framework complexity to the Android app.

## Context
Juzgon currently has direct Gemini-based enrichment code in the Android/Kotlin app. The next decision is not "add a framework immediately"; it is whether a framework helps enough to justify new runtime, dependency, testing, and maintenance costs.

Current official documentation positions the ecosystem as follows:

- LangChain provides standard abstractions and prebuilt agent patterns for building LLM applications quickly, primarily in Python and JavaScript/TypeScript.
- LangGraph is the lower-level orchestration/runtime layer for stateful, controllable, multi-step agents and workflows, including persistence, streaming, debugging, deployment, and human-in-the-loop patterns.
- LangChain4j is the JVM-oriented option with Java/Kotlin support, provider integrations, document loading, chat memory, agents, RAG, and Kotlin coroutine extensions.

Source references to verify during the spike:

- LangChain Python overview: <https://docs.langchain.com/oss/python>
- LangGraph overview: <https://docs.langchain.com/oss/python/langgraph>
- LangGraph workflows and agents: <https://docs.langchain.com/oss/python/langgraph/workflows-agents>
- LangGraph JavaScript reference: <https://reference.langchain.com/javascript/modules/_langchain_langgraph.html>
- LangChain4j documentation: <https://docs.langchain4j.dev/>
- LangChain4j Kotlin support: <https://docs.langchain4j.dev/tutorials/kotlin>

## Hypothesis
LangGraph may be beneficial only if Juzgon needs explicit multi-step agent orchestration, retries, tool routing, durable state, human approval, or reusable evaluation/observability around enrichment workflows. LangChain may be beneficial for learning and fast prototyping, but direct Android adoption is questionable unless we use LangChain4j in-process or introduce a small backend service.

## Acceptance Criteria
- [ ] Document at least three candidate architectures:
  - Keep direct Gemini integration in Android.
  - Add LangChain4j inside the Kotlin/JVM app boundary.
  - Add a backend prototype using LangChain or LangGraph outside the Android app.
- [ ] Compare LangChain, LangGraph, LangChain4j, and the current direct Gemini integration against Juzgon's needs.
- [ ] Include learning value as an explicit decision factor, not an informal side benefit.
- [ ] Build one tiny proof of concept that enriches a single attribute from a fixed prompt and returns a typed result.
- [ ] Measure complexity: dependency footprint, testability, API-key handling, offline behavior, latency, retry/error handling, and Android compatibility.
- [ ] Produce a recommendation: adopt now, run a larger spike, use only for backend tooling, or defer.
- [ ] Preserve current production behavior unless a later implementation issue is approved.

## Tests to write first (RED)
- [ ] Unit: typed enrichment response parsing accepts a valid single-attribute result.
- [ ] Unit: invalid or incomplete model output returns an explicit failure instead of a partial write.
- [ ] Unit: timeout/retry policy is deterministic and testable without network calls.
- [ ] Contract: framework-backed enrichment returns the same domain model shape as the current provider boundary.
- [ ] Documentation test: the spike report includes the decision matrix, recommendation, and learning notes.

## Spike Deliverables
1. **Decision matrix** comparing options across benefits, risks, Android fit, team learning value, and migration cost.
2. **Minimal proof of concept** behind an interface or prototype module, with fake model/provider tests and no production behavior switch.
3. **Learning notes** covering what the developer learned about LangChain/LangGraph concepts: tools, agents, graphs, state, memory, retries, evaluation, and observability.
4. **Recommendation** with a next issue if adoption is useful, or explicit deferral criteria if not.

## Decision Criteria
Adoption is beneficial only if the spike shows at least one of these advantages over the current direct Gemini implementation:

- Reduced code complexity at the enrichment boundary.
- Safer structured outputs, retries, or validation.
- Clear path to multi-step enrichment workflows.
- Better observability/evaluation support for prompt and model changes.
- Meaningful developer learning value with low production risk.
- A migration path that keeps API keys secure and does not force unnecessary app/backend complexity.

Adoption should be deferred if:

- The same result is simpler with the current provider abstraction.
- The framework requires a backend we are not ready to operate.
- Android dependency size, startup cost, or unsupported APIs are risky.
- Testability is worse than the current fake-provider approach.
- The primary benefit is curiosity but the spike cannot convert that learning into maintainable project value.

## Recommended First Experiment
Create a small, isolated proof of concept that does **not** alter production enrichment flow:

1. Define a framework-neutral `PrototypeEnrichmentGateway` interface.
2. Implement one fake/test gateway first.
3. Prototype one of these adapters:
   - LangChain4j adapter if the goal is Android/Kotlin fit.
   - LangGraph service adapter if the goal is learning stateful agent orchestration.
4. Use a fixed prompt to request one missing attribute suggestion.
5. Return the existing domain-style result shape.
6. Write the spike report and recommendation before any production integration issue is opened.

## Implementation Plan (GREEN)
1. Keep the spike isolated from production feature flags and dependency injection.
2. Add tests for parsing, error handling, and contract compatibility before wiring any real framework call.
3. Capture setup friction and learning notes while building the prototype.
4. Summarize the decision in a follow-up change document or design note.

## Refactor/Design Notes (REFACTOR)
- **SRP:** Keep provider orchestration separate from parsing, validation, persistence, and UI state.
- **DIP:** Depend on Juzgon enrichment interfaces; make LangChain/LangGraph an adapter detail.
- **Coupling risks:** Avoid leaking framework types into domain models or ViewModels.
- **Naming/structure cleanup:** Use `prototype` or `spike` package/file names until adoption is approved.

## Out of Scope
- Replacing the current Gemini enrichment implementation.
- Adding production API-key storage changes.
- Building a full RAG pipeline.
- Adding a long-running backend service without a separate architecture story.
- Changing Android UI flows.

## Definition of Done
- [ ] All spike code is tested and isolated.
- [ ] RED -> GREEN -> REFACTOR evidence is captured in the PR.
- [ ] The report clearly recommends adopt, defer, or continue with a scoped follow-up.
- [ ] No unrelated app behavior changes are included.
- [ ] Follow-up issues are created only for the selected next step.
