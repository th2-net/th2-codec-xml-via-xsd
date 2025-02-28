# Codec Xml via Xsd (2.4.1)
![version](https://img.shields.io/badge/version-0.0.4-blue.svg)

# How it works:

Codec operates with [message groups](https://github.com/th2-net/th2-grpc-common/blob/f2794b2c5c8ae945e7500677439809db9c576c43/src/main/proto/th2_grpc_common/common.proto#L97)
whom may contain a mix of [raw](https://github.com/th2-net/th2-grpc-common/blob/f2794b2c5c8ae945e7500677439809db9c576c43/src/main/proto/th2_grpc_common/common.proto#L84)
and [parsed](https://github.com/th2-net/th2-grpc-common/blob/f2794b2c5c8ae945e7500677439809db9c576c43/src/main/proto/th2_grpc_common/common.proto#L78) messages.

## Encoding

During encoding codec must replace each parsed message of empty or supported [protocol](https://github.com/th2-net/th2-grpc-common/blob/f2794b2c5c8ae945e7500677439809db9c576c43/src/main/proto/th2_grpc_common/common.proto#L47)
in a message group with a raw one by encoding parsed message's content.

## Decoding

During decoding codec must replace each raw message of empty or supported [protocol](https://github.com/th2-net/th2-grpc-common/blob/f2794b2c5c8ae945e7500677439809db9c576c43/src/main/proto/th2_grpc_common/common.proto#L54)
in a message group with a parsed one by decoding raw message's content.\
Attributes from xml will be parsed as fields with '-' at start of field name, as example *"-attributeName":"attributeValue"*.\
If field had attributes with value inside, value will be converted into field #text.

As example: 

---
```xml
<test f="456">some data</test>
``` 
into 
```kotlin
addField("test", message().apply {
    addFields("-f", "456")
    addFields("#text", "some data")
})
```
---

and

---

```xml
<h>A</h>
``` 
into
```kotlin
addField("h", "A")
```
---

All xml messages must have schemaLocation due validation. Xsd schema will be found in archive using this parameter.

# Configuration

Codec must be linked to dictionary. Dictionary is set of compressed xsd files. Zip archive must be converted to base64 and filled like a standard dictionary with converted data.\
Error from validation process can be disabled for test purposes by `dirtyValidation` option.

### Configuration example

* typePointer - Path to message type value for decode (null by default)
* dirtyValidation - Disable/enable error during validation phase. If disabled all errors will be only visible in log  (false by default)
* schemas - mapping between schema location value and a dictionary alias that should be used to load this schema

```yaml
typePointer: /root/node/node2/type
dirtyValidation: false
schemas:
  "test.xsd": "${dictionary_link:dict-xml}"
```

For example:

```yaml
apiVersion: th2.exactpro.com/v1
kind: Th2Box
metadata:
  name: codec-xml-xsd
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
  name: codec-xml-xsd
spec:
  image-name: ghcr.io/th2-net/th2-codec-xml-via-xsd
  image-version: #lastVersion
  type: th2-codec
#  custom-config:
#    codecSettings:
#      typePointer: /root/node/node2/type
#      dirtyValidation: false
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

## Changelog

### v2.4.1

* Update th2 gradle plugins: `0.1.3` -> `0.1.8`
* Correctly handle empty XML namespaces `xmlns=""` inside tag

### v2.4.0

* Codec decodes each raw message with `XML` (ignore case) or empty protocol
* Updated:
  * th2 gradle plugin `0.1.3` (bom: `4.8.0`)
  * common-utils: `2.3.0-dev`
  * underscore: `1.106`

### v2.3.0

* Migrated to th2 gradle plugin `0.1.1` (bom: `4.6.1`)
* Updated common: `5.14.0-dev`
* Updated common-utils `2.2.3-dev`
* Updated codec `5.5.0-dev`
* Updated workflows

### v2.2.0

* Migrate to StAX parser

### v2.1.0

* th2 transport protocol support

### v0.0.4

#### Changed:
* th2-common `3.44.0`
* th2-bom `4.1.0`
* kotlin `1.6.21`
* Dependency check pipeline step

### v0.0.4

#### Feature:

* Self-closing tags are ignored by the codec

### v0.0.3

#### Feature:

* Declaration check option
* Json step as converter was removed [ Message -> **Json** -> Map -> XmlString -> Xml ] to  [ Message -> Map -> XmlString -> Xml ] 

### v0.0.2

#### Feature:

* Check message protocol when decode as well as when encode

### v0.0.1

#### Feature:

* First realization using underscore library as parser for json
