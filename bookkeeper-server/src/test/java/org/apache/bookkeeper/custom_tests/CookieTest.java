package org.apache.bookkeeper.custom_tests;

import org.apache.bookkeeper.bookie.BookieException;
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
    public void encodeDirPathsTest(){
        String[] dirs = {"dir1", "dir2"};
        String result = Cookie.encodeDirPaths(dirs);
        Assert.assertEquals(result, "2\tdir1\tdir2");
    }

    @After
    public void tearDown() throws Exception {
        cookie = null;
    }
}
