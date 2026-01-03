package net.yalab.andromitron.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "connection_logs",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["domain"]),
        Index(value = ["action"]),
        Index(value = ["protocol"])
    ]
)
data class ConnectionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    
    @ColumnInfo(name = "domain")
    val domain: String?,
    
    @ColumnInfo(name = "source_ip")
    val sourceIp: String,
    
    @ColumnInfo(name = "destination_ip")
    val destinationIp: String,
    
    @ColumnInfo(name = "source_port")
    val sourcePort: Int?,
    
    @ColumnInfo(name = "destination_port")
    val destinationPort: Int?,
    
    @ColumnInfo(name = "protocol")
    val protocol: String,
    
    @ColumnInfo(name = "action")
    val action: String,
    
    @ColumnInfo(name = "bytes_sent")
    val bytesSent: Long = 0,
    
    @ColumnInfo(name = "bytes_received")
    val bytesReceived: Long = 0
)