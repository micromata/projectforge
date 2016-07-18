/*
 * Class for implementing the (notoriously weak) md4 hash algorithm.
 *
 * There are constructors for prepping the hash algorithm (doing the
 * padding, mainly) for a String or a byte[], and an mdcalc() method
 * for generating the hash. The results can be accessed as an int array
 * by getregs(), or as a String of hex digits with toString().
 *
 * Written for jotp, by Harry Mantakos harry@meretrix.com
 *
 * Feel free to do whatever you like with this code.
 * If you do modify or use this code in another application,
 * I'd be interested in hearing from you!
 *
 * Included in Ganymede
 *
 * Created: 15 March 2001
 * Version: $Revision$
 * Last Mod Date: $Date$
 */

package arlut.csd.crypto;

/*------------------------------------------------------------------------------
                                                                           class
                                                                             md4

------------------------------------------------------------------------------*/

public class Md4 {
  private int A,B,C,D;
  private int d[];
  private int numwords;

  /* -- */

  /**
   * For verification of a modicum of sanity, run a few
   * test strings through
   */

  public static void main(final String argv[])
  {
    /* Test cases, mostly taken from rfc 1320 */
    final String str[] = { "" , "a", "abc", "message digest",
        "abcdefghijklmnopqrstuvwxyz",
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789",
        "12345678901234567890123456789012345678901234567890123456789012345678901234567890",
    "01234567890123456789012345678901234567890123456789012345"};

    for (int i = 0; i < str.length; i++)
    {
      final Md4 mdc = new Md4(str[i]);
      mdc.calc();
      //System.out.println("md4(\"" + str[i] + "\") = " + mdc);
    }
  }

  static int F(final int x, final int y, final int z)
  {
    return ((x & y) | (~x & z));
  }

  static int G(final int x, final int y, final int z)
  {
    return ((x & y) | (x & z) | (y & z));
  }

  static int H(final int x, final int y, final int z)
  {
    return (x ^ y ^ z);
  }

  static int rotintlft(final int val, final int numbits)
  {
    return ((val << numbits) | (val >>> (32 - numbits)));
  }

  static String tohex(int i)
  {
    final StringBuilder buf = new StringBuilder();

    for (int b = 0; b < 4; b++)
    {
      buf.append(Integer.toString((i >> 4) & 0xf, 16));
      buf.append(Integer.toString(i & 0xf, 16));

      i >>= 8;
    }

    return buf.toString().toUpperCase();
  }

  // ---

  /**
   * String constructor for md4
   */

  public Md4(final String s)
  {
    final byte in[] = new byte[s.length()];
    int i;

    for (i=0; i < s.length(); i++)
    {
      in[i] = (byte) (s.charAt(i) & 0xff);
    }

    mdinit(in);
  }

  /**
   * Byte array constructor for md4
   */

  public Md4(final byte in[])
  {
    mdinit(in);
  }

  public int[] getregs()
  {
    final int regs[] = {this.A, this.B, this.C, this.D};

    return regs;
  }

  public void calc()
  {
    int AA, BB, CC, DD, i;

    for (i=0; i < numwords/16; i++)
    {
      AA = A; BB = B; CC = C; DD = D;
      round1(i);
      round2(i);
      round3(i);
      A += AA; B+= BB; C+= CC; D+= DD;
    }
  }

  @Override
  public String toString()
  {
    final String s;

    return (tohex(A) + tohex(B) + tohex(C) + tohex(D));
  }

  private void mdinit(final byte in[])
  {
    int newlen, endblklen, pad, i;
    long datalenbits;

    datalenbits = in.length  * 8;
    endblklen = in.length % 64;

    if (endblklen < 56)
    {
      pad = 64 - endblklen;
    }
    else
    {
      pad = (64 - endblklen) + 64;
    }

    newlen = in.length + pad;
    final byte b[] = new byte[newlen];

    for (i=0; i < in.length; i++)
    {
      b[i] = in[i];
    }

    b[in.length] = (byte) 0x80;

    for (i = b.length + 1; i < (newlen - 8); i++)
    {
      b[i] = 0;
    }

    for (i = 0; i < 8; i++)
    {
      b[newlen - 8 + i] = (byte) (datalenbits & 0xff);
      datalenbits >>= 8;
    }

    /* init registers */
    A = 0x67452301;
    B = 0xefcdab89;
    C = 0x98badcfe;
    D = 0x10325476;

    this.numwords = newlen/4;
    this.d = new int[this.numwords];

    for (i = 0; i < newlen; i += 4)
    {
      this.d[i/4] = (b[i] & 0xff) + ((b[i+1] & 0xff) << 8) +
          ((b[i+2] & 0xff) << 16) + ((b[i+3] & 0xff) << 24);
    }
  }

