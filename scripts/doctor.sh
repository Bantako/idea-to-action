#!/usr/bin/env bash
set -euo pipefail

red()   { printf '\033[31m%s\033[0m\n' "$*"; }
green() { printf '\033[32m%s\033[0m\n' "$*"; }
yellow(){ printf '\033[33m%s\033[0m\n' "$*"; }

check_cmd() {
  local cmd="$1"
  if command -v "$cmd" >/dev/null 2>&1; then
    green "OK   command: $cmd -> $(command -v "$cmd")"
  else
    red "FAIL command not found: $cmd"
    return 1
  fi
}

check_path() {
  local name="$1"
  local value="${2:-}"
  if [ -n "$value" ] && [ -e "$value" ]; then
    green "OK   env: $name=$value"
  else
    red "FAIL env missing or invalid: $name=${value:-<empty>}"
    return 1
  fi
}

main() {
  echo "== Android dev doctor =="

  check_cmd java
  check_cmd gradle
  check_cmd adb
  check_cmd sdkmanager

  check_path ANDROID_HOME "${ANDROID_HOME:-}"
  check_path ANDROID_SDK_ROOT "${ANDROID_SDK_ROOT:-}"
  check_path JAVA_HOME "${JAVA_HOME:-}"

  echo
  yellow "-- versions --"
  java -version || true
  echo
  gradle -v | sed -n '1,6p' || true
  echo
  adb version || true
  echo
  sdkmanager --list | head -n 20 || true

  echo
  if [ -d "${ANDROID_SDK_ROOT:-}/platform-tools" ]; then
    green "OK   platform-tools present"
  else
    red "FAIL platform-tools directory not found"
    exit 1
  fi

  if [ -d "${ANDROID_SDK_ROOT:-}/build-tools" ]; then
    green "OK   build-tools present"
  else
    red "FAIL build-tools directory not found"
    exit 1
  fi

  green "doctor passed"
}

main "$@"
