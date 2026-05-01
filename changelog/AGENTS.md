# Changelog Guidelines

This directory contains one Markdown file per released version.

## Entry Format

Use this structure for release notes:

```markdown
## X.Y.Z

Released version X.Y.Z - short human-readable release theme

### Added
- Added user-visible capability.

### Changed
- Changed behavior that users, rule authors, or maintainers need to understand.

### Fixed
- Fixed concrete bug, including the observed symptom when useful.

Important migration note: describe required user action here when a rule, configuration, thing type, or channel changes incompatibly.
```

Only include sections that have real entries. Do not leave an empty heading.

## Content Rules

- Write for users and maintainers, not for the release script.
- Do not add tautological entries such as "Bumped version to X.Y.Z"; the filename and heading already say that.
- Prefer impact-oriented wording: describe what broke, what changed, or what users can now do.
- Include dependency bumps only when they explain compatibility, behavior, security, or user-visible support.
- Mention internal refactors only when they affect behavior, reliability, diagnostics, packaging, or future maintenance risk.
- Keep bullets specific and concise. Avoid vague entries like "misc fixes", "cleanup", or "improvements".
- Put breaking changes or required rule/config migration in an explicit migration note.
- Use backticks for rule scopes, commands, thing types, channel IDs, filenames, and log snippets.
