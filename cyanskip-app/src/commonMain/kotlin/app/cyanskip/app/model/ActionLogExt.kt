package app.cyanskip.app.model

import app.cyanskip.app.app.ActivityNames.getShowActivityId
import app.cyanskip.app.time.format
import app.cyanskip.db.ActionLog

val ActionLog.showActivityId: String?
    get() = getShowActivityId(appId, activityId)

val ActionLog.date: String
    get() = ctime.format("MM-dd HH:mm:ss SSS")
