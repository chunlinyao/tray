package qz.utils;

import qz.common.ByteArrayBuilder;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Packbits compression utility class.<p>
 * Contains both unpack and packing method for the Macintosh Packbits compression standard.<p>
 * Contains Variation for 16 bit RLE packing used in PICT files.<p>
 * Contains a main() method with some test code for the pack unpack methods.
 *
 * @author  Robin Luiten
 * @version $Revision: 1.1.1.1 $
 */
public class Packbits
{
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
	   Reference Notes on RLE packbits compression algorithm. From TIFF6 spec.

	   Description

	   In choosing a simple byte-oriented run-length compression scheme, we arbitrarily
	   chose the Apple Macintosh PackBits scheme. It has a good worst case behavior
	   (at most 1 extra byte for every 128 input bytes). For Macintosh users, the toolbox
	   utilities PackBits and UnPackBits will do the work for you, but it is easy to imple-ment
	   your own routines.

	   A pseudo code fragment to unpack might look like this:

	   Loop until you get the number of unpacked bytes you are expecting:
	   Read the next source byte into n.
	   If n is between 0 and 127 inclusive, copy the next n+1 bytes literally.
	   Else if n is between -127 and -1 inclusive, copy the next byte -n+1 times.
	   Else if n is -128, noop.
	   Endloop

	   In the inverse routine, it is best to encode a 2-byte repeat run as a replicate run
	   except when preceded and followed by a literal run. In that case, it is best to merge
	   the three runs into one literal run. Always encode 3-byte repeats as replicate runs.
	   That is the essence of the algorithm. Here are some additional rules:
	   # Pack each row separately. Do not compress across row boundaries.
	   # The number of uncompressed bytes per row is defined to be (ImageWidth + 7) / 8.
	   If the uncompressed bitmap is required to have an even number of bytes per
	   row, decompress into word-aligned buffers.
	   # If a run is larger than 128 bytes, encode the remainder of the run as one or more
	   additional replicate runs.
	   When PackBits data is decompressed, the result should be interpreted as per com-pression
	   type 1 (no compression).
	* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    public static void main(String[] argv)  {

        byte[] in = new byte[] {(byte) 0xfb, 0x00, 0x00, (byte) 0x0f,  (byte)0xB6,  (byte)0xFF,0x00,  (byte)0xf8,  (byte)0xb2,0x00};
        byte[] out = new byte[162];
        unpackbits(in, out);
        byte[] ret = packbits(out);
        System.out.println(Arrays.equals(in, ret));
        System.out.println(Arrays.toString(in));
        System.out.println(Arrays.toString(out));
        System.out.println(Arrays.toString(ret));
    }
    /**
     * Decompress an RLE compressed buffer of bytes as per format as
     * described in source or in a document on TIFF packbits compression.
     *
     * Worst case for compression is that for every 128 bytes to
     * compress 1 extra byte is required to encode as a copy run.
     * therefore the input buffer should be large enough to accomodate
     * the worst case compression to the output buffer.
     *
     * Throws ArrayStoreException if the array is not large enough for the
     * unpacked data, which could be caused by corrupt input data or
     * the array not be allocated large enough.
     *
     * @param inb input buffer with compressed data
     * @param outb output buffer for decompressed data
     * the length of the output buffer must be exact decompressed
     * lenght of compressed input.
     */
    public static void unpackbits(byte[] inb, byte[] outb)
            throws ArrayStoreException, ArrayIndexOutOfBoundsException
    {
        int i;		// input index
        int o;		// output index
        int b;		// RLE compression marker byte
        byte rep;	// byte to replicate as required
        int end;	// end of byte replication run index

        i = 0;
        o = 0;
        for (o = 0; o < outb.length && i < inb.length;)	// for all output data required
        {
            b = inb[i++]; // P.rt(" b:"+b);
            if (b >= 0)					// duplicate bytes
            {
                ++b;	//P.rt(" copy:"+b);// convert to copy length
                System.arraycopy(inb, i, outb, o, b);
                i += b;					// new input location
                o += b;					// new output location
            }
            else if (b != -128) 		// replicate a byte
            {
                rep = inb[i++];	//P.rt(" r:"+rep);	// repetition byte
                end = o - b + 1;		// end of replication index
                for (; o < end; ++o)
                    outb[o] = rep;
            }
            // if b == -128 do nothing
        }
    }



    // states.
    final static int RAW = 0;
    final static int RLE = 1;
    final static int MAX_LENGTH = 127;

    public static byte[] packbits(byte[] inb)
            throws ArrayStoreException, ArrayIndexOutOfBoundsException
    {
        int state = RAW;
        int pos = 0;
        int repeatCount = 0;
        ByteArrayBuilder buf = new ByteArrayBuilder();
        ByteArrayBuilder result = new ByteArrayBuilder();
        for (;pos < inb.length -1; pos ++) {
            byte current = inb[pos];
            if (current == inb[pos + 1]) {
                if(state == RAW)  {
                    finishRaw(result, buf);
                    state = RLE;
                    repeatCount = 1;
                }else if (state == RLE) {
                    if(repeatCount == MAX_LENGTH) {
                        finishRle(result, repeatCount, current);
                        repeatCount = 0;
                    }
                    repeatCount ++;
                }
            } else {
                if(state == RLE)  {
                    repeatCount ++;
                    finishRle(result, repeatCount, current);
                    state = RAW;
                    repeatCount = 0;

                }else if (state == RAW) {
                    if (buf.getLength() == MAX_LENGTH) {
                        finishRaw(result, buf);
                    }
                    buf.append(new byte[]{current});
                }
            }

        }
        if (state == RAW) {
            buf.append(new byte[]{inb[pos]});
            finishRaw(result, buf);
        } else {
            repeatCount ++;
            finishRle(result, repeatCount, inb[pos]);
        }
        return result.getByteArray();
    }

    private static void finishRaw(ByteArrayBuilder result, ByteArrayBuilder buf) {
        if(buf.getLength() == 0) {
            return;
        }
        result.append(new byte[]{(byte) (buf.getLength() -1)});
        result.append(buf.getByteArray());
        buf.clear();
    }

    private static void finishRle(ByteArrayBuilder result, int repeatCount, byte data) {
        result.append(new byte[]{(byte) (256 - (repeatCount -1)), data});
    }
}