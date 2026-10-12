package app.cyanskip.app.subscription

import com.sun.net.httpserver.HttpServer
import io.ktor.client.plugins.ResponseException
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import app.cyanskip.db.Db

/** Reject HTTP errors before JSON parsing or persistence; the same URL can be retried. */
suspend fun assertSubscriptionDownloadStatus() {
    val id = 87654330L
    val status = AtomicInteger(503)
    val body = AtomicReference("{id:$id,name:'Retry subscription',version:1,apps:[]}")
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/cyanskip.json5") { exchange ->
        val bytes = body.get().toByteArray()
        exchange.responseHeaders.add("Content-Type", "application/json5; charset=utf-8")
        exchange.sendResponseHeaders(status.get(), bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
        exchange.close()
    }
    server.start()
    val url = "http://127.0.0.1:${server.address.port}/cyanskip.json5"
    try {
        for (code in listOf(403, 503)) {
            status.set(code)
            val result = assertIs<SubscriptionResult.Failure>(SubscriptionRepository.addOrModifyRemote(url))
            assertEquals(SubscriptionResult.FailureReason.Download, result.reason)
            assertEquals(code, assertIs<ResponseException>(result.cause).response.status.value)
            assertFalse(Db.subsItemDao.queryAll().any { it.id == id })
        }
        status.set(200)
        body.set("<html>Not a subscription</html>")
        val malformed = assertIs<SubscriptionResult.Failure>(SubscriptionRepository.addOrModifyRemote(url))
        assertEquals(SubscriptionResult.FailureReason.Parse, malformed.reason)
        body.set("{id:$id,name:'Retry subscription',version:1,apps:[]}")
        assertEquals(SubscriptionResult.Success(SubscriptionResult.SuccessKind.Added),
            SubscriptionRepository.addOrModifyRemote(url))
        assertEquals(url, Db.subsItemDao.queryAll().single { it.id == id }.updateUrl)
        assertEquals(1, SubscriptionRepository.awaitSnapshot().subscriptions.getValue(id).version)
    } finally {
        server.stop(0)
        SubscriptionRepository.delete(id)
    }
}
