package app.cyanskip.app.network

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.CallFailed
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.origin
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.request.uri
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import app.cyanskip.app.model.ActionResult
import app.cyanskip.app.model.ComplexSnapshot
import app.cyanskip.app.model.CsAction
import app.cyanskip.app.model.RpcError
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.screenshot_not_found
import app.cyanskip.app.resources.snapshot_delete_success
import app.cyanskip.app.resources.snapshot_missing_or_deleted
import app.cyanskip.app.resources.snapshot_not_found
import app.cyanskip.app.resources.subscription_memory
import app.cyanskip.app.snapshot.SnapshotStore
import app.cyanskip.app.subscription.RawSubscription
import app.cyanskip.app.subscription.SubscriptionRepository
import app.cyanskip.app.util.LogUtils
import app.cyanskip.db.Db
import app.cyanskip.db.LOCAL_HTTP_SUBS_ID
import app.cyanskip.db.SubsItem
import org.jetbrains.compose.resources.getString
import java.io.IOException

private val httpSubsItem = SubsItem(
    id = LOCAL_HTTP_SUBS_ID,
    order = -1,
    enableUpdate = false,
)

fun Application.configureInspectionServer(
    serverInfo: () -> ServerInfo,
    captureSnapshot: suspend () -> ComplexSnapshot,
    execSelector: suspend (CsAction) -> ActionResult,
    scriptUrl: String,
    jsonFormat: Json,
) {
    install(getKtorCorsPlugin())
    install(getKtorErrorPlugin())
    install(ContentNegotiation) { json(jsonFormat) }
    routing {
        get("/") { call.respondText(ContentType.Text.Html) { "<script type='module' src='$scriptUrl'></script>" } }
        route("/api") {
            post("/getServerInfo") { call.respond(serverInfo()) }
            post("/getSnapshot") {
                val data = call.receive<ReqId>()
                val fp = SnapshotStore.snapshotFile(data.id)
                if (!fp.exists()) {
                    throw RpcError(getString(Res.string.snapshot_not_found))
                }
                call.respondFile(fp)
            }
            post("/getScreenshot") {
                val data = call.receive<ReqId>()
                val fp = SnapshotStore.screenshotFile(data.id)
                if (!fp.exists()) {
                    throw RpcError(getString(Res.string.screenshot_not_found))
                }
                call.respondFile(fp)
            }
            post("/captureSnapshot") {
                call.respond(captureSnapshot())
            }
            post("/getSnapshots") {
                val list = Db.snapshotDao.query().first().mapNotNull {
                    try {
                        SnapshotStore.getMinSnapshot(it.id)
                    } catch (e: IOException) {
                        LogUtils.d("Cannot read snapshot ${it.id}", e)
                        null
                    } catch (e: SerializationException) {
                        LogUtils.d("Invalid snapshot ${it.id}", e)
                        null
                    }
                }
                call.respond(list)
            }
            post("/deleteSnapshot") {
                val data = call.receive<ReqId>()
                val allSnapshots = Db.snapshotDao.query().first()
                val snapshot = allSnapshots.find { it.id == data.id }
                if (snapshot != null) {
                    SnapshotStore.delete(snapshot)
                    call.respond(RpcOk(getString(Res.string.snapshot_delete_success)))
                } else {
                    throw RpcError(getString(Res.string.snapshot_missing_or_deleted))
                }
            }
            post("/updateSubscription") {
                val subscription =
                    RawSubscription.parse(call.receiveText(), json5 = false)
                        .copy(
                            id = LOCAL_HTTP_SUBS_ID,
                            name = getString(Res.string.subscription_memory),
                            version = 0,
                            author = "cyanskip"
                        )
                SubscriptionRepository.saveWithItem(subscription, httpSubsItem)
                call.respond(RpcOk())
            }
            post("/execSelector") {
                val csAction = call.receive<CsAction>()
                call.respond(execSelector(csAction))
            }
        }
    }
}

private fun getKtorCorsPlugin() = createApplicationPlugin(name = "KtorCorsPlugin") {
    onCall { call ->
        mapOf(
            HttpHeaders.AccessControlAllowOrigin to "*",
            HttpHeaders.AccessControlAllowMethods to "*",
            HttpHeaders.AccessControlAllowHeaders to "*",
            HttpHeaders.AccessControlExposeHeaders to "*",
            "Access-Control-Allow-Private-Network" to "true",
        ).forEach { (k, v) ->
            if (!call.response.headers.contains(k)) {
                call.response.header(k, v)
            }
        }
        if (call.request.httpMethod == HttpMethod.Options) {
            call.respond("all-cors-ok")
        }
    }
}

private fun getKtorErrorPlugin() =
    createApplicationPlugin(name = "KtorErrorPlugin") {
        onCall { call ->
            if (call.request.uri == "/" || call.request.uri.startsWith("/api/")) {
                LogUtils.d("onCall: ${call.request.origin.remoteAddress} -> ${call.request.uri}")
            }
        }
        on(CallFailed) { call, cause ->
            when (cause) {
                is CancellationException -> throw cause
                is RpcError -> {
                    // 主动抛出的错误
                    LogUtils.d("${call.request.uri}: ${cause.message}")
                    call.respond(cause)
                }

                is Exception -> {
                    // 未知错误
                    LogUtils.d("${call.request.uri}: ${cause.message}")
                    cause.printStackTrace()
                    call.respond(
                        RpcError(
                            message = cause.message ?: "unknown error",
                            unknown = true
                        )
                    )
                }

                else -> {
                    cause.printStackTrace()
                }
            }
        }
    }
