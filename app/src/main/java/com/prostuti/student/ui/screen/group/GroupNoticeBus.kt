package com.prostuti.student.ui.screen.group

import com.prostuti.student.ui.screen.group.component.GroupNotice
import java.util.concurrent.ConcurrentHashMap

/**
 * Hands a confirmation from a screen that just closed (e.g. the routine builder) to the group
 * screen it returns to, which shows it once in a snackbar. App-wide `single`.
 */
class GroupNoticeBus {
    private val pending = ConcurrentHashMap<String, GroupNotice>()

    fun post(groupId: String, notice: GroupNotice) {
        pending[groupId] = notice
    }

    /** The waiting notice for [groupId], removed so it shows only once. */
    fun take(groupId: String): GroupNotice? = pending.remove(groupId)
}
