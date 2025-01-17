package com.example.colorlinkgame

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.colorlinkgame.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var currentLevel = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtons()

        binding.gameView.setLevelCompleteListener {
            onLevelComplete()
        }

        binding.gameView.post {
            startLevel(currentLevel)
        }
    }

    private fun setupButtons() {
        // Exit button setup
        binding.exitButton.setOnClickListener {
            showExitConfirmationDialog()
        }

        // Next level button setup
        binding.nextLevelButton.setOnClickListener {
            currentLevel++
            binding.nextLevelButton.visibility = View.GONE
            binding.gameView.resetLevel()
            startLevel(currentLevel)
        }
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Exit Game")
            .setMessage("Are you sure you want to exit the game?")
            .setPositiveButton("Yes") { _, _ ->
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun onLevelComplete() {
        // Show the next level button
        binding.nextLevelButton.visibility = View.VISIBLE

        // Optional: Show a congratulations message
        AlertDialog.Builder(this)
            .setTitle("Level Complete!")
            .setMessage("Congratulations! You've completed level $currentLevel")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun startLevel(level: Int) {
        // Configure level-specific settings
        // This method can be expanded to load different dot configurations
        binding.gameView.setupLevel(level)
    }

    override fun onBackPressed() {
        // Override back button to show exit confirmation
        showExitConfirmationDialog()
    }
}