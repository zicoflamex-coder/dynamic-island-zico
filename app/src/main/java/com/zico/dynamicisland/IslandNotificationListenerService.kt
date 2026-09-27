package com.zico.dynamicisland

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class IslandNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // فعلا کاری با نوتیفیکیشن‌های عادی نداریم؛
        // وجود همین سرویس فقط برای گرفتن اطلاعات موزیک (MediaSession) لازمه.
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    }
}
