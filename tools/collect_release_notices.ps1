param([string]$CacheRoot = 'C:/Users/Admin/.gradle/caches/modules-2/files-2.1')
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Read-ArchiveNotices($Archive, [string]$Prefix) {
    foreach ($entry in $Archive.Entries) {
        if ($entry.FullName -match '(?i)(license|notice|copyright|copying)') {
            if ($entry.Length -gt 0 -and $entry.Length -lt 300000 -and $entry.FullName -notmatch '\.(class|png)$') {
                $reader = [IO.StreamReader]::new($entry.Open())
                try { [pscustomobject]@{path="$Prefix!$($entry.FullName)"; text=$reader.ReadToEnd()} } finally { $reader.Dispose() }
            }
        } elseif ($entry.FullName -match '\.jar$') {
            $mem = [IO.MemoryStream]::new()
            $stream = $entry.Open()
            try { $stream.CopyTo($mem) } finally { $stream.Dispose() }
            $mem.Position = 0
            $nested = [IO.Compression.ZipArchive]::new($mem, [IO.Compression.ZipArchiveMode]::Read)
            try { Read-ArchiveNotices $nested "$Prefix!$($entry.FullName)" } finally { $nested.Dispose(); $mem.Dispose() }
        }
    }
}
$coordinates = Get-Content (Join-Path $PSScriptRoot '../RELEASE_DEPENDENCY_INVENTORY.md') | Where-Object { $_ -match '^[\w.-]+:[\w.-]+:[\w.-]+$' }
$records = foreach ($coordinate in $coordinates) {
    $directory = Join-Path $CacheRoot ($coordinate.Split(':') -join '/')
    $pom = Get-ChildItem $directory -Recurse -Filter '*.pom' | Select-Object -First 1
    if (-not $pom) { throw "Missing POM: $coordinate" }
    [xml]$metadata = Get-Content $pom.FullName
    $notices = @(foreach ($artifact in Get-ChildItem $directory -Recurse -File | Where-Object { $_.Extension -in '.aar','.jar' -and $_.Name -notmatch '-(sources|javadoc)\.' }) {
        $archive = [IO.Compression.ZipFile]::OpenRead($artifact.FullName)
        try { Read-ArchiveNotices $archive $artifact.Name } finally { $archive.Dispose() }
    })
    [pscustomobject]@{coordinate=$coordinate; licenses=@($metadata.project.licenses.license | ForEach-Object { [string]$_.name }); notices=$notices}
}
ConvertTo-Json -InputObject @($records) -Depth 6
