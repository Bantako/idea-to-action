package org.mrlem.composesample.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [NodeEntity::class, EdgeEntity::class, ThemeEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(AppDatabase.Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun nodeDao(): NodeDao
    abstract fun edgeDao(): EdgeDao
    abstract fun themeDao(): ThemeDao

    class Converters {

        @TypeConverter
        fun fromNodeStatus(value: NodeStatus): String = value.name

        @TypeConverter
        fun toNodeStatus(value: String): NodeStatus = NodeStatus.valueOf(value)

        @TypeConverter
        fun fromEdgeType(value: EdgeType): String = value.name

        @TypeConverter
        fun toEdgeType(value: String): EdgeType = EdgeType.valueOf(value)
    }

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS themes (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "color TEXT NOT NULL DEFAULT '#607D8B', " +
                        "createdAt INTEGER NOT NULL" +
                        ")"
                )
                db.execSQL("ALTER TABLE nodes ADD COLUMN themeId INTEGER")
            }
        }
    }
}
