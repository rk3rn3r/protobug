package com.rk3rn3r.protobug

import com.google.protobuf.ByteString
import com.google.protobuf.DynamicMessage
import com.google.protobuf.EmptyProto
import com.rk3rn3r.protobug.internal.PrettyTextFormat

fun main(args : Array<String>) {
    var classicMode = false
    for (arg in args) {
        when (arg) {
            "--print-classic" -> classicMode = true
        }
    }
    println(
            PrettyTextFormat.printDynamicMessage(
            DynamicMessage.parseFrom(
                EmptyProto.getDescriptor().messageTypes[0],
                ByteString.readFrom(System.`in`)
            ),
            false,
            classicMode
        )
    )
}