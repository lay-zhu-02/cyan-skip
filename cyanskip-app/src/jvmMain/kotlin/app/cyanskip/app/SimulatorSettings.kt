package app.cyanskip.app

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

val defaultSimulatedDevice = SimulatedDevice()

/** The only simulator snapshot. DesktopEnvironment projects inputs for window and page rendering. */
@Serializable
data class SimulatorSettings(
    val device: SimulatedDevice = defaultSimulatedDevice,
    val permissions: SimulatedPermissions = SimulatedPermissions(),
    val services: SimulatedServices = SimulatedServices(),
    val prompts: SimulatedPrompts = SimulatedPrompts(),
) {
    val networkAvailable: Boolean get() = device.wifi || device.mobileSignal > 0

    fun environment() = DesktopEnvironment(
        width = device.width, height = device.height, density = device.density,
        fontScale = device.fontScale, dark = device.dark, locale = device.locale,
        android = AndroidWindow(
            statusBarHeight = device.statusBarHeight,
            navigationBarHeight = device.navigationBarHeight,
            statusBarVisible = device.statusBarVisible,
            navigationBarVisible = device.navigationBarVisible,
            gestureHandleVisible = device.gestureHandleVisible,
            cutoutWidth = device.cutoutWidth,
            cutoutHeight = device.cutoutHeight,
            batteryPercent = device.batteryPercent,
            charging = device.charging,
            wifi = device.wifi,
            mobileSignal = device.mobileSignal,
            imeVisible = device.imeVisible,
            imeHeight = device.imeHeight,
            topAppId = device.topAppId,
            localNetworkGranted = permissions.localNetworkGranted,
            restricted = permissions.restricted,
            ignoreBatteryOptimizations = permissions.ignoreBatteryOptimizations,
            serviceEnabled = services.serviceEnabled,
            a11yEnabled = services.a11yEnabled,
            activityRunning = services.activityRunning,
            restrictedWarning = prompts.restrictedWarning,
        ),
    )

    fun withEnvironment(value: DesktopEnvironment): SimulatorSettings {
        value.validate()
        val a = value.android
        return copy(
            device = SimulatedDevice(
                value.width,
                value.height,
                value.density,
                value.fontScale,
                value.dark,
                value.locale,
                a.statusBarHeight,
                a.navigationBarHeight,
                a.statusBarVisible,
                a.navigationBarVisible,
                a.gestureHandleVisible,
                a.cutoutWidth,
                a.cutoutHeight,
                a.batteryPercent,
                a.charging,
                a.wifi,
                a.mobileSignal,
                a.imeVisible,
                a.imeHeight,
                a.topAppId
            ),
            permissions = permissions.copy(
                restricted = a.restricted,
                localNetworkGranted = a.localNetworkGranted,
                ignoreBatteryOptimizations = a.ignoreBatteryOptimizations,
            ),
            services = services.copy(a11yEnabled = a.a11yEnabled),
            prompts = SimulatedPrompts(a.restrictedWarning),
        )
    }

    fun persistent() = copy(
        device = device.copy(imeVisible = false, batteryPercent = defaultSimulatedDevice.batteryPercent),
        services = services.persistent(),
    )

    fun validate() {
        environment().validate()
        require(services.plans.keys == SimulatedService.entries.toSet() - SimulatedService.Http) { "Invalid service plans" }
        require(services.runtime.keys == SimulatedService.entries.toSet()) { "Missing service states" }
    }
}

@Serializable
data class SimulatedDevice(
    val width: Int = 412,
    val height: Int = 820,
    val density: Float = 1f,
    val fontScale: Float = 1f,
    val dark: Boolean = false,
    val locale: String = "zh-CN",
    val statusBarHeight: Float = 24f,
    val navigationBarHeight: Float = 24f,
    val statusBarVisible: Boolean = true,
    val navigationBarVisible: Boolean = true,
    val gestureHandleVisible: Boolean = true,
    val cutoutWidth: Float = 0f,
    val cutoutHeight: Float = 0f,
    val batteryPercent: Int = 100,
    val charging: Boolean = false,
    val wifi: Boolean = true,
    val mobileSignal: Int = 4,
    val imeVisible: Boolean = false,
    val imeHeight: Float = 280f,
    val topAppId: String = "",
)

@Serializable
data class SimulatedPermissions(
    val notificationGranted: Boolean = true, val overlayGranted: Boolean = true,
    val canQueryPackages: Boolean = true, val queryPackagesAbnormal: Boolean = false,
    val restricted: Boolean = false, val localNetworkGranted: Boolean = true,
    val ignoreBatteryOptimizations: Boolean = false,
)

@Serializable
data class SimulatedPrompts(
    val restrictedWarning: Boolean = false,
)
