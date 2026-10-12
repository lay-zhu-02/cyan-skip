package app.cyanskip.app.network

actual suspend fun isNetworkAvailable(): Boolean = canReachNetwork()
