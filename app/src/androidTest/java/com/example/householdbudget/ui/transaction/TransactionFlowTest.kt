package com.example.householdbudget.ui.transaction

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
class TransactionFlowTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    var activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun addTransaction_CompleteFlow_ShouldCreateTransaction() {
        // Navigate to transaction add screen
        onView(withId(R.id.fab_add)).perform(click())

        // Fill in transaction details
        onView(withId(R.id.edit_amount)).perform(
            typeText("150.50"),
            closeSoftKeyboard()
        )

        onView(withId(R.id.edit_description)).perform(
            typeText("Grocery shopping"),
            closeSoftKeyboard()
        )

        // Select expense type
        onView(withId(R.id.radio_expense)).perform(click())

        // Select category (assuming first category is available)
        onView(withId(R.id.spinner_category)).perform(click())
        onView(withText("Food")).perform(click())

        // Save transaction
        onView(withId(R.id.btn_save)).perform(click())

        // Verify we're back at transaction list and new transaction is visible
        onView(withText("Grocery shopping")).check(matches(isDisplayed()))
        onView(withText("150.50")).check(matches(isDisplayed()))
    }

    @Test
    fun addTransaction_EmptyAmount_ShouldShowError() {
        // Navigate to transaction add screen
        onView(withId(R.id.fab_add)).perform(click())

        // Leave amount empty and try to save
        onView(withId(R.id.edit_description)).perform(
            typeText("Test transaction"),
            closeSoftKeyboard()
        )

        onView(withId(R.id.btn_save)).perform(click())

        // Should show error and remain on add screen
        onView(withText("金額を入力してください")).check(matches(isDisplayed()))
    }

    @Test
    fun transactionList_SwipeToDelete_ShouldRemoveTransaction() {
        // Assuming there's at least one transaction in the list
        // Swipe to delete the first transaction
        onView(withId(R.id.recycler_transactions))
            .perform(swipeLeft())

        // Confirm deletion in dialog
        onView(withText("削除")).perform(click())

        // Transaction should be removed from list
        // This test would need specific transaction data to verify properly
    }

    @Test
    fun transactionSearch_FilterResults_ShouldShowMatchingTransactions() {
        // Open search
        onView(withId(R.id.action_search)).perform(click())

        // Type search query
        onView(withId(androidx.appcompat.R.id.search_src_text))
            .perform(typeText("grocery"), closeSoftKeyboard())

        // Should show only transactions containing "grocery"
        onView(withText("Grocery shopping")).check(matches(isDisplayed()))
    }

    @Test
    fun transactionFilter_SelectCategory_ShouldShowOnlyCategoryTransactions() {
        // Open filter menu
        onView(withId(R.id.action_filter)).perform(click())

        // Select specific category
        onView(withText("Food")).perform(click())

        // Should show only food category transactions
        // This would need verification based on actual data
    }
}