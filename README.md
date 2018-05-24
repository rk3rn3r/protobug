# Protobug

Protobug is a tiny Java tool to debug [Google Protocol Buffers / protobuf](https://developers.google.com/protocol-buffers/)
messages and print its deserialized content without the need to have the original `.proto` definition file/s or at least
the descriptors.

Example output:

```
$ cat ../prototest/item1.protoserialized.message | java -jar build/libs/protobug-0.1-SNAPSHOT.jar
2: {
  1: -10001
}
3: "testname02"
4: -222
5: "rk3rn3r@protobuf"

$ cat ../prototest/item1.protoserialized.message | java -jar build/libs/protobug-0.1-SNAPSHOT.jar --print-classic
2: {
  1: 0xffffffffffffd8ef
}
3: "testname02"
4: 0xffffffffffffff22
5: "rk3rn3r@protobuf2"
```

## Requirements

Java 8+, Kotlin 1.2.41

## Installation

```
## checkout
git clone git@github.com:rk3rn3r/protobug.git
cd protobug

## build executable
./gradlew build
```

# Usage

Just pipe a protobuf serialized message to protobug will give you a structured
output of the available fields with their field number and the related value:

```
$ cat myprotobuf.serialized.message | java -jar build/libs/protobug-0.1-SNAPSHOT.jar
2: {
  1: -10001
}
3: "testname02"
4: -222
5: "rk3rn3r@protobuf" 
```
This output is using a quickly rewritten PrettyTextFormatter, based on
Googles' Java [TextFormat](https://github.com/google/protobuf/blob/master/java/core/src/main/java/com/google/protobuf/TextFormat.java)
implementation.

You can also use the original implementation using the `--print-classic` parameter:

```
$ cat myprotobuf.serialized.message | java -jar build/libs/protobug-0.1-SNAPSHOT.jar --print-classic
2: {
  1: 0xffffffffffffd8ef
}
3: "testname02"
4: 0xffffffffffffff22
5: "rk3rn3r@protobuf2"
```

## Current State / Supported Values

I tested protobug with Int32 and Int64 and similar types: signed and unsigned,
and String. Decimals not tested yet, but should work at least with a similar
output like in classic-mode.
Proto2 and Proto3 messages and sub-message/s tested.

## Contribution

Contribution is always welcome! Just open a PR or create an issue.

## License

Apache License, Version 2.0, January 2004
http://www.apache.org/licenses/
