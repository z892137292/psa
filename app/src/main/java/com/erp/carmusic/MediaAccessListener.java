package com.erp.carmusic;

import android.service.notification.NotificationListenerService;

/** Permission gateway for MediaSessionManager.getActiveSessions; no notification content is read. */
public class MediaAccessListener extends NotificationListenerService {
    @Override public void onListenerConnected() {
        super.onListenerConnected();
        MediaDiagnosticLog.record(this, "ACCESS", "通知访问已连接，仅用于查询媒体会话，不读取通知内容");
    }
}
