package com.example.mp3player.data

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Scans directories for media files and folders
 */
class FileScanner(private val context: Context) {
    
    /**
     * Get list of storage roots available for browsing
     */
    fun getStorageRoots(): List<File> {
        val roots = mutableListOf<File>()
        
        // Primary external storage
        val primaryStorage = Environment.getExternalStorageDirectory()
        if (primaryStorage.exists() && primaryStorage.canRead()) {
            roots.add(primaryStorage)
        }
        
        // Use getExternalFilesDirs to detect all mounted storage (including SD cards)
        // This returns app-specific directories on each storage volume
        // We extract the root path from each
        val externalDirs = ContextCompat.getExternalFilesDirs(context, null)
        for (dir in externalDirs) {
            if (dir == null) continue
            
            // Extract the root storage path from the app-specific path
            // e.g., /storage/XXXX-XXXX/Android/data/com.example.mp3player/files -> /storage/XXXX-XXXX
            val path = dir.absolutePath
            val storageIndex = path.indexOf("/Android/data")
            if (storageIndex > 0) {
                val storagePath = path.substring(0, storageIndex)
                val storageRoot = File(storagePath)
                if (storageRoot.exists() && storageRoot.canRead() && 
                    !roots.any { it.absolutePath == storageRoot.absolutePath }) {
                    roots.add(storageRoot)
                }
            }
        }
        
        // Additional: Check /storage for other mount points (for older devices)
        val storageDir = File("/storage")
        if (storageDir.exists() && storageDir.canRead()) {
            storageDir.listFiles()?.forEach { storage ->
                if (storage.isDirectory && storage.canRead() && 
                    storage.name != "self" && storage.name != "emulated" &&
                    !roots.any { it.absolutePath == storage.absolutePath }) {
                    roots.add(storage)
                }
            }
        }
        
        return roots
    }
    
    /**
     * Scan a directory and return folders and media files
     */
    fun scanDirectory(directory: File): DirectoryScanResult {
        val folders = mutableListOf<MediaFolder>()
        val files = mutableListOf<MediaFile>()
        
        if (!directory.exists() || !directory.canRead()) {
            return DirectoryScanResult(folders, files, false)
        }
        
        val contents = directory.listFiles() ?: return DirectoryScanResult(folders, files, false)
        
        for (item in contents.sortedBy { it.name.lowercase() }) {
            if (item.isHidden) continue
            
            if (item.isDirectory && item.canRead()) {
                MediaFolder.fromFile(item)?.let { folders.add(it) }
            } else if (item.isFile) {
                MediaFile.fromFile(item)?.let { files.add(it) }
            }
        }
        
        return DirectoryScanResult(folders, files, true)
    }
    
    /**
     * Get all media files in a directory (for playlist)
     */
    fun getMediaFilesInDirectory(directory: File): List<MediaFile> {
        if (!directory.exists() || !directory.canRead()) {
            return emptyList()
        }
        
        return directory.listFiles()
            ?.filter { it.isFile && !it.isHidden }
            ?.mapNotNull { MediaFile.fromFile(it) }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }
}

/**
 * Result of scanning a directory
 */
data class DirectoryScanResult(
    val folders: List<MediaFolder>,
    val files: List<MediaFile>,
    val success: Boolean
)
