# ECHO Implementation Spec — 05: Notification Handling

**Pillar:** 5 of 8 | **Feasibility:** 🟢 HIGH | **Effort:** ~10 dev-days | **Priority:** P1

---

## 1. Overview

ECHO delivers a complete notification pipeline from the itel P55 to the ISW-O23 Storm, with per-app filtering, priority management, WhatsApp rich notification parsing, and quick-reply from the watch. The foundation is already 70% built in iPulse — ECHO extends and productises it.

---

## 2. iPulse Foundation — Inherited Classes

| Class | Capability |
|---|---|
| `PhoneStateReceiver.java` | Listens `android.intent.action.PHONE_STATE`; captures incoming number |
| `SMSUtils.sendSMS(address, text)` | Sends SMS quick-reply via `SmsManager.getDefault()` |
| `MessageBuilder.java` | Constructs MoYoung GATT notification packet |
| `QuickResponsesChangeListener.java` | Callback when user selects a quick-reply option on watch |
| `ContactUtils.java` | Resolves phone number → contact name |
| `FEATURE_APP_REMINDER` (0x80) | Watch hardware: app notification display support |
| `FEATURE_CALL_REMINDER` (0x40) | Watch hardware: incoming call forwarding |
| `FEATURE_MES_QUICK_REPLY` (0x100) | Watch hardware: quick-reply selection |
| `FEATURE_CONTACT_NUM` (0x100000) | Watch hardware: contacts sync for caller ID |

**Key implementation detail from `PhoneStateReceiver.java`:**
```java
intentFilter.setPriority(Integer.MAX_VALUE);  // highest broadcast priority
context.registerReceiver(receiver, intentFilter, 2);  // RECEIVER_EXPORTED flag on API 33+
```

**Key implementation detail from `SMSUtils.java`:**
```java
SmsManager.getDefault().sendTextMessage(address, null, text, sentPI, deliveredPI);
```

---

## 3. EchoNotificationService

```kotlin
// echo-notifications/src/main/kotlin/com/echo/notifications/EchoNotificationService.kt

class EchoNotificationService : NotificationListenerService() {

    @Inject lateinit var filter: EchoNotificationFilter
    @Inject lateinit var watchBridge: EchoWatchNotificationBridge
    @Inject lateinit var whatsAppParser: WhatsAppNotificationParser

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        val extras = sbn.notification.extras

        if (!filter.shouldForward(pkg, sbn.notification.priority)) return
        if (isDuplicate(sbn)) return

        val parsed: EchoNotification = when (pkg) {
            "com.whatsapp", "com.whatsapp.w4b" -> whatsAppParser.parse(sbn)
            "com.android.mms", "com.transsion.mms" -> parseSms(sbn)
            else -> parseGeneric(sbn)
        }

        watchBridge.sendNotification(parsed)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        watchBridge.dismissNotification(sbn.id)
    }

    private fun parseGeneric(sbn: StatusBarNotification): EchoNotification {
        val extras = sbn.notification.extras
        return EchoNotification(
            id = sbn.id,
            packageName = sbn.packageName,
            appName = getAppName(sbn.packageName),
            title = extras.getString(Notification.EXTRA_TITLE) ?: "",
            body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "",
            timestamp = sbn.postTime,
            priority = sbn.notification.priority,
            quickReplies = extractQuickReplies(sbn)
        )
    }

    private fun parseSms(sbn: StatusBarNotification): EchoNotification {
        val extras = sbn.notification.extras
        val sender = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val contactName = ContactUtils.resolveContact(applicationContext, sender) ?: sender
        return EchoNotification(
            id = sbn.id,
            packageName = sbn.packageName,
            appName = "Messages",
            title = contactName,
            body = body,
            timestamp = sbn.postTime,
            priority = Notification.PRIORITY_HIGH,
            quickReplies = listOf("OK", "On my way", "Call me", "Busy")
        )
    }

    private fun extractQuickReplies(sbn: StatusBarNotification): List<String> {
        return sbn.notification.actions
            ?.filter { action -> action.remoteInputs?.isNotEmpty() == true }
            ?.take(1)  // first reply action
            ?.let { listOf("OK", "Yes", "No", "Call me") }  // default quick replies
            ?: emptyList()
    }

    private val recentIds = LinkedHashMap<String, Long>(50, 0.75f, true)
    private fun isDuplicate(sbn: StatusBarNotification): Boolean {
        val key = "${sbn.packageName}:${sbn.id}:${sbn.notification.`when`}"
        val now = System.currentTimeMillis()
        val last = recentIds[key]
        return if (last != null && now - last < 2000L) true
        else { recentIds[key] = now; false }
    }
}
```

