package org.apache.bookkeeper.bookie;

import io.netty.buffer.*;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static org.mockito.Mockito.*;
// @SuppressWarnings("unused")
public class BufferedChannelUtils {
    public static final String BC_TEST_FILE = "bc_test_file.txt";
    public static final String BC_FC_STRING_TEST = "Hello world!";
    public static final String BC_BB_STRING_TEST = "Ciao mondoo?";

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
        ByteBuf buffer = Unpooled.buffer(BC_BB_STRING_TEST.length() * 2, BC_BB_STRING_TEST.length() * 2);
        buffer.writeBytes(BC_BB_STRING_TEST.getBytes());
        return buffer;
    }
    public static ByteBuf fullByteBuf() {
        ByteBuf buffer = Unpooled.buffer(BC_BB_STRING_TEST.length(), BC_BB_STRING_TEST.length());
        buffer.writeBytes(BC_BB_STRING_TEST.getBytes());
        return buffer;
    }
    public static ByteBuf invalidWriteIndexByteBuf() {
        ByteBuf buffer = mock(ByteBuf.class);
        buffer.writeBytes(BC_BB_STRING_TEST.getBytes());
        when(buffer.writerIndex()).thenReturn(BC_BB_STRING_TEST.length());
        when(buffer.readerIndex()).thenReturn(BC_BB_STRING_TEST.length() + 1);
        return buffer;
    }
    public static ByteBuf invalidByteBuf() {
        ByteBuf buffer = mock(ByteBuf.class);
        buffer.writeBytes(BC_BB_STRING_TEST.getBytes());
        when(buffer.readableBytes()).thenReturn(BC_BB_STRING_TEST.length());
        when(buffer.readerIndex()).thenReturn(-1);
        return buffer;
    }
    public static ByteBuf deallocatedByteBuf() {
        ByteBuf buffer = Unpooled.buffer(BC_BB_STRING_TEST.length());
        buffer.writeBytes(BC_BB_STRING_TEST.getBytes());
        // buffer reference count starts from 1, release decrements it by 1 so deallocating the buffer
        buffer.release();
        return buffer;
    }

//    public static void main(String[] args){
//        try {
//            FileChannel f = readOnlyFileChannel();
//            ByteBuf b = fullByteBuf();
//            System.out.println("X");
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//
//        }
//    }
}