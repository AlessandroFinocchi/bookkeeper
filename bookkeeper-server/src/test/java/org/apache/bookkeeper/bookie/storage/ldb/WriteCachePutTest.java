package org.apache.bookkeeper.bookie.storage.ldb;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.apache.bookkeeper.util.collections.ConcurrentLongLongPairHashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.apache.bookkeeper.bookie.custom_utils.Utils.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WriteCachePutTest {
    private enum WcType {
        WRITTEN,
        NON_WRITTEN
    }

    private static Stream<Arguments> data() {
        WriteCacheState t4invalid = new WriteCacheState(unpooledByteBufAllocator(),  0, 1, WcType.NON_WRITTEN);
        WriteCacheState validNonWritten1 = new WriteCacheState(unpooledByteBufAllocator(), 512, 256, WcType.NON_WRITTEN);
        WriteCacheState validNonWritten2 = new WriteCacheState(unpooledByteBufAllocator(), 16, 8, WcType.NON_WRITTEN);
        WriteCacheState validWritten = new WriteCacheState(unpooledByteBufAllocator(), 512, 256, WcType.WRITTEN);

        return Stream.of(
                // Constructor failed tests
                Arguments.of(t4invalid, 1, 1, fullByteBuf(), false, null),

                // Varying ledgerId
                Arguments.of(validNonWritten1, -1, 1, fullByteBuf(), false, Exception.class),
                Arguments.of(validNonWritten1,  0, 1, fullByteBuf(), true, null),

                // Varying entryId
                Arguments.of(validNonWritten1, 1, -1, fullByteBuf(), false, Exception.class),
                Arguments.of(validNonWritten1, 1,  0, fullByteBuf(), true, null),

                // Varying entry
                Arguments.of(validNonWritten1, 1, 1, invalidReadIndexByteBuf(),   false, Exception.class),
                Arguments.of(validNonWritten1, 1, 1, deallocatedByteBuf(),        false, Exception.class),
                Arguments.of(validNonWritten1, 1, 1, null,                        false, Exception.class),

                // Varying entry for different segment sizes
                Arguments.of(validNonWritten1, 1, 1, fullByteBuf(),               true, null),
                Arguments.of(validNonWritten2, 1, 1, fullByteBuf(),               false, null),
                Arguments.of(validNonWritten1, 1, 1, emptyByteBuf(),              true, null),

                // After jacoco
                Arguments.of(validWritten, 1, 1, fullByteBuf(),              true, null),
                Arguments.of(validWritten, 1, 3, fullByteBuf(),              true, null)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
//    @Timeout(value = 5, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void put(WriteCacheState s, long ledgerId, long entryId, ByteBuf entry, boolean expectedReturn,
                          Class<Exception> expectedException) {
        WriteCache wc = new WriteCache(s.allocator, s.maxCacheSize, s.maxSegmentSize);
        Assertions.assertNotNull(wc);

        int firstPutEntryId = -1;
        int firstPutEntrySize = 0;
        if(s.type == WcType.WRITTEN){
            ByteBuf firstPutEntry = fullByteBuf();
            firstPutEntryId = 2;
            firstPutEntrySize = firstPutEntry.toString(StandardCharsets.UTF_8).length();
            if(!wc.put(ledgerId, firstPutEntryId, firstPutEntry)) throw new RuntimeException("Put failed");
        }


        if (expectedException != null)
            Assertions.assertThrows(expectedException, () -> wc.put(ledgerId, entryId, entry));
        else {
            try {
                boolean actualReturn = wc.put(ledgerId, entryId, entry);

                // ====================================== Check class fields  ====================================== //
                String expectedWrittenString = entry.toString(StandardCharsets.UTF_8);
                long expectedSize  = expectedReturn ? expectedWrittenString.length() : 0;
                long expectedCount = expectedReturn ? 1 : 0;
                if (s.type == WcType.WRITTEN) {
                    expectedSize += firstPutEntrySize;
                    expectedCount += 1;
                }

                Assertions.assertEquals(expectedSize,   wc.size(),    "failed on size");
                Assertions.assertEquals(expectedCount,  wc.count(),   "failed on count");
                Assertions.assertEquals(expectedReturn, actualReturn, "failed on return");

                // ======================================== Check hash maps ========================================= //
                ConcurrentLongLongPairHashMap.LongPair actualStoredPair = wc.getIndex().get(ledgerId, entryId);
                ConcurrentLongLongPairHashMap.LongPair expectedStoredPair = wc.getIndex().get(ledgerId, entryId);
                long actualStoredEntryId = wc.getLastEntryMap().get(ledgerId);
                long expectedStoredEntryId = actualReturn && entryId > firstPutEntryId ? entryId : firstPutEntryId;

                Assertions.assertEquals(expectedStoredEntryId, actualStoredEntryId, "failed on entry id");
                Assertions.assertEquals(expectedStoredPair, actualStoredPair, "failed on stored pair");
            } catch (Exception e) { throw new RuntimeException(e); }
        }
    }

    private static final class WriteCacheState {
        private final ByteBufAllocator allocator;
        private final long maxCacheSize;
        private final int maxSegmentSize;
        private final WcType type;

        WriteCacheState(ByteBufAllocator allocator, long maxCacheSize, int maxSegmentSize, WcType type) {
            this.allocator = allocator;
            this.maxCacheSize = maxCacheSize;
            this.maxSegmentSize = maxSegmentSize;
            this.type = type;
        }
    }
}