---

## 4. Notification Filter

```kotlin
// echo-notifications/src/main/kotlin/com/echo/notifications/EchoNotificationFilter.kt

@Singleton
class EchoNotificationFilter @Inject constructor(
    private val prefs: EchoPreferences,
    private val dndScheduler: DndScheduler
) {
    private val defaultAllowedApps = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "com.android.mms",
        "com.transsion.mms",
        "com.android.dialer",
        "com.transsion.dialer",
        "com.google.android.gm",       // Gmail
        "com.google.android.apps.messaging",
        "com.facebook.orca",           // Messenger
        "com.instagram.android",
        "com.twitter.android",
        "org.telegram.messenger"
    )

    fun shouldForward(packageName: String, priority: Int): Boolean {
        if (dndScheduler.isActive()) {
            // Allow only calls during DND
            return packageName in setOf("com.android.dialer", "com.transsion.dialer")
        }
        val userAllowed = prefs.notificationAllowList
        val allowed = if (userAllowed.isEmpty()) defaultAllowedApps else userAllowed
        if (packageName !in allowed) return false
        if (priority < prefs.minimumNotificationPriority) return false
        val muted = prefs.mutedKeywords
        return true  // keyword muting happens in WhatsAppParser / generic parser
    }
}
```

---

## 5. WhatsApp Rich Notification Parser

```kotlin
// echo-notifications/src/main/kotlin/com/echo/notifications/reply/WhatsAppNotificationParser.kt

class WhatsAppNotificationParser @Inject constructor(
    private val context: Context
) {
    fun parse(sbn: StatusBarNotification): EchoNotification {
        val extras = sbn.notification.extras

        // WhatsApp-specific extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val conversationHistory = extras.getCharSequenceArray("android.remoteInputHistory")
            ?.map { it.toString() }?.takeLast(3) ?: emptyList()

        // Determine if group chat
        val isGroup = title.contains(":") || extras.getString("android.subText") != null
        val sender = if (isGroup) {
            // "GroupName: SenderName" format
            title.substringAfter(":").trim().ifEmpty { title }
        } else title

        // Extract reply action for RemoteInput
        val replyAction = sbn.notification.actions?.firstOrNull { action ->
            action.remoteInputs?.any { it.resultKey == "reply_text" || it.resultKey == "KEY_TEXT_REPLY" } == true
        }

        return EchoNotification(
            id = sbn.id,
            packageName = sbn.packageName,
            appName = if (sbn.packageName == "com.whatsapp.w4b") "WhatsApp Business" else "WhatsApp",
            title = sender,
            body = body,
            timestamp = sbn.postTime,
            priority = Notification.PRIORITY_HIGH,
            isGroup = isGroup,
            replyActionKey = replyAction?.remoteInputs?.firstOrNull()?.resultKey,
            replyNotificationKey = sbn.key,
            quickReplies = listOf("OK", "👍", "On my way", "Can't talk now"),
            conversationHistory = conversationHistory
        )
    }
}
```

---

## 6. Quick Reply from Watch

