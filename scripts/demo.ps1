param([string]$BaseUrl = 'http://localhost:8080')
$ErrorActionPreference = 'Stop'
if ($PSVersionTable.PSVersion.Major -lt 7) { throw 'Ejecutar con PowerShell 7 (pwsh).' }
$service = Read-Host 'JWT SERVICE usuario 4 (sin Bearer)' -MaskInput
$client = Read-Host 'JWT CLIENTE usuario 4 (sin Bearer)' -MaskInput
function Request-Api($method, $path, $token, $body = $null, $key = $null, $expected = 200) {
    $headers = @{Authorization = 'Bearer ' + $token.Trim()}
    if ($key) { $headers['Idempotency-Key'] = $key }
    $options = @{Method=$method; Uri=($BaseUrl.TrimEnd('/') + '/api/notifications' + $path); Headers=$headers; SkipHttpErrorCheck=$true}
    if ($null -ne $body) { $options.ContentType='application/json'; $options.Body=($body | ConvertTo-Json) }
    $response=Invoke-WebRequest @options
    if ([int]$response.StatusCode -ne $expected) { throw "HTTP inesperado en $method $path : $($response.StatusCode). Revisar la consola del servicio." }
    if ($response.Content) { return ($response.Content | ConvertFrom-Json) }
}
try {
    $key='demo-' + [guid]::NewGuid().ToString()
    $body=@{idReceiver='4';type='DEMO_FAIL_ONCE';title='Pedido de demostracion';content='Fallo inicial y reintento controlado.'}
    $created=Request-Api POST '' $service $body $key 201
    if ($created.status -ne 'FAILED') { throw 'Activar perfil local y comprobar que el usuario 4 no este en fail-for-receivers.' }
    $id=$created.id
    $null=Request-Api GET "/$id/status" $client
    $read=Request-Api PATCH "/$id/read" $client
    $repeated=Request-Api PATCH "/$id/read" $client
    if ($read.readAt -ne $repeated.readAt) { throw 'readAt no se conservo.' }
    $retried=Request-Api POST "/$id/retry" $service
    if ($retried.id -ne $id -or $retried.status -ne 'SENT') { throw 'El reintento no conservo ID o no envio.' }
    $replay=Request-Api POST '' $service $body $key 200
    if ($replay.id -ne $id) { throw 'La repeticion creo otra notificacion.' }
    $null=Request-Api POST "/$id/retry" $client $null $null 403
    $null=Request-Api GET '?size=0' $client $null $null 400
    $null=Request-Api GET '?status=SENT&read=true&sort=createdAt,desc' $client
    $body.type='DEMO_ALWAYS_FAIL'
    $failed=Request-Api POST '' $service $body ($key+'-limit') 201
    1..3 | ForEach-Object { $null=Request-Api POST "/$($failed.id)/retry" $service }
    $null=Request-Api POST "/$($failed.id)/retry" $service $null $null 409
    Write-Output "Demostracion correcta. ID recuperado: $id. ID con limite agotado: $($failed.id)."
} finally { $service=$null; $client=$null }
