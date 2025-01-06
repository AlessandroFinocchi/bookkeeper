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
                    Arguments.of(invalidByteBufAllocator(),  validFileChannel(), 20, 20, 0, Exception.class),        // T2
                    Arguments.of(unpooledByteBufAllocator(), validFileChannel(), 20, 20, -1, Exception.class)       // T17
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest
    @Timeout(value = 5)
    @MethodSource("data")
    public void write(ByteBufAllocator allocator, FileChannel fc, int writeCapacity, int readCapacity,
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
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @ParameterizedTest
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Disabled
    @MethodSource("data")
    public void read(ByteBufAllocator allocator, FileChannel fc, int writeCapacity, int readCapacity,
                     long unpersistedBytesBound, ByteBuf dest, long pos, int length, int expectedReturn,
                     Class<Exception> expectedException) {

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
