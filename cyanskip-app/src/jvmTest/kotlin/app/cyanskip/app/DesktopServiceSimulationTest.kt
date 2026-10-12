package app.cyanskip.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import app.cyanskip.app.testing.TestFiles
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopServiceSimulationTest {
    @Test
    fun lifecycleFeedbackWaitsForConnectionAndDoesNotRepeatForNoOpCommands() {
        val store = SimulatorStore()
        val changes = mutableListOf<Pair<SimulatedService, Boolean>>()
        store.onServiceRunningChanged = { service, running -> changes += service to running }
        val simulation = DesktopServiceSimulation(store, { error("HTTP is exercised through its real server") })
        val service = SimulatedService.Screenshot
        simulation.configure(service, ServiceSimulationPlan(ServiceStartResult.Manual, ServiceAuthorization.Manual))
        simulation.setEnabled(service, true)
        val attempt = store.settings.value.services.state(service).attempt
        simulation.event(ServiceEvent(service, ServiceEventType.Authorized, attempt))
        assertTrue(changes.isEmpty())
        simulation.event(ServiceEvent(service, ServiceEventType.Connected, attempt))
        simulation.setEnabled(service, true)
        simulation.setEnabled(service, false)
        simulation.setEnabled(service, false)
        assertEquals(listOf(service to true, service to false), changes)
    }

    @Test
    fun oldConnectionCannotResurrectStoppedOrRestartedService() {
        val store = SimulatorStore()
        val simulation = DesktopServiceSimulation(store, { error("HTTP is exercised through its real server") })
        val service = SimulatedService.Activity
        simulation.configure(service, ServiceSimulationPlan(startResult = ServiceStartResult.Manual))
        simulation.setEnabled(service, true)
        val first = store.settings.value.services.state(service)
        assertEquals(ServicePhase.Starting, first.phase)
        simulation.setEnabled(service, false)
        simulation.event(ServiceEvent(service, ServiceEventType.Connected, first.attempt))
        assertEquals(ServicePhase.Stopped, store.settings.value.services.state(service).phase)
        simulation.setEnabled(service, true)
        val second = store.settings.value.services.state(service)
        simulation.event(ServiceEvent(service, ServiceEventType.Connected, first.attempt))
        assertEquals(second, store.settings.value.services.state(service))
        simulation.event(ServiceEvent(service, ServiceEventType.Connected, second.attempt))
        assertTrue(store.settings.value.environment().android.activityRunning)
        simulation.close()
        simulation.event(ServiceEvent(service, ServiceEventType.Connected, second.attempt))
        assertFalse(store.settings.value.environment().android.activityRunning)
    }

    @Test
    fun screenshotCancellationIsNotFailureAndLateAuthorizationIsIgnored() {
        val store = SimulatorStore()
        val simulation = DesktopServiceSimulation(store, { error("HTTP is exercised through its real server") })
        val service = SimulatedService.Screenshot
        simulation.setEnabled(service, true)
        val pending = store.settings.value.services.state(service)
        assertEquals(ServicePhase.AwaitingAuthorization, pending.phase)
        simulation.event(ServiceEvent(service, ServiceEventType.Cancelled, pending.attempt))
        simulation.event(ServiceEvent(service, ServiceEventType.Authorized, pending.attempt))
        assertEquals(ServicePhase.Stopped, store.settings.value.services.state(service).phase)
        assertNull(store.settings.value.services.state(service).failure)
        simulation.setEnabled(service, true)
        simulation.event(ServiceEvent(service, ServiceEventType.Authorized, store.settings.value.services.state(service).attempt))
        assertTrue(store.settings.value.services.running(service))
    }

    @Test
    fun deniedStartCanBeRetriedAfterPermissionAndStopReleasesRunningService() {
        val store = SimulatorStore(SimulatorSettings(permissions = SimulatedPermissions(notificationGranted = false)))
        val simulation = DesktopServiceSimulation(store, { error("Unexpected HTTP command") })
        simulation.setEnabled(SimulatedService.Button, true)
        assertEquals(ServicePhase.Failed, store.settings.value.services.state(SimulatedService.Button).phase)
        store.update { it.copy(permissions = it.permissions.copy(notificationGranted = true)) }
        simulation.setEnabled(SimulatedService.Button, true)
        assertTrue(store.settings.value.services.running(SimulatedService.Button))
        store.update { it.copy(permissions = it.permissions.copy(notificationGranted = false)) }
        simulation.setEnabled(SimulatedService.Button, false)
        assertFalse(store.settings.value.services.running(SimulatedService.Button))
    }

    @Test
    fun failureRetainsCauseAndCanBeRetriedWithoutChangingUnrelatedServices() {
        val store = SimulatorStore()
        val simulation = DesktopServiceSimulation(store, { error("HTTP is exercised through its real server") })
        simulation.setEnabled(SimulatedService.Activity, true)
        simulation.configure(SimulatedService.Event, ServiceSimulationPlan(ServiceStartResult.Failure, failure = "Injected startup failure"))
        simulation.setEnabled(SimulatedService.Event, true)
        assertEquals("Injected startup failure", store.settings.value.services.state(SimulatedService.Event).failure)
        assertTrue(store.settings.value.services.activityRunning)
        simulation.configure(SimulatedService.Event, ServiceSimulationPlan())
        simulation.setEnabled(SimulatedService.Event, true)
        assertTrue(store.settings.value.services.running(SimulatedService.Event))
        assertNull(store.settings.value.services.state(SimulatedService.Event).failure)
        // The same failure event covers an interrupted connection and a running service crash.
        val attempt = store.settings.value.services.state(SimulatedService.Event).attempt
        simulation.event(ServiceEvent(SimulatedService.Event, ServiceEventType.Failed, attempt, "Connection lost"))
        val failed = store.settings.value.services.state(SimulatedService.Event)
        assertEquals(ServicePhase.Failed, failed.phase)
        assertEquals("Connection lost", failed.failure)
        assertTrue(store.settings.value.services.activityRunning)
        simulation.event(ServiceEvent(SimulatedService.Event, ServiceEventType.Connected, attempt))
        assertEquals(failed, store.settings.value.services.state(SimulatedService.Event))
    }



    @Test
    fun disablingAccessibilitySystemSettingStopsPendingConnection() {
        val store = SimulatorStore()
        val simulation = DesktopServiceSimulation(store, { error("HTTP is exercised through its real server") })
        simulation.configure(SimulatedService.Accessibility, ServiceSimulationPlan(ServiceStartResult.Manual))
        simulation.setEnabled(SimulatedService.Accessibility, true)
        val attempt = store.settings.value.services.state(SimulatedService.Accessibility).attempt
        assertTrue(store.settings.value.services.a11yEnabled)
        store.patch(Json.parseToJsonElement("""{"services":{"a11yEnabled":false}}""").jsonObject)
        simulation.event(ServiceEvent(SimulatedService.Accessibility, ServiceEventType.Connected, attempt))
        assertEquals(ServicePhase.Stopped, store.settings.value.services.state(SimulatedService.Accessibility).phase)
        assertFalse(store.settings.value.services.a11yEnabled)
    }

    @Test
    fun persistenceRestoresPlansButNotLiveServicesAndConfigurationEditsPreserveRuntime() {
        val file = TestFiles.root.resolve("service-simulation/simulator.json")
        val storage = DesktopSimulatorStorage(file)
        val store = SimulatorStore(SimulatorSettings(), storage::save)
        val simulation = DesktopServiceSimulation(store, { error("HTTP is exercised through its real server") })
        simulation.configure(SimulatedService.Event, ServiceSimulationPlan(ServiceStartResult.Manual))
        simulation.setEnabled(SimulatedService.Activity, true)
        store.patch(Json.parseToJsonElement("""{"device":{"width":420}}""").jsonObject)
        store.replace(store.settings.value.copy(device = store.settings.value.device.copy(width = 430)))
        assertTrue(store.settings.value.services.activityRunning)
        assertFailsWith<IllegalArgumentException> {
            store.patch(Json.parseToJsonElement("""{"services":{"runtime":{}}}""").jsonObject)
        }
        val restored = storage.load(DesktopEnvironment())
        assertEquals(ServiceStartResult.Manual, restored.services.plans.getValue(SimulatedService.Event).startResult)
        assertFalse(restored.services.activityRunning)
        assertTrue(restored.services.logs.isEmpty())
    }
}
