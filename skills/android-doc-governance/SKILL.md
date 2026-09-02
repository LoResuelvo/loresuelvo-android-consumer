# android-doc-governance

Load this skill when changing `AGENTS.md`, `CLAUDE.md`, `README.md`, a skill,
repository commands, or documented conventions.

## Source ownership

- `AGENTS.md` is the canonical agent contract: architecture, security,
  naming, testing policy, and repository-specific rules.
- `CLAUDE.md` is a short pointer; do not duplicate `AGENTS.md`.
- `README.md` is for human setup, commands, and troubleshooting.
- Skills explain how to perform a task; they should not copy the whole agent contract.

## Skill format

Every skill should contain:

1. when to load it;
2. when not to load it;
3. concise operational rules;
4. commands or a checklist when useful;
5. one or two current repository examples.

Write skills in English. Keep code, commands, paths, and identifiers exact.
Keep user-facing repository documentation in its established language.

Target: fewer than 150 lines per skill. Prefer removing repetition over
adding cross-references. Do not include completed roadmap phases, historical
migrations, or speculative future structure.

## Update rules

Update `AGENTS.md` when architecture, security, commands, conventions, or the
skill index changes. Update `README.md` only for human-facing setup or command
changes. Update the affected skill when its procedure or trigger changes.

If a rule is needed by every task, keep it in `AGENTS.md`; if it is needed only
for one workflow, keep it in that skill.

## Review checklist

- Is each rule in the correct document?
- Does every repository-specific example still exist?
- Are paths, commands, versions, and package names current?
- Is the skill atomic and below the line target?
- Did the change introduce duplicated or contradictory guidance?
- Does `git diff --check` pass?

Examples: `AGENTS.md`, `README.md`, and `skills/android-hilt-governance/SKILL.md`.
