package app.cyanskip.app.model

import app.cyanskip.app.time.format
import app.cyanskip.db.Snapshot

val Snapshot.date: String get() = id.format("MM-dd HH:mm:ss")
