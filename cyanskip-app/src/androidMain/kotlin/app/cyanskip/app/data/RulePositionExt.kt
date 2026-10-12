package app.cyanskip.app.data

import android.graphics.Rect
import app.cyanskip.app.subscription.RawSubscription
import app.cyanskip.app.subscription.RuleBounds
import app.cyanskip.app.util.LogUtils
import app.cyanskip.app.util.ScreenUtils
import app.cyanskip.app.util.ToastUtils

fun RawSubscription.Position.calc(rect: Rect): Pair<Float, Float>? = calc(
    RuleBounds(rect.left, rect.top, rect.right, rect.bottom),
    ScreenUtils.getWidth(), ScreenUtils.getHeight(),
) { error ->
    LogUtils.d("Position.calc", error)
    ToastUtils.show(error.message ?: error.stackTraceToString())
}
