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

// Based on com.google.protobuf.TextFormat , @see https://github.com/google/protobuf/blob/master/java/core/src/main/java/com/google/protobuf/TextFormat.java

package com.rk3rn3r.protobug.internal

import com.google.protobuf.ByteString
import com.google.protobuf.DynamicMessage
import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.UnknownFieldSet
import com.google.protobuf.WireFormat
import unsigned.and
import unsigned.toUint
import unsigned.ushr
import java.io.IOException
import java.math.BigInteger

object PrettyTextFormat {

    /**
     * An inner class for writing text to the output stream.
     */
    class TextGenerator (private val output: Appendable, private val singleLineMode: Boolean) {
        private val indent = StringBuilder()
        // While technically we are "at the start of a line" at the very beginning of the output, all
        // we would do in response to this is emit the (zero length) indentation, so it has no effect.
        // Setting it false here does however suppress an unwanted leading space in single-line mode.
        private var atStartOfLine = false

        /**
         * Indent text by two spaces.  After calling Indent(), two spaces will be
         * inserted at the beginning of each line of text.  Indent() may be called
         * multiple times to produce deeper indents.
         */
        fun indent() {
            indent.append("  ")
        }

        /**
         * Reduces the current indent level by two spaces, or crashes if the indent
         * level is zero.
         */
        fun outdent() {
            val length = indent.length
            if (length == 0) {
                throw IllegalArgumentException(
                        " Outdent() without matching Indent().")
            }
            indent.setLength(length - 2)
        }

        /**
         * Print text to the output stream. Bare newlines are never expected to be passed to this
         * method; to indicate the end of a line, call "eol()".
         */
        @Throws(IOException::class)
        fun print(text: CharSequence) {
            if (atStartOfLine) {
                atStartOfLine = false
                output.append(if (singleLineMode) " " else (indent as CharSequence))
            }
            output.append(text)
        }

        /**
         * Signifies reaching the "end of the current line" in the output. In single-line mode, this
         * does not result in a newline being emitted, but ensures that a separating space is written
         * before the next output.
         */
        @Throws(IOException::class)
        fun eol() {
            if (!singleLineMode) {
                output.append("\n")
            }
            atStartOfLine = true
        }
    }

    @JvmStatic
    fun printDynamicMessage(dynamicMessage: DynamicMessage, singleLineMode: Boolean): String {
        return printDynamicMessage(dynamicMessage, singleLineMode, false)
    }

    @JvmStatic
    fun printDynamicMessage(dynamicMessage: DynamicMessage, singleLineMode: Boolean, classicMode: Boolean): String {
        if (classicMode)
        {
            if (singleLineMode) {
                return com.google.protobuf.TextFormat.shortDebugString(dynamicMessage.unknownFields)
            }
            else {
                return com.google.protobuf.TextFormat.printToString(dynamicMessage.unknownFields)
            }
        }
        val sb = StringBuffer()
        val generator = TextGenerator(sb, singleLineMode)
        for (entry in dynamicMessage.unknownFields.asMap().entries) {
            val number = entry.key
            val field = entry.value
            printUnknownField(number, WireFormat.WIRETYPE_VARINT,
                    field.varintList, generator)
            printUnknownField(number, WireFormat.WIRETYPE_FIXED32,
                    field.fixed32List, generator)
            printUnknownField(number, WireFormat.WIRETYPE_FIXED64,
                    field.fixed64List, generator)
            printUnknownField(number, WireFormat.WIRETYPE_LENGTH_DELIMITED,
                    field.lengthDelimitedList, generator)
            for (value in field.groupList) {
                generator.print(entry.key.toString())
                generator.print(" {")
                generator.eol()
                generator.indent()
                printUnknownFields(value, generator)
                generator.outdent()
                generator.print("}")
                generator.eol()
            }
        }
        return sb.toString()
    }

