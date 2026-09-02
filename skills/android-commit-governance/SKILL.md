# android-commit-governance

Load this skill when creating a commit, preparing a PR, or reviewing commit
history. Do not load it for code changes that are not being committed yet.

## Commit format

Use the repository convention:

```text
<type>[<us_number>]: <message>
```

Examples:

```text
feat[US-8]: show provider profile photos
chore[US-8]: rename instrumented test suite
docs[US-0]: streamline agent skills
```

The message must be in English, imperative, present tense, concise, and have
no trailing period. Keep the subject under 72 characters when practical.

Allowed types:

`feat`, `fix`, `refactor`, `test`, `chore`, `docs`, `build`, `ci`, `perf`,
`style`.

Use the user-story number that owns the change. Do not invent a number; ask
for it or use the repository's agreed maintenance number.

## Atomicity

- One commit represents one logical change.
- Keep documentation/tooling changes separate from product behavior.
- Do not mix unrelated fixes, formatting, or renames into a feature commit.
- Keep generated files, secrets, `local.properties`, and debug artifacts out of commits.

## Before committing

```bash
git status --short
git diff --check
```

Run the smallest relevant validation. For a full Android change, use:

```bash
make lint
make test
make build
make instrumented
```

If the change only affects documentation or skills, `git diff --check` is the
required minimum validation.

## Pull requests

Use an English PR title with the same commit format. Keep the description
focused on:

- What changed and why.
- How it was validated.
- Files or areas touched.
- Remaining risks.

Prefer one PR per coherent feature, fix, or maintenance phase.

## Examples

Good:

```text
docs[US-0]: streamline Hilt API and BDD guidance
refactor[US-8]: move UI checks to instrumented package
```

Bad:

```text
Fix: Pipeline
feat: Updated login screen.
feat[US-8]: fix login, refactor tests, and clean docs
```

## Review checklist

- Does the subject match `<type>[<us_number>]: <message>`?
- Is the user-story number correct?
- Is the commit atomic and written in English?
- Are validation results and residual risks known?
