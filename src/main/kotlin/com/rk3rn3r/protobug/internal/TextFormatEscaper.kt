// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
// https://developers.google.com/protocol-buffers/
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//     * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

// Based on com.google.protobuf.TextFormatEscaper , @see https://github.com/google/protobuf/blob/master/java/core/src/main/java/com/google/protobuf/TextFormatEscaper.java

package com.rk3rn3r.protobug.internal

import com.google.protobuf.ByteString
import unsigned.ushr
import unsigned.and
import unsigned.toUbyte

/**
 * Provide text format escaping support for proto2 instances.
 */
internal object TextFormatEscaper {

    internal interface ByteSequence {
        fun size(): Int
        fun byteAt(offset: Int): Byte
    }

    /**
     * Escapes bytes in the format used in protocol buffer text format, which
     * is the same as the format used for C string literals.  All bytes
     * that are not printable 7-bit ASCII characters are escaped, as well as
     * backslash, single-quote, and double-quote characters.  Characters for
     * which no defined short-hand escape sequence is defined will be escaped
     * using 3-digit octal sequences.
     */
    fun escapeBytes(input: ByteSequence): String {
        val builder = StringBuilder(input.size())
        for (i in 0 until input.size()) {
            val b = input.byteAt(i).toChar()
            when (b) {
            // Java does not recognize \a or \v, apparently.
                0x07.toChar() -> builder.append("\\a")
                '\b' -> builder.append("\\b")
                //'\f' -> builder.append("\\f") // @TODO need to be fixed
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                0x0b.toChar() -> builder.append("\\v")
                '\\' -> builder.append("\\\\")
                '\'' -> builder.append("\\\'")
                '"' -> builder.append("\\\"")
                else ->
                    // Only ASCII characters between 0x20 (space) and 0x7e (tilde) are
                    // printable.  Other byte values must be escaped.
                    if (b.toByte() >= 0x20 && b.toByte() <= 0x7e) {
                        builder.append(b.toChar())
                    } else {
                        builder.append('\\')
                        builder.append(('0'.toInt() + ((b.toByte() ushr 6) and 3.toUbyte())).toChar())
                        builder.append(('0'.toInt() + ((b.toByte() ushr 3) and 7.toUbyte())).toChar())
                        builder.append(('0'.toInt() + (b.toByte() and 7.toUbyte())).toChar())
                    }
            }
        }
        return builder.toString()
    }

    /**
     * Escapes bytes in the format used in protocol buffer text format, which
     * is the same as the format used for C string literals.  All bytes
     * that are not printable 7-bit ASCII characters are escaped, as well as
     * backslash, single-quote, and double-quote characters.  Characters for
     * which no defined short-hand escape sequence is defined will be escaped
     * using 3-digit octal sequences.
     */
    fun escapeBytes(input: ByteString): String {
        return escapeBytes(object : ByteSequence {
            override fun size(): Int {
                return input.size()
            }

            override fun byteAt(offset: Int): Byte {
                return input.byteAt(offset)
            }
        })
    }

    /**
     * Like [.escapeBytes], but used for byte array.
     */
    fun escapeBytes(input: ByteArray): String {
        return escapeBytes(object : ByteSequence {
            override fun size(): Int {
                return input.size
            }

            override fun byteAt(offset: Int): Byte {
                return input[offset]
            }
        })
    }

    /**
     * Like [.escapeBytes], but escapes a text string.
     * Non-ASCII characters are first encoded as UTF-8, then each byte is escaped
     * individually as a 3-digit octal escape.  Yes, it's weird.
     */
    fun escapeText(input: String): String {
        return escapeBytes(ByteString.copyFromUtf8(input))
    }

    /**
     * Escape double quotes and backslashes in a String for unicode output of a message.
     */
    fun escapeDoubleQuotesAndBackslashes(input: String): String {
        return input.replace("\\", "\\\\").replace("\"", "\\\"")
    }
}
