/**
 * ==============================================================================
 * BUDDIES ONE-COMMAND RELEASE AUTOMATION ENGINE
 * ==============================================================================
 *
 * Usage:
 *   node scripts/release.js <version> [options]
 *
 * Examples:
 *   node scripts/release.js 0.3.0
 *   node scripts/release.js v0.3.0 --dry-run
 *   node scripts/release.js 0.3.0 --notes "Added P2P Media Sharing,Bug fixes"
 *   node scripts/release.js 0.3.0 --mandatory
 *
 * Options:
 *   --dry-run          Simulates the entire pipeline without publishing to GitHub or website.
 *   --notes <text>     Comma-separated list of release notes or path to notes file.
 *   --mandatory        Sets isMandatory = true in the update manifest.
 *   --skip-build       Reuses an existing APK build instead of running Gradle.
 *   --allow-downgrade  Bypasses version comparison validation.
 * ==============================================================================
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const isWin = process.platform === 'win32';
const PROJECT_ROOT = path.resolve(__dirname, '..');
const GRADLE_KTS_PATH = path.join(PROJECT_ROOT, 'app', 'build.gradle.kts');
const UNIVERSAL_APK_PATH = path.join(PROJECT_ROOT, 'app', 'build', 'outputs', 'apk', 'debug', 'app-universal-debug.apk');

// Target website manifest paths
const LOCAL_WEBSITE_MANIFEST = isWin ? path.join('c:', 'Users', 'KD', 'Desktop', 'BUDDYS-WEBSITE', 'public', 'update.json') : '';
const ROOT_MANIFEST_PATH = path.join(PROJECT_ROOT, 'update.json');
const GITHUB_REPO = 'arjunkpatil2311-2325/BUDDYS';

// Parse CLI arguments
const args = process.argv.slice(2);
const isDryRun = args.includes('--dry-run');
const isMandatory = args.includes('--mandatory');
const skipBuild = args.includes('--skip-build');
const allowDowngrade = args.includes('--allow-downgrade');

let rawVersion = args.find(a => !a.startsWith('--') && a !== 'release' && a !== 'Buddies');
const notesArgIndex = args.indexOf('--notes');
const customNotes = notesArgIndex !== -1 && args[notesArgIndex + 1] ? args[notesArgIndex + 1] : null;

if (!rawVersion) {
    console.error('\n❌ Error: No release version specified.');
    console.error('Usage: node scripts/release.js <version> [--dry-run] [--mandatory] [--notes "..."]\n');
    process.exit(1);
}

// 1. Version Normalization
const cleanVersion = rawVersion.replace(/^[vV]/, '').trim();
const releaseTag = 'v' + cleanVersion;

if (!/^\d+\.\d+\.\d+$/.test(cleanVersion)) {
    console.error(`\n❌ Error: Invalid semantic version format: "${rawVersion}". Expected format: X.Y.Z (e.g. 0.3.0)\n`);
    process.exit(1);
}

console.log('\n==================================================');
console.log('🚀 BUDDIES ONE-COMMAND RELEASE AUTOMATION');
console.log('==================================================');
console.log(`Target Version:     ${cleanVersion}`);
console.log(`GitHub Release Tag: ${releaseTag}`);
console.log(`Environment:        ${isWin ? 'Windows (Local Command)' : 'Linux (GitHub Actions CI)'}`);
console.log(`Mode:               ${isDryRun ? '🔍 DRY RUN (Simulation - Zero publishing)' : '🌐 FULLY AUTOMATIC LIVE RELEASE'}`);
console.log('--------------------------------------------------');

// 2. Read Current Gradle Version
if (!fs.existsSync(GRADLE_KTS_PATH)) {
    console.error(`❌ Error: ${GRADLE_KTS_PATH} not found!`);
    process.exit(1);
}

const gradleContent = fs.readFileSync(GRADLE_KTS_PATH, 'utf8');
const currentVersionNameMatch = gradleContent.match(/versionName\s*=\s*"([^"]+)"/);
const currentVersionCodeMatch = gradleContent.match(/versionCode\s*=\s*(\d+)/);

if (!currentVersionNameMatch || !currentVersionCodeMatch) {
    console.error('❌ Error: Could not parse current versionName or versionCode from app/build.gradle.kts');
    process.exit(1);
}

const currentVersionName = currentVersionNameMatch[1];
const currentVersionCode = parseInt(currentVersionCodeMatch[1], 10);
const newVersionCode = currentVersionCode + 1;

console.log(`Current versionName: ${currentVersionName}`);
console.log(`Current versionCode: ${currentVersionCode}`);
console.log(`New versionCode:     ${newVersionCode}`);

// Version comparison check
function parseSemVer(v) {
    return v.split('.').map(n => parseInt(n, 10));
}

if (!allowDowngrade) {
    const [cMajor, cMinor, cPatch] = parseSemVer(currentVersionName);
    const [nMajor, nMinor, nPatch] = parseSemVer(cleanVersion);

    const isGreater = (nMajor > cMajor) ||
        (nMajor === cMajor && nMinor > cMinor) ||
        (nMajor === cMajor && nMinor === cMinor && nPatch > cPatch);

    if (!isGreater && cleanVersion === currentVersionName) {
        console.warn(`⚠️ Warning: Target version ${cleanVersion} matches current versionName ${currentVersionName}.`);
    } else if (!isGreater) {
        console.error(`❌ Error: Target version ${cleanVersion} is lower than current version ${currentVersionName}.`);
        console.error('Pass --allow-downgrade to override.');
        process.exit(1);
    }
}

// 3. Pre-flight GitHub Check (Check tag collision)
async function checkGitHubTag() {
    try {
        const url = `https://api.github.com/repos/${GITHUB_REPO}/releases/tags/${releaseTag}`;
        const res = await fetch(url, { headers: { 'User-Agent': 'Buddies-Release-Engine' } });
        if (res.status === 200) {
            const data = await res.json();
            throw new Error(`GitHub Release ${releaseTag} already exists! Published at: ${data.published_at}. Please increment the version.`);
        }
    } catch (e) {
        if (e.message.includes('already exists')) {
            throw e;
        }
    }
}

// 4. Update Gradle File
function updateGradleFile() {
    console.log('\n[Step 1/6] Updating app/build.gradle.kts...');
    let updatedContent = gradleContent.replace(
        /versionCode\s*=\s*\d+/,
        `versionCode = ${newVersionCode}`
    );
    updatedContent = updatedContent.replace(
        /versionName\s*=\s*"[^"]+"/,
        `versionName = "${cleanVersion}"`
    );

    if (!isDryRun) {
        fs.writeFileSync(GRADLE_KTS_PATH, updatedContent, 'utf8');
        console.log(`✅ Updated app/build.gradle.kts (versionCode = ${newVersionCode}, versionName = "${cleanVersion}")`);
    } else {
        console.log(`[DRY RUN] Would update build.gradle.kts: versionCode = ${newVersionCode}, versionName = "${cleanVersion}"`);
    }
}

// 5. Build Universal APK Locally to Validate Compilation
function buildApk() {
    console.log('\n[Step 2/6] Building Universal APK via Gradle...');
    if (skipBuild && fs.existsSync(UNIVERSAL_APK_PATH)) {
        console.log('⏩ Skipping Gradle build (--skip-build specified). Using existing APK.');
        return;
    }

    try {
        const cmd = isWin ? 'gradlew.bat assembleDebug' : './gradlew assembleDebug --no-daemon';
        console.log(`Running: ${cmd}`);
        execSync(cmd, { cwd: PROJECT_ROOT, stdio: 'inherit' });
        console.log('✅ Gradle build completed successfully.');
    } catch (e) {
        throw new Error('Gradle build failed: ' + e.message);
    }
}

// 6. Validate Artifact
function validateArtifact() {
    console.log('\n[Step 3/6] Validating output APK...');
    if (!fs.existsSync(UNIVERSAL_APK_PATH)) {
        throw new Error(`Universal APK not found at: ${UNIVERSAL_APK_PATH}`);
    }

    const stats = fs.statSync(UNIVERSAL_APK_PATH);
    const sizeInBytes = stats.size;
    const sizeInMB = (sizeInBytes / (1024 * 1024)).toFixed(1);
    const formattedSize = `${sizeInMB} MB`;

    if (sizeInBytes < 10 * 1024 * 1024) {
        throw new Error(`APK size is suspiciously small (${sizeInBytes} bytes). Build may be corrupted.`);
    }

    console.log(`✅ Universal APK verified:`);
    console.log(`   File: ${path.basename(UNIVERSAL_APK_PATH)}`);
    console.log(`   Path: ${UNIVERSAL_APK_PATH}`);
    console.log(`   Size: ${formattedSize} (${sizeInBytes} bytes)`);

    return { sizeInBytes, formattedSize };
}

// 7. Prepare Release Notes
function getReleaseNotes() {
    if (customNotes) {
        return customNotes.split(',').map(s => s.trim()).filter(Boolean);
    }
    return [
        'Tap to Buddy peer-to-peer media sharing',
        'Rebranded to Buddies with ultra-smooth UI',
        'Enhanced connectivity and stability improvements'
    ];
}

// 8. Update Public Manifest
function updatePublicManifest(artifactInfo, notesList) {
    console.log('\n[Step 4/6] Generating update manifest (update.json)...');
    const today = new Date().toISOString().split('T')[0];
    const apkDownloadUrl = `https://github.com/${GITHUB_REPO}/releases/download/${releaseTag}/app-universal-debug.apk`;

    const manifestData = {
        latestVersion: cleanVersion,
        versionCode: newVersionCode,
        apkUrl: apkDownloadUrl,
        apkFileName: 'app-universal-debug.apk',
        fileSize: artifactInfo.formattedSize,
        releaseDate: today,
        releaseNotes: notesList,
        isMandatory: isMandatory,
        minimumSupportedVersionCode: 1
    };

    const manifestJson = JSON.stringify(manifestData, null, 2);

    if (isDryRun) {
        console.log('[DRY RUN] Generated update.json:');
        console.log(manifestJson);
        return apkDownloadUrl;
    }

    fs.writeFileSync(ROOT_MANIFEST_PATH, manifestJson, 'utf8');
    console.log(`✅ Updated in-repository manifest at ${ROOT_MANIFEST_PATH}`);

    if (LOCAL_WEBSITE_MANIFEST && fs.existsSync(path.dirname(LOCAL_WEBSITE_MANIFEST))) {
        fs.writeFileSync(LOCAL_WEBSITE_MANIFEST, manifestJson, 'utf8');
        console.log(`✅ Updated website update manifest at ${LOCAL_WEBSITE_MANIFEST}`);
    }

    return apkDownloadUrl;
}

// 9. Publish via Git Tag & Trigger GitHub Actions Cloud Release
function triggerGitRelease() {
    console.log('\n[Step 5/6] Triggering GitHub Actions automated release pipeline...');

    if (isDryRun) {
        console.log(`[DRY RUN] Would execute:`);
        console.log(`   git add .`);
        console.log(`   git commit -m "chore(release): release Buddies ${releaseTag}"`);
        console.log(`   git tag ${releaseTag}`);
        console.log(`   git push origin HEAD --tags`);
        return;
    }

    try {
        console.log('Staging changes and creating git tag...');
        execSync('git add -A', { cwd: PROJECT_ROOT, stdio: 'inherit' });
        
        try {
            execSync(`git commit -m "chore(release): release Buddies ${releaseTag}"`, { cwd: PROJECT_ROOT, stdio: 'inherit' });
        } catch (_) {
            console.log('Working tree already committed.');
        }

        console.log(`Creating tag ${releaseTag}...`);
        execSync(`git tag ${releaseTag}`, { cwd: PROJECT_ROOT, stdio: 'inherit' });

        console.log('Pushing commit and tag to GitHub (automatically triggering GitHub Actions)...');
        execSync(`git push origin HEAD:main --tags`, { cwd: PROJECT_ROOT, stdio: 'inherit' });

        console.log('✅ Successfully pushed release tag to GitHub.');
    } catch (e) {
        throw new Error('Failed to push release tag to GitHub: ' + e.message);
    }
}

// 10. Deploy Website to Vercel
function deployWebsite() {
    console.log('\n[Step 6/6] Deploying Buddies website update endpoint...');

    if (isDryRun) {
        console.log('[DRY RUN] Would deploy website to Vercel production: https://buddys01.vercel.app');
        return;
    }

    if (LOCAL_WEBSITE_MANIFEST && fs.existsSync(path.dirname(LOCAL_WEBSITE_MANIFEST))) {
        try {
            const vercelCmd = isWin ? 'cmd /c "npx --yes vercel --scope arjunagency --prod --yes"' : 'npx --yes vercel --scope arjunagency --prod --yes';
            execSync(vercelCmd, {
                cwd: path.dirname(path.dirname(LOCAL_WEBSITE_MANIFEST)),
                stdio: 'inherit'
            });
            console.log('✅ Buddies website deployed to Vercel production.');
        } catch (e) {
            console.warn(`⚠️ Notice: Local Vercel CLI deploy: ${e.message}`);
        }
    }
}

// Main Execution Flow
async function main() {
    await checkGitHubTag();
    updateGradleFile();
    buildApk();
    const artifactInfo = validateArtifact();
    const notesList = getReleaseNotes();
    const apkDownloadUrl = updatePublicManifest(artifactInfo, notesList);
    triggerGitRelease();
    deployWebsite();

    console.log('\n==================================================');
    if (isDryRun) {
        console.log('🎉 DRY RUN COMPLETED SUCCESSFULLY (Zero Changes Published)');
        console.log('==================================================');
        console.log(`All validations passed for Buddies v${cleanVersion} (code ${newVersionCode}).`);
        console.log('To execute the real release automatically, run:');
        console.log(`  node scripts/release.js ${cleanVersion}`);
    } else {
        console.log('🎉 ONE-COMMAND RELEASE TRIGGERED SUCCESSFULLY');
        console.log('==================================================');
        console.log(`Version:        ${cleanVersion} (versionCode: ${newVersionCode})`);
        console.log(`Tag:            ${releaseTag}`);
        console.log(`GitHub Actions: Triggered automatically`);
        console.log(`APK URL:        ${apkDownloadUrl}`);
        console.log(`Update Endpoint: https://buddys01.vercel.app/update.json`);
        console.log('Existing Buddies app installations will detect and offer the update.');
    }
    console.log('==================================================\n');
}

main().catch(err => {
    console.error('\n❌ Release Error:', err.message);
    process.exit(1);
});
