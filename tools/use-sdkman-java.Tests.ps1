Describe 'use-sdkman-java.ps1' {
    BeforeAll {
        $script:helperPath = Join-Path $PSScriptRoot 'use-sdkman-java.ps1'
        $script:originalUserProfile = $env:USERPROFILE
        $script:originalJavaHome = $env:JAVA_HOME
        $script:originalPath = $env:PATH
    }

    BeforeEach {
        $script:fakeUserProfile = Join-Path $TestDrive ([guid]::NewGuid().ToString('N'))
        $script:sdkmanJavaRoot = Join-Path $script:fakeUserProfile '.sdkman\candidates\java'
        $script:pinnedJavaHome = Join-Path $script:sdkmanJavaRoot '25.0.4-tem'
        $script:otherJavaHome = Join-Path $script:sdkmanJavaRoot '21.0.10-tem'
        $script:pinnedJavaBin = Join-Path $script:pinnedJavaHome 'bin'
        $script:otherJavaBin = Join-Path $script:otherJavaHome 'bin'
        $script:currentJavaHome = Join-Path $script:sdkmanJavaRoot 'current'

        New-Item -ItemType Directory -Path $script:pinnedJavaBin -Force | Out-Null
        New-Item -ItemType Directory -Path $script:otherJavaBin -Force | Out-Null
        New-Item -ItemType File -Path (Join-Path $script:pinnedJavaBin 'java.exe') -Force | Out-Null
        New-Item -ItemType File -Path (Join-Path $script:otherJavaBin 'java.exe') -Force | Out-Null
        New-Item -ItemType Junction -Path $script:currentJavaHome -Target $script:otherJavaHome | Out-Null

        $env:USERPROFILE = $script:fakeUserProfile
        $env:JAVA_HOME = 'before-test'
        $env:PATH = (@($script:otherJavaBin, $script:pinnedJavaBin, 'C:\Tools') -join ';')
    }

    AfterAll {
        $env:USERPROFILE = $script:originalUserProfile
        $env:JAVA_HOME = $script:originalJavaHome
        $env:PATH = $script:originalPath
    }

    It 'selects the exact Java candidate declared by the repository .sdkmanrc' {
        # Arrange: BeforeEach points SDKMAN current at 21.0.10-tem while .sdkmanrc pins 25.0.4-tem.

        # Act
        & $script:helperPath | Out-Null

        # Assert
        $pathEntries = $env:PATH -split ';'
        if ($env:JAVA_HOME -cne $script:pinnedJavaHome) {
            throw "Expected JAVA_HOME to be $script:pinnedJavaHome; found $env:JAVA_HOME."
        }
        if ($pathEntries[0] -cne $script:pinnedJavaBin) {
            throw "Expected PATH to start with $script:pinnedJavaBin; found $($pathEntries[0])."
        }
        if ($pathEntries -notcontains 'C:\Tools') {
            throw 'The helper must preserve unrelated PATH entries.'
        }
        if ($pathEntries -contains $script:otherJavaBin) {
            throw 'The helper must remove stale SDKMAN Java candidates from PATH.'
        }
    }

    It 'fails without changing the environment when the pinned SDKMAN candidate is unavailable' {
        # Arrange
        Remove-Item -LiteralPath $script:pinnedJavaHome -Recurse -Force
        $javaHomeBefore = $env:JAVA_HOME
        $pathBefore = $env:PATH

        # Act / Assert
        $failureMessage = $null
        try {
            & $script:helperPath | Out-Null
        }
        catch {
            $failureMessage = $_.Exception.Message
        }

        if ([string]::IsNullOrWhiteSpace($failureMessage) -or -not $failureMessage.Contains('25.0.4-tem')) {
            throw "Expected a missing-pinned-JDK error; found '$failureMessage'."
        }
        if ($env:JAVA_HOME -cne $javaHomeBefore) {
            throw 'JAVA_HOME changed despite the pinned candidate being unavailable.'
        }
        if ($env:PATH -cne $pathBefore) {
            throw 'PATH changed despite the pinned candidate being unavailable.'
        }
    }
}
