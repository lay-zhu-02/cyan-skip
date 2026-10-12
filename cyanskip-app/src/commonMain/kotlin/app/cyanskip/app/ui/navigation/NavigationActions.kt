package app.cyanskip.app.ui.navigation

import app.cyanskip.app.ui.share.DeletionTarget

typealias ConfirmDeletion = (String, String, () -> Set<DeletionTarget>, () -> Unit, suspend () -> Unit) -> Unit
