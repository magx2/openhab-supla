#!/usr/bin/env bash
set -euo pipefail

COLOR_RESET="\e[0m"
COLOR_CYAN="\e[36m"
COLOR_GREEN="\e[32m"
COLOR_YELLOW="\e[33m"
COLOR_RED="\e[31m"

log_step()  { echo -e "${COLOR_CYAN}[STEP]${COLOR_RESET} $1"; }
log_info()  { echo -e "${COLOR_GREEN}[INFO]${COLOR_RESET} $1"; }
log_warn()  { echo -e "${COLOR_YELLOW}[WARN]${COLOR_RESET} $1"; }
log_error() { echo -e "${COLOR_RED}[ERROR]${COLOR_RESET} $1" >&2; }

SEC_DIR="/usr/lib/jvm/java-1.21.0-openjdk-amd64/conf/security"
SEC_FILE="${SEC_DIR}/java.security"
BACKUP_FILE="${SEC_FILE}.bak"

log_info "Version 1.0.0"
log_step "Switching to Java security directory: ${SEC_DIR}"
cd "${SEC_DIR}" || { log_error "Failed to cd into ${SEC_DIR}"; exit 1; }

[[ -f "${SEC_FILE}" ]] || { log_error "Security file not found: ${SEC_FILE}"; exit 1; }

log_step "Creating backup"
cp -p "${SEC_FILE}" "${BACKUP_FILE}"
log_info "Backup created: ${BACKUP_FILE}"

log_step "Current jdk.tls.disabledAlgorithms (may be multi-line):"
grep -nE '^jdk\.tls\.disabledAlgorithms=' -n "${SEC_FILE}" || log_warn "Entry not found"

log_step "Updating jdk.tls.disabledAlgorithms (supports backslash continuations)"

TMP_FILE="$(mktemp "${SEC_FILE}.XXXXXX")"

awk '
function rtrim_bs(s) { sub(/[[:space:]]*\\[[:space:]]*$/, "", s); return s }
function trim(s)     { gsub(/^[[:space:]]+|[[:space:]]+$/, "", s); return s }

BEGIN { mode=0; key=""; val="" }

# Start collecting the property (first line)
/^jdk\.tls\.disabledAlgorithms=/ {
mode=1
split($0, parts, "=")
key=parts[1]
val = substr($0, index($0, "=")+1)

# If ends with "\" then keep collecting continuation lines
if ($0 ~ /\\[[:space:]]*$/) {
	val = rtrim_bs(val)
	next
} else {
	mode=2
}
}

# Continuation lines (only while collecting)
/^[[:space:]]+/ {
if (mode==1) {
	line=$0
	line=trim(line)

	if ($0 ~ /\\[[:space:]]*$/) {
	line=rtrim_bs(line)
	val = val " " line
	next
	} else {
	val = val " " line
	mode=2
	}
}
}

{
# Once collected full value, rewrite the property exactly once
if (mode==2) {
	n = split(val, arr, ",")
	out=""
	for (i=1; i<=n; i++) {
	alg = trim(arr[i])

	# remove exactly these tokens
	if (alg == "SSLv3" || alg == "TLSv1" || alg == "TLSv1.1") continue

	if (alg != "") out = (out=="" ? alg : out ", " alg)
	}

	print key "=" out
	mode=3
}

# Skip original property line and its continuation lines (we already printed the rewritten one)
if (mode==3) {
	if ($0 ~ /^jdk\.tls\.disabledAlgorithms=/) next
	if ($0 ~ /^[[:space:]]+/) next
	mode=0
}

print
}
' "${SEC_FILE}" > "${TMP_FILE}"

mv "${TMP_FILE}" "${SEC_FILE}"
log_info "Update applied successfully"

log_step "Updated jdk.tls.disabledAlgorithms line:"
grep -nE '^jdk\.tls\.disabledAlgorithms=' "${SEC_FILE}" || log_warn "Entry not found after update"

log_step "Comparing changes with backup:"
if diff -u "${BACKUP_FILE}" "${SEC_FILE}"; then
log_warn "No changes detected (files identical?)"
else
log_info "Differences shown above"
fi

log_step "Quick sanity check: java -version"
java -version >/dev/null 2>&1 && log_info "JVM loads java.security OK" || {
log_error "JVM still fails to load java.security. Restoring backup."
cp -p "${BACKUP_FILE}" "${SEC_FILE}"
exit 1
}

log_step "Done."
