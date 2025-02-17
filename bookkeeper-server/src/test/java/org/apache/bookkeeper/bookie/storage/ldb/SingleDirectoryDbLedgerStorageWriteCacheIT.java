package org.apache.bookkeeper.bookie.storage.ldb;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.apache.bookkeeper.bookie.*;
import org.apache.bookkeeper.bookie.storage.EntryLogger;
import org.apache.bookkeeper.conf.ServerConfiguration;
import org.apache.bookkeeper.conf.TestBKConfiguration;
import org.apache.bookkeeper.meta.LedgerManager;
import org.apache.bookkeeper.stats.StatsLogger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

public class SingleDirectoryDbLedgerStorageWriteCacheIT {

    private DbLedgerStorage storage;
    private File tmpDir;
    public static WriteCache currentWriteCache;

    // Implementation from bookkeeper original code
    private static class MockedDbLedgerStorage extends DbLedgerStorage {

        @Override
        protected SingleDirectoryDbLedgerStorage newSingleDirectoryDbLedgerStorage(ServerConfiguration conf,
                                                                                   LedgerManager ledgerManager, LedgerDirsManager ledgerDirsManager,
                                                                                   LedgerDirsManager indexDirsManager,
                                                                                   EntryLogger entryLogger, StatsLogger statsLogger,
                                                                                   long writeCacheSize, long readCacheSize, int readAheadCacheBatchSize,
                                                                                   long readAheadCacheBatchBytesSize)
                throws IOException {
            return new MockedSingleDirectoryDbLedgerStorage(conf, ledgerManager, ledgerDirsManager, indexDirsManager,
                    entryLogger, statsLogger, allocator, writeCacheSize,
                    readCacheSize, readAheadCacheBatchSize, readAheadCacheBatchBytesSize);
        }

        private static class MockedSingleDirectoryDbLedgerStorage extends SingleDirectoryDbLedgerStorage {
            public MockedSingleDirectoryDbLedgerStorage(ServerConfiguration conf, LedgerManager ledgerManager,
                                                        LedgerDirsManager ledgerDirsManager, LedgerDirsManager indexDirsManager, EntryLogger entryLogger,
                                                        StatsLogger statsLogger,
                                                        ByteBufAllocator allocator, long writeCacheSize,
                                                        long readCacheSize, int readAheadCacheBatchSize, long readAheadCacheBatchBytesSize)
                    throws IOException {
                super(conf, ledgerManager, ledgerDirsManager, indexDirsManager, entryLogger,
                        statsLogger, allocator, writeCacheSize, readCacheSize, readAheadCacheBatchSize,
                        readAheadCacheBatchBytesSize);
                currentWriteCache = spy(this.writeCache);
                this.writeCache = currentWriteCache;
            }
        }
    }

    // Implementation from bookkeeper original code
    private ByteBuf customByteBuf(long ledgerId, long entryId) {
        ByteBuf entry = Unpooled.buffer(512 + 2 * 8);
        entry.writeLong(ledgerId); // ledger id
        entry.writeLong(entryId); // entry id
        entry.writeZero(512);

        return entry;
    }

    // Implementation from bookkeeper original code
    @Before
    public void setup() throws Exception {
        tmpDir = File.createTempFile("bkTest", ".dir");
        tmpDir.delete();
        tmpDir.mkdir();
        File curDir = BookieImpl.getCurrentDirectory(tmpDir);
        BookieImpl.checkDirectoryStructure(curDir);

        int gcWaitTime = 1000;
        ServerConfiguration conf = TestBKConfiguration.newServerConfiguration();
        conf.setGcWaitTime(gcWaitTime);
        conf.setLedgerStorageClass(MockedDbLedgerStorage.class.getName());
        conf.setProperty(DbLedgerStorage.WRITE_CACHE_MAX_SIZE_MB, 1);
        conf.setProperty(DbLedgerStorage.MAX_THROTTLE_TIME_MILLIS, 1000);
        conf.setLedgerDirNames(new String[] { tmpDir.toString() });
        Bookie bookie = new TestBookieImpl(conf);

        storage = (DbLedgerStorage) bookie.getLedgerStorage();
    }

    // Implementation from bookkeeper original code
    @After
    public void teardown() throws Exception {
        if(storage != null) {
            storage.shutdown();
        }
        tmpDir.delete();
    }

    @Test
    public void testWriteCacheStubbed1() {
        ByteBuf entry;

        try {
            entry = customByteBuf(1, 2);
            doReturn(true).when(currentWriteCache).put(anyLong(), anyLong(), any());
            storage.addEntry(entry);
            verify(currentWriteCache, times(1)).put(1, 2, entry);

        } catch (Exception e) {
            Assertions.fail("Unexpected exception", e);
        }
    }

    @Test
    public void testWriteCacheStubbed2() {
        ByteBuf entry;

        try {
            entry = customByteBuf(1, 3);
            doReturn(false, true).when(currentWriteCache).put(anyLong(), anyLong(), any());
            storage.addEntry(entry);
            verify(currentWriteCache, times(2)).put(1, 3, entry);

        } catch (Exception e) {
            Assertions.fail("Unexpected exception", e);
        }
    }

    @Test
    public void testWriteCacheRotation() throws Exception {
        // Add enough entries to fill the 1st write cache
        for (int i = 0; i < 5; i++) {
            ByteBuf entry = Unpooled.buffer(100 * 1024 + 2 * 8);
            entry.writeLong(4); // ledger id
            entry.writeLong(i); // entry id
            entry.writeZero(100 * 1024);
            storage.addEntry(entry);
        }

        // Next add should make caches rotate, thus making one put more
        ByteBuf entry = Unpooled.buffer(100 * 1024 + 2 * 8);
        entry.writeLong(4); // ledger id
        entry.writeLong(22); // entry id
        entry.writeZero(100 * 1024);
        storage.addEntry(entry);

        verify(currentWriteCache, atLeast(2)).put(4, 22, entry);
    }
}
