package com.farywave.memehive.ui.activity

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.farywave.memehive.ui.activity.main.MainActivity
import com.farywave.memehive.ui.activity.picker.PickerActivity
import org.junit.Rule
import org.junit.Test

class MainActivityRecreationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun searchSurvivesActivityRecreation() {
        assertSearchSurvivesRecreation(compose)
    }
}

class PickerActivityRecreationTest {
    @get:Rule val compose = createAndroidComposeRule<PickerActivity>()

    @Test
    fun searchSurvivesActivityRecreation() {
        assertSearchSurvivesRecreation(compose)
    }
}

private fun <A : ComponentActivity> assertSearchSurvivesRecreation(
    compose: AndroidComposeTestRule<ActivityScenarioRule<A>, A>,
) {
    compose.onNode(hasSetTextAction()).performTextReplacement("recreation-test-query")
    Espresso.closeSoftKeyboard()
    compose.activityRule.scenario.recreate()
    compose.waitForIdle()
    compose.onNode(hasSetTextAction()).assertTextEquals("recreation-test-query")
}
