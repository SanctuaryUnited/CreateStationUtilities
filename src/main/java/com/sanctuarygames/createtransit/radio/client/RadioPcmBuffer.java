package com.sanctuarygames.createtransit.radio.client;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.BooleanSupplier;

/** Bounded PCM ring. Network/decode backpressure stays on the producer thread. */
final class RadioPcmBuffer {
    static final int CAPACITY_SECONDS = 8, STARTUP_SECONDS = 6;
    private final byte[] samples;
    private final int frameSize, startupBytes, silenceBytes;
    private int head, tail, size;
    private long underruns;
    private boolean ended, closed;
    private IOException failure;

    RadioPcmBuffer(int bytesPerSecond, int frameSize) {
        if (bytesPerSecond <= 0 || frameSize <= 0 || bytesPerSecond % frameSize != 0)
            throw new IllegalArgumentException("Invalid PCM format");
        this.frameSize = frameSize;
        samples = new byte[Math.multiplyExact(bytesPerSecond, CAPACITY_SECONDS)];
        startupBytes = Math.multiplyExact(bytesPerSecond, STARTUP_SECONDS);
        silenceBytes = Math.max(frameSize, bytesPerSecond / 4 / frameSize * frameSize);
    }

    synchronized void write(ByteBuffer input) throws InterruptedException {
        if (input.remaining() % frameSize != 0) throw new IllegalArgumentException("Partial PCM frame");
        while (input.hasRemaining() && !closed) {
            while (size == samples.length && !closed) wait();
            if (closed) return;
            int count = Math.min(input.remaining(), Math.min(samples.length - size, samples.length - tail));
            input.get(samples, tail, count);
            tail = (tail + count) % samples.length;
            size += count;
            notifyAll();
        }
    }

    synchronized void awaitStartup(BooleanSupplier canceled, long timeoutMillis) throws IOException {
        long deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
        while (size < startupBytes && !ended && !closed) {
            if (canceled.getAsBoolean()) throw new InterruptedIOException("Radio playback canceled");
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) throw new IOException("Radio stream could not fill its startup buffer");
            try { wait(Math.min(50, Math.max(1, remaining / 1_000_000L))); }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException("Radio playback interrupted");
            }
        }
        if (closed || canceled.getAsBoolean()) throw new InterruptedIOException("Radio playback canceled");
        if (size == 0) throw failure != null ? failure : new IOException("Radio stream contained no audio");
    }

    /** Never waits for network or decoding. Partial real audio is NOT padded with silence. */
    synchronized ByteBuffer read(int requested) {
        int wanted = Math.max(0, Math.min(requested, samples.length)) / frameSize * frameSize;
        int count = Math.min(wanted, size);
        if (count == 0 && wanted > 0 && !ended && !closed) {
            // An actual network underrun, not merely a decoder-chunk boundary.
            // A short fallback keeps OpenAL's source alive while fresh data arrives.
            underruns++;
            return ByteBuffer.allocateDirect(Math.min(wanted, silenceBytes)).order(ByteOrder.LITTLE_ENDIAN);
        }
        var output = ByteBuffer.allocateDirect(count).order(ByteOrder.LITTLE_ENDIAN);
        int first = Math.min(count, samples.length - head);
        output.put(samples, head, first);
        output.put(samples, 0, count - first);
        head = (head + count) % samples.length;
        size -= count;
        notifyAll();
        return output.flip();
    }

    synchronized void finish(IOException cause) { failure = cause; ended = true; notifyAll(); }
    synchronized void close() { closed = true; size = 0; notifyAll(); }
    synchronized int bufferedBytes() { return size; }
    int capacityBytes() { return samples.length; }
    synchronized boolean ended() { return ended; }
    synchronized long underruns() { return underruns; }
}
