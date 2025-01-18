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
    private static Stream<Arguments> data() {
        WriteCacheState valid1 = new WriteCacheState(unpooledByteBufAllocator(), 32, 16);
        WriteCacheState valid2 = new WriteCacheState(unpooledByteBufAllocator(), 16, 8);
        WriteCacheState t4invalid = new WriteCacheState(unpooledByteBufAllocator(),  0, 1);

        return Stream.of(
                // Constructor failed tests
                Arguments.of(t4invalid, 1, 1, fullByteBuf(), false, null),

                // Varying ledgerId
                Arguments.of(valid1, -1, 1, fullByteBuf(), false, Exception.class),
                Arguments.of(valid1,  0, 1, fullByteBuf(), true, null),

                // Varying entryId
                Arguments.of(valid1, 1, -1, fullByteBuf(), false, Exception.class),
                Arguments.of(valid1, 1,  0, fullByteBuf(), true, null),

                // Varying entry
                Arguments.of(valid1, 1, 1, invalidReadIndexByteBuf(),   false, Exception.class),
                Arguments.of(valid1, 1, 1, deallocatedByteBuf(),        false, Exception.class),
                Arguments.of(valid1, 1, 1, null,                        false, Exception.class),

                // Varying entry for different segment sizes
                Arguments.of(valid1, 1, 1, fullByteBuf(),               true, null),
                Arguments.of(valid2, 1, 1, fullByteBuf(),               false, null),
                Arguments.of(valid1, 1, 1, emptyByteBuf(),              true, null)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
//    @Timeout(value = 5, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void put(WriteCacheState s, long ledgerId, long entryId, ByteBuf entry, boolean expectedReturn,
                          Class<Exception> expectedException) {
        WriteCache wc = new WriteCache(s.allocator, s.maxCacheSize, s.maxSegmentSize);;
        Assertions.assertNotNull(wc);

        if (expectedException != null) Assertions.assertThrows(expectedException, () -> wc.put(ledgerId, entryId, entry));
        else {
            try {
                boolean actualReturn = wc.put(ledgerId, entryId, entry);

                // ====================================== Check class fields  ====================================== //
                String expectedWrittenString = entry.toString(StandardCharsets.UTF_8);
                long expectedSize  = expectedReturn ? expectedWrittenString.length() : 0;
                long expectedCount = expectedReturn ? 1 : 0;

                Assertions.assertEquals(wc.size(),  expectedSize);
                Assertions.assertEquals(wc.count(), expectedCount);
                Assertions.assertEquals(expectedReturn, actualReturn);

                // ======================================== Check hash maps ========================================= //
                ConcurrentLongLongPairHashMap.LongPair actualStoredPair = wc.getIndex().get(ledgerId, entryId);
                ConcurrentLongLongPairHashMap.LongPair expectedStoredPair = wc.getIndex().get(ledgerId, entryId);
                long actualStoredEntryId = wc.getLastEntryMap().get(ledgerId);
                long expectedStoredEntryId = actualReturn ? entryId : -1;

                Assertions.assertEquals(expectedStoredEntryId, actualStoredEntryId);
                Assertions.assertEquals(expectedStoredPair, actualStoredPair);
            } catch (Exception e) { throw new RuntimeException(e); }
        }
    }

    private static final class WriteCacheState {
        private final ByteBufAllocator allocator;
        private final long maxCacheSize;
        private final int maxSegmentSize;

        WriteCacheState(ByteBufAllocator allocator, long maxCacheSize, int maxSegmentSize) {
            this.allocator = allocator;
            this.maxCacheSize = maxCacheSize;
            this.maxSegmentSize = maxSegmentSize;
        }
    }
}
