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

log_info "Version 1.0.1"
log_step "Switching to Java security directory: ${SEC_DIR}"
cd "${SEC_DIR}" || { log_error "Failed to cd into ${SEC_DIR}"; exit 1; }

[[ -f "${SEC_FILE}" ]] || { log_error "Security file not found: ${SEC_FILE}"; exit 1; }

# -----------------------------
# Backup
# -----------------------------
log_step "Creating backup"
cp -p "${SEC_FILE}" "${BACKUP_FILE}"
log_info "Backup created: ${BACKUP_FILE}"

# -----------------------------
# Show current value
# -----------------------------
log_step "Current jdk.tls.disabledAlgorithms entries (may be multi-line):"
if ! grep -nE '^jdk\.tls\.disabledAlgorithms=' "${SEC_FILE}"; then
log_warn "Entry not found"
fi

# -----------------------------
# Update (handles backslash continuations + removes duplicates)
# -----------------------------
log_step "Updating jdk.tls.disabledAlgorithms (remove SSLv3/TLSv1/TLSv1.1; keep others; supports \\ continuations; de-duplicates key)"

TMP_FILE="$(mktemp "${SEC_FILE}.XXXXXX")"

# Notes:
# - We collect one (the last) jdk.tls.disabledAlgorithms value, even if the file
#   contains duplicates.
# - We support multi-line values using backslash continuations.
# - We then write the file back with exactly one normalized property line.
awk '
function rtrim_bs(s) { sub(/[[:space:]]*\\[[:space:]]*$/, "", s); return s }
function trim(s)     { gsub(/^[[:space:]]+|[[:space:]]+$/, "", s); return s }

BEGIN {
# collection state
collecting = 0
key = ""
val = ""
have_final = 0
final_val = ""
}

# Start of (possibly multi-line) jdk.tls.disabledAlgorithms
/^jdk\\.tls\\.disabledAlgorithms=/ {
collecting = 1

key = "jdk.tls.disabledAlgorithms"
# everything after first '='
val = substr($0, index($0, "=") + 1)

# strip trailing backslash if continued
if ($0 ~ /\\[[:space:]]*$/) {
	val = rtrim_bs(val)
	next
}

# single-line value ends here
collecting = 0
final_val = val
have_final = 1
next
}

# Continuation lines: only meaningful right after the property start
/^[[:space:]]+/ {
if (collecting == 1) {
	line = trim($0)
	if ($0 ~ /\\[[:space:]]*$/) {
	line = rtrim_bs(line)
	val = val " " line
	next
	}
	val = val " " line
	collecting = 0
	final_val = val
	have_final = 1
	next
}
}

# While collecting, do not print the continuation lines (we rebuild later)
{
# Skip ALL occurrences of the property and its continuation lines.
# Property start is handled above with `next`.
if (collecting == 1) {
	next
}

# Also skip any additional duplicates (if present) that appear later
if ($0 ~ /^jdk\\.tls\\.disabledAlgorithms=/) {
	next
}

print
}

END {
if (key == "") {
	# If key never found, do nothing special.
	exit
}
}
' "${SEC_FILE}" > "${TMP_FILE}.body"

# If the property exists, normalize its value and append a single definitive line
# right where the first occurrence used to be is more complex; to keep this safe
# and simple, we append it near the original section by inserting it before the
# "Legacy algorithms" header if present; otherwise append at end.

# Extract the last observed value by re-scanning the original file (handles duplicates)
LAST_VAL="$(
awk '
function rtrim_bs(s) { sub(/[[:space:]]*\\[[:space:]]*$/, "", s); return s }
function trim(s)     { gsub(/^[[:space:]]+|[[:space:]]+$/, "", s); return s }
BEGIN{collecting=0; val=""; last=""}
/^jdk\\.tls\\.disabledAlgorithms=/ {
	collecting=1
	val = substr($0, index($0,"=")+1)
	if ($0 ~ /\\[[:space:]]*$/) { val=rtrim_bs(val); next }
	last=val; collecting=0; next
}
/^[[:space:]]+/ {
	if (collecting==1) {
	line=trim($0)
	if ($0 ~ /\\[[:space:]]*$/) { line=rtrim_bs(line); val=val " " line; next }
	val=val " " line
	last=val
	collecting=0
	next
	}
}
END{ print last }
' "${SEC_FILE}"
)"

if [[ -z "${LAST_VAL}" ]]; then
log_warn "jdk.tls.disabledAlgorithms not found; leaving file unchanged"
rm -f "${TMP_FILE}.body" "${TMP_FILE}"
exit 0
fi

# Normalize list: split by comma, trim, remove SSLv3/TLSv1/TLSv1.1, keep rest, join with ", "
NORMALIZED_VAL="$(
awk -v v="${LAST_VAL}" '
function trim(s){ gsub(/^[[:space:]]+|[[:space:]]+$/, "", s); return s }
BEGIN {
	n=split(v, arr, ",")
	out=""
	for(i=1;i<=n;i++){
	alg=trim(arr[i])
	if (alg=="SSLv3" || alg=="TLSv1" || alg=="TLSv1.1") continue
	if (alg=="") continue
	out = (out=="" ? alg : out ", " alg)
	}
	print out
}
'
)"

if [[ -z "${NORMALIZED_VAL}" ]]; then
log_error "Normalization produced empty value; refusing to write. Restoring backup."
cp -p "${BACKUP_FILE}" "${SEC_FILE}"
exit 1
fi

# Insert the normalized line before the Legacy header if possible
if grep -q "^# Legacy algorithms for Secure Socket Layer/Transport Layer Security" "${TMP_FILE}.body"; then
awk -v line="jdk.tls.disabledAlgorithms=${NORMALIZED_VAL}" '
	BEGIN{done=0}
	/^# Legacy algorithms for Secure Socket Layer\/Transport Layer Security/ {
	if(!done){ print line; print ""; done=1 }
	print
	next
	}
	{ print }
	END{ if(!done){ print ""; print line } }
' "${TMP_FILE}.body" > "${TMP_FILE}"
else
# fallback: append at end
cat "${TMP_FILE}.body" > "${TMP_FILE}"
printf "\n%s\n" "jdk.tls.disabledAlgorithms=${NORMALIZED_VAL}" >> "${TMP_FILE}"
fi

rm -f "${TMP_FILE}.body"

# Atomically replace
mv "${TMP_FILE}" "${SEC_FILE}"
log_info "Update applied successfully"

# -----------------------------
# Show updated line
# -----------------------------
log_step "Updated jdk.tls.disabledAlgorithms entry:"
grep -nE '^jdk\.tls\.disabledAlgorithms=' "${SEC_FILE}" || log_warn "Entry not found after update (unexpected)"

# Ensure it is unique
COUNT="$(grep -cE '^jdk\.tls\.disabledAlgorithms=' "${SEC_FILE}" || true)"
if [[ "${COUNT}" != "1" ]]; then
log_warn "Expected exactly 1 jdk.tls.disabledAlgorithms entry, found ${COUNT}";
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

# -----------------------------
# Sanity check
# -----------------------------
log_step "Quick sanity check: java -version"
if java -version >/dev/null 2>&1; then
log_info "JVM loads java.security OK"
else
log_error "JVM still fails to load java.security. Restoring backup."
cp -p "${BACKUP_FILE}" "${SEC_FILE}"
exit 1
fi

log_step "Done."
