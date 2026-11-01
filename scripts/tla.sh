#!/usr/bin/env bash
set -euo pipefail
repo_root=$(cd "$(dirname "$0")/.." && pwd)
cd "$repo_root"
source "$repo_root/tla/toolchain.env"
command_name=${1:-all}
if [[ $# -gt 1 ]]; then
    echo 'usage: bash scripts/tla.sh [setup|check|mutations|all]' >&2
    exit 2
fi
case "$command_name" in
    setup|check|mutations|all) ;;
    *) echo 'usage: bash scripts/tla.sh [setup|check|mutations|all]' >&2; exit 2 ;;
esac
unavailable() { echo "UNAVAILABLE $*" >&2; exit 3; }
command -v python3 >/dev/null || unavailable 'Python 3가 필요합니다.'
tools_dir=${TLA_TOOLS_DIR:-$repo_root/.tla-tools}
case "$tools_dir" in /*) ;; *) tools_dir="$repo_root/$tools_dir" ;; esac
helper_archive="$tools_dir/formal-verify-$FORMAL_VERIFY_REV.tar.gz"
export TLC_CACHE="$tools_dir/tlc"
jar="$TLC_CACHE/tla2tools-v$TLC_VERSION.jar"
# Always verify the project pin instead of trusting an inherited custom JAR.
unset TLC_JAR
if [[ -z ${JAVA:-} ]]; then
    if [[ -n ${JAVA_HOME:-} ]]; then export JAVA="$JAVA_HOME/bin/java"; else export JAVA=java; fi
fi
export TLC_WORKERS=${TLC_WORKERS:-2}
run_dir=''
download_tmp=''
cleanup() {
    if [[ -n "$run_dir" ]]; then rm -rf "$run_dir"; fi
    if [[ -n "$download_tmp" ]]; then rm -f "$download_tmp"; fi
}
trap cleanup EXIT
verify_file() {
    [[ -f "$1" ]] || unavailable "$1 없음: bash scripts/tla.sh setup을 먼저 실행하세요."
    python3 - "$1" "$2" <<'PY'
import hashlib
from pathlib import Path
import sys
path = Path(sys.argv[1])
actual = hashlib.sha256(path.read_bytes()).hexdigest()
if actual != sys.argv[2]:
    print(f"UNAVAILABLE SHA256 mismatch: {path}; 캐시를 삭제하고 setup을 다시 실행하세요.", file=sys.stderr)
    sys.exit(3)
PY
}
download_file() {
    local url=$1 destination=$2 expected=$3
    if [[ ! -f "$destination" ]]; then
        download_tmp=$(mktemp "$tools_dir/download.XXXXXX")
        curl --fail --location --silent --show-error --retry 3 --connect-timeout 15 --max-time 180 \
            "$url" --output "$download_tmp" || unavailable "도구 다운로드 실패: $url"
        verify_file "$download_tmp" "$expected"
        mv "$download_tmp" "$destination"
        download_tmp=''
    fi
    verify_file "$destination" "$expected"
}
if [[ "$command_name" == setup ]]; then
    command -v curl >/dev/null || unavailable '도구 준비에 curl이 필요합니다.'
    mkdir -p "$TLC_CACHE"
    download_file "https://codeload.github.com/michael-denyer/agent-formal-verify/tar.gz/$FORMAL_VERIFY_REV" \
        "$helper_archive" "$FORMAL_VERIFY_SHA256"
    download_file "https://github.com/tlaplus/tlaplus/releases/download/v$TLC_VERSION/tla2tools.jar" \
        "$jar" "$TLC_SHA256"
else
    verify_file "$helper_archive" "$FORMAL_VERIFY_SHA256"
    verify_file "$jar" "$TLC_SHA256"
fi
# Extract verified bytes for each invocation; cached helper edits cannot affect checks.
run_dir=$(mktemp -d "$tools_dir/run.XXXXXX")
tar -xzf "$helper_archive" --strip-components=1 -C "$run_dir"
skill_dir="$run_dir/skills/formal-verify"
[[ -f "$skill_dir/scripts/tlc-matrix.sh" ]] || unavailable '고정 도구 아카이브의 경로가 잘못됐습니다.'
if [[ "$command_name" == setup ]]; then
    bash "$skill_dir/scripts/setup.sh" tla
    exit 0
fi
shopt -s nullglob
matrices=("$repo_root"/tla/*.matrix)
if [[ ${#matrices[@]} -eq 0 ]]; then echo 'FAIL tla/*.matrix가 없습니다.' >&2; exit 1; fi
run_checks() {
    local matrix
    for matrix in "${matrices[@]}"; do
        bash "$skill_dir/scripts/tlc-matrix.sh" "$matrix"
    done
}
run_mutations() {
    local matrix mutations
    for matrix in "${matrices[@]}"; do
        mutations="${matrix%.matrix}.mutations"
        [[ -f "$mutations" ]] || { echo "FAIL $mutations 없음" >&2; return 1; }
        python3 "$skill_dir/scripts/mutate.py" "$mutations"
    done
}
case "$command_name" in
    check) run_checks ;;
    mutations) run_mutations ;;
    all) run_checks; run_mutations ;;
esac
