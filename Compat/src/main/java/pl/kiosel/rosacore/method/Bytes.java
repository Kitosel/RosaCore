package pl.kiosel.playerlist.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;

public final class Bytes implements AutoCloseable {

    private ObjectOutputStream output;
    private ObjectInputStream input;
    private ByteArrayOutputStream cache;

    public Bytes() {
        this(new ByteArrayOutputStream());
    }

    public Bytes(byte[] bytes) {
        this(new ByteArrayInputStream(bytes));
    }

    public Bytes(File file, boolean read) {
        try {
            if (read) {
                input = new ObjectInputStream(Files.newInputStream(file.toPath()));
            } else {
                output = new ObjectOutputStream(Files.newOutputStream(file.toPath()));
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public Bytes(InputStream stream) {
        try {
            input = new ObjectInputStream(stream);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public Bytes(OutputStream stream) {
        try {
            if (stream instanceof ByteArrayOutputStream) {
                cache = (ByteArrayOutputStream) stream;
            }
            output = new ObjectOutputStream(stream);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    public void close() {
        IOException failure = null;
        try {
            if (output != null) {
                output.close();
            }
        } catch (IOException exception) {
            failure = exception;
        }
        try {
            if (input != null) {
                input.close();
            }
        } catch (IOException exception) {
            if (failure == null) {
                failure = exception;
            } else {
                failure.addSuppressed(exception);
            }
        }
        if (failure != null) {
            throw new UncheckedIOException(failure);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T read() {
        if (input == null) {
            throw new IllegalStateException("Bytes was not opened for reading");
        }
        try {
            return (T) input.readObject();
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Serialized class is unavailable", exception);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public synchronized byte[] toByteArray() {
        if (cache == null || output == null) {
            throw new IllegalStateException("Bytes is not backed by an in-memory output stream");
        }
        try {
            output.flush();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return cache.toByteArray();
    }

    public void write(Object value) {
        if (output == null) {
            throw new IllegalStateException("Bytes was not opened for writing");
        }
        try {
            output.writeObject(value);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public Bytes writes(Object... values) {
        for (Object value : values) {
            write(value);
        }
        return this;
    }
}
