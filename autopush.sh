#!/usr/bin/env bash

set -e

cd "$(dirname "$0")"

echo "========== $(date) =========="

# Stage all changes
git add -A

# Commit only if there are changes
if ! git diff --cached --quiet; then
    git commit -m "Auto Backup $(date '+%Y-%m-%d %H:%M:%S')"
else
    echo "No local changes to commit."
fi

# Pull latest changes (automatically stashes if necessary)
git pull --rebase --autostash origin main

# Push
git push origin main

echo "Backup complete."