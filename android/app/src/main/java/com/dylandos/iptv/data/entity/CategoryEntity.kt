package com.dylandos.iptv.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A provider category grouping channels. [isUserVisible] allows the user to hide
 * unwanted categories from the main UI ("Manage Categories" screen toggles this).
 */
@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["providerAccountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["providerAccountId", "categoryName"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val providerAccountId: Long,
    val categoryId: String,
    val categoryName: String,
    val parentId: Long? = null,
    val type: String = "live",          // "live", "movie", "series"
    val isUserVisible: Boolean = true,
    val sortOrder: Int = 0
)
