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

log_info "Version 1.0.2"
log_step "Switching to Java security directory: ${SEC_DIR}"
cd "${SEC_DIR}" || { log_error "Failed to cd into ${SEC_DIR}"; exit 1; }

[[ -f "${SEC_FILE}" ]] || { log_error "Security file not found: ${SEC_FILE}"; exit 1; }

log_step "Creating backup"
cp -p "${SEC_FILE}" "${BACKUP_FILE}"
log_info "Backup created: ${BACKUP_FILE}"

log_step "Current jdk.tls.disabledAlgorithms entries (may be multi-line):"
grep -nE '^jdk\.tls\.disabledAlgorithms=' "${SEC_FILE}" || log_warn "Entry not found"

log_step "Updating jdk.tls.disabledAlgorithms (supports \\ continuations; removes duplicates)"

TMP_FILE="$(mktemp "${SEC_FILE}.XXXXXX")"

awk '
function rtrim_bs(s) { sub(/[[:space:]]*\\[[:space:]]*$/, "", s); return s }
function trim(s)     { gsub(/^[[:space:]]+|[[:space:]]+$/, "", s); return s }

BEGIN {
n=0
collecting=0
first_start=0
last=""
val=""
}

{
lines[++n] = $0
skip[n] = 0

# start of property
if (collecting==0 && $0 ~ /^jdk\.tls\.disabledAlgorithms=/) {
	if (first_start==0) first_start=n
	skip[n]=1

	val = substr($0, index($0, "=")+1)

	if ($0 ~ /\\[[:space:]]*$/) {
	val = rtrim_bs(val)
	collecting=1
	next
	} else {
	collecting=0
	last=val
	next
	}
}

# continuation lines
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
	# unexpected: stop collecting, do not skip this line
	collecting=0
	last=val
	skip[n]=0
	}
}
}

END {
if (first_start==0 || last=="") {
	# not found -> print original unchanged
	for (i=1;i<=n;i++) print lines[i]
	exit 0
}

# normalize last value
nn = split(last, arr, ",")
out=""
for (i=1;i<=nn;i++) {
	alg = trim(arr[i])
	if (alg=="SSLv3" || alg=="TLSv1" || alg=="TLSv1.1") continue
	if (alg=="") continue
	out = (out=="" ? alg : out ", " alg)
}

if (out=="") {
	# refuse to write empty
	for (i=1;i<=n;i++) print lines[i]
	exit 3
}

# write file with exactly one property (at first occurrence position)
for (i=1;i<=n;i++) {
	if (i==first_start) print "jdk.tls.disabledAlgorithms=" out
	if (skip[i]==1) continue
	if (i==first_start) continue  # avoid printing original line at that position
	print lines[i]
}
}
' "${SEC_FILE}" > "${TMP_FILE}"

mv "${TMP_FILE}" "${SEC_FILE}"
log_info "Update applied successfully"

log_step "Updated jdk.tls.disabledAlgorithms entries:"
grep -nE '^jdk\.tls\.disabledAlgorithms=' "${SEC_FILE}" || log_warn "Entry not found after update (unexpected)"

COUNT="$(grep -cE '^jdk\.tls\.disabledAlgorithms=' "${SEC_FILE}" || true)"
if [[ "${COUNT}" != "1" ]]; then
log_warn "Expected exactly 1 jdk.tls.disabledAlgorithms entry, found ${COUNT}"
fi

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
