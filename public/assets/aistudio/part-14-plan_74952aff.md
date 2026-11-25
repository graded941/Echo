# ECHO phased rebuild plan and agent handoffs

## 14.1 Purpose and scope

ECHO is a new, personal Android companion app for the itel P55 and itel ISW-023 Storm. Use this plan to turn the iPulse analysis in Parts 1–13 into a staged implementation. Do not treat the iPulse package name, source, assets, cloud services, or signing identity as ECHO components.

The target is a useful, testable app for one phone and one watch. It is not a promise to reproduce all 49 iPulse device models, every iPulse screen, or the behavior of Google Fit or Strava. Match specific user workflows and prove them with tests. Do not claim equivalent health accuracy without device validation.

Keep the implementation in a clean ECHO source project. Treat the decompiled iPulse files as read-only evidence. Reuse only code, protocols, libraries, and assets that the project owner has the right to use. Do not copy proprietary artwork, fonts, firmware, private signing keys, or undocumented cloud credentials into ECHO.

## 14.2 Product goals and feasibility boundaries

ECHO's requested capabilities are:

| Capability | Initial target | Boundary to verify |
| :--- | :--- | :--- |
| Fitness tracking | Daily steps and supported watch health readings, with local history | Confirm which ISW-023 sensors expose usable data. Health readings are estimates, not medical measurements. |
| Activity history | Record phone GPS workouts and show saved activity details | Verify background location behavior, route quality, battery use, and whether the watch can provide workout records. |
| Watch-face customization | Let the user customize the supported center or background elements | Confirm the ISW-023 dial format, layout controls, image dimensions, transfer limits, and vendor SDK terms on the physical watch. |
| itel Buds Neo 3 | Use the earbuds as a phone audio device | Pair and control audio through Android's Bluetooth and media APIs. Do not assume the watch BLE protocol controls the earbuds. |
| Notifications | Let the user choose which phone notifications the watch mirrors | Android requires the user to grant notification access. Show clear controls for sensitive message content. |
| Alarms and Do Not Disturb | Configure phone alarms and mirror watch settings when supported | Exact alarms and DND changes require Android permissions or user-granted policy access. Test each action on the itel P55. |
| Phone integration | Use supported Android and itel services | Launcher cards, background exemptions, and system settings may require an official itel API or privileged signing. Do not promise access without one. |
| Assistant and app tasks | Open or control supported phone features, including Gemini, YouTube Music, weather, calls, messages, and WhatsApp | Use public Android intents, media-session APIs, and documented provider APIs. Do not depend on private app APIs or silently read or send messages. |

The iPulse analysis identifies an `ItelISWO23New` MoYoung adapter and a device capability bridge. That is evidence that iPulse has an ISW-O23 integration. It does not prove that ECHO can legally reuse the vendor SDK, that every listed capability works on the user's watch, or that the feature is exposed to third-party apps. Verify each claim on the device before implementation.

Use Android-first native Kotlin as the provisional implementation choice because ECHO targets one Android phone and needs Bluetooth, notifications, alarms, location, and media integration. Confirm this choice in Phase 2 before creating the project. If the owner chooses Flutter, keep Android services and permissions behind a typed native interface.

## 14.3 Agent responsibilities

Gemini AI Studio is the lead agent and keeps the accepted requirements, architecture decisions, phase status, and final verification record. Only Gemini closes a phase after reading its evidence and test results. Assign each coding task to one agent and one branch or isolated file set.

| Agent | Assigned work | Handoff |
| :--- | :--- | :--- |
| Gemini AI Studio | Lead the plan, resolve scope conflicts, approve interfaces, assign work, integrate results, and check phase gates | Decision log, scoped task, acceptance criteria, merged result, and verification summary |
| Figma AI design | Create the ECHO user flows, wireframes, reusable design tokens, and screen specifications from the approved product scope | Figma link or export, component states, responsive notes, accessibility notes, and asset-license notes |
| GitHub Copilot CLI | Implement Android Kotlin features, tests, and build changes in the approved ECHO project | Changed-file list, commands run, test output, known gaps, and commit or branch reference |
| Antigravity CLI | Build the assigned UI slice from approved Figma specs; do not edit the same screens as Copilot in the same phase | UI implementation, screenshots or test evidence, changed-file list, and unresolved design questions |
| Freebuff CLI | Run a bounded source or platform feasibility check, or implement an explicitly assigned test fixture after the interfaces are fixed | Evidence with file or API references, a reproducible check, and pass or fail against the stated question |
| Claude Chat | Review requirements, privacy, accessibility, architecture trade-offs, and phase acceptance criteria; do not make unrequested source edits | Short findings ranked by severity, with a specific correction or a clear statement that no blocker was found |

If Freebuff CLI cannot inspect the source or run the assigned check, Gemini reassigns the task. Do not treat an unverified agent claim as a device or platform fact.

