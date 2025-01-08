package org.apache.bookkeeper.bookie;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.apache.bookkeeper.bookie.BufferedChannelUtils.*;

/**
 * Unit testing for {@link BufferedChannel}. class <br>
 * Tested method: {@link BufferedChannel#write(ByteBuf)}
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BufferedChannelWriteTest {

    private static Stream<Arguments> data() {
        try {
            return Stream.of(
                    // Constructor failed tests
                    Arguments.of(invalidByteBufAllocator(),  validFileChannel(),            100, 100, 0,    fullByteBuf(), Exception.class),            // T2 passed
//                    Arguments.of(unpooledByteBufAllocator(), invalidPositionFileChannel(),  100, 100, 0,    fullByteBuf(), Exception.class), // T7  not passed
//                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(),            100, 100, 101,  fullByteBuf(), Exception.class), // T15 not passed
//                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(),            100, 100, -1,   fullByteBuf(), Exception.class), // T20 not passed

                    // Varying fc
//                    Arguments.of(unpooledByteBufAllocator(), readOnlyFileChannel(), 100, 100, 0,  fullByteBuf(), Exception.class),           // not passed
                    Arguments.of(unpooledByteBufAllocator(),    readOnlyFileChannel(),  100, 100, 1,  fullByteBuf(), Exception.class),                  // passed

                    // Varying writeCapacity
//                    Arguments.of(unpooledByteBufAllocator(),  validFileChannel(),       0,      100, 0, fullByteBuf(), Exception.class),      // blocking

                    // Varying unpersistedBytesBound -> already in constructor failed tests

                    // Varying src
                    Arguments.of(unpooledByteBufAllocator(),  validFileChannel(), 100, 100, 0,      emptyByteBuf(),                 null),              // passed
                    Arguments.of(unpooledByteBufAllocator(),  validFileChannel(), 100, 100, 0,      semiFullByteBuf(),              null),              // passed
                    Arguments.of(unpooledByteBufAllocator(),  validFileChannel(), 100, 100, 0,      invalidReadIndexByteBuf(),      Exception.class),   // passed
                    Arguments.of(unpooledByteBufAllocator(),  validFileChannel(), 100, 100, 0,      deallocatedByteBuf(),           Exception.class)    // passed


            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    public void write(ByteBufAllocator allocator, FileChannel fc, int writeCapacity, int readCapacity,
                     long unpersistedBytesBound, ByteBuf src, Class<Exception> expectedException) {
        BufferedChannel bc;

        try {
//            fc.position(0);
            bc = new BufferedChannel(allocator, fc, writeCapacity, readCapacity, unpersistedBytesBound);
            Assertions.assertNotNull(bc);
        } catch (Exception e) { throw new RuntimeException(e); }

        if (expectedException != null) Assertions.assertThrows(expectedException, () -> bc.write(src));
        else {
            try{
                bc.write(src);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

//    @AfterEach
//    public void deleteTestFile() throws IOException {
//        Path path = Paths.get(BC_TEST_FILE);
//        if (Files.exists(path)) Files.delete(path);
//    }
}
