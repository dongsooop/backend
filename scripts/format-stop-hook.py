#!/usr/bin/env python3
"""Give Codex/Claude one repair turn for pending Java formatting changes."""
import json
from pathlib import Path
import subprocess
import sys


def main():
    event = json.load(sys.stdin)
    if event.get("stop_hook_active"):
        print("{}")
        return
    root = Path(__file__).resolve().parent.parent
    result = subprocess.run(
        ["git", "status", "--porcelain", "-z", "--untracked-files=all", "--",
         "src/**/*.java", "build.gradle", "settings.gradle", "gradle.properties",
         "gradlew", "gradle/wrapper"],
        cwd=root, capture_output=True, check=True,
    )
    if not result.stdout:
        print("{}")
        return
    result = subprocess.run(
        ["bash", "scripts/check-format.sh"], cwd=root,
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
    )
    if result.returncode:
        sys.stderr.write(result.stdout[-8000:])
        print(json.dumps({
            "decision": "block",
            "reason": "Java 포맷 검사가 실패했습니다. ./gradlew spotlessApply 후 "
                      "변경 사항을 확인하고 ./gradlew spotlessCheck를 실행하세요. "
                      "JDK/네트워크 등 환경 오류라면 원인을 보고하세요.",
        }, ensure_ascii=False))
    else:
        print("{}")


if __name__ == "__main__":
    main()
