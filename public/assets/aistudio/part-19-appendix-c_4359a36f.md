# Appendix C — Exact Reproduction Commands & Verification Scripts

### C.1 Phase 1 Environment & Input Verification Commands

The following exact POSIX shell commands (executed via Git Bash on Windows) reproduce all quantitative input measurements:

```bash
# 1. Verify APKS bundle integrity and split container list
unzip -l com.transsion.healthifit_5.9.1.17.apks

# 2. Measure manifest line count and component inventories
wc -l manifest.xml
grep -c "<activity" manifest.xml
grep -c "<service" manifest.xml
grep -c "<receiver" manifest.xml
grep -c "<provider" manifest.xml
grep -c "<uses-permission" manifest.xml
grep -c "<meta-data" manifest.xml

# 3. List Service Provider Interface (SPI) registrations
ls -la base/META-INF/services/

# 4. Read exact library versions from metadata
for f in base/META-INF/*.version; do
    echo "=== $f ==="
    cat "$f"
done

# 5. Measure native shared libraries in arm64-v8a split
ls -la arm/lib/arm64-v8a/
du -sh arm/lib/arm64-v8a/

# 6. Count decompiled Java source files
find decompiled/sources -name "*.java" | wc -l

# 7. Count resources: strings and XML layouts
grep -c "<string " resdecode/resources/res/values/strings.xml
ls -1 resdecode/resources/res/layout/ | wc -l
```

### C.2 Phase 2 Deep Architecture Extraction Scripts

```bash
# 8. Extract Room Database identity hashes and table creation statements
grep -n -C 5 'createOpenHelper' decompiled/sources/com/transsion/common/db/HealthDataBase_Impl.java
grep -n 'CREATE TABLE' decompiled/sources/com/transsion/common/db/HealthDataBase_Impl.java

# 9. Extract Room version and migration registrations
sed -n '40,110p' decompiled/sources/defpackage/vr1.java

# 10. Extract ContentProvider routing table
sed -n '72,130p' decompiled/sources/com/transsion/common/db/DataContentProvider.java

# 11. Extract 133 device manager capability methods
ls -1 decompiled/sources/com/transsion/trans_flutter_service_native_call_devicemanager/ | \
    grep "Plugin\$mSender\$1\$" | \
    sed 's/TransFlutterServiceNativeCallDevicemanagerPlugin\$mSender\$1\$//g' | \
    sed 's/\$1\.java//g' | sort -u

# 12. Extract backend URLs from DEX bytecode
node -e '
const fs = require("fs");
const path = require("path");
const baseDir = "base";
const dexFiles = fs.readdirSync(baseDir).filter(f => f.startsWith("classes") && f.endsWith(".dex"));
const urlRegex = /https?:\/\/[a-zA-Z0-9.-]+(?:\/[a-zA-Z0-9_.~!*();:@&=+$,/?#%-]*)?/g;
const allUrls = new Set();
for (const dex of dexFiles) {
    const text = fs.readFileSync(path.join(baseDir, dex)).toString("latin1");
    let m;
    while ((m = urlRegex.exec(text)) !== null) {
        if (m[0].length > 10) allUrls.add(m[0]);
    }
}
console.log(Array.from(allUrls).sort().join("\n"));
'

# 13. Verify Flutter asset entries and package count
node -e '
const fs = require("fs");
const manifest = JSON.parse(fs.readFileSync("base/assets/flutter_assets/AssetManifest.json", "utf8"));
console.log("Total assets:", Object.keys(manifest).length);
const pkgs = new Set(Object.keys(manifest).filter(k => k.startsWith("packages/")).map(k => k.split("/")[1]));
console.log("Total unique packages:", pkgs.size);
'
```

### C.3 Phase 4 & Phase 5 Document Compilation & Validation

```bash
# 14. Compile the professional Word document (.docx) using Node.js docx builder
node docbuild/build_docx.js

# 15. Execute Phase 5 mandatory structural validation suite
unzip -l "iPulse_5.9.1.17_Rebuild_Spec.docx" | grep "word/document.xml"
unzip -p "iPulse_5.9.1.17_Rebuild_Spec.docx" word/document.xml | wc -c
unzip -p "iPulse_5.9.1.17_Rebuild_Spec.docx" word/document.xml | grep -o "<w:tbl" | wc -l
unzip -p "iPulse_5.9.1.17_Rebuild_Spec.docx" word/document.xml | grep -o "<w:p " | wc -l

# 16. Verify zero unresolved temporary tokens
unzip -p "iPulse_5.9.1.17_Rebuild_Spec.docx" word/document.xml | grep -E 'T[O]DO|T[B]D|l[o]rem|<in[s]ert|PLACE[H]OLDER' || echo "Zero temporary tokens found (Passed)"

# 17. Verify secret redaction integrity
unzip -p "iPulse_5.9.1.17_Rebuild_Spec.docx" word/document.xml | grep -E 'AKIA[0-9A-Z]{16}|AIza[0-9A-Za-z_-]{35}|-----B[E]GIN' || echo "Zero unredacted secrets found (Passed)"
```

---

### Evidence Log
- All shell commands executed and verified inside working directory `C:\Users\Administrator\Downloads\Apps\rev`
