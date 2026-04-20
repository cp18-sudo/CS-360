package com.example.weighttracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Instrumented tests for the Android-dependent persistence layer. These run
 * against a real Room database on a device or emulator from Android Studio.
 */
@RunWith(AndroidJUnit4.class)
public class AppDatabaseInstrumentedTest {

    private AppDatabase database;
    private UserDao userDao;
    private WeightDao weightDao;
    private GoalDao goalDao;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        userDao = database.userDao();
        weightDao = database.weightDao();
        goalDao = database.goalDao();
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void userDao_rejectsDuplicateUsernamesAndReturnsStoredUser() {
        long firstInsert = userDao.insertUser(new User("chris", "hashed-password"));
        long duplicateInsert = userDao.insertUser(new User("chris", "different-hash"));

        assertTrue(firstInsert > 0);
        assertEquals(-1, duplicateInsert);

        User stored = userDao.getUserByUsername("chris");
        assertNotNull(stored);
        assertEquals("hashed-password", stored.password);
        assertNull(userDao.getUserByUsername("missing-user"));
    }

    @Test
    public void weightDao_queriesAreScopedToUserAndOrderedByDate() {
        insertUsers("chris", "sam");
        weightDao.insertWeight(new WeightEntry("chris", "2026-04-01", 190.0));
        weightDao.insertWeight(new WeightEntry("chris", "2026-04-03", 188.0));
        weightDao.insertWeight(new WeightEntry("sam", "2026-04-02", 210.0));
        weightDao.insertWeight(new WeightEntry("chris", "2026-04-02", 189.0));

        List<WeightEntry> descending = weightDao.getAllWeights("chris");
        assertEquals(3, descending.size());
        assertEquals("2026-04-03", descending.get(0).getDate());
        assertEquals("2026-04-02", descending.get(1).getDate());
        assertEquals("2026-04-01", descending.get(2).getDate());

        List<WeightEntry> ascending = weightDao.getAllWeightsAscending("chris");
        assertEquals("2026-04-01", ascending.get(0).getDate());
        assertEquals("2026-04-03", ascending.get(2).getDate());

        assertEquals(3, weightDao.getEntryCount("chris"));
        assertEquals(1, weightDao.getEntryCount("sam"));
    }

    @Test
    public void weightDao_dateRangeQueryIsInclusiveAndScopedToUser() {
        insertUsers("chris", "sam");
        weightDao.insertWeight(new WeightEntry("chris", "2026-04-01", 190.0));
        weightDao.insertWeight(new WeightEntry("chris", "2026-04-02", 189.0));
        weightDao.insertWeight(new WeightEntry("chris", "2026-04-03", 188.0));
        weightDao.insertWeight(new WeightEntry("sam", "2026-04-02", 210.0));

        List<WeightEntry> range = weightDao.getWeightsByDateRange(
                "chris", "2026-04-02", "2026-04-03");

        assertEquals(2, range.size());
        assertEquals("2026-04-02", range.get(0).getDate());
        assertEquals("2026-04-03", range.get(1).getDate());
        assertEquals("chris", range.get(0).getUserId());
        assertEquals("chris", range.get(1).getUserId());
    }

    @Test
    public void weightDao_updateAndDeleteMutateExistingRows() {
        insertUsers("chris");
        weightDao.insertWeight(new WeightEntry("chris", "2026-04-01", 190.0));
        WeightEntry stored = weightDao.getAllWeights("chris").get(0);

        WeightEntry updated = new WeightEntry("chris", stored.getDate(), 187.5);
        updated.setId(stored.getId());
        weightDao.updateWeight(updated);

        List<WeightEntry> afterUpdate = weightDao.getAllWeights("chris");
        assertEquals(1, afterUpdate.size());
        assertEquals(187.5, afterUpdate.get(0).getWeight(), 0.001);

        weightDao.deleteWeight(afterUpdate.get(0));
        assertTrue(weightDao.getAllWeights("chris").isEmpty());
        assertEquals(0, weightDao.getEntryCount("chris"));
    }

    @Test(expected = SQLiteConstraintException.class)
    public void weightDao_rejectsEntriesForMissingUsers() {
        weightDao.insertWeight(new WeightEntry("missing-user", "2026-04-01", 190.0));
    }

    @Test
    public void goalDao_returnsMostRecentGoalAndSupportsUpdateDelete() {
        insertUsers("chris");
        goalDao.insertGoal(new Goal("chris", 160.0, "2026-04-01"));
        goalDao.insertGoal(new Goal("chris", 150.0, "2026-04-10"));
        goalDao.insertGoal(new Goal("chris", 149.0, "2026-04-10"));

        Goal active = goalDao.getActiveGoal("chris");
        assertNotNull(active);
        assertEquals(149.0, active.getTargetWeight(), 0.001);

        Goal updated = new Goal("chris", 148.0, active.getCreatedDate());
        updated.setId(active.getId());
        goalDao.updateGoal(updated);

        Goal afterUpdate = goalDao.getActiveGoal("chris");
        assertEquals(148.0, afterUpdate.getTargetWeight(), 0.001);

        goalDao.deleteGoal(afterUpdate);
        Goal afterDelete = goalDao.getActiveGoal("chris");
        assertNotNull(afterDelete);
        assertEquals(150.0, afterDelete.getTargetWeight(), 0.001);
    }

    @Test
    public void repository_computeTrendReadsEntriesThroughRoom() {
        insertUsers("chris");
        weightDao.insertWeight(new WeightEntry("chris", "2026-04-07", 174.0));
        weightDao.insertWeight(new WeightEntry("chris", "2026-04-01", 180.0));
        weightDao.insertWeight(new WeightEntry("chris", "2026-04-03", 178.0));
        weightDao.insertWeight(new WeightEntry("chris", "2026-04-02", 179.0));
        weightDao.insertWeight(new WeightEntry("chris", "2026-04-04", 177.0));
        weightDao.insertWeight(new WeightEntry("chris", "2026-04-06", 175.0));
        weightDao.insertWeight(new WeightEntry("chris", "2026-04-05", 176.0));

        WeightRepository repository = new WeightRepository(database);
        WeightTrendResult result = repository.computeTrend("chris", 177.0);

        assertFalse(result.hasInsufficientData());
        assertEquals(1, result.getRollingAverages().size());
        assertEquals(177.0, result.getLatestRollingAverage(), 0.001);
        assertTrue(result.isGoalReached());
    }

    @Test
    public void liveWeightQuery_emitsCurrentRows() throws InterruptedException {
        insertUsers("chris");
        weightDao.insertWeight(new WeightEntry("chris", "2026-04-01", 190.0));

        List<WeightEntry> emitted = getOrAwaitValue(weightDao.getAllWeightsLive("chris"));

        assertEquals(1, emitted.size());
        assertEquals("2026-04-01", emitted.get(0).getDate());
        assertEquals(190.0, emitted.get(0).getWeight(), 0.001);
    }

    private void insertUsers(String... usernames) {
        for (String username : usernames) {
            userDao.insertUser(new User(username, "hash-" + username));
        }
    }

    private <T> T getOrAwaitValue(LiveData<T> liveData) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> data = new AtomicReference<>();
        Observer<T> observer = value -> {
            data.set(value);
            latch.countDown();
        };

        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> liveData.observeForever(observer));
        try {
            assertTrue("LiveData value was never emitted",
                    latch.await(2, TimeUnit.SECONDS));
            return data.get();
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(
                    () -> liveData.removeObserver(observer));
        }
    }
}
