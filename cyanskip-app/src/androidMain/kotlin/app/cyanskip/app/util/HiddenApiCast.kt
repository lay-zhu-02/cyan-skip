@file:Suppress("CAST_NEVER_SUCCEEDS")
package app.cyanskip.app.util
import android.content.pm.PackageInfo
import android.content.pm.PackageInfoHidden
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfoHidden
inline val AccessibilityNodeInfo.toHidden get() = this as AccessibilityNodeInfoHidden
inline val PackageInfo.toHidden get() = this as PackageInfoHidden
