param(
    [uri]$BaseUri = [uri]"http://127.0.0.1:8080",
    [string[]]$Paths = @("/api/v1/academic-structure", "/api/v1/academic-periods"),
    [ValidateRange(0, 10000)]
    [int]$WarmupRequests = 15,
    [ValidateRange(10, 10000)]
    [int]$Samples = 100,
    [ValidateRange(1, 300)]
    [int]$TimeoutSeconds = 10
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Net.Http

function Get-Percentile([double[]]$SortedSamples, [double]$Percentile) {
    $index = [Math]::Max(0, [Math]::Ceiling($SortedSamples.Count * $Percentile) - 1)
    return $SortedSamples[$index]
}

$httpClient = [System.Net.Http.HttpClient]::new()
$httpClient.Timeout = [TimeSpan]::FromSeconds($TimeoutSeconds)
$results = [System.Collections.Generic.List[object]]::new()

try {
    foreach ($path in $Paths) {
        if ([string]::IsNullOrWhiteSpace($path) -or -not $path.StartsWith("/")) {
            throw "Every path must be an absolute path beginning with '/'."
        }

        $uri = [uri]::new($BaseUri, $path)
        for ($warmup = 0; $warmup -lt $WarmupRequests; $warmup++) {
            $response = $httpClient.GetAsync($uri).GetAwaiter().GetResult()
            try {
                $response.EnsureSuccessStatusCode() | Out-Null
                $response.Content.ReadAsByteArrayAsync().GetAwaiter().GetResult() | Out-Null
            }
            finally {
                $response.Dispose()
            }
        }

        $measurements = [System.Collections.Generic.List[double]]::new()
        for ($sample = 0; $sample -lt $Samples; $sample++) {
            $watch = [System.Diagnostics.Stopwatch]::StartNew()
            $response = $httpClient.GetAsync($uri).GetAwaiter().GetResult()
            try {
                $response.EnsureSuccessStatusCode() | Out-Null
                $response.Content.ReadAsByteArrayAsync().GetAwaiter().GetResult() | Out-Null
                $statusCode = [int]$response.StatusCode
            }
            finally {
                $watch.Stop()
                $response.Dispose()
            }
            $measurements.Add($watch.Elapsed.TotalMilliseconds)
        }

        $sortedSamples = [double[]]@($measurements | Sort-Object)
        $middle = [Math]::Floor($sortedSamples.Count / 2)
        if ($sortedSamples.Count % 2 -eq 0) {
            $median = ($sortedSamples[$middle - 1] + $sortedSamples[$middle]) / 2
        }
        else {
            $median = $sortedSamples[$middle]
        }

        $results.Add([pscustomobject]@{
            endpoint       = $uri.AbsoluteUri
            statusCode     = $statusCode
            samples        = $measurements.Count
            warmupRequests = $WarmupRequests
            averageMs      = [Math]::Round(($measurements | Measure-Object -Average).Average, 2)
            medianMs       = [Math]::Round($median, 2)
            p95Ms          = [Math]::Round((Get-Percentile $sortedSamples 0.95), 2)
            maxMs          = [Math]::Round($sortedSamples[-1], 2)
        })
    }

    @($results) | ConvertTo-Json -Depth 3
}
finally {
    $httpClient.Dispose()
}
