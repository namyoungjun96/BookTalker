#!/bin/bash

APP_NAME="book_talker_backend"
JAVA_CMD="java"
JAVA_OPTS="-Xms128m -Xmx384m -XX:TieredStopAtLevel=1"
PROFILE="prod"

get_pid() {
  local port=$1
  ps -ef | grep java | grep "${APP_NAME}" | grep "server.port=${port}" | grep -v grep | awk '{print $2}'
}

start() {
  local port=$1
  local jar=$2
  local PID=$(get_pid ${port})

  if [ -n "$PID" ]; then
    echo "❌ ${APP_NAME} already running on port ${port} (PID: $PID)"
    return 1
  fi

  echo "▶ Starting ${APP_NAME} on port ${port} with ${jar}..."

  if [ ! -f /home/ubuntu/.env ]; then
    echo "❌ .env not found"
    return 1
  fi
  . /home/ubuntu/.env

  nohup ${JAVA_CMD} ${JAVA_OPTS} -jar ${jar} \
    --spring.profiles.active=${PROFILE} \
    --server.port=${port} \
    > /home/ubuntu/apps/backend/app-${port}.log 2>&1 &

  sleep 2

  PID=$(get_pid ${port})
  if [ -n "$PID" ]; then
    echo "✅ Started successfully on port ${port} (PID: $PID)"
  else
    echo "❌ Failed to start on port ${port}"
  fi
}

stop() {
  local port=$1
  local PID=$(get_pid ${port})

  if [ -z "$PID" ]; then
    echo "ℹ️ ${APP_NAME} is not running on port ${port}"
    return 0
  fi

  echo "🛑 Stopping ${APP_NAME} on port ${port} (PID: $PID)"
  kill "$PID"

  sleep 3

  PID=$(get_pid ${port})
  if [ -n "$PID" ]; then
    echo "⚠️ Graceful stop failed, force killing..."
    kill -9 "$PID"
  fi

  echo "✅ Stopped port ${port}"
}

status() {
  local port=$1
  local PID=$(get_pid ${port})
  if [ -n "$PID" ]; then
    echo "✅ ${APP_NAME} running on port ${port} (PID: $PID)"
  else
    echo "❌ ${APP_NAME} not running on port ${port}"
  fi
}

case "$1" in
  start)
    start $2 $3
    ;;
  stop)
    stop $2
    ;;
  status)
    status $2
    ;;
  *)
    echo "Usage: $0 {start|stop|status} {port} [jar_path]"
    exit 1
    ;;
esac
