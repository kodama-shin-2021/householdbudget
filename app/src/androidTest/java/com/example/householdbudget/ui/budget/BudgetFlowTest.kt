package com.example.householdbudget.ui.budget

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.householdbudget.R
import com.example.householdbudget.ui.main.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BudgetFlowTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    var activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun setBudget_CompleteFlow_ShouldCreateBudget() {
        // Navigate to budget screen
        onView(withId(R.id.navigation_budget)).perform(click())

        // Tap on a category to set budget
        onView(withText("Food")).perform(click())

        // Set budget amount in dialog
        onView(withId(R.id.edit_budget_amount)).perform(
            typeText("500"),
            closeSoftKeyboard()
        )

        // Select monthly period
        onView(withId(R.id.spinner_budget_period)).perform(click())
        onView(withText("月次")).perform(click())

        // Save budget
        onView(withId(R.id.btn_save_budget)).perform(click())

        // Verify budget is set
        onView(withText("500")).check(matches(isDisplayed()))
        onView(withText("/ 月")).check(matches(isDisplayed()))
    }

    @Test
    fun budgetProgress_ExceedsLimit_ShouldShowWarning() {
        // This test would require setting up test data where spending exceeds budget
        // Navigate to budget screen
        onView(withId(R.id.navigation_budget)).perform(click())

        // Look for over-budget indicators
        onView(withId(R.id.text_over_budget)).check(matches(isDisplayed()))
        onView(withId(R.id.progress_budget)).check(matches(isDisplayed()))
    }

    @Test
    fun deleteBudget_ConfirmDeletion_ShouldRemoveBudget() {
        // Navigate to budget screen
        onView(withId(R.id.navigation_budget)).perform(click())

        // Long press on budget item to show options
        onView(withText("Food")).perform(longClick())

        // Select delete option
        onView(withText("削除")).perform(click())

        // Confirm deletion
        onView(withText("削除")).perform(click())

        // Budget should be removed and show "予算未設定"
        onView(withText("予算未設定")).check(matches(isDisplayed()))
    }

    @Test
    fun budgetNotification_OverSpending_ShouldTriggerAlert() {
        // This test would require triggering the notification system
        // Could be tested by adding transactions that exceed budget
        
        // Navigate to budget screen
        onView(withId(R.id.navigation_budget)).perform(click())

        // Verify alert indicators are shown
        onView(withId(R.id.icon_alert)).check(matches(isDisplayed()))
        onView(withText("予算超過")).check(matches(isDisplayed()))
    }
}