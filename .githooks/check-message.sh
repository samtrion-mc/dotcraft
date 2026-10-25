#!/bin/sh
# Usage: check-message.sh "<message>"   or   echo "<message>" | check-message.sh
# Exit 0 if the first meaningful line is a Conventional Commit (or exempt), 1 otherwise.
if [ $# -gt 0 ]; then msg=$1; else msg=$(cat); fi
first=$(printf '%s\n' "$msg" | grep -v '^#' | sed -n '/./{p;q;}')
case "$first" in
  Merge\ *|fixup!\ *|squash!\ *|'Revert "'*) exit 0 ;;
esac
if printf '%s\n' "$first" | grep -Eq '^(feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert)(\([a-z0-9._-]+\))?!?: .+'; then
  exit 0
fi
cat >&2 <<MSG
Invalid commit message: $first
Expected: <type>[(scope)][!]: <description>
Types: feat fix docs style refactor perf test build ci chore revert
MSG
exit 1
