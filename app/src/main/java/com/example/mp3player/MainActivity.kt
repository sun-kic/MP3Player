package com.example.mp3player

import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.view.WindowManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.navigation.NavigationView
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.mp3player.databinding.ActivityMainBinding
import com.example.mp3player.data.FileScanner
import com.example.mp3player.data.MediaFile
import com.example.mp3player.player.MediaPlayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    
    // Save original margins to restore when exiting fullscreen
    private var originalLeftMargin = 0
    private var originalRightMargin = 0
    private var isFullscreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)
        
        // Keep screen on for car use
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Hide FAB - not needed for media player
        binding.appBarMain.fab?.visibility = View.GONE
        
        // Save original margins from FragmentContainerView
        val navHostContainerView = findViewById<View>(R.id.nav_host_fragment_content_main)
        (navHostContainerView?.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
            originalLeftMargin = it.leftMargin
            originalRightMargin = it.rightMargin
        }
        
        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment?)!!
        
        val navController = navHostFragment.navController

        // Find NavigationView from anywhere in the view hierarchy
        // This works for both w600dp (drawer) and w1240dp (permanent sidebar)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        
        if (navView != null) {
            // NavigationView exists - set up navigation
            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.nav_file_browser, R.id.nav_media_player, R.id.nav_now_playing
                ),
                binding.drawerLayout  // null on w1240dp (no drawer), DrawerLayout on w600dp
            )
            setupActionBarWithNavController(navController, appBarConfiguration)
            navView.setupWithNavController(navController)
            
            // Also add manual listener as failsafe
            navView.setNavigationItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.nav_file_browser -> {
                        // If currently playing a video, navigate to its folder
                        val playerManager = MediaPlayerManager.getInstance(this)
                        val currentFolder = playerManager.getCurrentFolder()
                        val bundle = if (currentFolder != null) {
                            bundleOf("folder_path" to currentFolder.absolutePath)
                        } else {
                            bundleOf()
                        }
                        navController.navigate(R.id.nav_file_browser, bundle)
                    }
                    R.id.nav_media_player -> navController.navigate(R.id.nav_media_player)
                    R.id.nav_now_playing -> navController.navigate(R.id.nav_now_playing)
                }
                // Close drawer if it exists
                binding.drawerLayout?.closeDrawers()
                true
            }
        }

        // Setup for default layout BottomNavigationView (if it exists)
        binding.appBarMain.contentMain.bottomNavView?.let { bottomNav ->
            if (navView == null) {
                // Only set appBarConfiguration if not already set by NavigationView above
                appBarConfiguration = AppBarConfiguration(
                    setOf(
                        R.id.nav_file_browser, R.id.nav_media_player, R.id.nav_now_playing
                    )
                )
                setupActionBarWithNavController(navController, appBarConfiguration)
            }
            bottomNav.setOnItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.nav_file_browser -> {
                        // If currently playing a video, navigate to its folder
                        val playerManager = MediaPlayerManager.getInstance(this)
                        val currentFolder = playerManager.getCurrentFolder()
                        val bundle = if (currentFolder != null) {
                            bundleOf("folder_path" to currentFolder.absolutePath)
                        } else {
                            bundleOf()
                        }
                        navController.navigate(R.id.nav_file_browser, bundle)
                        true
                    }
                    R.id.nav_media_player -> {
                        navController.navigate(R.id.nav_media_player)
                        true
                    }
                    R.id.nav_now_playing -> {
                        navController.navigate(R.id.nav_now_playing)
                        true
                    }
                    else -> false
                }
            }
        }
        
        // Restore last playback state if available (on fresh start, not on rotation)
        if (savedInstanceState == null) {
            restoreLastPlaybackState(navController)
        }
    }
    
    /**
     * Restore last playback state from preferences
     */
    private fun restoreLastPlaybackState(navController: NavController) {
        val playerManager = MediaPlayerManager.getInstance(this)

        if (playerManager.hasSavedPlaybackState()) {
            val restoredState = playerManager.restoreLastPlaybackState()
            restoredState?.let { state ->
                val file: File = state.first
                val position: Long = state.second
                // Use coroutine to scan folder and load playlist in background
                CoroutineScope(Dispatchers.IO).launch {
                    val fileScanner = FileScanner(this@MainActivity)
                    val folder = file.parentFile

                    if (folder != null && folder.exists()) {
                        val allFiles = fileScanner.getMediaFilesInDirectory(folder)
                        val startIndex = allFiles.indexOfFirst { it.path == file.path }

                        if (startIndex >= 0) {
                            withContext(Dispatchers.Main) {
                                // Load playlist with restored position
                                playerManager.loadPlaylist(allFiles, startIndex, folder)
                                playerManager.seekTo(position)

                                // Navigate to player
                                navController.navigate(R.id.nav_media_player)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Enter fullscreen mode - hides toolbar, bottom navigation, NavigationView,
     * system bars, and removes fragment container margins for true edge-to-edge display.
     */
    fun enterFullscreen() {
        if (isFullscreen) return
        isFullscreen = true
        
        // Hide ActionBar
        supportActionBar?.hide()
        
        // Hide bottom navigation
        binding.appBarMain.contentMain.bottomNavView?.visibility = View.GONE
        
        // Hide the entire AppBarLayout (not just the toolbar) to reclaim its space
        val appBarLayout = binding.appBarMain.toolbar.parent as? AppBarLayout
        appBarLayout?.visibility = View.GONE
        
        // Hide NavigationView (works for both w600dp drawer and w1240dp permanent sidebar)
        findViewById<View>(R.id.nav_view)?.visibility = View.GONE
        
        // Remove ALL margins and padding from the fragment container to eliminate white bars
        val navHostView = findViewById<View>(R.id.nav_host_fragment_content_main)
        navHostView?.let { view ->
            // Clear all margins
            (view.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
                params.leftMargin = 0
                params.rightMargin = 0
                params.topMargin = 0
                params.bottomMargin = 0
                view.layoutParams = params
            }
            // Clear padding
            view.setPadding(0, 0, 0, 0)
        }
        
        // Also clear padding/margin from the content_main LinearLayout and force it to top
        binding.appBarMain.contentMain.root.setPadding(0, 0, 0, 0)
        // Force content_main to take full space by resetting its layout params
        binding.appBarMain.contentMain.root.layoutParams = CoordinatorLayout.LayoutParams(
            CoordinatorLayout.LayoutParams.MATCH_PARENT,
            CoordinatorLayout.LayoutParams.MATCH_PARENT
        )
        
        // Disable fitsSystemWindows on DrawerLayout to prevent system bar padding
        binding.drawerLayout?.fitsSystemWindows = false
        // Clear cached padding from system insets on all relevant views
        binding.drawerLayout?.setPadding(0, 0, 0, 0)
        binding.activityContainer.setPadding(0, 0, 0, 0)
        binding.activityContainer.fitsSystemWindows = false
        binding.appBarMain.coordinatorLayout.setPadding(0, 0, 0, 0)
        binding.appBarMain.coordinatorLayout.fitsSystemWindows = false
        
        // Ensure root view doesn't have system window padding
        binding.root.setPadding(0, 0, 0, 0)
        binding.root.fitsSystemWindows = false
        
        // Force immediate layout refresh to apply changes
        binding.appBarMain.coordinatorLayout.post {
            binding.appBarMain.coordinatorLayout.requestLayout()
        }
        binding.root.requestLayout()
        
        // Use FLAG_LAYOUT_NO_LIMITS to draw behind system bars completely
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        
        // Hide system bars for immersive mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    /**
     * Exit fullscreen mode - restores toolbar, bottom navigation, NavigationView,
     * system bars, and fragment container margins.
     */
    fun exitFullscreen() {
        if (!isFullscreen) return
        isFullscreen = false
        
        // Show ActionBar
        supportActionBar?.show()
        
        // Show bottom navigation
        binding.appBarMain.contentMain.bottomNavView?.visibility = View.VISIBLE
        
        // Show the AppBarLayout
        val appBarLayout = binding.appBarMain.toolbar.parent as? AppBarLayout
        appBarLayout?.visibility = View.VISIBLE
        
        // Show toolbar
        binding.appBarMain.toolbar.visibility = View.VISIBLE
        
        // Show NavigationView
        findViewById<View>(R.id.nav_view)?.visibility = View.VISIBLE
        
        // Restore original margins and padding on the fragment container
        val navHostView = findViewById<View>(R.id.nav_host_fragment_content_main)
        navHostView?.let { view ->
            (view.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
                params.leftMargin = originalLeftMargin
                params.rightMargin = originalRightMargin
                params.topMargin = 0
                params.bottomMargin = 0
                view.layoutParams = params
            }
        }
        
        // Restore fitsSystemWindows on DrawerLayout and other views
        binding.drawerLayout?.fitsSystemWindows = true
        binding.activityContainer.fitsSystemWindows = true
        binding.appBarMain.coordinatorLayout.fitsSystemWindows = true
        binding.root.fitsSystemWindows = true
        
        // Restore content_main layout params with scrolling behavior for AppBar
        val contentMainParams = CoordinatorLayout.LayoutParams(
            CoordinatorLayout.LayoutParams.MATCH_PARENT,
            CoordinatorLayout.LayoutParams.MATCH_PARENT
        ).apply {
            behavior = AppBarLayout.ScrollingViewBehavior()
        }
        binding.appBarMain.contentMain.root.layoutParams = contentMainParams
        
        // Force layout refresh
        binding.root.requestLayout()
        
        // Remove FLAG_LAYOUT_NO_LIMITS
        window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        
        // Show system bars
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, binding.root).show(WindowInsetsCompat.Type.systemBars())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val result = super.onCreateOptionsMenu(menu)
        val navView: NavigationView? = findViewById(R.id.nav_view)
        if (navView == null) {
            menuInflater.inflate(R.menu.bottom_navigation, menu)
        }
        return result
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_file_browser -> {
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                // If currently playing a video, navigate to its folder
                val playerManager = MediaPlayerManager.getInstance(this)
                val currentFolder = playerManager.getCurrentFolder()
                val bundle = if (currentFolder != null) {
                    bundleOf("folder_path" to currentFolder.absolutePath)
                } else {
                    bundleOf()
                }
                navController.navigate(R.id.nav_file_browser, bundle)
            }
            R.id.nav_media_player -> {
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.nav_media_player)
            }
            R.id.nav_now_playing -> {
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.nav_now_playing)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}