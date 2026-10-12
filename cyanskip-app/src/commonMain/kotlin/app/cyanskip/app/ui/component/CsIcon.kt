package app.cyanskip.app.ui.component

import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.action_add
import app.cyanskip.app.resources.action_back
import app.cyanskip.app.resources.action_close
import app.cyanskip.app.resources.action_delete
import app.cyanskip.app.resources.action_edit
import app.cyanskip.app.resources.action_share
import app.cyanskip.app.resources.action_turn_on
import app.cyanskip.app.resources.help_title
import app.cyanskip.app.resources.history_title
import app.cyanskip.app.resources.more_actions
import app.cyanskip.app.resources.open_new_page
import app.cyanskip.app.resources.settings_title
import app.cyanskip.app.resources.sort_filter
import app.cyanskip.app.resources.text_copy
import app.cyanskip.app.ui.icon.AndroidHead
import app.cyanskip.app.ui.icon.LinkDiagonal as SharedLinkDiagonal
import li.songe.morph.compose.AnimatedMorphIcon
import org.jetbrains.compose.resources.stringResource
import app.cyanskip.app.ui.icon.CollapseContent as SharedCollapseContent
import app.cyanskip.app.ui.icon.ExpandContent as SharedExpandContent
import app.cyanskip.app.ui.icon.FlashOff as SharedFlashOff
import app.cyanskip.app.ui.icon.FlashOn as SharedFlashOn
import app.cyanskip.app.ui.icon.Flowchart as SharedFlowchart
import app.cyanskip.app.ui.icon.PageInfo as SharedPageInfo
import app.cyanskip.app.ui.icon.StackedDocuments as SharedStackedDocuments

@Composable
fun CsIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = getIconDefaultDesc(imageVector),
    animateMorph: Boolean = false,
) {
    if (animateMorph) {
        AnimatedMorphIcon(
            imageVector = imageVector,
            modifier = modifier,
            contentDescription = contentDescription,
            tint = tint,
            animationSpec = tween(300),
        )
    } else {
        Icon(
            imageVector = imageVector,
            modifier = modifier,
            contentDescription = contentDescription,
            tint = tint,
        )
    }
}

@Composable
fun CsIconButton(
    imageVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    contentDescription: String? = getIconDefaultDesc(imageVector),
    onClickLabel: String? = null,
    animateMorph: Boolean = false,
) = CsTooltipIconButtonBox(
    contentDescription = contentDescription,
) {
    IconButton(
        modifier = modifier.semantics {
            if (onClickLabel != null) {
                this.onClick(label = onClickLabel, action = null)
            }
        },
        enabled = enabled,
        onClick = onClick,
        colors = colors,
    ) {
        CsIcon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            animateMorph = animateMorph,
        )
    }
}

@Composable
fun getIconDefaultDesc(imageVector: ImageVector): String? = when (imageVector) {
    CsIcons.Add -> stringResource(Res.string.action_add)
    CsIcons.Edit -> stringResource(Res.string.action_edit)
    CsIcons.Delete -> stringResource(Res.string.action_delete)
    CsIcons.Share -> stringResource(Res.string.action_share)
    CsIcons.Settings -> stringResource(Res.string.settings_title)
    CsIcons.Close -> stringResource(Res.string.action_close)
    CsIcons.ArrowBack -> stringResource(Res.string.action_back)
    CsIcons.HelpOutline -> stringResource(Res.string.help_title)
    CsIcons.ToggleOff -> stringResource(Res.string.action_close)
    CsIcons.ToggleOn -> stringResource(Res.string.action_turn_on)
    CsIcons.History -> stringResource(Res.string.history_title)
    CsIcons.Sort -> stringResource(Res.string.sort_filter)
    CsIcons.OpenInNew -> stringResource(Res.string.open_new_page)
    CsIcons.ContentCopy -> stringResource(Res.string.text_copy)
    CsIcons.MoreVert -> stringResource(Res.string.more_actions)
    else -> null
}

object CsIcons {
    val PlayArrow get() = Icons.Filled.PlayArrow
    val Link get() = Icons.Outlined.Link
    val LinkDiagonal get() = SharedLinkDiagonal
    val CheckCircle get() = Icons.Outlined.CheckCircle
    val RemoveCircleOutline get() = Icons.Outlined.RemoveCircleOutline
    val StackedDocuments get() = SharedStackedDocuments
    val PageInfo get() = SharedPageInfo
    val FlashOn get() = SharedFlashOn
    val FlashOff get() = SharedFlashOff
    val Block get() = Icons.Default.Block
    val History get() = Icons.Default.History
    val Schedule get() = Icons.Outlined.Schedule
    val CollapseContent get() = SharedCollapseContent
    val ExpandContent get() = SharedExpandContent
    val Sort get() = Icons.AutoMirrored.Filled.Sort
    val Add get() = Icons.Outlined.Add
    val KeyboardArrowRight get() = Icons.AutoMirrored.Filled.KeyboardArrowRight
    val ContentCopy get() = Icons.Outlined.ContentCopy
    val MoreVert get() = Icons.Default.MoreVert
    val ArrowBack get() = Icons.AutoMirrored.Filled.ArrowBack
    val Android get() = AndroidHead
    val Edit get() = Icons.Outlined.Edit
    val Share get() = Icons.Default.Share
    val Delete get() = Icons.Outlined.Delete
    val Close get() = Icons.Default.Close
    val OpenInNew get() = Icons.AutoMirrored.Outlined.OpenInNew
    val Settings get() = Icons.Outlined.Settings
    val Home get() = Icons.Outlined.Home
    val FormatListBulleted get() = Icons.AutoMirrored.Filled.FormatListBulleted
    val CalendarMonth get() = Icons.Outlined.CalendarMonth
    val Apps get() = Icons.Default.Apps
    val Info get() = Icons.Outlined.Info
    val Flowchart get() = SharedFlowchart
    val ToggleOff get() = Icons.Outlined.ToggleOff
    val ToggleOn get() = Icons.Outlined.ToggleOn
    val HelpOutline get() = Icons.AutoMirrored.Outlined.HelpOutline
    val ArrowForward get() = Icons.AutoMirrored.Filled.ArrowForward
    val Image get() = Icons.Outlined.Image
    val AddPhotoAlternate get() = Icons.Outlined.AddPhotoAlternate
    val WarningAmber get() = Icons.Default.WarningAmber
    val RocketLaunch get() = Icons.Outlined.RocketLaunch
    val CenterFocusWeak get() = Icons.Default.CenterFocusWeak
    val AutoMode get() = Icons.Outlined.AutoMode
    val BrightnessAuto get() = Icons.Outlined.BrightnessAuto
    val LightMode get() = Icons.Outlined.LightMode
    val DarkMode get() = Icons.Outlined.DarkMode
    val VerifiedUser get() = Icons.Outlined.VerifiedUser
    val Autorenew get() = Icons.Default.Autorenew
    val UnfoldMore get() = Icons.Default.UnfoldMore
    val ExpandLess get() = Icons.Default.ExpandLess
    val ExpandMore get() = Icons.Default.ExpandMore
    val Memory get() = Icons.Default.Memory
    val Notifications get() = Icons.Outlined.Notifications
    val Layers get() = Icons.Outlined.Layers
    val Lock get() = Icons.Outlined.Lock
    val Title get() = Icons.Outlined.Title
    val TextFields get() = Icons.Outlined.TextFields
    val ArrowDownward get() = Icons.Outlined.ArrowDownward
    val Check get() = Icons.Outlined.Check
}