## 14.4 Shared execution rules

Use these rules for every phase:

1. Gemini writes a task with one owner, one bounded outcome, target files or artifacts, dependencies, and acceptance checks.
2. Keep code agents from editing the same files at the same time. Merge one task before assigning dependent work.
3. Keep all ECHO code outside the decompiled iPulse tree. Do not overwrite or patch reverse-engineered files.
4. Link each implementation claim to source evidence, platform documentation, a physical-device observation, or a test.
5. Test the smallest behavior that proves the task. Record failed checks and blockers; do not report a success-shaped fallback.
6. Require user approval before enabling collection of sensitive data, sending messages, changing DND, or starting a workout in the background.
7. Keep API keys, private certificates, production credentials, and personal health data out of prompts, commits, logs, and test fixtures.
8. Use licensed or original ECHO visual assets. Do not import iPulse images, fonts, or watch-face binaries without permission.

Every agent handoff includes:

```text
Phase and task:
Owner:
Outcome:
Inputs and evidence:
Files or artifacts changed:
Commands and tests run:
Results:
Known limitations:
Next dependency:
```

## 14.5 Phase overview

| Phase | Goal | Lead | Gate |
| :--- | :--- | :--- | :--- |
| P0 | Fix scope, evidence, rights, and target environment | Gemini | Product brief and unresolved questions accepted |
| P1 | Prove phone, watch, and Android capability boundaries | Freebuff, reviewed by Claude | Feasibility matrix includes evidence and device tests |
| P2 | Approve user flows, architecture, and data contracts | Gemini and Figma | Platform choice and MVP contracts approved |
| P3 | Create the ECHO project and verification baseline | Copilot | Clean debug build and automated test baseline pass |
| P4 | Connect one ISW-023 watch and ingest supported data | Copilot | Pair, reconnect, and persist confirmed metrics on hardware |
| P5 | Deliver fitness history and phone-GPS activity recording | Copilot | Recorded data survives restart and passes replay tests |
| P6 | Add dial customization and phone-to-watch controls | Copilot and Antigravity | Each enabled feature passes a physical-device acceptance test |
| P7 | Add supported assistant, media, weather, and task handoffs | Copilot | Each integration uses a documented public API and a user-visible flow |
| P8 | Harden privacy, reliability, and release readiness | Gemini, Copilot, Claude | Release checklist passes on the itel P55 and ISW-023 |

Figma can start the visual direction during P0. Do not finalize screens before P1 confirms which device features ECHO can expose. Freebuff can prepare test cases during P2 after Gemini approves the interfaces.

## 14.6 Phase details

### Phase 0: Set the project boundaries

**Lead:** Gemini AI Studio. **Review:** Claude Chat.

**Work**

- Confirm that ECHO is a new app, not an iPulse repackaging or a modification of the vendor APK.
- Define the first release around the itel P55 and itel ISW-023 Storm only.
- Record the user's priority order for fitness history, activity tracking, dial customization, notifications, alarms, DND, earbuds, and phone integrations.
- Create an evidence ledger that separates observed iPulse facts from ECHO assumptions.
- Resolve the driver-count mismatch in this specification: Parts 1 and 4 report 49 models, while Part 4 and Part 7 list 47 `IHealthDevice` registrations.
- Identify the clean ECHO source-project location and confirm that all agents can read and write only the approved project.

**Deliverables**

- Approved ECHO product brief.
- In-scope and out-of-scope feature list.
- Evidence ledger with source, confidence, and verification status.
- Initial risk list and user-data handling rules.

**Gate:** Do not start implementation until the target repository, first-release scope, and rights to any intended SDK are clear.

### Phase 1: Verify device and platform feasibility

**Lead:** Freebuff CLI. **Review:** Claude Chat. **Device checks:** Owner with Copilot CLI support.

**Work**

- Inspect the ISW-023 model entries and capability calls cited in Parts 7 and 9. Record what the decompiled iPulse evidence shows without treating it as authorization to reuse vendor code.
- Pair the physical ISW-023 with the itel P55 using the current supported app. Record model identifiers, available sensors, dial options, alarms, notification settings, and firmware version.
- Check whether an authorized MoYoung SDK or documented integration path is available for a separate app.
- Confirm how the earbuds pair and play audio through Android. Separate phone audio behavior from watch control.
- Test the Android permissions and settings needed for notification access, location, exact alarms, DND policy access, and background BLE.
- Check whether the itel P55 exposes public launcher or system-service APIs. Mark privileged or undocumented access as unavailable unless the OEM provides documentation and authorization.

**Deliverables**

- Capability matrix with one row per requested feature and one of `confirmed`, `blocked`, or `needs device test`.
- Short evidence note for every confirmed claim.
- Fallback behavior for features that Android or the watch does not expose.

