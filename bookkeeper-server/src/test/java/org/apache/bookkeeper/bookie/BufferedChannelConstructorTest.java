package org.apache.bookkeeper.bookie;

import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.apache.bookkeeper.bookie.BufferedChannelUtils.*;

/**
 * Unit testing for {@link BufferedChannel}. class <br>
 * Tested method: constructor
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BufferedChannelConstructorTest {

    private static Stream<Arguments> data() {
        try {
            return Stream.of(
                    // Varying allocator
                    Arguments.of(unpooledByteBufAllocator(),    validFileChannel(), 100, 100, 1, null),
//                    Arguments.of(invalidByteBufAllocator(),     validFileChannel(), 100, 100, 1, Exception.class),              // T2 Not passed
                    Arguments.of(null,                          validFileChannel(), 100, 100, 1, Exception.class),

                    // Varying FileChannel
                    Arguments.of(unpooledByteBufAllocator(),    readOnlyFileChannel(),          100, 100, 1, null),
                    Arguments.of(unpooledByteBufAllocator(),    writeOnlyFileChannel(),         100, 100, 1, null),
                    Arguments.of(unpooledByteBufAllocator(),    closedFileChannel(),            100, 100, 1, Exception.class),
//                    Arguments.of(unpooledByteBufAllocator(),    invalidPositionFileChannel(),   100, 100, 1, Exception.class),  // T7 Not passed
                    Arguments.of(unpooledByteBufAllocator(),    null,                           100, 100, 1, Exception.class),

                    // Varying writeCapacity
                    Arguments.of(unpooledByteBufAllocator(),    validFileChannel(),    1, 100, 1, null),
                    Arguments.of(unpooledByteBufAllocator(),    validFileChannel(),    0, 100, 1, null),
                    Arguments.of(unpooledByteBufAllocator(),    validFileChannel(),    -1, 100, 1, Exception.class),

                    // Varying readCapacity
                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100,       1,      1, null),
                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100,       0,      1, null),
                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100,       -1,     1, Exception.class),

                    // Varying unpersistedBytesBound
//                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100, 100,      101,    Exception.class),       // T15 Not passed
                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100, 100,      100,    null),
                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100, 100,      99,     null),
                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100, 100,      50,     null),
                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100, 100,      0,      null)
//                    ,Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100, 100,     -1,      Exception.class)        // T20 Not passed
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest
    @Timeout(value = 5)
    @MethodSource("data")
    public void construct(ByteBufAllocator allocator, FileChannel fc, int writeCapacity, int readCapacity,
                     long unpersistedBytesBound, Class<Exception> expectedException) {
        if (expectedException != null) {
            Assertions.assertThrows(expectedException,
                    () -> new BufferedChannel(allocator, fc, writeCapacity, readCapacity, unpersistedBytesBound),
                    "Expected exception: " + expectedException.getName());
        }
        else {
            try{
                BufferedChannel bc = new BufferedChannel(allocator, fc, writeCapacity, readCapacity, unpersistedBytesBound);
                Assertions.assertNotNull(bc);

                // =====================================  Check class fields ====================================== //
                Assertions.assertEquals(fc.position(), bc.position);
                Assertions.assertEquals(0, bc.unpersistedBytes.get());
                Assertions.assertEquals(unpersistedBytesBound, bc.unpersistedBytesBound);
                // Since we don't know what buffer it's been used,
                // then we just check the allocator that it's been used
                Assertions.assertSame(allocator, bc.writeBuffer.alloc());
                Assertions.assertEquals(fc.position(), bc.writeBufferStartPosition.get());
                Assertions.assertEquals(writeCapacity, bc.writeCapacity);

                // ========================  Check superclass BufferedReadChannel fields ========================== //
                Assertions.assertEquals(0, bc.cacheHitCount);
                Assertions.assertEquals(0, bc.invocationCount);
                // Since by javadoc superclass doesn't use any allocator in their constructor,
                // then we can't assert anything about the read buffer
                Assertions.assertNotNull(bc.readBuffer);
                Assertions.assertEquals(readCapacity, bc.readCapacity);
                Assertions.assertFalse(bc.sealed);

                // ========================  Check superclass BufferedChannelBase fields ========================== //
                Assertions.assertEquals(fc, bc.fileChannel);

                // ====================================== Added after Pitest ====================================== //
                Assertions.assertEquals(unpersistedBytesBound > 0, bc.getDoRegularFlushes());
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @AfterEach
    public void deleteTestFile() throws IOException {
        Path path = Paths.get(BC_TEST_FILE);
        if (Files.exists(path)) Files.delete(path);
    }
}
