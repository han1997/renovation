# Install frontend-design as the frontend modification skill

## Goal

Install frontend-design in this project and make it the default design skill for frontend modifications, as requested by the user.

## Requirements

- Install the official `anthropics/skills` skill from `skills/frontend-design` into `.agents/skills/frontend-design/` using the skill-installer helper.
- Preserve upstream skill content and its license; do not overwrite the existing user-level skill.
- Add a project-level routing rule outside the Trellis-managed block in `AGENTS.md`.
- Document the frontend skill and its source in `.trellis/spec/frontend/index.md`.
- Keep the current zero-build vanilla HTML/CSS/JavaScript architecture, existing behavior, and project conventions authoritative over generic skill examples.

## Acceptance Criteria

- [x] The project skill contains a valid `SKILL.md` named `frontend-design` and the referenced license.
- [x] Frontend page, layout, styling, component, and interaction changes are routed to the project skill.
- [x] The frontend spec records the local skill path, upstream source, and compatibility constraints.
- [x] Existing global skill files and frontend application code remain unchanged.
- [x] All new local links resolve and `git diff --check` passes.

## Definition of Done

- Verify installed content against upstream and check skill metadata, license, and documentation links.
- Review the complete diff and preserve the original Trellis-managed instructions.
- No application build or runtime tests are needed because this task changes only skill and documentation files.

## Assumptions and Decisions

- Use a project-level installation so the frontend editing convention is reproducible with the repository.
- An existing user-level skill is available at `C:/Users/hanhu/.agents/skills/frontend-design`; leave it untouched.
- The OpenAI curated catalog does not contain `frontend-design`; use the official Anthropic skills repository instead.
- No blocking product decisions remain; the user's installation request defines the scope.

## Out of Scope

- Frontend redesigns, application code changes, framework or dependency additions.
- Changes to global skills, Trellis workflow semantics, or unrelated design skills.

## Technical Notes

- Shared project skill directory: `.agents/skills/`.
- Local customization guidance: `trellis-meta`, especially `change-skills-or-commands.md`.
- Frontend conventions: `.trellis/spec/frontend/index.md`.
- Upstream: https://github.com/anthropics/skills/tree/main/skills/frontend-design

## Verification Results

- Passed skill metadata, referenced license, local documentation links, Trellis managed-block preservation, change-scope, and whitespace checks.
- Installed files match official upstream Git blob hashes:
  - `LICENSE.txt`: `f433b1a53f5b830a205fd2df78e2b34974656c7b`
  - `SKILL.md`: `a5333457c414d20d625f307df945842c0952ecc3`
- No app runtime tests were run: application code and dependencies are unchanged.
- Implementation is complete; the user approved committing the changes and running finish-work on 2026-09-05.