**Gate:** Stop or narrow any feature that requires private APIs, vendor credentials, privileged signing, or unlicensed binaries.

### Phase 2: Approve user flows and architecture

**Lead:** Gemini AI Studio. **Design:** Figma AI design. **Review:** Claude Chat.

**Work**

- Select the Android-first Kotlin implementation or record the approved alternative.
- Define an MVP: watch pairing, supported health and activity history, phone GPS workouts, notification controls, and only the device settings confirmed in P1.
- Define user flows for first launch, permissions, pairing, sync status, activity recording, notification setup, and disconnected-device recovery.
- Design the main screens and error states in Figma. Include empty, loading, permission-denied, offline, disconnected, and unsupported-device states.
- Specify module boundaries and contracts for pairing, telemetry, workout recording, local storage, and platform integrations.
- Choose a local data model and retention rules. Use Health Connect only for metrics and permissions that the Android platform and product policy allow.
- Decide which integrations are launch-blocking and which are later additions.

**Deliverables**

- Approved screen map and Figma handoff.
- Architecture note with module and data-flow diagrams.
- Versioned interface and data contracts.
- MVP acceptance criteria and feature flags.

**Gate:** Gemini approves the platform, permissions, data model, and API contracts before coding begins.

### Phase 3: Create the ECHO project baseline

**Lead:** GitHub Copilot CLI. **UI prototype:** Antigravity CLI after the Figma handoff.

**Work**

- Create the new ECHO Android project in the approved source location. Use a new application ID, signing configuration, and app identity.
- Add only required dependencies. Keep secrets in local or CI secret storage.
- Add a permission rationale flow and a central feature-availability model.
- Implement the initial app shell, navigation, local persistence, logging policy, and test setup.
- Add CI or a repeatable local command for lint, unit tests, and debug assembly.
- Build one static UI slice from the approved Figma screen. Keep Antigravity's files isolated from Copilot's project and domain changes.

**Deliverables**

- Project skeleton and dependency record.
- Debug build that installs on the itel P55.
- Unit-test and lint baseline.
- One navigable shell with permission-denied and empty states.

**Gate:** A clean checkout builds and tests without proprietary iPulse binaries, credentials, or extracted artwork.

### Phase 4: Connect the ISW-023 and ingest supported data

**Lead:** GitHub Copilot CLI. **Test fixtures:** Freebuff CLI. **Review:** Claude Chat.

**Work**

- Implement pairing and connection status through an authorized SDK or documented protocol.
- Support only the ISW-023 model in the first release. Reject unknown models with a clear unsupported-device state.
- Handle Bluetooth permission denial, Bluetooth-off state, disconnect, reconnect, and app process restart.
- Ingest only metrics confirmed in P1. Preserve source, timestamp, units, and device identity with each record.
- Store data locally and expose sync freshness and missing-data states in the UI.
- Add fake device data for tests. Do not use fabricated readings as live health data.

**Deliverables**

- Pairing and connection module.
- Metric adapters for confirmed ISW-023 data.
- Local storage and migration tests.
- Hardware test record with phone model, watch firmware, steps, and observed results.

**Gate:** Pair, disconnect, reconnect, sync, and database persistence pass on the physical phone and watch. Unsupported metrics remain hidden or labeled unavailable.

### Phase 5: Build fitness history and activity tracking

**Lead:** GitHub Copilot CLI. **Design:** Antigravity CLI for approved history views.

**Work**

- Display daily steps and confirmed health metrics with timestamps, units, and source labels.
- Add workout start, pause, resume, stop, and discard flows.
- Record phone GPS only after the user grants location access and starts a workout.
- Handle approximate location, GPS loss, background restrictions, and interrupted sessions.
- Calculate distance and pace from recorded points. Document filtering and known accuracy limits.
- Add activity history, workout detail, and export only if the P2 scope includes it.
- Test duplicate packets, out-of-order timestamps, empty days, timezone changes, and app restarts.

**Deliverables**

- Health history screens and activity record model.
- GPS workout service with visible recording status.
- Replayable location tests and history UI tests.
- Optional GPX export if approved in P2.

**Gate:** A test route produces a saved record, survives process restart, and presents gaps or GPS uncertainty instead of inventing data.

### Phase 6: Add confirmed watch customization and phone controls

**Lead:** GitHub Copilot CLI. **UI:** Antigravity CLI from Figma designs. **Review:** Gemini.

**Work**

- Implement only the ISW-023 dial controls confirmed in P1. Start with a preview and supported background or center customization.
- Validate image dimensions, format, storage requirements, transfer progress, cancellation, and recovery after a dropped connection.
- Add notification app selection and watch notification settings. Explain notification access before Android opens system settings.
- Add alarms and DND controls only where the watch and Android expose them. Request exact-alarm or DND access in context.
- Let Android manage the Buds Neo 3 pairing. Use public media APIs for phone playback controls if Android permits them.
- Keep each capability behind a feature flag until its device acceptance test passes.

