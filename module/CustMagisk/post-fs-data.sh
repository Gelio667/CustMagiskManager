#!/system/bin/sh
BASE=/data/adb/CustMagisk
API=$BASE/api
LOG=$BASE/logs
mkdir -p $API
mkdir -p $LOG
chmod 700 $BASE
chmod 700 $API
chmod 700 $LOG
rm -f $API/request.json
rm -f $API/response.json
touch $LOG/module.log
chmod 600 $LOG/module.log