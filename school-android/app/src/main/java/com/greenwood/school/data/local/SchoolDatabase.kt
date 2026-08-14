package com.greenwood.school.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction

/**
 * A deliberately small local cache.
 *
 * Only two things are worth persisting on this app: the dashboard headline numbers
 * and the notice board — both are read constantly, change slowly, and are the first
 * things a user opens after launching. Everything else (student lists, fee ledgers,
 * marks) is either large, sensitive, or must be authoritative, so it is always
 * fetched live and never written to disk.
 */
@Database(
    entities = [CachedNoticeEntity::class, DashboardSnapshotEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SchoolDatabase : RoomDatabase() {
    abstract fun noticeDao(): NoticeDao
    abstract fun dashboardDao(): DashboardDao

    companion object {
        const val NAME = "greenwood-cache.db"
    }
}

@Entity(tableName = "cached_notices")
data class CachedNoticeEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val description: String,
    val targetRole: String?,
    val publishedByName: String?,
    val publishedAt: String?,
    val expiryDate: String?,
    val attachmentUrl: String?,
    /** Wall-clock millis of the write, used to show "last updated" when offline. */
    val cachedAt: Long,
)

/**
 * One row per signed-in user id. The payload is the JSON of the dashboard the
 * screen already knows how to render, so adding a widget never needs a migration.
 */
@Entity(tableName = "dashboard_snapshots")
data class DashboardSnapshotEntity(
    @PrimaryKey val userId: Long,
    val payloadJson: String,
    val cachedAt: Long,
)

@Dao
interface NoticeDao {

    @Query("SELECT * FROM cached_notices ORDER BY publishedAt DESC")
    suspend fun getAll(): List<CachedNoticeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(notices: List<CachedNoticeEntity>)

    @Query("DELETE FROM cached_notices")
    suspend fun clear()

    /** Replaces the cache wholesale so deleted notices don't linger offline. */
    @Transaction
    suspend fun replaceAll(notices: List<CachedNoticeEntity>) {
        clear()
        upsertAll(notices)
    }
}

@Dao
interface DashboardDao {

    @Query("SELECT * FROM dashboard_snapshots WHERE userId = :userId")
    suspend fun get(userId: Long): DashboardSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snapshot: DashboardSnapshotEntity)

    @Query("DELETE FROM dashboard_snapshots")
    suspend fun clear()
}
