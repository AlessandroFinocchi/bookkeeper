package org.apache.bookkeeper.bookie.storage.ldb;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.apache.bookkeeper.bookie.custom_utils.Utils.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WriteCachePutTest {
    private static Stream<Arguments> data() {
        WriteCacheState valid1 = new WriteCacheState(unpooledByteBufAllocator(), 10, 8);

        return Stream.of(
                // example test
                Arguments.of(valid1, 1, 1, fullByteBuf(), true, null)

        );
    }

    @ParameterizedTest
    @MethodSource("data")
//    @Timeout(value = 5, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void put(WriteCacheState s, long ledgerId, long entryId, ByteBuf entry, boolean expectedReturn,
                          Class<Exception> expectedException) {
        WriteCache wc;

        try {
            wc = new WriteCache(s.allocator, s.maxCacheSize, s.maxSegmentSize);
            Assertions.assertNotNull(wc);
        } catch (Exception e) { throw new RuntimeException(e); }

        if (expectedException != null) Assertions.assertThrows(expectedException, () -> wc.put(ledgerId, entryId, entry));
        else {
            try {
                boolean actualReturn = wc.put(ledgerId, entryId, entry);

                //=============== Compare expected and actual written bytes on the destination buffer  ===============//
                Assertions.assertEquals(expectedReturn, actualReturn);
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
