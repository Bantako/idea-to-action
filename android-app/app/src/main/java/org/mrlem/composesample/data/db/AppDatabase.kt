package org.mrlem.composesample.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(
    entities = [NodeEntity::class, EdgeEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(AppDatabase.Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun nodeDao(): NodeDao
    abstract fun edgeDao(): EdgeDao

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
}