    /** Convert an unsigned 64-bit integer to a string.  */
    private fun unsignedToString(value: Long): String {
        return if (value >= 0) {
            java.lang.Long.toString(value)
        } else {
            // Pull off the most-significant bit so that BigInteger doesn't think
            // the number is negative, then set it again using setBit().
            BigInteger.valueOf(value and 0x7FFFFFFFFFFFFFFFL)
                    .setBit(63).toString()
        }
    }

    @Throws(IOException::class)
    private fun printUnknownFields(unknownFields: UnknownFieldSet,
                                   generator: TextGenerator) {
        for ((number, field) in unknownFields.asMap()) {
            printUnknownField(number, WireFormat.WIRETYPE_VARINT,
                    field.varintList, generator)
            printUnknownField(number, WireFormat.WIRETYPE_FIXED32,
                    field.fixed32List, generator)
            printUnknownField(number, WireFormat.WIRETYPE_FIXED64,
                    field.fixed64List, generator)
            printUnknownField(number, WireFormat.WIRETYPE_LENGTH_DELIMITED,
                    field.lengthDelimitedList, generator)
            for (value in field.groupList) {
                generator.print(number.toString())
                generator.print(" {")
                generator.eol()
                generator.indent()
                printUnknownFields(value, generator)
                generator.outdent()
                generator.print("}")
                generator.eol()
            }
        }
    }

    interface ByteSequence {
        fun size(): Int
        fun byteAt(offset: Int): Byte
    }

    private fun escapeBytes(input: ByteString): String {
        return escapeBytesInt(object : ByteSequence {
            override fun size(): Int {
                return input.size()
            }

            override fun byteAt(offset: Int): Byte {
                return input.byteAt(offset)
            }
        })
    }

    private fun escapeBytesInt(input: ByteSequence): String {
        val builder = StringBuilder(input.size())
        for (i in 0 until input.size()) {
            val b = input.byteAt(i).toChar()
            when (b) {
            // Java does not recognize \a or \v, apparently.
                0x07.toChar() -> builder.append("\\a")
                '\b' -> builder.append("\\b")
//            "\f" -> builder.append("\\f")
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
                        builder.append(('0'.toInt() + ((b.toByte() ushr 6) and 3.toUint())).toChar())
                        builder.append(('0'.toInt() + ((b.toByte() ushr 3) and 7.toUint())).toChar())
                        builder.append(('0'.toInt() + (b.toByte() and 7.toUint())).toChar())
                    }
            }
        }
        return builder.toString()
    }

    @Throws(IOException::class)
    private fun printUnknownFieldValue(tag: Int,
                                       value: kotlin.Any?,
                                       generator: TextGenerator) {
        when (WireFormat.getTagWireType(tag)) {
            WireFormat.WIRETYPE_VARINT -> generator.print(unsignedToString(value as Long))
            WireFormat.WIRETYPE_FIXED32 -> generator.print(
                    (value as Int).toString())
        //String.format("0x%08x", value as Int))
            WireFormat.WIRETYPE_FIXED64 -> generator.print((value as Long).toString())
            WireFormat.WIRETYPE_LENGTH_DELIMITED -> try {
                // Try to parse and print the field as an embedded message
                val message = UnknownFieldSet.parseFrom(value as ByteString)
                generator.print("{")
                generator.eol()
                generator.indent()
                printUnknownFields(message, generator)
                generator.outdent()
                generator.print("}")
            } catch (e: InvalidProtocolBufferException) {
                // If not parseable as a message, print as a String
                generator.print("\"")
                generator.print(escapeBytes(value as ByteString))
                generator.print("\"")
            }

            WireFormat.WIRETYPE_START_GROUP -> printUnknownFields(value as UnknownFieldSet, generator)
            else -> throw IllegalArgumentException("Bad tag: $tag")
        }
    }

    @Throws(IOException::class)
    private fun printUnknownField(number: Int,
                                  wireType: Int,
                                  values: List<*>,
                                  generator: TextGenerator) {
        for (value in values) {
            generator.print(number.toString())
            generator.print(": ")
            printUnknownFieldValue(wireType, value, generator)
            generator.eol()
        }
    }
}