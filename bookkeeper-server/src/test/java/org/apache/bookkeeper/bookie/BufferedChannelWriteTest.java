package org.apache.bookkeeper.bookie;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.apache.bookkeeper.bookie.BufferedChannelUtils.*;
import static org.apache.bookkeeper.bookie.BufferedChannelUtils.fullByteBuf;

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
                    Arguments.of(invalidByteBufAllocator(),  validFileChannel(),            100, 100, 1,    fullByteBuf(), Exception.class), // T2  -> W1 passed
//                    Arguments.of(unpooledByteBufAllocator(), invalidPositionFileChannel(),  100, 100, 1,    fullByteBuf(), Exception.class), // T7  -> W2 not passed
//                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(),            100, 100, 101,  fullByteBuf(), Exception.class), // T15 -> W3 not passed
//                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(),            100, 100, -1,   fullByteBuf(), Exception.class), // T20 -> W4 not passed

                    // Varying fc
                    Arguments.of(unpooledByteBufAllocator(), readOnlyFileChannel(),  100, 100, 1,  fullByteBuf(), Exception.class),      // W5 passed

                    // Varying writeCapacity
//                    Arguments.of(unpooledByteBufAllocator(),  validFileChannel(),       0,      100, 1, fullByteBuf(), Exception.class),              // W6 blocking

                    // Varying unpersistedBytesBound
//                    Arguments.of(unpooledByteBufAllocator(),  validFileChannel(), 100, 100,     0,                              fullByteBuf(), null),  // W7  not passed
                    Arguments.of(unpooledByteBufAllocator(),  validFileChannel(), 100, 100,     BC_BB_STRING_TEST.length()-1,   fullByteBuf(), null),  // W8  passed
                    Arguments.of(unpooledByteBufAllocator(),  validFileChannel(), 100, 100,     BC_BB_STRING_TEST.length(),     fullByteBuf(), null),  // W9  passed
                    Arguments.of(unpooledByteBufAllocator(),  validFileChannel(), 100, 100,     BC_BB_STRING_TEST.length()+1,   fullByteBuf(), null),  // W10 passed

                    // Varying src
                    Arguments.of(unpooledByteBufAllocator(),  validFileChannel(), 100, 100, 1,      emptyByteBuf(),                 null),              // W11 passed
                    Arguments.of(unpooledByteBufAllocator(),  validFileChannel(), 100, 100, 1,      semiFullByteBuf(),              null),              // W12 passed
                    Arguments.of(unpooledByteBufAllocator(),  validFileChannel(), 100, 100, 1,      fullByteBuf(),                  null),              // W13 passed
                    Arguments.of(unpooledByteBufAllocator(),  validFileChannel(), 100, 100, 1,      invalidReadIndexByteBuf(),      Exception.class),   // W14 passed
                    Arguments.of(unpooledByteBufAllocator(),  validFileChannel(), 100, 100, 1,      deallocatedByteBuf(),           Exception.class),   // W15 passed
                    Arguments.of(unpooledByteBufAllocator(),  validFileChannel(), 100, 100, 1,      null,                           Exception.class),    // W16 passed

                    // Added test after jacoco analysis
                    Arguments.of(unpooledByteBufAllocator(),  validFileChannel(), BC_BB_STRING_TEST.length()/2,    100, 0, fullByteBuf(), null)    // J-W1
//                    ,Arguments.of(unpooledByteBufAllocator(),  validFileChannel(), BC_BB_STRING_TEST.length()/2+1,  100, 0, fullByteBuf(), null)            // J-W2

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
        String expectedWrittenContent;
        int expectedWrittenContentLength;
        long initialFileChannelPosition;
        long expectedPosition, expectedUnpersistedBytes, expectedWriteBufferStartPosition;

        try {
            bc = new BufferedChannel(allocator, fc, writeCapacity, readCapacity, unpersistedBytesBound);
            Assertions.assertNotNull(bc);
            initialFileChannelPosition = fc.position();
        } catch (Exception e) { throw new RuntimeException(e); }

        if (expectedException != null) Assertions.assertThrows(expectedException, () -> bc.write(src));
        else {
            try{
                bc.write(src);

                expectedWrittenContent    = src.toString(StandardCharsets.UTF_8);
                expectedWrittenContentLength = expectedWrittenContent.length();

                // ====================================  Check written content ===================================== //
                boolean fileChannelWritten = expectedWrittenContentLength >= unpersistedBytesBound;
                String actualWrittenContent;

                if(fileChannelWritten) {
                    ByteBuffer bb = ByteBuffer.allocate(BC_FC_STRING_TEST.length());
                    fc.read(bb, initialFileChannelPosition);
                    bb.flip();
                    actualWrittenContent = new String(bb.array(), 0, bb.limit());
                    Assertions.assertEquals(expectedWrittenContent, actualWrittenContent);

                    expectedPosition                 = initialFileChannelPosition + expectedWrittenContentLength;
                    expectedUnpersistedBytes         = 0L;
                    expectedWriteBufferStartPosition = initialFileChannelPosition + expectedWrittenContentLength;
                }
                else { // is the write buffer that has been written
                    ByteBuf actualWrittenBuffer = Unpooled.buffer(BC_FC_STRING_TEST.length());
                    bc.writeBuffer.getBytes(0, actualWrittenBuffer, expectedWrittenContentLength);
                    actualWrittenContent = actualWrittenBuffer.toString(StandardCharsets.UTF_8);
                    Assertions.assertEquals(expectedWrittenContent, actualWrittenContent);

                    expectedPosition                 = initialFileChannelPosition + expectedWrittenContentLength;
                    expectedUnpersistedBytes         = expectedWrittenContentLength;
                    expectedWriteBufferStartPosition = initialFileChannelPosition;
                }

                // =====================================  Check class fields ====================================== //
                Assertions.assertEquals(expectedPosition,                   bc.position);
                Assertions.assertEquals(expectedUnpersistedBytes,           bc.unpersistedBytes.get());
                Assertions.assertEquals(expectedWriteBufferStartPosition,   bc.writeBufferStartPosition.get());
            }
            catch (Exception e) {  throw new RuntimeException(e); }
        }
    }

    @AfterEach
    public void deleteTestFile() throws IOException {
        Path path = Paths.get(BC_TEST_FILE);
        if (Files.exists(path)) Files.delete(path);
    }
    @Test
    void testWriteTriggeringForceWrite() throws Exception {
        // Create a temporary file and FileChannel for testing
        File tempFile = File.createTempFile("test", ".tmp");
        tempFile.deleteOnExit();

        try (FileChannel fileChannel = new RandomAccessFile(tempFile, "rw").getChannel()) {
            // Define the write buffer capacity and unpersistedBytesBound
            int writeCapacity = 1024;
            long unpersistedBytesBound = 712; // Enable doRegularFlushes

            // Initialize BufferedChannel
            BufferedChannel bufferedChannel = new BufferedChannel(unpooledByteBufAllocator(), fileChannel, writeCapacity, unpersistedBytesBound);

            // Create a ByteBuf with data to write
            ByteBuf src = Unpooled.buffer(600);
            src.writeBytes(new byte[600]); // Write 600 bytes of data

            // Write to BufferedChannel
            bufferedChannel.write(src);

            // Check that unpersistedBytes has been updated
            Assertions.assertEquals(600, bufferedChannel.getUnpersistedBytes());

            // Clean up resources
            src.release();
        }
    }
}