**Deliverables**

- Dial editor and tested transfer flow, if supported.
- Notification configuration and privacy controls.
- Alarm, DND, and media controls limited to confirmed APIs.
- Feature support table shown in the app or its help page.

**Gate:** No UI reports success until the corresponding watch or phone confirms the change. Verify every enabled control on the itel P55 and ISW-023.

### Phase 7: Add assistant and app handoffs

**Lead:** GitHub Copilot CLI. **Integration review:** Freebuff CLI. **Privacy review:** Claude Chat.

**Work**

- Open Gemini or another assistant through an installed app's supported Android intent. Do not claim direct Gemini model access unless an authorized API and key are configured.
- Control YouTube Music only through supported Android media-session or app-link behavior.
- Add weather through a documented provider with a clear location and network choice.
- Use Android dialer, SMS, and app-link intents for phone, messages, and WhatsApp. Require user confirmation before placing a call, sending a message, or sharing data.
- Define task actions as a short allowlist of explicit intents. Do not create a hidden automation layer that bypasses Android permission or confirmation screens.
- Keep each integration optional and provide a useful fallback when the target app is missing or the provider is offline.

**Deliverables**

- Integration support matrix with API and permission references.
- User-visible intent flows and failure states.
- Tests for unavailable apps, denied permissions, offline provider, and canceled user actions.

**Gate:** Every integration uses a public API, shows the user what will happen, and passes review for data minimization.

### Phase 8: Verify, harden, and prepare a release

**Lead:** Gemini AI Studio. **Implementation and tests:** Copilot CLI. **Independent review:** Claude Chat.

**Work**

- Run unit, integration, UI, migration, and hardware tests for the accepted feature set.
- Test on the itel P55 with the ISW-023 and Buds Neo 3. Record OS version, watch firmware, permissions, and test conditions.
- Test background behavior, battery use, reconnection, notification privacy, data deletion, and account logout if accounts exist.
- Review exported Android components, permissions, network transport, log redaction, and secret storage.
- Verify that the app handles missing SDKs, network services, and unsupported watch capabilities without crashing.
- Produce a release build using ECHO's own signing key and identifier. Do not attempt to sign as iPulse or Transsion.
- Keep unresolved defects visible in a release note. Do not label the result "Google Fit parity" or "Strava parity".

**Deliverables**

- Phase verification report with command output and hardware evidence.
- Privacy and permissions checklist.
- Release build, installation instructions, and known-limitations list.

**Gate:** Release only when all required MVP checks pass and no critical privacy, data-loss, or device-connection defect remains open.

## 14.7 Feature verification matrix

Use this matrix as the minimum product acceptance checklist. Gemini may add tests, but must not remove a required check without recording the decision.

| Feature | Minimum check |
| :--- | :--- |
| Pairing | The app pairs with the ISW-023, shows the connected model, and reports Bluetooth-off or permission-denied states correctly. |
| Health history | A confirmed metric appears with source, unit, timestamp, and a visible no-data state. |
| Activity recording | A user-started GPS workout can pause, resume, stop, and persist after app restart. |
| Dial customization | The app previews the selected change and reports transfer success or failure based on the watch response. |
| Notifications | The user can grant or revoke access and choose enabled apps without exposing message content by default. |
| Alarms and DND | The app shows the required Android access and confirms the resulting state instead of assuming it changed. |
| Earbuds and media | Android pairs the Buds Neo 3 independently; supported playback controls do not depend on undocumented watch commands. |
| Assistant and tasks | Each action opens a supported public app flow and waits for user confirmation where the action has side effects. |
| Data handling | The user can inspect and delete locally stored ECHO data. Sensitive values do not appear in logs or test reports. |

## 14.8 Evidence and implementation references

Use these parts as starting points for investigation, not as implementation instructions:

- Part 4 describes iPulse's module boundaries and Flutter shell.
- Part 6 describes the iPulse Room schema and ContentProvider. ECHO should define its own schema rather than copy the iPulse database identity hash.
- Part 7 lists iPulse wearable adapters and device capabilities. Confirm the ISW-023 behavior and SDK rights before implementation.
- Part 8 lists observed cloud URLs. Do not call these endpoints without authorization or assume the inferred endpoints are valid contracts.
- Part 9 lists iPulse Flutter channels and routes. ECHO should define new interfaces for its own UI.
- Part 10 records iPulse screen and theme observations. Figma should create original ECHO assets and screens.
- Part 12 records asset licensing concerns. Use original or licensed material.

The source analysis reports 49 device models in some sections and 47 SPI registrations in others. Verify the source inventory before using either count as a project target. The ECHO first release supports one watch only.