  private void round1(final int blk)
  {
    A = rotintlft((A + F(B, C, D) + d[0 + 16 * blk]), 3);
    D = rotintlft((D + F(A, B, C) + d[1 + 16 * blk]), 7);
    C = rotintlft((C + F(D, A, B) + d[2 + 16 * blk]), 11);
    B = rotintlft((B + F(C, D, A) + d[3 + 16 * blk]), 19);

    A = rotintlft((A + F(B, C, D) + d[4 + 16 * blk]), 3);
    D = rotintlft((D + F(A, B, C) + d[5 + 16 * blk]), 7);
    C = rotintlft((C + F(D, A, B) + d[6 + 16 * blk]), 11);
    B = rotintlft((B + F(C, D, A) + d[7 + 16 * blk]), 19);

    A = rotintlft((A + F(B, C, D) + d[8 + 16 * blk]), 3);
    D = rotintlft((D + F(A, B, C) + d[9 + 16 * blk]), 7);
    C = rotintlft((C + F(D, A, B) + d[10 + 16 * blk]), 11);
    B = rotintlft((B + F(C, D, A) + d[11 + 16 * blk]), 19);

    A = rotintlft((A + F(B, C, D) + d[12 + 16 * blk]), 3);
    D = rotintlft((D + F(A, B, C) + d[13 + 16 * blk]), 7);
    C = rotintlft((C + F(D, A, B) + d[14 + 16 * blk]), 11);
    B = rotintlft((B + F(C, D, A) + d[15 + 16 * blk]), 19);
  }

  private void round2(final int blk)
  {
    A = rotintlft((A + G(B, C, D) + d[0 + 16 * blk] + 0x5a827999), 3);
    D = rotintlft((D + G(A, B, C) + d[4 + 16 * blk] + 0x5a827999), 5);
    C = rotintlft((C + G(D, A, B) + d[8 + 16 * blk] + 0x5a827999), 9);
    B = rotintlft((B + G(C, D, A) + d[12 + 16 * blk] + 0x5a827999), 13);

    A = rotintlft((A + G(B, C, D) + d[1 + 16 * blk] + 0x5a827999), 3);
    D = rotintlft((D + G(A, B, C) + d[5 + 16 * blk] + 0x5a827999), 5);
    C = rotintlft((C + G(D, A, B) + d[9 + 16 * blk] + 0x5a827999), 9);
    B = rotintlft((B + G(C, D, A) + d[13 + 16 * blk] + 0x5a827999), 13);

    A = rotintlft((A + G(B, C, D) + d[2 + 16 * blk] + 0x5a827999), 3);
    D = rotintlft((D + G(A, B, C) + d[6 + 16 * blk] + 0x5a827999), 5);
    C = rotintlft((C + G(D, A, B) + d[10 + 16 * blk] + 0x5a827999), 9);
    B = rotintlft((B + G(C, D, A) + d[14 + 16 * blk] + 0x5a827999), 13);

    A = rotintlft((A + G(B, C, D) + d[3 + 16 * blk] + 0x5a827999), 3);
    D = rotintlft((D + G(A, B, C) + d[7 + 16 * blk] + 0x5a827999), 5);
    C = rotintlft((C + G(D, A, B) + d[11 + 16 * blk] + 0x5a827999), 9);
    B = rotintlft((B + G(C, D, A) + d[15 + 16 * blk] + 0x5a827999), 13);
  }

  private void round3(final int blk)
  {
    A = rotintlft((A + H(B, C, D) + d[0 + 16 * blk] + 0x6ed9eba1), 3);
    D = rotintlft((D + H(A, B, C) + d[8 + 16 * blk] + 0x6ed9eba1), 9);
    C = rotintlft((C + H(D, A, B) + d[4 + 16 * blk] + 0x6ed9eba1), 11);
    B = rotintlft((B + H(C, D, A) + d[12 + 16 * blk] + 0x6ed9eba1), 15);

    A = rotintlft((A + H(B, C, D) + d[2 + 16 * blk] + 0x6ed9eba1), 3);
    D = rotintlft((D + H(A, B, C) + d[10 + 16 * blk] + 0x6ed9eba1), 9);
    C = rotintlft((C + H(D, A, B) + d[6 + 16 * blk] + 0x6ed9eba1), 11);
    B = rotintlft((B + H(C, D, A) + d[14 + 16 * blk] + 0x6ed9eba1), 15);

    A = rotintlft((A + H(B, C, D) + d[1 + 16 * blk] + 0x6ed9eba1), 3);
    D = rotintlft((D + H(A, B, C) + d[9 + 16 * blk] + 0x6ed9eba1), 9);
    C = rotintlft((C + H(D, A, B) + d[5 + 16 * blk] + 0x6ed9eba1), 11);
    B = rotintlft((B + H(C, D, A) + d[13 + 16 * blk] + 0x6ed9eba1), 15);

    A = rotintlft((A + H(B, C, D) + d[3 + 16 * blk] + 0x6ed9eba1), 3);
    D = rotintlft((D + H(A, B, C) + d[11 + 16 * blk] + 0x6ed9eba1), 9);
    C = rotintlft((C + H(D, A, B) + d[7 + 16 * blk] + 0x6ed9eba1), 11);
    B = rotintlft((B + H(C, D, A) + d[15 + 16 * blk] + 0x6ed9eba1), 15);
  }
}
