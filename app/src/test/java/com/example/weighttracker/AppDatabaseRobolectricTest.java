package com.example.weighttracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

/**
 * JVM-run Android persistence tests. Robolectric gives Room the Android
 * Context it needs, so these tests can run from Android Studio even when an
 * emulator is not available.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AppDatabaseRobolectricTest {

    private AppDatabase database;
    private UserDao userDao;
    private WeightDao weightDao;
    private GoalDao goalDao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
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
    public void userAndWeightDaos_persistScopedOrderedRows() {
        insertUsers("chris", "sam");
        weightDao.insertWeight(new WeightEntry("chris", "2026-04-01", 190.0));
        weightDao.insertWeight(new WeightEntry("sam", "2026-04-02", 210.0));
        weightDao.insertWeight(new WeightEntry("chris", "2026-04-03", 188.0));
        weightDao.insertWeight(new WeightEntry("chris", "2026-04-02", 189.0));

        List<WeightEntry> chrisDescending = weightDao.getAllWeights("chris");

        assertEquals(3, chrisDescending.size());
        assertEquals("2026-04-03", chrisDescending.get(0).getDate());
        assertEquals("2026-04-02", chrisDescending.get(1).getDate());
        assertEquals("2026-04-01", chrisDescending.get(2).getDate());
        assertEquals(1, weightDao.getEntryCount("sam"));
    }

    @Test
    public void weightDao_enforcesUserForeignKey() {
        try {
            weightDao.insertWeight(new WeightEntry("missing-user", "2026-04-01", 190.0));
            fail("Expected a foreign key constraint failure");
        } catch (SQLiteConstraintException expected) {
            assertNotNull(expected);
        }
    }

    @Test
    public void goalDao_returnsNewestGoalAndAllowsUpdates() {
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

        assertEquals(148.0, goalDao.getActiveGoal("chris").getTargetWeight(), 0.001);
    }

    @Test
    public void repository_computeTrendReadsRoomRowsInDateOrder() {
        insertUsers("chris");
        weightDao.insertWeight(new WeightEntry("chris", "2026-04-07", 174.0));
        weightDao.insertWeight(new WeightEntry("chris", "2026-04-01", 180.0));
        weightDao.insertWeight(new WeightEntry("chris", "2026-04-03", 178.0));
        weightDao.insertWeight(new WeightEntry("chris", "2026-04-02", 179.0));
        weightDao.insertWeight(new WeightEntry("chris", "2026-04-04", 177.0));
        weightDao.insertWeight(new WeightEntry("chris", "2026-04-06", 175.0));
        weightDao.insertWeight(new WeightEntry("chris", "2026-04-05", 176.0));

        WeightTrendResult result = new WeightRepository(database).computeTrend("chris", 177.0);

        assertFalse(result.hasInsufficientData());
        assertEquals(177.0, result.getLatestRollingAverage(), 0.001);
        assertTrue(result.isGoalReached());
    }

    private void insertUsers(String... usernames) {
        for (String username : usernames) {
            userDao.insertUser(new User(username, "hash-" + username));
        }
    }
}
