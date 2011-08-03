package org.jibble.pircbot;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

public class FallbackInputStreamReader extends Reader {

    /**
     * Turn this on for low level debugging output
     */
    private static final boolean debug = false;

    private final PircBotLogger logger;
    private final InputStream in;
    private final Charset primaryCharset;
    private final Charset fallbackCharset;

    private boolean gotEof; // if true, fillBuffer() will not try to read more
    private ByteBuffer buf = ByteBuffer.allocate(1024); // if null after call to fillBuffer(), no more data available
    private int bufFillLevel; // how much of buf is filled (position in buf) - always kept up to date, even inside method calls
    private byte[] tmp = new byte[1024];

    private CharBuffer decBuf; // if null after call to fillDecodeBuffer(), no more data available
    private Charset lastCharset;

    public FallbackInputStreamReader(PircBotLogger logger, InputStream in, String primaryCharsetName, String fallbackCharsetName) throws UnsupportedEncodingException {
        this.logger = logger;
        this.in = in;
        this.primaryCharset = Charset.forName(primaryCharsetName);
        this.fallbackCharset = Charset.forName(fallbackCharsetName);
        buf.limit(0);
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        fillDecodeBuffer();
        if(decBuf == null) {
            return -1;
        }
        int rem = decBuf.remaining();
        if(rem < len) {
            len = rem;
        }
        decBuf.get(cbuf, off, len);
        if(debug) logger.log(lastCharset + " READ return '" + new String(cbuf, off, len) + "'");
        return len;
    }

    private void fillDecodeBuffer() throws IOException {
        if (decBuf != null && decBuf.hasRemaining()) {
            return;
        }
        fillBuffer();
        if (buf == null) {
            decBuf = null;
            return;
        }
        CharsetDecoder d = primaryCharset.newDecoder();
        d.onMalformedInput(CodingErrorAction.REPORT);
        d.onUnmappableCharacter(CodingErrorAction.REPORT);
        int origPos = buf.position();
        try {
            decBuf = d.decode(buf);
            lastCharset = primaryCharset;
        } catch(IOException e) {
            d = fallbackCharset.newDecoder();
            d.onMalformedInput(CodingErrorAction.REPLACE);
            d.onUnmappableCharacter(CodingErrorAction.REPLACE);
            try {
                // rewind
                buf.position(origPos);
                decBuf = d.decode(buf);
                lastCharset = fallbackCharset;
            } catch (CharacterCodingException e1) {
                throw new RuntimeException(e1);
            }
        }
    }

    private void fillBuffer() throws IOException {
        if(debug) logger.log(buf == null ? "FISR bp null bl null fl n/a" : ("bp " + buf.position() + " bl " + buf.limit() + " fl " + bufFillLevel));

        // if we got EOF, just give everything we got left, if any
        if(gotEof) {
            if(buf != null && !buf.hasRemaining()) {
                buf = null;
            }
            if(debug) logger.log("FISR  -> EOF");
            return;
        }

        // sanity check
        if(buf != null && buf.hasRemaining()) {
            // this should never happen
            logger.log("### BUG in FISR: pos<lim: " + buf.position() + " < " + buf.limit());
            // discard remaining
            buf.position(buf.limit());
        }

        // compact remaining data
        buf.limit(bufFillLevel);
        buf.compact();
        bufFillLevel = buf.position();

        // see if we got a line feed already last round
        if(checkLF(0)) {
            return;
        }

        try {
            while(true) {
                // read some more data
                int r = in.read(tmp);

                // if we got EOF, just toss the remaining data back and remember EOF happened
                if(r == -1) {
                    gotEof = true;
                    tmp = null;
                    buf.flip();
                    if(debug) logger.log("FISR  -> 2 bp " + buf.position() + " bl " + buf.limit() + " fl " + bufFillLevel);
                    if(buf.remaining() == 0) {
                        buf = null;
                    }
                    return;
                }

                // make sure we have space to receive the data
                if(buf.remaining() < r) {
                    if(debug) logger.log("FISR bp Doubling buffer");
                    buf.flip();
                    ByteBuffer bb = ByteBuffer.allocate(buf.capacity() * 2);
                    bb.put(buf);
                    buf = bb;
                }

                // copy the newly received data to the buffer
                int start = buf.position();
                buf.put(tmp, 0, r);
                bufFillLevel = buf.position();

                // see if we got a line feed
                if(checkLF(start)) {
                    return;
                }
            }
        } catch (IOException e) {
            // this might happen non-fatally, for example SocketTimeoutException, so just update state so we can continue later.
            
            // set limit to 0 since nothing of what may be in the buffer will get consumed this round. This way next fillBuffer() call will continue where we left
            buf.limit(0);
            if(debug) logger.log("FISR  -> " + e + " bp " + buf.position() + " bl " + buf.limit() + " fl " + bufFillLevel);
            throw e;
        }
    }

    private boolean checkLF(int start) {
        for(int i=start; i<buf.position(); ++i) {
            if(buf.get(i) == '\n') {
                // do a flip, but only give away up until and including LF
                buf.position(0);
                buf.limit(i+1);
                if(debug) logger.log("FISR  -> 1 / " + start + " bp " + buf.position() + " bl " + buf.limit() + " fl " + bufFillLevel);
                return true;
            }
        }
        return false;
    }
}
