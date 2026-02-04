#!/system/bin/sh
BASE=/data/adb/CustMagisk
API=$BASE/api
LOG=$BASE/logs/module.log
STATE=$BASE/state.json
REQ=$API/request.json
RES=$API/response.json
BOOTTS=$API/boot_ts

if [ ! -f "$BOOTTS" ]; then
  date +%s > "$BOOTTS"
fi

log() {
  printf "%s|%s\n" "$(date +%s)" "$1" >> "$LOG"
}

js_escape() {
  echo "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

write_state() {
  uid="$(id -u 2>/dev/null)"
  sel="$(getenforce 2>/dev/null)"
  su_path="$(command -v su 2>/dev/null)"
  magisk_path="$(command -v magisk 2>/dev/null)"
  slot="$(getprop ro.boot.slot_suffix 2>/dev/null)"
  bootid="$(cat /proc/sys/kernel/random/boot_id 2>/dev/null)"
  ver="1.0"
  now="$(date +%s)"
  boott="$(cat "$BOOTTS" 2>/dev/null)"

  printf "{\n" > "$STATE"
  printf "\"api\":\"%s\",\n" "$(js_escape "$ver")" >> "$STATE"
  printf "\"uid\":\"%s\",\n" "$(js_escape "$uid")" >> "$STATE"
  printf "\"selinux\":\"%s\",\n" "$(js_escape "$sel")" >> "$STATE"
  printf "\"su\":\"%s\",\n" "$(js_escape "$su_path")" >> "$STATE"
  printf "\"magisk\":\"%s\",\n" "$(js_escape "$magisk_path")" >> "$STATE"
  printf "\"slot\":\"%s\",\n" "$(js_escape "$slot")" >> "$STATE"
  printf "\"boot_id\":\"%s\",\n" "$(js_escape "$bootid")" >> "$STATE"
  printf "\"boot_ts\":\"%s\",\n" "$(js_escape "$boott")" >> "$STATE"
  printf "\"now_ts\":\"%s\"\n" "$(js_escape "$now")" >> "$STATE"
  printf "}\n" >> "$STATE"
  chmod 600 "$STATE"
}

extract_field() {
  echo "$1" | tr -d '\n' | sed "s/.*\"$2\"[ ]*:[ ]*\"\([^\"]*\)\".*/\1/"
}

respond_ok() {
  rid="$1"
  msg="$2"
  now="$(date +%s)"
  printf "{\n\"rid\":\"%s\",\n\"ok\":true,\n\"ts\":\"%s\",\n\"msg\":\"%s\"\n}\n" \
    "$(js_escape "$rid")" "$now" "$(js_escape "$msg")" > "$RES"
  chmod 600 "$RES"
}

respond_err() {
  rid="$1"
  msg="$2"
  now="$(date +%s)"
  printf "{\n\"rid\":\"%s\",\n\"ok\":false,\n\"ts\":\"%s\",\n\"msg\":\"%s\"\n}\n" \
    "$(js_escape "$rid")" "$now" "$(js_escape "$msg")" > "$RES"
  chmod 600 "$RES"
}

handle_req() {
  raw="$(cat "$REQ" 2>/dev/null)"
  rid="$(extract_field "$raw" "rid")"
  act="$(extract_field "$raw" "action")"
  arg="$(extract_field "$raw" "arg")"

  rm -f "$REQ"

  if [ -z "$rid" ]; then
    rid="no_rid"
  fi

  if [ "$act" = "ping" ]; then
    log "REQ ping rid=$rid"
    respond_ok "$rid" "pong"
    return
  fi

  if [ "$act" = "collect_basic" ]; then
    OUT="$BASE/diag/basic_$(date +%s).txt"
    mkdir -p "$BASE/diag"
    {
      echo "ts=$(date +%s)"
      echo "id=$(id 2>/dev/null)"
      echo "getenforce=$(getenforce 2>/dev/null)"
      echo "slot=$(getprop ro.boot.slot_suffix 2>/dev/null)"
      echo "ro.build.version.release=$(getprop ro.build.version.release 2>/dev/null)"
      echo "ro.build.version.sdk=$(getprop ro.build.version.sdk 2>/dev/null)"
      echo "ro.product.device=$(getprop ro.product.device 2>/dev/null)"
      echo "ro.product.model=$(getprop ro.product.model 2>/dev/null)"
      echo "ro.hardware=$(getprop ro.hardware 2>/dev/null)"
      echo "uname=$(uname -a 2>/dev/null)"
      echo "mount=$(mount 2>/dev/null | head -n 80)"
    } > "$OUT"
    chmod 600 "$OUT"
    log "REQ collect_basic rid=$rid -> $OUT"
    respond_ok "$rid" "$OUT"
    return
  fi

  if [ "$act" = "collect_logs" ]; then
    OUT="$BASE/diag/logs_$(date +%s).txt"
    mkdir -p "$BASE/diag"
    {
      echo "ts=$(date +%s)"
      logcat -d 2>/dev/null | tail -n 400
      echo "-----"
      dmesg 2>/dev/null | tail -n 200
    } > "$OUT"
    chmod 600 "$OUT"
    log "REQ collect_logs rid=$rid -> $OUT"
    respond_ok "$rid" "$OUT"
    return
  fi

  if [ "$act" = "reboot" ]; then
    log "REQ reboot rid=$rid"
    respond_ok "$rid" "reboot_requested"
    reboot
    return
  fi

  respond_err "$rid" "unknown_action"
  log "REQ unknown_action rid=$rid act=$act"
}

log "SERVICE start"
while true; do
  write_state
  if [ -f "$REQ" ]; then
    handle_req
  fi
  sleep 2
done