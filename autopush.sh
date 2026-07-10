#!/usr/bin/env bash

set -Eeuo pipefail

########################################
# CONFIGURATION
########################################

BRANCH="main"
REMOTE="origin"

########################################
# SCRIPT
########################################

cd "$(dirname "$0")"

LOG="autopush.log"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG"
}

log "----------------------------------------"
log "Auto Backup Started"

# Verify this is a git repository
if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    log "ERROR: Not inside a Git repository."
    exit 1
fi

# Verify HEAD is valid
if ! git rev-parse --verify HEAD >/dev/null 2>&1; then
    log "ERROR: Repository HEAD is invalid."
    log "Run: git status"
    exit 1
fi

# Fetch latest references
log "Fetching latest changes..."
git fetch "$REMOTE"

# Stage all changes
git add -A

# Commit only if necessary
if git diff --cached --quiet; then
    log "No local changes."
else
    MSG="Auto Backup $(date '+%Y-%m-%d %H:%M:%S')"

    log "Creating commit..."
    git commit -m "$MSG"
fi

# Pull latest changes safely
log "Rebasing..."
git pull --rebase --autostash "$REMOTE" "$BRANCH"

# Push
log "Pushing..."
git push "$REMOTE" "$BRANCH"

log "Backup completed successfully."