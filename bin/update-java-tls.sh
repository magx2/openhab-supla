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

log_info "Version 1.1.1"

# --- detect real java.security used by `java` ---
JAVA_BIN="$(command -v java || true)"
[[ -n "${JAVA_BIN}" ]] || { log_error "java not found in PATH"; exit 1; }

JAVA_HOME_DETECTED="$(java -XshowSettings:properties -version 2>&1 | awk -F' = ' '/^[[:space:]]*java\.home =/ {print $2; exit}')"
[[ -n "${JAVA_HOME_DETECTED}" ]] || { log_error "Could not detect java.home"; exit 1; }

if [[ -f "${JAVA_HOME_DETECTED}/conf/security/java.security" ]]; then
SEC_FILE="${JAVA_HOME_DETECTED}/conf/security/java.security"
elif [[ -f "${JAVA_HOME_DETECTED}/lib/security/java.security" ]]; then
SEC_FILE="${JAVA_HOME_DETECTED}/lib/security/java.security"
else
log_error "java.security not found under ${JAVA_HOME_DETECTED}"
exit 1
fi

SEC_DIR="$(dirname "${SEC_FILE}")"
BACKUP_FILE="${SEC_FILE}.bak"

log_step "Using java: ${JAVA_BIN}"
log_step "java.home: ${JAVA_HOME_DETECTED}"
log_step "Security file: ${SEC_FILE}"

cd "${SEC_DIR}" || { log_error "Failed to cd into ${SEC_DIR}"; exit 1; }
[[ -f "${SEC_FILE}" ]] || { log_error "Security file not found: ${SEC_FILE}"; exit 1; }

log_step "Creating backup"
cp -p "${SEC_FILE}" "${BACKUP_FILE}"
log_info "Backup created: ${BACKUP_FILE}"

log_step "Current jdk.tls.disabledAlgorithms entries (may be multi-line):"
grep -nE '^jdk\.tls\.disabledAlgorithms=' "${SEC_FILE}" || log_warn "Entry not found"

log_step "Updating jdk.tls.disabledAlgorithms (remove SSLv3/TLSv1/TLSv1.1; supports \\ continuations; de-duplicates key)"

TMP_FILE="$(mktemp "${SEC_FILE}.XXXXXX")"

awk '
function rtrim_bs(s) { sub(/[[:space:]]*\\[[:space:]]*$/, "", s); return s }
function trim(s)     { gsub(/^[[:space:]]+|[[:space:]]+$/, "", s); return s }

BEGIN { n=0; collecting=0; first_start=0; last=""; val="" }

{
lines[++n] = $0
skip[n] = 0

# IMPORTANT: correct regex (single \.)
if (collecting==0 && $0 ~ /^jdk\.tls\.disabledAlgorithms=/) {
	if (first_start==0) first_start=n
	skip[n]=1

	val = substr($0, index($0, "=") + 1)

	if ($0 ~ /\\[[:space:]]*$/) {
	val = rtrim_bs(val)
	collecting=1
	next
	} else {
	last=val
	next
	}
}

if (collecting==1) {
	if ($0 ~ /^[[:space:]]+/) {
	skip[n]=1
	line = trim($0)

	if ($0 ~ /\\[[:space:]]*$/) {
		line = rtrim_bs(line)
		val = val " " line
		next
	} else {
		val = val " " line
		collecting=0
		last=val
		next
	}
	} else {
	collecting=0
	last=val
	skip[n]=0
	}
}
}

END {
if (first_start==0 || last=="") {
	for (i=1;i<=n;i++) print lines[i]
	exit 0
}

nn = split(last, arr, ",")
out=""
for (i=1;i<=nn;i++) {
	alg = trim(arr[i])
	if (alg=="SSLv3" || alg=="TLSv1" || alg=="TLSv1.1") continue
	if (alg=="") continue
	out = (out=="" ? alg : out ", " alg)
}

if (out=="") {
	for (i=1;i<=n;i++) print lines[i]
	exit 3
}

for (i=1;i<=n;i++) {
	if (i==first_start) print "jdk.tls.disabledAlgorithms=" out
	if (skip[i]==1) continue
	if (i==first_start) continue
	print lines[i]
}
}
' "${SEC_FILE}" > "${TMP_FILE}"

mv "${TMP_FILE}" "${SEC_FILE}"
# Ensure readable for non-root JVM users (openhab runs as 'openhab')
chmod 0644 "${SEC_FILE}" || true
chmod 0644 "${BACKUP_FILE}" || true
log_info "Update applied successfully"

log_step "Updated jdk.tls.disabledAlgorithms entry:"
grep -nE '^jdk\.tls\.disabledAlgorithms=' "${SEC_FILE}" || log_warn "Entry not found after update (unexpected)"

COUNT="$(grep -cE '^jdk\.tls\.disabledAlgorithms=' "${SEC_FILE}" || true)"
[[ "${COUNT}" == "1" ]] || log_warn "Expected exactly 1 jdk.tls.disabledAlgorithms entry, found ${COUNT}"

log_step "Comparing changes with backup:"
if diff -u "${BACKUP_FILE}" "${SEC_FILE}"; then
log_warn "No changes detected (files identical?)"
else
log_info "Differences shown above"
fi

log_step "Quick sanity check: java -version"
if java -version >/dev/null 2>&1; then
log_info "JVM loads java.security OK"
else
log_error "JVM still fails to load java.security. Restoring backup."
cp -p "${BACKUP_FILE}" "${SEC_FILE}"
exit 1
fi

log_step "Done."
