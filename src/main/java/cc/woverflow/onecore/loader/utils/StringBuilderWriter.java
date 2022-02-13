package cc.woverflow.onecore.loader.utils;

import java.io.Serializable;
import java.io.Writer;

/**
 * <code>java.io.StringWriter</code> but un-synchronized.
 * Taken from https://commons.apache.org/proper/commons-io/download_io.cgi under Apache License 2.0.
 */
public class StringBuilderWriter extends Writer implements Serializable {

    private final StringBuilder builder = new StringBuilder();

    @Override
    public Writer append(char value) {
        builder.append(value);
        return this;
    }

    @Override
    public Writer append(CharSequence value) {
        builder.append(value);
        return this;
    }

    @Override
    public Writer append(CharSequence value, int start, int end) {
        builder.append(value, start, end);
        return this;
    }

    @Override
    public void close() {
    }

    @Override
    public void flush() {
    }


    @Override
    public void write(String value) {
        builder.append(value);
    }

    @Override
    public void write(char[] value, int offset, int length) {
        builder.append(value, offset, length);
    }

    public StringBuilder getBuilder() {
        return builder;
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
