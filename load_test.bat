@echo off
echo === Connection Pool Load Test ===
echo Testing concurrent connections...

set TOKEN=eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiU1VQRVJfQURNSU4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc1OTA2NjYyOSwiZXhwIjoxNzU5MTUzMDI5fQ.sU4eeNVTja8QLpIoZtO8zbeE-pwu35YX9pSC-vSAacw

rem Start 5 concurrent requests
start /b curl -w "Request 1 Time: %%{time_total}s\n" -X GET "http://localhost:8080/api/transactions" -H "Authorization: Bearer %TOKEN%"
start /b curl -w "Request 2 Time: %%{time_total}s\n" -X GET "http://localhost:8080/api/funds" -H "Authorization: Bearer %TOKEN%"
start /b curl -w "Request 3 Time: %%{time_total}s\n" -X GET "http://localhost:8080/api/transactions" -H "Authorization: Bearer %TOKEN%"
start /b curl -w "Request 4 Time: %%{time_total}s\n" -X GET "http://localhost:8080/api/funds" -H "Authorization: Bearer %TOKEN%"
start /b curl -w "Request 5 Time: %%{time_total}s\n" -X GET "http://localhost:8080/api/transactions" -H "Authorization: Bearer %TOKEN%"

timeout /t 3 > nul
echo All concurrent requests completed
echo ================================