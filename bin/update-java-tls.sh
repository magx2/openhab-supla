#!/usr/bin/env bash
set -euo pipefail

# -----------------------------
# Colored logging functions
# -----------------------------
COLOR_RESET="\e[0m"
COLOR_CYAN="\e[36m"
COLOR_GREEN="\e[32m"
COLOR_YELLOW="\e[33m"
COLOR_RED="\e[31m"

log_step()  { echo -e "${COLOR_CYAN}[STEP]${COLOR_RESET} $1"; }
log_info()  { echo -e "${COLOR_GREEN}[INFO]${COLOR_RESET} $1"; }
log_warn()  { echo -e "${COLOR_YELLOW}[WARN]${COLOR_RESET} $1"; }
log_error() { echo -e "${COLOR_RED}[ERROR]${COLOR_RESET} $1" >&2; }

# -----------------------------
# Configuration
# -----------------------------
SEC_DIR="/usr/lib/jvm/java-1.21.0-openjdk-amd64/conf/security"
SEC_FILE="${SEC_DIR}/java.security"
BACKUP_FILE="${SEC_FILE}.bak"

log_step "Switching to Java security directory: ${SEC_DIR}"
cd "${SEC_DIR}" || { log_error "Failed to cd into ${SEC_DIR}"; exit 1; }

# -----------------------------
# Backup
# -----------------------------
if [[ ! -f "${SEC_FILE}" ]]; then
    log_error "Security file not found: ${SEC_FILE}"
    exit 1
fi

log_step "Creating backup"
cp -p "${SEC_FILE}" "${BACKUP_FILE}"
log_info "Backup created: ${BACKUP_FILE}"

# -----------------------------
# Read current value
# -----------------------------
log_step "Current jdk.tls.disabledAlgorithms:"
grep '^jdk.tls.disabledAlgorithms=' "${SEC_FILE}" || log_warn "Entry not found"

# -----------------------------
# Apply update
# -----------------------------
log_step "Updating jdk.tls.disabledAlgorithms (removing SSLv3 / TLSv1 / TLSv1.1)"

sed -i -E '/^jdk\.tls\.disabledAlgorithms=/ s/(,\s*)?(SSLv3|TLSv1\.1?|TLSv1)(,\s*)?//g' "${SEC_FILE}"

log_info "Update applied successfully"

# -----------------------------
# Show updated line
# -----------------------------
log_step "Updated jdk.tls.disabledAlgorithms:"
grep '^jdk.tls.disabledAlgorithms=' "${SEC_FILE}" || log_warn "Entry not found"

# -----------------------------
# Diff vs backup
# -----------------------------
log_step "Comparing changes with backup:"
if diff -u "${BACKUP_FILE}" "${SEC_FILE}"; then
    log_warn "No changes detected (files identical?)"
else
    log_info "Differences shown above"
fi

log_step "Done."