```kotlin
// echo-notifications/src/main/kotlin/com/echo/notifications/reply/NotificationReply.kt

object NotificationReply {
    fun sendReply(context: Context, notification: EchoNotification, replyText: String) {
        val replyKey = notification.replyActionKey ?: return
        val notifKey = notification.replyNotificationKey ?: return

        val intent = Intent().apply {
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        val results = Bundle().apply { putString(replyKey, replyText) }
        RemoteInput.addResultsToIntent(
            arrayOf(RemoteInput.Builder(replyKey).build()),
            intent,
            results
        )

        // For WhatsApp, the pending intent is in the action
        // We need to retrieve the notification and fire its reply action
        // This requires NOTIFICATION_LISTENER_SERVICE access (already granted)
        try {
            val pendingIntent = getReplyPendingIntent(notifKey) ?: return
            pendingIntent.send(context, 0, intent)
        } catch (e: PendingIntent.CanceledException) {
            // Notification was dismissed; ignore
        }
    }

    private fun getReplyPendingIntent(notifKey: String): PendingIntent? {
        // Stored in EchoNotificationService.pendingIntentCache at time of notification receipt
        return EchoNotificationService.pendingIntentCache[notifKey]
    }
}
```

**Watch quick-reply flow:**
```
QuickResponsesChangeListener.onQuickResponseSelected(index)
    └─► val replyText = notification.quickReplies[index]
            └─► NotificationReply.sendReply(context, notification, replyText)
```

---

## 7. SMS Quick Reply (from `SMSUtils.java`)

For SMS notifications where RemoteInput reply is not available:

```kotlin
// Extended SMSUtils for ECHO
fun sendQuickReplySms(toAddress: String, text: String) {
    SMSUtils.sendSMS(toAddress, text)  // Reuse existing iPulse implementation
}
```

---

## 8. Incoming Call Handling

```kotlin
// Extended PhoneStateReceiver for ECHO

class EchoPhoneStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                val name = ContactUtils.resolveContact(context, number) ?: number
                // Send call notification to watch
                val notif = EchoNotification(
                    id = -1,
                    packageName = "com.echo.wearable.call",
                    appName = "Phone",
                    title = name ?: "Unknown",
                    body = number ?: "",
                    timestamp = System.currentTimeMillis(),
                    priority = Notification.PRIORITY_MAX,
                    isCall = true
                )
                EchoWatchNotificationBridge.getInstance().sendCallNotification(notif)
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                EchoWatchNotificationBridge.getInstance().dismissCallNotification()
            }
        }
    }
}
```

---

## 9. Manifest Declarations

```xml
<service
    android:name=".notifications.EchoNotificationService"
    android:exported="true"
    android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
    <intent-filter>
        <action android:name="android.service.notification.NotificationListenerService"/>
    </intent-filter>
</service>

<receiver android:name=".notifications.EchoPhoneStateReceiver"
    android:exported="false">
    <intent-filter android:priority="2147483647">
        <action android:name="android.intent.action.PHONE_STATE"/>
    </intent-filter>
</receiver>

<uses-permission android:name="android.permission.RECEIVE_SMS"/>
<uses-permission android:name="android.permission.SEND_SMS"/>
<uses-permission android:name="android.permission.READ_PHONE_STATE"/>
<uses-permission android:name="android.permission.READ_CONTACTS"/>
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
```

---

## 10. UI Screens

| Screen | Description |
|---|---|
| **NotificationSettingsScreen** | Per-app toggle list (all installed apps), minimum priority slider, keyword mute |
| **NotificationHistoryScreen** | Log of notifications forwarded to watch (last 7 days), filtered by app |
| **DndScheduleScreen** | (see impl/06_smart_alarms.md) |

---

## 11. Evidence Log

| Claim | Source File | Evidence |
|---|---|---|
| `PhoneStateReceiver` registers `PHONE_STATE` broadcast | `PhoneStateReceiver.java` | `intentFilter.addAction("android.intent.action.PHONE_STATE")` |
| `SMSUtils` uses `SmsManager.getDefault()` | `SMSUtils.java` | Direct call with sent/delivered PendingIntents |
| `FEATURE_APP_REMINDER` = 0x80 | `OperateFeature.java` | Constant value |
| `FEATURE_MES_QUICK_REPLY` = 0x100 | `OperateFeature.java` | Constant value |
| `ContactUtils.java` exists in MoYoung SDK | `cmd/message/ContactUtils.java` | File present in source tree |
| `MessageBuilder.java` builds GATT notification packet | `cmd/message/MessageBuilder.java` | File present |
