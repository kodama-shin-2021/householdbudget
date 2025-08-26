package com.example.householdbudget.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.householdbudget.data.entity.Notification
import com.example.householdbudget.data.entity.NotificationType
import java.util.Date

@Dao
interface NotificationDao {
    
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun getAllNotifications(): LiveData<List<Notification>>
    
    @Query("SELECT * FROM notifications WHERE id = :id")
    suspend fun getNotificationById(id: Long): Notification?
    
    @Query("SELECT * FROM notifications WHERE isRead = 0 ORDER BY createdAt DESC")
    fun getUnreadNotifications(): LiveData<List<Notification>>
    
    @Query("SELECT * FROM notifications WHERE isRead = 1 ORDER BY readAt DESC")
    fun getReadNotifications(): LiveData<List<Notification>>
    
    @Query("SELECT * FROM notifications WHERE type = :type ORDER BY createdAt DESC")
    fun getNotificationsByType(type: NotificationType): LiveData<List<Notification>>
    
    @Query("SELECT * FROM notifications WHERE scheduledAt <= :currentDate AND isRead = 0")
    suspend fun getScheduledNotifications(currentDate: Date): List<Notification>
    
    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadNotificationCount(): LiveData<Int>
    
    @Insert
    suspend fun insertNotification(notification: Notification): Long
    
    @Insert
    suspend fun insertNotifications(notifications: List<Notification>)
    
    @Update
    suspend fun updateNotification(notification: Notification)
    
    @Delete
    suspend fun deleteNotification(notification: Notification)
    
    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: Long)
    
    @Query("UPDATE notifications SET isRead = 1, readAt = :readAt WHERE id = :id")
    suspend fun markNotificationAsRead(id: Long, readAt: Date)
    
    @Query("UPDATE notifications SET isRead = 1, readAt = :readAt WHERE isRead = 0")
    suspend fun markAllNotificationsAsRead(readAt: Date)
    
    @Query("DELETE FROM notifications WHERE isRead = 1 AND readAt < :beforeDate")
    suspend fun deleteOldReadNotifications(beforeDate: Date)
}