#!/usr/bin/env bash
set -euo pipefail

# -----------------------------
# Colored logging functions
# -----------------------------
COLOR_RESET="\e[0m"
COLOR_BLUE="\e[34m"
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

if [[ ! -f "${SEC_FILE}" ]]; then
    log_error "Security file not found: ${SEC_FILE}"
    exit 1
fi

# -----------------------------
# Backup
# -----------------------------
log_step "Creating backup"
cp -p "${SEC_FILE}" "${BACKUP_FILE}"
log_info "Backup created: ${BACKUP_FILE}"

# -----------------------------
# Show current value
# -----------------------------
log_step "Current jdk.tls.disabledAlgorithms line:"
if ! grep '^jdk.tls.disabledAlgorithms=' "${SEC_FILE}"; then
    log_warn "Entry jdk.tls.disabledAlgorithms not found"
fi

# -----------------------------
# Safely update algorithms
# -----------------------------
log_step "Updating jdk.tls.disabledAlgorithms (removing SSLv3 / TLSv1 / TLSv1.1 without corrupting list)"

TMP_FILE="$(mktemp "${SEC_FILE}.XXXXXX")"

awk -F= '
BEGIN {
    OFS="=";
}
# Process only the jdk.tls.disabledAlgorithms line
/^jdk\.tls\.disabledAlgorithms=/ {
    key = $1;
    value = $2;

    # Split by comma into algorithms
    n = split(value, arr, ",");

    out = "";
    for (i = 1; i <= n; i++) {
        alg = arr[i];

        # trim leading/trailing spaces
        gsub(/^[[:space:]]+|[[:space:]]+$/, "", alg);

        # skip unwanted protocols
        if (alg ~ /^(SSLv3|TLSv1\.1?|TLSv1)$/) {
            continue;
        }

        # rebuild with ", " as separator
        if (alg != "") {
            if (out == "") {
                out = alg;
            } else {
                out = out ", " alg;
            }
        }
    }

    print key, out;
    next;
}
# All other lines unchanged
{ print }
' "${SEC_FILE}" > "${TMP_FILE}"

mv "${TMP_FILE}" "${SEC_FILE}"
log_info "Update applied successfully"

# -----------------------------
# Show updated line
# -----------------------------
log_step "Updated jdk.tls.disabledAlgorithms line:"
if ! grep '^jdk.tls.disabledAlgorithms=' "${SEC_FILE}"; then
    log_warn "Entry jdk.tls.disabledAlgorithms not found after update (unexpected)"
fi

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
