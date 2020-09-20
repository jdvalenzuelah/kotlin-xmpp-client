# xmpp client

> smack + kotlin client

## Requirements
- JRE 1.8
- Kotlin SDK 1.3+

## To compile dist:

**from main folder**
```
./gradlew compileKotlin distZip 
```

## To run:

First unzip the compiled dist
```
$ cd build/distributions
$ unzip chat-xmpp-0.1.zip 
```

Run passing the args:
```
./chat-xmpp-0.1/bin/chat-xmpp  --host <server host> --domain <server domain>
```
