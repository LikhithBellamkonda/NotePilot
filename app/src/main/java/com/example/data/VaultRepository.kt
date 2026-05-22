package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class VaultRepository(private val vaultDao: VaultDao) {
    val allItems: Flow<List<VaultItem>> = vaultDao.getAllItems()

    suspend fun insertItem(item: VaultItem): Long {
        return vaultDao.insertItem(item)
    }

    suspend fun updateItem(item: VaultItem) {
        vaultDao.updateItem(item)
    }

    suspend fun deleteItemById(id: Int) {
        vaultDao.deleteItemById(id)
    }

    suspend fun getItemById(id: Int): VaultItem? {
        return vaultDao.getItemById(id)
    }

    suspend fun prePopulateIfNeeded() {
        // Database is clean by default. No hardcoded default content seeded.
    }
}
