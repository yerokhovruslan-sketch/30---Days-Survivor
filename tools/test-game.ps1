$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskJavaHome = $env:JAVA_HOME
if (-not $taskJavaHome) { $taskJavaHome = Join-Path $env:ProgramFiles 'Android\Android Studio\jbr' }
$taskJava = Join-Path $taskJavaHome 'bin\java.exe'
if (-not (Test-Path -LiteralPath $taskJava)) { throw 'Set JAVA_HOME to your JDK directory.' }
$env:JAVA_HOME = $taskJavaHome
$taskCache = $env:GRADLE_USER_HOME
if (-not $taskCache) { $taskCache = Join-Path $env:USERPROFILE '.gradle' }
Push-Location $taskRoot
try {
    & .\gradlew.bat :app:compileDebugUnitTestJavaWithJavac --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
    $taskJunit = Get-ChildItem (Join-Path $taskCache 'caches\modules-2\files-2.1\junit\junit') -Recurse -Filter 'junit-4.13.2.jar' | Select-Object -First 1 -ExpandProperty FullName
    $taskHamcrest = Get-ChildItem (Join-Path $taskCache 'caches\modules-2\files-2.1\org.hamcrest\hamcrest-core') -Recurse -Filter 'hamcrest-core-1.3.jar' | Select-Object -First 1 -ExpandProperty FullName
    if (-not $taskJunit -or -not $taskHamcrest) { throw 'JUnit dependencies were not found in the Gradle cache.' }
    $taskClasspath = @(
        (Join-Path $taskRoot 'app\build\intermediates\javac\debug\compileDebugJavaWithJavac\classes'),
        (Join-Path $taskRoot 'app\build\intermediates\javac\debugUnitTest\compileDebugUnitTestJavaWithJavac\classes'),
        $taskJunit, $taskHamcrest
    ) -join ';'
    & $taskJava -cp $taskClasspath org.junit.runner.JUnitCore com.example.survivor.game.LiveClockTest com.example.survivor.game.EconomyTest com.example.survivor.game.CraftingTest com.example.survivor.game.WeaponBalanceTest com.example.survivor.game.SoundEventsTest com.example.survivor.game.ResidentialStoriesTest com.example.survivor.game.BattleNoiseTest com.example.survivor.game.EquipmentStateTest com.example.survivor.game.RadioSignalTest com.example.survivor.game.TacticalBattleTest com.example.survivor.IntroTimelineTest com.example.survivor.game.GameSessionTest com.example.survivor.game.ShopTest com.example.survivor.game.SurvivalSystemsTest com.example.survivor.game.WorldStoriesTest com.example.survivor.game.ShelterTest com.example.survivor.game.TimeSearchTest com.example.survivor.game.ShelterUpgradesTest com.example.survivor.game.LocationPreviewTest
    if ($LASTEXITCODE -ne 0) { throw 'Game tests failed.' }
} finally { Pop-Location }


