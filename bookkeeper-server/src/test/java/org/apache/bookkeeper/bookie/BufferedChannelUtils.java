package org.apache.bookkeeper.bookie;

import io.netty.buffer.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static org.mockito.Mockito.*;
// @SuppressWarnings("unused")
public class BufferedChannelUtils {
    /**
     * FileChannel file name
     */
    public static final String BC_TEST_FILE = "bc_test_file.txt";
    /**
     * String written in ByteBuf instances
     */
    public static final String BC_FC_STRING_TEST = "Hello world!";
    /**
     * String written in FileChannel files
     */
    public static final String BC_BB_STRING_TEST = "Ciao mondo?!";

    public static ByteBufAllocator unpooledByteBufAllocator() {
        return UnpooledByteBufAllocator.DEFAULT;
    }
    public static ByteBufAllocator invalidByteBufAllocator() {
        ByteBufAllocator bba = mock(ByteBufAllocator.class);

        when(bba.buffer()).thenReturn(null);
        when(bba.buffer(anyInt())).thenReturn(null);
        when(bba.buffer(anyInt(), anyInt())).thenReturn(null);

        when(bba.directBuffer()).thenReturn(null);
        when(bba.directBuffer(anyInt())).thenReturn(null);
        when(bba.directBuffer(anyInt(), anyInt())).thenReturn(null);

        return bba;
    }

    public static FileChannel validFileChannel() throws IOException {
        Path path = Paths.get(BC_TEST_FILE);
        if (Files.exists(path)) Files.delete(path);
        Files.createFile(path);
        Files.write(path, BC_FC_STRING_TEST.getBytes());

        FileChannel fc = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
        fc.position(BC_FC_STRING_TEST.length());
        return fc;
    }
    public static FileChannel closedFileChannel() throws IOException {
        FileChannel fileChannel = validFileChannel();
        fileChannel.close();
        return fileChannel;
    }
    public static FileChannel readOnlyFileChannel() throws IOException {
        Path path = Paths.get(BC_TEST_FILE);
        if (Files.exists(path)) Files.delete(path);
        Files.createFile(path);
        Files.write(path, BC_FC_STRING_TEST.getBytes());

        FileChannel fc = FileChannel.open(path, StandardOpenOption.READ);
        fc.position(BC_FC_STRING_TEST.length());
        return fc;
    }
    public static FileChannel writeOnlyFileChannel() throws IOException {
        Path path = Paths.get(BC_TEST_FILE);
        if (Files.exists(path)) Files.delete(path);
        Files.createFile(path);
        Files.write(path, BC_FC_STRING_TEST.getBytes());

        FileChannel fc = FileChannel.open(path, StandardOpenOption.WRITE);
        fc.position(BC_FC_STRING_TEST.length());
        return fc;
    }
    public static FileChannel invalidPositionFileChannel() throws IOException {
        FileChannel fc = spy(validFileChannel());
        when(fc.position()).thenReturn(-1L);
        return fc;
    }

    public static ByteBuf emptyByteBuf() {
        return Unpooled.buffer(BC_BB_STRING_TEST.length(), BC_BB_STRING_TEST.length());
    }
    public static ByteBuf semiFullByteBuf() {
        ByteBuf buffer = Unpooled.buffer(BC_BB_STRING_TEST.length(), BC_BB_STRING_TEST.length() );
        buffer.writeBytes(BC_BB_STRING_TEST.substring(0, BC_BB_STRING_TEST.length()/2).getBytes());
        return buffer;
    }
    public static ByteBuf fullByteBuf() {
        ByteBuf buffer = Unpooled.buffer(BC_BB_STRING_TEST.length(), BC_BB_STRING_TEST.length());
        buffer.writeBytes(BC_BB_STRING_TEST.getBytes());
        return buffer;
    }
    public static ByteBuf invalidWriteIndexByteBuf() {
        ByteBuf buffer = spy(fullByteBuf());
        when(buffer.writerIndex()).thenReturn(BC_BB_STRING_TEST.length());
        when(buffer.readerIndex()).thenReturn(BC_BB_STRING_TEST.length() + 1);
        return buffer;
    } /* For read testing */
    public static ByteBuf invalidReadIndexByteBuf() {
        ByteBuf buffer = spy(fullByteBuf());
        when(buffer.readerIndex()).thenReturn(-1);
        return buffer;
    } /* For write testing */
    public static ByteBuf deallocatedByteBuf() {
        ByteBuf buffer = Unpooled.buffer(BC_BB_STRING_TEST.length());
        buffer.writeBytes(BC_BB_STRING_TEST.getBytes());
        // buffer reference count starts from 1, release decrements it by 1 so deallocating the buffer
        buffer.release();
        return buffer;
    }

    public static void main(String[] args){
        try {
            // WRITE IN FILE CHANNEL
//            ByteBuf writeBuffer = unpooledByteBufAllocator().directBuffer(12);
//            ByteBuf src = fullByteBuf();
//            writeBuffer.writeBytes(src, 0, 12);
//            ByteBuffer toWrite = writeBuffer.internalNioBuffer(0, 12);
//            FileChannel f = readOnlyFileChannel();
//            f.position(0);
//            int numBytesWritten = f.write(toWrite);
//            System.out.println(numBytesWritten);


            // READ FROM FILE CHANNEL
//            ByteBuffer bb = ByteBuffer.allocate(BC_BB_STRING_TEST.length());
//            FileChannel f = readOnlyFileChannel();
//            f.read(bb, 6);
//            bb.flip();
//            System.out.println(new String(bb.array(), 0, bb.limit()));


            ByteBuf b1 = emptyByteBuf();
            ByteBuf b2 = semiFullByteBuf();
            ByteBuf b3 = fullByteBuf();
            ByteBuf b4 = invalidReadIndexByteBuf();
            System.out.println("X");
        } catch (Exception e) {
            throw new RuntimeException(e);

        }
    }
}