package us.zoom.livesdkdemo

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import us.zoom.livesdkdemo.databinding.ActivityStartBinding

class StartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnScenario1.setOnClickListener {
            val intent = Intent(this, MainKotlinActivity::class.java).apply { putExtra(MainKotlinActivity.ARG_URL, "https://zoom.us") }
            startActivity(intent)
        }
        binding.btnScenario2.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
    }
}