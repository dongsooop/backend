#!/usr/bin/env bash
set -euo pipefail
repo_root=$(git rev-parse --show-toplevel)
cd "$repo_root"
if ! bash ./gradlew --console=plain spotlessCheck; then
    echo 'Java 포맷 검사 실패: ./gradlew spotlessApply 실행 후 변경 사항을 확인하세요.' >&2
    exit 1
fi
