#!/usr/bin/env bash
set -euo pipefail

# Usage: ./debug-jar.sh path/to/app.jar [--port PORT] [--suspend y|n] [--] [extra java args...]

JAR="$1"; shift

if [[ ! -f "$JAR" ]]; then
  echo "Jar not found: $JAR"
  echo "Usage: $0 path/to/app.jar [--port PORT] [--suspend y|n] [--] [extra java args...]"
  exit 1
fi

# Defaults
PORT=5005
SUSPEND="n"

# Parse optional flags
while [[ $# -gt 0 ]]; do
  case "$1" in
    --port)
      PORT="$2"; shift 2;;
    --port=*)
      PORT="${1#*=}"; shift;;
    --suspend)
      SUSPEND="$2"; shift 2;;
    --suspend=*)
      SUSPEND="${1#*=}"; shift;;
    --)
      shift; break;;
    *)
      break;;
  esac
done

EXTRA_ARGS=("$@")

JDWP_OPT="transport=dt_socket,server=y,suspend=${SUSPEND},address=${PORT}"
JAVA_OPTS="${JAVA_OPTS:--Xmx1G}"

echo "Running JAR: $JAR"
echo "JDWP: $JDWP_OPT"
echo "Additional Java args: ${EXTRA_ARGS[*]:-<none>}"

exec java $JAVA_OPTS -agentlib:jdwp=$JDWP_OPT -jar "$JAR" "${EXTRA_ARGS[@]}"
