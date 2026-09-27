#!/usr/bin/env bash
# The Sunday routine: pull, regenerate the repo map, commit everything, push.
#
#   scripts/weekly-update.sh <week-number> "<one-line summary>"
#
# It does NOT write the docs/PROGRESS.md entry or tick PLAN.md — do those first
# (by hand or with an assistant), then run this. It refuses to run if PROGRESS.md
# has no entry for the given week, so a push can't go out without the log.
set -euo pipefail

WEEK="${1:?usage: weekly-update.sh <week-number> \"<summary>\"}"
SUMMARY="${2:?usage: weekly-update.sh <week-number> \"<summary>\"}"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if ! grep -q "^## Week ${WEEK} " docs/PROGRESS.md; then
  echo "docs/PROGRESS.md has no '## Week ${WEEK} — ...' entry. Write it first." >&2
  exit 1
fi

git pull --rebase --autostash
scripts/repo-map.sh

# Tests must pass before a weekly push once the app exists.
if [ -x app/mvnw ]; then
  (cd app && ./mvnw -q test)
fi

git add -A
if git diff --cached --quiet; then
  echo "nothing to commit"
  exit 0
fi

git commit -m "Week ${WEEK}: ${SUMMARY}" -m "See docs/PROGRESS.md for the weekly entry."
git push
echo "pushed week ${WEEK}"
