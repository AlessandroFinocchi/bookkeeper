package org.apache.bookkeeper.custom_tests;

import org.apache.bookkeeper.bookie.Cookie;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CookieTest {
    private Cookie cookie;

    @Before
    public void setUp() throws Exception {
        Cookie.Builder builder = Cookie.newBuilder();

        builder.setLayoutVersion(1)
                .setBookieId("bookieId")
                .setJournalDirs("journalDirs")
                .setIndexDirs("indexDirs")
                .setLedgerDirs("ledgerDirs")
                .setInstanceId("instanceId");

        cookie = builder.build();
    }

    @Test
    public void myFirstTest(){
        Assert.assertTrue(true);
    }

    @After
    public void tearDown() throws Exception {
        cookie = null;
    }
}
