/**
 * Copyright (c) 2013, xembly.org
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the xembly.org nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.xembly;

import com.jcabi.aspects.Immutable;
import com.jcabi.aspects.Loggable;
import lombok.EqualsAndHashCode;

/**
 * Argument properly escaped.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @since 0.1
 */
@Immutable
@EqualsAndHashCode(of = "value")
@Loggable(Loggable.DEBUG)
final class Arg {

    /**
     * Value of it.
     */
    private final transient String value;

    /**
     * Public ctor.
     * @param val Value of it
     * @throws XmlContentException If fails
     */
    protected Arg(final String val) throws XmlContentException {
        for (char chr : val.toCharArray()) {
            Arg.legal(chr);
        }
        this.value = val;
    }

    @Override
    public String toString() {
        return new StringBuilder()
            .append('"')
            .append(Arg.escape(this.value))
            .append('"')
            .toString();
    }

    /**
     * Get it's raw value.
     * @return Value
     */
    public String raw() {
        return this.value;
    }

    /**
     * Un-escape all XML symbols.
     * @param text XML text
     * @return Clean text
     * @throws XmlContentException If fails
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public static String unescape(final String text)
        throws XmlContentException {
        final StringBuilder output = new StringBuilder();
        final char[] chars = text.toCharArray();
        if (chars.length < 2) {
            throw new IllegalArgumentException(
                "internal error, argument can't be shorter than 2 chars"
            );
        }
        for (int idx = 1; idx < chars.length - 1; ++idx) {
            if (chars[idx] == '&') {
                final StringBuilder sbuf = new StringBuilder();
                while (chars[idx] != ';') {
                    // @checkstyle ModifiedControlVariable (1 line)
                    ++idx;
                    if (idx == chars.length) {
                        throw new XmlContentException(
                            "reached EOF while parsing XML symbol"
                        );
                    }
                    sbuf.append(chars[idx]);
                }
                output.append(Arg.symbol(sbuf.substring(0, sbuf.length() - 1)));
            } else {
                output.append(chars[idx]);
            }
        }
        return output.toString();
    }

    /**
     * Escape all unprintable characters.
     * @param text Raw text
     * @return Clean text
     */
    private static String escape(final String text) {
        final StringBuilder output = new StringBuilder();
        for (char chr : text.toCharArray()) {
            if (chr < ' ') {
                output.append("&#").append((int) chr).append(';');
            } else if (chr == '"') {
                output.append("&quot;");
            } else if (chr == '&') {
                output.append("&amp;");
            } else if (chr == '\'') {
                output.append("&apos;");
            } else if (chr == '<') {
                output.append("&lt;");
            } else if (chr == '>') {
                output.append("&gt;");
            } else {
                output.append(chr);
            }
        }
        return output.toString();
    }

    /**
     * Convert XML symbol to char.
     * @param symbol XML symbol, without leading ampersand
     * @return Character
     * @throws XmlContentException If fails
     */
    private static char symbol(final String symbol) throws XmlContentException {
        final char chr;
        if (symbol.charAt(0) == '#') {
            final int num = Integer.parseInt(symbol.substring(1));
            chr = (char) Arg.legal(num);
        } else if ("apos".equals(symbol)) {
            chr = '\'';
        } else if ("quot".equals(symbol)) {
            chr = '"';
        } else if ("lt".equals(symbol)) {
            chr = '<';
        } else if ("gt".equals(symbol)) {
            chr = '>';
        } else if ("amp".equals(symbol)) {
            chr = '&';
        } else {
            throw new XmlContentException(
                String.format("unknown XML symbol &%s;", symbol)
            );
        }
        return chr;
    }

    /**
     * Validate char number and throw exception if it's not legal.
     * @param number Char number
     * @return The same number
     * @throws XmlContentException If illegal
     */
    private static int legal(final int number) throws XmlContentException {
        // @checkstyle MagicNumber (5 lines)
        Arg.range(number, 0x00, 0x08);
        Arg.range(number, 0x0B, 0x0C);
        Arg.range(number, 0x0E, 0x1F);
        Arg.range(number, 0x7F, 0x84);
        Arg.range(number, 0x86, 0x9F);
        return number;
    }

    /**
     * Throw if number is in the range.
     * @param number Char number
     * @param left Left number (inclusive)
     * @param right Right number (inclusive)
     * @throws XmlContentException If illegal
     */
    private static void range(final int number, final int left, final int right)
        throws XmlContentException {
        if (number >= left && number <= right) {
            throw new XmlContentException(
                String.format(
                    // @checkstyle LineLength (1 line)
                    "Character #%02X is in restricted XML range #%02X-#%02X, see http://www.w3.org/TR/2004/REC-xml11-20040204/#charsets",
                    number, left, right
                )
            );
        }
    }

}
