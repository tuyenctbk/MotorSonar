package com.soloprono.motorsonar

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.soloprono.motorsonar.data.AppDatabase
import com.soloprono.motorsonar.data.EngineScanRepository
import com.soloprono.motorsonar.ui.DashboardScreen
import com.soloprono.motorsonar.ui.EngineSoundViewModel
import com.soloprono.motorsonar.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

    @get:Rule 
    val composeTestRule = createComposeRule()
    private lateinit var database: AppDatabase
    private lateinit var repository: EngineScanRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = EngineScanRepository(database.engineScanDao(), database.maintenanceActivityDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun greeting_screenshot() {
        val viewModel = EngineSoundViewModel(repository)
        composeTestRule.setContent {
            MyApplicationTheme {
                DashboardScreen(viewModel = viewModel)
            }
        }

        // Capture screenshot of the newly created EngineSound Dashboard
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    }
}
