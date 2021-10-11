# How it works:

Codec operates with [message groups](https://github.com/th2-net/th2-grpc-common/blob/f2794b2c5c8ae945e7500677439809db9c576c43/src/main/proto/th2_grpc_common/common.proto#L97)
whom may contain a mix of [raw](https://github.com/th2-net/th2-grpc-common/blob/f2794b2c5c8ae945e7500677439809db9c576c43/src/main/proto/th2_grpc_common/common.proto#L84)
and [parsed](https://github.com/th2-net/th2-grpc-common/blob/f2794b2c5c8ae945e7500677439809db9c576c43/src/main/proto/th2_grpc_common/common.proto#L78) messages.

## Encoding

During encoding codec must replace each parsed message of supported [protocol](https://github.com/th2-net/th2-grpc-common/blob/f2794b2c5c8ae945e7500677439809db9c576c43/src/main/proto/th2_grpc_common/common.proto#L47)
in a message group with a raw one by encoding parsed message's content

> **NOTE**: codec can merge content of subsequent raw messages into a resulting raw message  
> (e.g. when a codec encodes only a transport layer and its payload is already encoded)

## Decoding

During decoding codec must replace each raw message in a message group with a parsed one by decoding raw message's content

> **NOTE**: codec can replace raw message with a parsed message followed by a several raw messages
> (e.g. when a codec decodes only a transport layer it can produce a parsed message for the transport layer and several raw messages for its payload)

# Configuration

Codec has four types of connection: stream and general for encode and decode functions.

* stream encode / decode connections works 24 / 7
* general encode / decode connections works on demand

Codec never mixes messages from the _stream_ and the _general_ connections

### Configuration example

* typePointer - Path to message type value for decode (null by default)

```yaml
typePointer: /root/node/node2/type
```

For example:

```yaml
apiVersion: th2.exactpro.com/v1
kind: Th2Box
metadata:
  name: codec
spec:
  custom-config:
    codecSettings:
      typePointer: /root/node/node2/type
```

## Required pins

Every type of connection has two `subscribe` and `publish` pins.
The first one is used to receive messages to decode/encode while the second one is used to send decoded/encoded messages further.
**Configuration should include at least one pin for each of the following sets of attributes:**
+ Pin for the stream encoding input: `encoder_in` `parsed` `subscribe`
+ Pin for the stream encoding output: `encoder_out` `raw` `publish`
+ Pin for the general encoding input: `general_encoder_in` `parsed` `subscribe`
+ Pin for the general encoding output: `general_encoder_out` `raw` `publish`
+ Pin for the stream decoding input: `decoder_in` `raw` `subscribe`
+ Pin for the stream decoding output: `decoder_out` `parsed` `publish`
+ Pin for the stream decoding input: `general_decoder_in` `raw` `subscribe`
+ Pin for the stream decoding output: `general_decoder_out` `parsed` `publish`

### Configuration example

```yaml
apiVersion: th2.exactpro.com/v1
kind: Th2Box
metadata:
  name: codec
spec:
  custom-config:
    codecSettings:
      #typePointer: /root/node/node2/type
  pins:
    # encoder
    - name: in_codec_encode
      connection-type: mq
      attributes: [ 'encoder_in', 'parsed', 'subscribe' ]
    - name: out_codec_encode
      connection-type: mq
      attributes: [ 'encoder_out', 'raw', 'publish' ]
    # decoder
    - name: in_codec_decode
      connection-type: mq
      attributes: ['decoder_in', 'raw', 'subscribe']
    - name: out_codec_decode
      connection-type: mq
      attributes: ['decoder_out', 'parsed', 'publish']
    # encoder general (technical)
    - name: in_codec_general_encode
      connection-type: mq
      attributes: ['general_encoder_in', 'parsed', 'subscribe']
    - name: out_codec_general_encode
      connection-type: mq
      attributes: ['general_encoder_out', 'raw', 'publish']
    # decoder general (technical)
    - name: in_codec_general_decode
      connection-type: mq
      attributes: ['general_decoder_in', 'raw', 'subscribe']
    - name: out_codec_general_decode
      connection-type: mq
      attributes: ['general_decoder_out', 'parsed', 'publish']
```

## Message routing

Schema API allows configuring routing streams of messages via links between connections and filters on pins.
Let's consider some examples of routing in codec box.

### Split on 'publish' pins

For example, you got a big source data stream, and you want to split them into some pins via session alias.
You can declare multiple pins with attributes `['decoder_out', 'parsed', 'publish']` and filters instead of common pin or in addition to it.
Every decoded messages will be direct to all declared pins and will send to MQ only if it passes the filter.

```yaml
apiVersion: th2.exactpro.com/v1
kind: Th2Box
metadata:
  name: codec
spec:
  pins:
    # decoder
    - name: out_codec_decode_first_session_alias
      connection-type: mq
      attributes: ['decoder_out', 'parsed', 'publish', 'first_session_alias']
      filters:
        - metadata:
            - field-name: session_alias
              expected-value: first_session_alias
              operation: EQUAL
    - name: out_codec_decode_secon_session_alias
      connection-type: mq
      attributes: ['decoder_out', 'parsed', 'publish', 'second_session_alias']
      filters:
        - metadata:
            - field-name: session_alias
              expected-value: second_session_alias
              operation: EQUAL
```

The filtering can also be applied for pins with `subscribe` attribute.

## Changelog

### v0.0.1

#### Feature:

* First realization using underscore library
