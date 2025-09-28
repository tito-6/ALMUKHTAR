# PostgreSQL Performance Test Script
$token = "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiU1VQRVJfQURNSU4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc1OTA2NjI3MiwiZXhwIjoxNzU5MTUyNjcyfQ.bMX3QoY_fw_CmnT2ueeaTg-HtvUYWa9csBaHgPpocJA"
$baseUrl = "http://localhost:8080"

Write-Host "=== PostgreSQL Performance Test ===" -ForegroundColor Green
Write-Host "Testing transaction endpoint performance..." -ForegroundColor Yellow

# Test 1: Single Transaction Response Time
Write-Host "`n1. Single Transaction Test:" -ForegroundColor Cyan
$singleTest = Measure-Command {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/transactions/transfer" `
        -Method POST `
        -Headers @{"Authorization"="Bearer $token"; "Content-Type"="application/json"} `
        -Body '{"senderUsername":"admin","receiverUsername":"manager","fundName":"General Fund","amount":10.00}'
}
Write-Host "   Single transaction time: $($singleTest.TotalMilliseconds) ms" -ForegroundColor White

# Test 2: Multiple Sequential Transactions
Write-Host "`n2. Sequential Transactions Test (10 requests):" -ForegroundColor Cyan
$totalTime = Measure-Command {
    for ($i = 1; $i -le 10; $i++) {
        try {
            $response = Invoke-RestMethod -Uri "$baseUrl/api/transactions/transfer" `
                -Method POST `
                -Headers @{"Authorization"="Bearer $token"; "Content-Type"="application/json"} `
                -Body "{`"senderUsername`":`"admin`",`"receiverUsername`":`"manager`",`"fundName`":`"General Fund`",`"amount`":5.00}"
            Write-Host "   Transaction $i completed" -ForegroundColor Gray
        }
        catch {
            Write-Host "   Transaction $i failed: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}
$avgTime = $totalTime.TotalMilliseconds / 10
Write-Host "   Total time for 10 transactions: $($totalTime.TotalMilliseconds) ms" -ForegroundColor White
Write-Host "   Average time per transaction: $avgTime ms" -ForegroundColor White

# Test 3: Database Query Performance
Write-Host "`n3. Database Query Performance Test:" -ForegroundColor Cyan
$queryTest = Measure-Command {
    $transactions = Invoke-RestMethod -Uri "$baseUrl/api/transactions" `
        -Method GET `
        -Headers @{"Authorization"="Bearer $token"}
}
Write-Host "   Transaction list query time: $($queryTest.TotalMilliseconds) ms" -ForegroundColor White
Write-Host "   Number of transactions retrieved: $($transactions.Count)" -ForegroundColor White

Write-Host "`n=== Performance Test Complete ===" -ForegroundColor Green