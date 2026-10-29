#!/usr/bin/env bash
set -euo pipefail
repo_root=$(git rev-parse --show-toplevel)
cd "$repo_root"
existing=$(git config --get core.hooksPath || true)
if [[ -n "$existing" && "$existing" != '.githooks' ]]; then
    echo "기존 core.hooksPath($existing)를 유지했습니다. 기존 훅에 scripts/check-format.sh를 연결하세요." >&2
    exit 1
fi
if [[ -z "$existing" ]]; then
    hooks_dir=$(git rev-parse --git-path hooks)
    for hook in pre-commit pre-push; do
        if [[ -x "$hooks_dir/$hook" ]]; then
            echo "기존 $hook 훅을 유지했습니다. 기존 훅에 scripts/check-format.sh를 연결하세요." >&2
            exit 1
        fi
    done
fi
git config --local core.hooksPath .githooks
echo 'pre-commit / pre-push 포맷 훅을 설치했습니다.'
