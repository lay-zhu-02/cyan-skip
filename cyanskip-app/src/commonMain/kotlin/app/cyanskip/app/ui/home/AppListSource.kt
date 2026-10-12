package app.cyanskip.app.ui.home

import app.cyanskip.app.model.AppInfo
import app.cyanskip.app.state.Loadable
import app.cyanskip.app.ui.component.CsRuleStatsData

data class AppListSource(
    val apps: List<AppInfo>,
    val ruleStats: Map<String, CsRuleStatsData>,
    val actionOrder: List<String>,
    val visitOrder: Map<String, Int>,
    val canQueryPackages: Boolean = true,
    val queryPackagesAbnormal: Boolean = false,
    val refreshing: Boolean = false,
    val actionOrderState: Loadable<Unit> = Loadable.Ready(Unit),
    val visitOrderState: Loadable<Unit> = Loadable.Ready(Unit),
    val ruleStatsFailed: Boolean = false,
)

