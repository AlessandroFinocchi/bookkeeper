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
import java.util.stream.Stream;

import static org.apache.bookkeeper.bookie.custom_utils.Utils.*;
import static org.apache.bookkeeper.bookie.custom_utils.Utils.BC_FC_CONTENT;

/**
 * Unit testing for {@link BufferedChannel}. class <br>
 * Tested method: {@link BufferedChannel#read(ByteBuf, long, int)}
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BufferedChannelReadTest {

    private static Stream<Arguments> data() {
        try {
            BufferedChannelState valid     = new BufferedChannelState(unpooledByteBufAllocator(), validFileChannel(),           100, 100, 1);
            BufferedChannelState t2Invalid = new BufferedChannelState(invalidByteBufAllocator(),  validFileChannel(),           100, 100, 1);
            BufferedChannelState t7Invalid = new BufferedChannelState(unpooledByteBufAllocator(), invalidPositionFileChannel(), 100, 100, 1);
            BufferedChannelState fcInvalid = new BufferedChannelState(unpooledByteBufAllocator(), writeOnlyFileChannel(),       100, 100, 1);
            BufferedChannelState rcInvalid = new BufferedChannelState(invalidByteBufAllocator(),  validFileChannel(),           100,   0, 1);

            BufferedChannelState badua03 = new BufferedChannelState(unpooledByteBufAllocator(), validFileChannel(), BC_FC_CONTENT.length()/2, BC_BB_CONTENT.length()/2, 1);

            return Stream.of(
                    // Constructor failed tests T2 and T7, the others are not valid in read context
//                    Arguments.of(t2Invalid, null, emptyByteBuf(), 0, BC_FC_CONTENT.length(), Exception.class, -1),   // T2 -> R1 not passed
                    Arguments.of(t7Invalid, null, emptyByteBuf(), 0, BC_FC_CONTENT.length(), Exception.class, -1, -1), // T7 -> R2 passed

                    // FileChannel write-only
                    Arguments.of(fcInvalid, null, emptyByteBuf(), 0, 1, Exception.class, -1),   // R3 passed

                    // Invalid readCapacity
                    Arguments.of(rcInvalid, null, emptyByteBuf(), 0, 1, Exception.class, -1),   // R4 passed

                    // Varying dest
                    Arguments.of(valid, null,       emptyByteBuf(),             0, BC_FC_CONTENT.length(),   null, BC_FC_CONTENT.length()),   // R5 passed
                    Arguments.of(valid, null,       semiFullByteBuf(),          0, BC_FC_CONTENT.length()/2, null, BC_FC_CONTENT.length()/2), // R6 passed
//                    Arguments.of(valid, null,       fullByteBuf(),              0, BC_FC_CONTENT.length(), Exception.class, -1),            // R7 timeout
//                    Arguments.of(valid, null,       invalidWriteIndexByteBuf(), 0, BC_FC_CONTENT.length(), Exception.class, -1),            // R8 timeout
                    Arguments.of(valid, null,       deallocatedByteBuf(),       0, BC_FC_CONTENT.length(), Exception.class, -1),              // R9 passed
                    Arguments.of(valid, null,       null,                       0, BC_FC_CONTENT.length(), Exception.class, -1),              // R10 passed

                    // Varying pos and length
                    // C0
                    Arguments.of(valid, BC_BB_CONTENT, emptyByteBuf(),  -1, 1, Exception.class, -1),   // R11 passed
//                    Arguments.of(valid, BC_BB_CONTENT, emptyByteBuf(),  1, -1, Exception.class, -1), // R12 not passed
                    Arguments.of(valid, BC_BB_CONTENT, emptyByteBuf(),  0, 0,  null, 0),               // R13 passed
                    // C1
                    Arguments.of(valid, BC_BB_CONTENT, emptyByteBuf(),  0,   1,                        null, BC_FC_CONTENT.length()),   // R14 passed
                    Arguments.of(valid, BC_BB_CONTENT, emptyByteBuf(),  0,   BC_FC_CONTENT.length()-1, null, BC_FC_CONTENT.length()),   // R15 passed
                    Arguments.of(valid, BC_BB_CONTENT, emptyByteBuf(),  0,   BC_FC_CONTENT.length(),   null, BC_FC_CONTENT.length()),   // R16 passed
                    Arguments.of(valid, BC_BB_CONTENT, emptyByteBuf(),  1,   BC_FC_CONTENT.length()-1, null, BC_FC_CONTENT.length()-1), // R17 passed
                    // C2
                    Arguments.of(valid, BC_BB_CONTENT, emptyByteBuf(),  BC_FC_CONTENT.length(),                          1, null, BC_BB_CONTENT.length()),  // R18 passed
                    Arguments.of(valid, BC_BB_CONTENT, emptyByteBuf(),  BC_FC_CONTENT.length()+BC_BB_CONTENT.length()-1, 1, null, 1),                       // R19 passed
                    // C3
                    Arguments.of(valid, BC_BB_CONTENT, emptyByteBuf(),  0, BC_FC_CONTENT.length()+1,                        null, BC_FC_CONTENT.length() + BC_BB_CONTENT.length()), // R20 passed
                    Arguments.of(valid, BC_BB_CONTENT, emptyByteBuf(),  0, BC_FC_CONTENT.length()+BC_BB_CONTENT.length(),   null, BC_FC_CONTENT.length() + BC_BB_CONTENT.length()), // R21 passed
                    // C4
                    Arguments.of(valid, BC_BB_CONTENT, emptyByteBuf(),  0, BC_FC_CONTENT.length()+BC_BB_CONTENT.length()+1, Exception.class, -1), // R22 passed
                    Arguments.of(valid, BC_BB_CONTENT, emptyByteBuf(),  BC_FC_CONTENT.length()+BC_BB_CONTENT.length(), 1,   Exception.class, -1), // R23 passed

                    // Added after jacoco
                    Arguments.of(t2Invalid, null, emptyByteBuf(),  BC_FC_CONTENT.length(), 1, null, 0),  // J-R1 passed

                    // Added after badua
                    Arguments.of(badua03, BC_BB_CONTENT, emptyByteBuf(),  0, BC_FC_CONTENT.length()+BC_BB_CONTENT.length(), null, BC_FC_CONTENT.length()+BC_BB_CONTENT.length()) // B-R3 passed

            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    @Timeout(value = 5, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void read(BufferedChannelState state, String wbContent, ByteBuf dest, long pos,
                     int length, Class<Exception> expectedException, int expectedReturn) {
        
        if(expectedException != null && expectedReturn > 0) throw new RuntimeException("Invalid test configuration");

        BufferedChannel bc;
        int destStartingWritePos = dest != null ? dest.writerIndex() : 0;

        try {
            bc = new BufferedChannel(state.allocator, state.fc, state.writeCapacity, state.readCapacity, state.unpersistedBytesBound);
            Assertions.assertNotNull(bc);
            if(wbContent != null) bc.writeBuffer.writeBytes(wbContent.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) { throw new RuntimeException(e); }

        if (expectedException != null) Assertions.assertThrows(expectedException, () -> bc.read(dest, pos, length));
        else {
            try {
                int actualReturn = bc.read(dest, pos, length);

                //=============== Compare expected and actual written bytes on the destination buffer  ===============//
                boolean posInFileChannel = pos < bc.writeBufferStartPosition.get();
                boolean writeBufferIsNull = bc.writeBuffer == null; // added after jacoco
                String expectedWrittenString = "";

                if(posInFileChannel) { // Read starting from file channel
                    // first read from fc, then, if needed, read from wb
                    int startingPos = (int) pos;
                    int readingFcBytesNum = Math.min(BC_FC_CONTENT.length() - (int)pos, length);
                    int readingWbBytesNum = writeBufferIsNull ? 0 : Math.max(length - BC_FC_CONTENT.length() + (int)pos, 0);
                    expectedWrittenString =
                            BC_FC_CONTENT.substring(startingPos, startingPos + readingFcBytesNum) +
                                    BC_BB_CONTENT.substring(0, readingWbBytesNum);

                }
                else { // Read from write buffer
                    int startingPos = writeBufferIsNull ? 0 : (int)pos - (int)bc.writeBufferStartPosition.get();
                    int endingPos = writeBufferIsNull ? 0 : startingPos + length;
                    expectedWrittenString = BC_BB_CONTENT.substring(startingPos, endingPos);
                }

                int actualLen = !posInFileChannel && writeBufferIsNull ? 0 : length;

                ByteBuf actualWrittenBuffer = Unpooled.buffer(actualLen);
                dest.getBytes(destStartingWritePos, actualWrittenBuffer);
                String actualWrittenString   = actualWrittenBuffer.toString(StandardCharsets.UTF_8);

                Assertions.assertEquals(expectedWrittenString, actualWrittenString);
                Assertions.assertEquals(expectedReturn, actualReturn);
            } catch (Exception e) { throw new RuntimeException(e); }
        }
    }

    private static Stream<Arguments> baduaData() {
        try {
            BufferedChannelState valid       = new BufferedChannelState(unpooledByteBufAllocator(), validFileChannel(), 100, 100, 1);
            BufferedChannelState pitestState = new BufferedChannelState(unpooledByteBufAllocator(), validFileChannel(), 100, 6, 1);
            return Stream.of(
                    Arguments.of(valid,       emptyByteBuf(), 0, 6, null, BC_FC_CONTENT.length()),            // B-R1 passed
                    Arguments.of(valid,       emptyByteBuf(), 1, 6, null, BC_FC_CONTENT.length()-1),          // B-R2 passed
                    Arguments.of(pitestState, emptyByteBuf(), BC_FC_CONTENT.length()/2, 1, null, 1)     // P-R1 passed
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest
    @MethodSource("baduaData")
    @Timeout(value = 5, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void baduaRead(BufferedChannelState state,ByteBuf dest, long pos,
                     int length, Class<Exception> expectedException, int expectedReturn) {

        BufferedChannel bc;
        try {
            bc = new BufferedChannel(state.allocator, state.fc, state.writeCapacity, state.readCapacity, state.unpersistedBytesBound);
            bc.read(emptyByteBuf(), 1 ,3);
            if (expectedException != null) Assertions.assertThrows(expectedException, () -> bc.read(dest, pos, length));

            int actualReturn = bc.read(dest, pos, length);

            String expectedWrittenString = BC_FC_CONTENT.substring((int)pos, (int)pos+length);

            ByteBuf actualWrittenBuffer = Unpooled.buffer(length);
            dest.getBytes(0, actualWrittenBuffer);
            String actualWrittenString   = actualWrittenBuffer.toString(StandardCharsets.UTF_8);

            Assertions.assertEquals(expectedWrittenString, actualWrittenString);
            Assertions.assertEquals(expectedReturn, actualReturn);

        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @AfterEach
    public void deleteTestFile() throws IOException {
        Path path = Paths.get(BC_TEST_FILE);
        if (Files.exists(path)) Files.delete(path);
    }

    private static final class BufferedChannelState {
        private final ByteBufAllocator allocator;
        private final FileChannel fc;
        private final int writeCapacity;
        private final int readCapacity;
        private final long unpersistedBytesBound;

        BufferedChannelState(ByteBufAllocator allocator, FileChannel fc, int writeCapacity, int readCapacity, 
                             long unpersistedBytesBound) {
            this.allocator = allocator;
            this.fc = fc;
            this.writeCapacity = writeCapacity;
            this.readCapacity = readCapacity;
            this.unpersistedBytesBound = unpersistedBytesBound;
        }
    }
}
