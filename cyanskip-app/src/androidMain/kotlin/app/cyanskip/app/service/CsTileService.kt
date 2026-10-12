package app.cyanskip.app.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.Settings

class CsTileService : BaseTileService() {
    override val activeFlow = A11yService.isRunning
    override fun onTileClick() {
        val service = A11yService.instance
        if (service != null) {
            service.disableSelf()
        } else {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= 34) {
                startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }
    }
}
