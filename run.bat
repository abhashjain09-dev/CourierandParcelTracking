@echo off
echo Starting Courier and Parcel Tracking System...
java -cp "CourierTrackingSystem.jar;lib\sqlite-jdbc.jar;lib\slf4j-api.jar;lib\slf4j-nop.jar" com.courier.Main
pause
