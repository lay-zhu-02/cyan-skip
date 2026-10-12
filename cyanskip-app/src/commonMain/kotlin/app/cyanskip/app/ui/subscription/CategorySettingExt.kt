package app.cyanskip.app.ui.subscription

import androidx.compose.ui.graphics.vector.ImageVector
import app.cyanskip.app.rule.CategorySetting
import app.cyanskip.app.ui.component.CsIcons

val CategorySetting.icon: ImageVector
    get() = when (this) {
        CategorySetting.FollowSubscription -> CsIcons.StackedDocuments
        CategorySetting.Enabled -> CsIcons.CheckCircle
        CategorySetting.Disabled -> CsIcons.RemoveCircleOutline
        CategorySetting.GroupDefault -> CsIcons.FlashOn
    }
