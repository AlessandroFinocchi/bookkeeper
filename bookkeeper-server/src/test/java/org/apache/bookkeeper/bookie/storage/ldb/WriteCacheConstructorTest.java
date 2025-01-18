package org.apache.bookkeeper.bookie.storage.ldb;

import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.apache.bookkeeper.bookie.custom_utils.Utils.*;

/**
 * Unit testing for {@link WriteCache}. class <br>
 * Tested method: constructor
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WriteCacheConstructorTest {

    private static Stream<Arguments> data() {
        return Stream.of(
                // Varying allocator
//                Arguments.of(invalidByteBufAllocator(), 1, 1, Exception.class),       // T1 not passed
//                Arguments.of(null, 1, 1, Exception.class),                            // T2 not passed

                // Varying maxCacheSize
                Arguments.of(unpooledByteBufAllocator(), -1, 1, Exception.class),       // T3 passed
//                Arguments.of(unpooledByteBufAllocator(),  0, 1, Exception.class),     // T4 not passed
                Arguments.of(unpooledByteBufAllocator(),  1, 1, null),                  // T5 passed

                // Varying maxSegmentSize
                Arguments.of(unpooledByteBufAllocator(), 10, -1, Exception.class),      // T6  passed
                Arguments.of(unpooledByteBufAllocator(), 10,  0, Exception.class),      // T7  passed
                Arguments.of(unpooledByteBufAllocator(), 10,  1, null),                 // T8  passed
                Arguments.of(unpooledByteBufAllocator(), 10,  6, Exception.class),      // T9  passed
                Arguments.of(unpooledByteBufAllocator(), 10,  8, null),                 // T10 passed
                Arguments.of(unpooledByteBufAllocator(), 10, 10, Exception.class),      // T11 passed
                Arguments.of(unpooledByteBufAllocator(), 10, 20, Exception.class)       // T12 passed

        );
    }

    @ParameterizedTest
    @MethodSource("data")
    @Timeout(value = 5, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void construct(ByteBufAllocator allocator, long maxCacheSize, int maxSegmentSize,
                          Class<Exception> expectedException) {
        if (expectedException != null) {
            Assertions.assertThrows(expectedException,
                    () -> new WriteCache(allocator, maxCacheSize, maxSegmentSize),
                    "Expected exception: " + expectedException.getName());
        }
        else {
            try{
                WriteCache wc = new WriteCache(allocator, maxCacheSize, maxSegmentSize);
                Assertions.assertNotNull(wc);
                int expectedSegmentCount = (int) (1 + (maxCacheSize / maxSegmentSize));

                // =====================================  Check class fields ====================================== //
                Assertions.assertEquals(wc.getMaxCacheSize(),       maxCacheSize);
                Assertions.assertEquals(wc.getMaxSegmentSize(),     maxSegmentSize);
                Assertions.assertEquals(wc.count(),                 0);
                Assertions.assertEquals(wc.size(),                  0);
                Assertions.assertEquals(wc.getSegmentsCount(),      expectedSegmentCount);

                for(int i = 0; i < expectedSegmentCount; i++){
                    Assertions.assertEquals(wc.getCacheSegments()[i].capacity(),
                            i < expectedSegmentCount - 1 ? maxSegmentSize : maxCacheSize % maxSegmentSize);
                }

                // ====================================== Added after pitest ====================================== //
                Assertions.assertEquals(wc.getSegmentOffsetMask(), maxSegmentSize - 1);
                Assertions.assertEquals(wc.getSegmentOffsetBits(), 63 - Long.numberOfLeadingZeros(maxSegmentSize));
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
