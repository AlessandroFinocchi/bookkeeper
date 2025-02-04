package org.apache.bookkeeper.bookie.custom_utils;

import io.netty.buffer.*;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static org.mockito.Mockito.*;

public class Utils {
    /**
     * FileChannel file name
     */
    public static final String BC_TEST_FILE = "bc_test_file.txt";
    /**
     * String written in ByteBuf instances
     */
    public static final String BC_FC_CONTENT = "Hello world!";
    /**
     * String written in FileChannel files
     */
    public static final String BC_BB_CONTENT = "Ciao mondo?!";

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
        Files.write(path, BC_FC_CONTENT.getBytes());

        FileChannel fc = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
        fc.position(BC_FC_CONTENT.length());
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
        Files.write(path, BC_FC_CONTENT.getBytes());

        FileChannel fc = FileChannel.open(path, StandardOpenOption.READ);
        fc.position(BC_FC_CONTENT.length());
        return fc;
    }
    public static FileChannel writeOnlyFileChannel() throws IOException {
        Path path = Paths.get(BC_TEST_FILE);
        if (Files.exists(path)) Files.delete(path);
        Files.createFile(path);
        Files.write(path, BC_FC_CONTENT.getBytes());

        FileChannel fc = FileChannel.open(path, StandardOpenOption.WRITE);
        fc.position(BC_FC_CONTENT.length());
        return fc;
    }
    public static FileChannel invalidPositionFileChannel() throws IOException {
        FileChannel fc = spy(validFileChannel());
        when(fc.position()).thenReturn(-1L);
        return fc;
    }

    public static ByteBuf emptyByteBuf() {
        return Unpooled.buffer(BC_BB_CONTENT.length()+BC_FC_CONTENT.length(), BC_BB_CONTENT.length()+BC_FC_CONTENT.length());
    }
    public static ByteBuf semiFullByteBuf() {
        ByteBuf buffer = Unpooled.buffer(BC_BB_CONTENT.length(), BC_BB_CONTENT.length() );
        buffer.writeBytes(BC_BB_CONTENT.substring(0, BC_BB_CONTENT.length()/2).getBytes());
        return buffer;
    }
    public static ByteBuf fullByteBuf() {
        ByteBuf buffer = Unpooled.buffer(BC_BB_CONTENT.length(), BC_BB_CONTENT.length());
        buffer.writeBytes(BC_BB_CONTENT.getBytes());
        return buffer;
    }
    public static ByteBuf lenFullByteBuf(int len) {
        ByteBuf buffer = Unpooled.buffer(len);
        String s = new String(new char[len]).replace("\0", "a");
        buffer.writeBytes(s.getBytes());
        return buffer;
    }
    @SuppressWarnings("unused") public static ByteBuf invalidWriteIndexByteBuf() {
        ByteBuf buffer = spy(fullByteBuf());
        when(buffer.writerIndex()).thenReturn(BC_BB_CONTENT.length());
        when(buffer.readerIndex()).thenReturn(BC_BB_CONTENT.length() + 1);
        return buffer;
    } /* For read testing */
    public static ByteBuf invalidReadIndexByteBuf() {
        ByteBuf buffer = spy(fullByteBuf());
        when(buffer.readerIndex()).thenReturn(-1);
        return buffer;
    } /* For write testing */
    public static ByteBuf deallocatedByteBuf() {
        ByteBuf buffer = Unpooled.buffer(BC_BB_CONTENT.length());
        buffer.writeBytes(BC_BB_CONTENT.getBytes());
        // buffer reference count starts from 1, release decrements it by 1 so deallocating the buffer
        buffer.release();
        return buffer;
    }
}