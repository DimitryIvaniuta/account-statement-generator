#!/usr/bin/env sh

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

echo "Gradle 8.14+ is required and was not available in this environment." >&2
exit 1
