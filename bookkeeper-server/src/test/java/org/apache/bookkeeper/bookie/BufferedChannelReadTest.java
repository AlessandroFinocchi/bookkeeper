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
import java.time.Duration;
import java.util.stream.Stream;

import static org.apache.bookkeeper.bookie.BufferedChannelUtils.*;
import static org.apache.bookkeeper.bookie.BufferedChannelUtils.BC_FC_STRING_TEST;

/**
 * Unit testing for {@link BufferedChannel}. class <br>
 * Tested method: {@link BufferedChannel#read(ByteBuf, long, int)}
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BufferedChannelReadTest {

    private static Stream<Arguments> data() {
        try {
            return Stream.of(
                    // Constructor failed tests
//                    Arguments.of(invalidByteBufAllocator(),  validFileChannel(), 100, 100, 0, emptyByteBuf(), 0, BC_FC_STRING_TEST.length(), -1, Exception.class),                    // T2
//                    Arguments.of(unpooledByteBufAllocator(), invalidPositionFileChannel(), 100, 100, 0, emptyByteBuf(), 0, BC_FC_STRING_TEST.length(), BC_FC_STRING_TEST.length(), null),    // T9

                    // Varying valid FileChannel
                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100, 100, 0, emptyByteBuf(), 0, BC_FC_STRING_TEST.length(), BC_FC_STRING_TEST.length(), null),
                    Arguments.of(unpooledByteBufAllocator(), writeOnlyFileChannel(), 100, 100, 0, emptyByteBuf(), 0, BC_FC_STRING_TEST.length(), -1, Exception.class),

                    // Varying valid readCapacity
                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100, 0, 0, emptyByteBuf(), 0, BC_FC_STRING_TEST.length(), -1, Exception.class),

                    // Varying dest
                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100, 100, 0, emptyByteBuf(),             0, BC_FC_STRING_TEST.length(), BC_FC_STRING_TEST.length(), null)
//                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100, 100, 0, semiFullByteBuf(),          0, BC_FC_STRING_TEST.length(), BC_FC_STRING_TEST.length()/2, null),
//                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100, 100, 0, fullByteBuf(),              0, BC_FC_STRING_TEST.length(), -1, Exception.class), // blocking
//                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100, 100, 0, invalidWriteIndexByteBuf(), 0, BC_FC_STRING_TEST.length(), -1, Exception.class), // blocking
//                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100, 100, 0, invalidByteBuf(),           0, BC_FC_STRING_TEST.length(), -1, Exception.class), // blocking
//                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100, 100, 0, deallocatedByteBuf(),       0, BC_FC_STRING_TEST.length(), -1, Exception.class),
//                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100, 100, 0, null,                       0, BC_FC_STRING_TEST.length(), -1, NullPointerException.class),
//
//                    // Varying pos
//                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100, 100, 0, emptyByteBuf(), -1,                      BC_FC_STRING_TEST.length(), -1, Exception.class),
//                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100, 100, 0, emptyByteBuf(), 1,                       BC_FC_STRING_TEST.length()-1, BC_FC_STRING_TEST.length()-1, null),
//                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100, 100, 0, emptyByteBuf(), BC_FC_STRING_TEST.length(),   BC_FC_STRING_TEST.length(), -1, Exception.class),
//                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100, 100, 0, emptyByteBuf(), BC_FC_STRING_TEST.length()+1, BC_FC_STRING_TEST.length(), -1, Exception.class),
//
//                    // Varying length
//                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100, 100, 0, emptyByteBuf(), 0, -1,                          -1, Exception.class),
//                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100, 100, 0, emptyByteBuf(), 0, 0,                           0, null),
//                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100, 100, 0, emptyByteBuf(), 0, 1,                           1, null),
//                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100, 100, 0, emptyByteBuf(), 0, BC_FC_STRING_TEST.length(),     BC_FC_STRING_TEST.length(), null),
//                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 100, 100, 0, emptyByteBuf(), 0, BC_FC_STRING_TEST.length() + 1, -1, Exception.class)
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest
//    @Disabled
    @Timeout(6)
    @MethodSource("data")
    public void read(ByteBufAllocator allocator, FileChannel fc, int writeCapacity, int readCapacity,
                     long unpersistedBytesBound, ByteBuf dest, long pos, int length, int expectedReturn,
                     Class<Exception> expectedException) {
//        Assertions.assertTimeout(Duration.ofSeconds(5), () -> Thread.sleep(5000)); // To overcome infinite loops
        BufferedChannel bc;
        int destStartingWritePos = dest != null ? dest.writerIndex() : 0;
        if (expectedException != null && expectedReturn != -1) throw new RuntimeException("Invalid test configuration");

        try {
            bc = new BufferedChannel(allocator, fc, writeCapacity, readCapacity, unpersistedBytesBound);
            Assertions.assertNotNull(bc);

            // Assure that the BufferedChannel know the bytes to read
        } catch (Exception e) { throw new RuntimeException(e); }

        if (expectedException != null) {
            Assertions.assertThrows(expectedException, () -> {
                bc.readBuffer.writerIndex((int)fc.size());
//                bc.writeBuffer.writerIndex((int)fc.size());
                bc.read(dest, pos, length);
            }
            );
        } else {
            try {
                bc.readBuffer.writerIndex((int)fc.size());
//                bc.writeBuffer.writerIndex((int)fc.size());
                int actualReturn = bc.read(dest, pos, length);

                Assertions.assertEquals(expectedReturn, actualReturn);

                /* Compare expected and actual written bytes on the destination buffer 2, */
                // Get the substring that should be written
                int startingPos = (int) pos;
                int endingPos = startingPos + length;
                String expectedWrittenString = BC_FC_STRING_TEST.substring(startingPos, endingPos);

                // Get the substring that was actually written
                ByteBuf actualWrittenBuffer = Unpooled.buffer(BC_FC_STRING_TEST.length());
                dest.getBytes(destStartingWritePos, actualWrittenBuffer, destStartingWritePos+length);
                String actualWrittenBytes   = actualWrittenBuffer.toString(StandardCharsets.UTF_8);

                Assertions.assertEquals(expectedWrittenString, actualWrittenBytes);
            } catch (Exception e) { throw new RuntimeException(e); }
        }
    }

    @AfterEach
    public void deleteTestFile() throws IOException {
        Path path = Paths.get(BC_TEST_FILE);
        if (Files.exists(path)) Files.delete(path);
    }
}
