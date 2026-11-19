package com.sanctuarygames.createtransit.radio.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.DoubleSupplier;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.AudioStream;

/** Per-emitter gain and bass EQ over a private copy of the shared mono PCM. */
public final class RadioGainStream implements AudioStream {
    private final AudioStream source;
    private final DoubleSupplier gain;
    private final DoubleSupplier bassDb;
    private final double bassCoefficient;
    private double low;

    public RadioGainStream(AudioStream source, DoubleSupplier gain, DoubleSupplier bassDb) {
        this.source = source;
        this.gain = gain;
        this.bassDb = bassDb;
        bassCoefficient = -Math.expm1(-2 * Math.PI * 120 / source.getFormat().getSampleRate());
    }

    @Override public AudioFormat getFormat() { return source.getFormat(); }

    @Override public ByteBuffer read(int size) throws IOException {
        var data = source.read(size).order(ByteOrder.LITTLE_ENDIAN);
        double volume = Math.clamp(gain.getAsDouble(), 1, 3);
        double bass = Math.pow(10, Math.clamp(bassDb.getAsDouble(), 0, 6) / 20) - 1;
        for (int i = data.position(); i + 1 < data.limit(); i += 2) {
            short sample = data.getShort(i);
            low += (sample - low) * bassCoefficient;
            double boosted = (sample + bass * low) * volume;
            data.putShort(i, (short)Math.clamp(Math.round(boosted), -32768, 32767));
        }
        return data;
    }

    @Override public void close() throws IOException { source.close(); }
}
