package us.zoom.livesdkdemo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import us.zoom.livesdkdemo.databinding.ActivityHomeBinding

class HomeActivity: AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        binding.btnCase1.setOnClickListener {
            val intent = Intent(this, MainKotlinActivity::class.java).apply { putExtra(MainKotlinActivity.ARG_URL, Constants.URL_CASE1) }
            startActivity(intent)
        }
        binding.btnCase2.setOnClickListener {
            val intent = Intent(this, MainKotlinActivity::class.java).apply { putExtra(MainKotlinActivity.ARG_URL, Constants.URL_CASE2) }
            startActivity(intent)
        }
        binding.btnCase3.setOnClickListener {
            val intent = Intent(this, MainKotlinActivity::class.java).apply { putExtra(MainKotlinActivity.ARG_URL, Constants.URL_CASE3) }
            startActivity(intent)
        }
        binding.btnCase4.setOnClickListener {
            val intent = Intent(this, MainJavaActivity::class.java).apply {
                putExtra(MainJavaActivity.ARG_URL, Constants.URL_CASE4)
                putExtra(MainJavaActivity.ARG_OPEN_URL_IN_SYSTEM_BROWSER, true)
                putExtra(MainJavaActivity.ARG_USE_JS_URL_HANDLER, true)
            }
            startActivity(intent)
        }
        binding.btnCase4New.setOnClickListener {
            val intent = Intent(this, MainKotlinActivity::class.java).apply {
                putExtra(MainKotlinActivity.ARG_URL, Constants.URL_CASE4)
                putExtra(MainKotlinActivity.ARG_OPEN_URL_IN_SYSTEM_BROWSER, true)
                putExtra(MainKotlinActivity.ARG_USE_JS_URL_HANDLER, false)
            }
            startActivity(intent)
        }
        binding.btnCase5.setOnClickListener {
            val intent = Intent(this, MainKotlinActivity::class.java).apply {
                putExtra(MainKotlinActivity.ARG_URL, Constants.URL_CASE5)
                putExtra(MainKotlinActivity.ARG_OPEN_URL_IN_SYSTEM_BROWSER, false)
            }
            startActivity(intent)
        }
        binding.btnCase6.setOnClickListener {
            val intent = Intent(this, MainKotlinActivity::class.java).apply { putExtra(MainKotlinActivity.ARG_URL, Constants.URL_CASE6) }
            startActivity(intent)
        }
    }

}