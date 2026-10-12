package app.cyanskip.app.ui.icon

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.action_back
import app.cyanskip.app.resources.action_close
import app.cyanskip.app.ui.component.CsIcons
import app.cyanskip.app.ui.component.CsTooltipIconButtonBox
import li.songe.morph.compose.AnimatedMorphIcon
import li.songe.morph.compose.MorphOptions
import li.songe.morph.compose.MorphRotationPreference
import li.songe.morph.compose.MorphTransitionMode
import org.jetbrains.compose.resources.stringResource

@Composable
fun CsBackCloseIcon(
    backOrClose: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String = if (backOrClose) stringResource(Res.string.action_back) else stringResource(
        Res.string.action_close
    ),
    tint: Color = LocalContentColor.current
) = CsTooltipIconButtonBox(
    contentDescription = contentDescription,
) {
    AnimatedMorphIcon(
        from = CsIcons.Close,
        to = CsIcons.ArrowBack,
        targetState = backOrClose,
        modifier = modifier.size(24.dp),
        contentDescription = contentDescription,
        tint = tint,
        options = MorphOptions(
            transitionMode = MorphTransitionMode.ExperimentalStrokeInference,
            rotationPreference = MorphRotationPreference.PreferCounterClockwise,
        ),
        animationSpec = tween(300),
    )
}
