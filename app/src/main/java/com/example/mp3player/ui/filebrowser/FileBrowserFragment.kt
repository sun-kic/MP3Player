package com.example.mp3player.ui.filebrowser

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mp3player.R
import com.example.mp3player.data.FileScanner
import com.example.mp3player.data.MediaFile
import com.example.mp3player.data.MediaFolder
import com.example.mp3player.databinding.FragmentFileBrowserBinding
import com.example.mp3player.player.MediaPlayerManager
import java.io.File
import java.util.Stack

/**
 * Fragment for browsing folders and media files
 */
class FileBrowserFragment : Fragment() {

    private var _binding: FragmentFileBrowserBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: FileBrowserAdapter
    private lateinit var fileScanner: FileScanner
    
    private var currentDirectory: File? = null
    private val navigationStack = Stack<File>()
    private var lastNavigatedFolderPath: String? = null
    


    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            showStorageRoots()
        } else {
            showPermissionDenied()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFileBrowserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize fileScanner with context for storage detection
        fileScanner = FileScanner(requireContext())
        
        setupRecyclerView()
        setupClickListeners()
        checkPermissionsAndLoad()
    }

    override fun onResume() {
        super.onResume()
        // Recheck permissions when returning from settings (for MANAGE_EXTERNAL_STORAGE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && _binding != null) {
            if (Environment.isExternalStorageManager()) {
                showStorageRoots()
            }
        }
        
        // Check if we need to navigate to a specific folder (e.g., from video playback)
        // This needs to be in onResume because fragment might already exist in back stack
        arguments?.getString("folder_path")?.let { folderPath ->
            if (folderPath.isNotEmpty() && 
                folderPath != lastNavigatedFolderPath && 
                hasStoragePermission()) {
                val targetFolder = File(folderPath)
                if (targetFolder.exists() && targetFolder.isDirectory) {
                    // Track that we've processed this navigation
                    lastNavigatedFolderPath = folderPath
                    // Only navigate if we're not already in that folder
                    if (currentDirectory?.absolutePath != targetFolder.absolutePath) {
                        navigationStack.clear()
                        navigateToFolder(targetFolder)
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = FileBrowserAdapter(
            onFolderClick = { folder -> navigateToFolder(folder.file) },
            onFileClick = { file -> playFile(file) }
        )
        
        binding.recyclerFiles.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFiles.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { navigateBack() }
        binding.btnRefresh.setOnClickListener { refreshCurrentDirectory() }
        binding.btnPermission.setOnClickListener { requestPermissions() }
    }

    private fun checkPermissionsAndLoad() {
        if (hasStoragePermission()) {
            // Check if there's a specific folder to navigate to
            val targetFolderPath = arguments?.getString("folder_path")
            if (targetFolderPath?.isNotEmpty() == true) {
                val targetFolder = File(targetFolderPath)
                if (targetFolder.exists() && targetFolder.isDirectory) {
                    navigateToFolder(targetFolder)
                } else {
                    showStorageRoots()
                }
            } else {
                showStorageRoots()
            }
        } else {
            showPermissionRequired()
        }
    }

    private fun hasStoragePermission(): Boolean {
        // For Android 11+ (R), we need MANAGE_EXTERNAL_STORAGE for full SD card access
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        // For Android 11+ (R), redirect to system settings for MANAGE_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:" + requireContext().packageName)
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback if the specific intent is not available
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        } else {
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            permissionLauncher.launch(permissions)
        }
    }

    private fun showPermissionRequired() {
        binding.recyclerFiles.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
        binding.btnPermission.visibility = View.VISIBLE
    }

    private fun showPermissionDenied() {
        Toast.makeText(
            requireContext(),
            R.string.permission_denied,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showStorageRoots() {
        binding.btnPermission.visibility = View.GONE
        binding.recyclerFiles.visibility = View.VISIBLE
        
        currentDirectory = null
        navigationStack.clear()
        
        binding.tvCurrentPath.text = getString(R.string.storage_roots)
        
        // Get storage roots
        val roots = fileScanner.getStorageRoots()
        val folders = roots.mapNotNull { MediaFolder.fromFile(it) }
        
        if (folders.isEmpty()) {
            // Fallback to external storage
            val externalStorage = Environment.getExternalStorageDirectory()
            navigateToFolder(externalStorage)
        } else {
            adapter.setItems(folders, emptyList())
            showEmptyStateIfNeeded(folders.isNotEmpty())
        }
    }

    private fun navigateToFolder(folder: File) {
        currentDirectory?.let { navigationStack.push(it) }
        currentDirectory = folder
        
        binding.tvCurrentPath.text = folder.absolutePath
        
        val result = fileScanner.scanDirectory(folder)
        adapter.setItems(result.folders, result.files)
        
        showEmptyStateIfNeeded(result.folders.isNotEmpty() || result.files.isNotEmpty())
        
        // Scroll to top
        binding.recyclerFiles.scrollToPosition(0)
    }

    private fun navigateBack() {
        if (navigationStack.isNotEmpty()) {
            val previousFolder = navigationStack.pop()
            currentDirectory = previousFolder
            
            binding.tvCurrentPath.text = previousFolder.absolutePath
            
            val result = fileScanner.scanDirectory(previousFolder)
            adapter.setItems(result.folders, result.files)
            showEmptyStateIfNeeded(result.folders.isNotEmpty() || result.files.isNotEmpty())
        } else if (currentDirectory != null) {
            showStorageRoots()
        }
    }

    private fun refreshCurrentDirectory() {
        currentDirectory?.let { folder ->
            val result = fileScanner.scanDirectory(folder)
            adapter.setItems(result.folders, result.files)
            showEmptyStateIfNeeded(result.folders.isNotEmpty() || result.files.isNotEmpty())
        } ?: showStorageRoots()
    }

    private fun showEmptyStateIfNeeded(hasItems: Boolean) {
        binding.recyclerFiles.visibility = if (hasItems) View.VISIBLE else View.GONE
        binding.tvEmpty.visibility = if (hasItems) View.GONE else View.VISIBLE
    }

    private fun playFile(file: MediaFile) {
        // Get all media files in current folder for playlist
        currentDirectory?.let { folder ->
            val allFiles = fileScanner.getMediaFilesInDirectory(folder)
            val startIndex = allFiles.indexOfFirst { it.path == file.path }
            
            // Load playlist and start playback
            val playerManager = MediaPlayerManager.getInstance(requireContext())
            playerManager.loadPlaylist(allFiles, startIndex.coerceAtLeast(0), folder)
            
            // Navigate to player
            findNavController().navigate(R.id.nav_media_player)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
