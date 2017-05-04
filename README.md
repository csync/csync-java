# Contextual Sync 

[![Build Status - Master](https://travis-ci.org/csync/csync-java.svg?branch=master)](https://travis-ci.org/csync/csync-java)
[![License][license-svg]][license-link] 

[license-svg]: https://img.shields.io/hexpm/l/plug.svg
[license-link]: https://github.com/csync/csync-server/blob/master/LICENSE

Contextual Sync (CSync) is an open source, real-time, continuous data synchronization service for building modern applications. The CSync data store is organized with key/values where keys have a hierarchical structure. Clients can obtain the current value for a key and any subsequent updates by listening on the key. Updates are delivered to all online clients in near-real time. Clients can also listen on a key pattern where some components contain wildcards. 

## Keys
CSync is structured as a tree of nodes referenced by period-delimited strings called keys.

To illustrate :

```
          companies
       /              \
    ibm                google
   /   \               /     \ 
stock   offices    stock   offices
```

The above tree consists of the following keys : `companies`, `companies.ibm`, `companies.google`, `companies.ibm.stock`, `companies.ibm.offices`, `companies.google.stock`, `companies.google.offices`. Any one of these keys can be listened to at a time and all changes to that singular node will be synced to the client device. 

### Key Limitations
Keys can have a maximum of 16 parts and a total length of 200 characters. Key components may contain only uppercase and lowercase alphabetic, numeric, "_", and "-".

Valid key: `this.is.a.valid.key.123456.7.8.9.10`

Invalid key: `this is an.invalidkey.🍕.4.5.6.7.8.9.10.11.12.13.14.15.16.17.18`

### Wildcards in Keys
Suppose a developer wishes to listen to a subset of the tree containing multiple nodes, CSync provides this ability through wildcards. Currently CSync supports `*` and `#` as wildcards. 

#### Astrix Wildcard
An astrix (`*`) wildcard will match any value in the part of the key where the wildcard is. As an example, if a developer listens to `companies.*.stock` in the above tree, the client will sync with all stock nodes for all companies.

#### Hash Wildcard
If a developer wishes to listen to all child nodes in a subset of the tree, the `#` can appended to the end of a key and the client will sync with all child nodes of the specified key. For instance in the above tree if a user listens to `companies.ibm.#`, then the client will sync with all child nodes of `companies.ibm` which include `companies.ibm.stock` and `companies.ibm.offices`. 

**Note:** Each listen is independent. For example, if a developer listens to both `companies.*.stock` and `companies.companyX.stock`, the data from `companies.companyX.stock` will be received by both of the listeners. 

## Guaranteed Relevance
Only the latest, most recent, values sync, so you’re never left with old data. CSync provides a consistent view of the values for keys in the CSync store. If no updates are made to a key for a long enough period of time, all subscribers to the key will see the same consistent value. CSync guarantees that the latest update will be reflected at all connected, subscribed clients, but not that all updates to a key will be delivered. Clients will not receive an older value than what they have already received for a given key.

## Local Storage
Work offline, read and write, and have data automatically sync the next time you’re connected. CSync maintains a local cache of data that is available to the client even when the client is offline or otherwise not connected to the CSync service. The client may perform listens, writes, and deletes on the local store while offline. When the client reestablishes connectivity to the CSync service, the local cache is efficiently synchronized with the latest data from the CSync store. The local cache is persistent across application restarts and device reboots.

## Access Controls
Use simple access controls to clearly state who can read and write, keeping your data safe. Each key in the CSync store has an associated access control list (ACL) that specifies which users can access the key. 

Three specific forms of access are defined:
- Create: Users with create permission may create child keys of this key.
- Read: Users with read permission may read the data for the key.
- Write: Users with write permission may write the data for the key.

The creator of a key in the CSync store has special permissions to that key. In particular, the creator always has Read, Write, and Create permissions, and they also have permission to delete the key and change its ACL.

CSync provides eight "static" ACLs that can be used to provide any combination of Read, Write, and Create access to just the key's creator or all users.
- Private
- PublicRead
- PublicWrite
- PublicCreate
- PublicReadWrite
- PublicReadCreate
- PublicWriteCreate
- PublicReadWriteCreate

The ACL for a key is set when the key is created by the first write performed to the key. If the write operation specified an ACL, then this ACL is attached to the key. If no ACL was specified in the write, then the key inherits the ACL from its closest ancestor in the key space—its parent if the parent exists, else its grandparent if that key exists, possibly all the way back to the root key. The ACL of the root key is `PublicCreate`, which permits any user to create a child key but does not allow public read or write access.

# Getting Started

Adding the java SDK to your java project:
    
Maven:
```
<dependency>
        <groupId>com.ibm.csync</groupId>
        <artifactId>csync-java</artifactId>
        <version>1.4.0</version>
</dependency>
```
Gradle:
```
compile 'com.ibm.csync:csync-java:1.4.0'
```

#Usage

## Connecting to a CSync store

Applications use the CSync class to create a connection to a specific CSync service.

```
final CSync csync = new CSync.builder()
    .host("localhost")
    .port(6005)
    .build();
```

Note: Update the `host` and `port` to your specific csync server instance.

## Authenticating
```
csync.authenticate("demo", "demoToken")
    .whenComplete((result, ex) -> {
        if(ex == null) {
            //We successfully logged in
        }
        else {
            ex.printStackTrace()
        }
    });
```

Note: The provider and token can be updated to allow other providers to authenticate the csync session.

## Listening to values on a key
```
Key myKey = Key.of("your.pattern");
csync.listen(
    myKey,
    data -> {
        //logger.info("s1 {}", data);
        if (data.isDeleted) {
            // value does not exist
        } else {
            // value exists
        }
    }
);
```

## Blocking and Non-Blocking API

The Java SDK allows the client to specify how they want to execute calls. We provide the ability to write and delete nodes on CSync using blocking and non-blocking calls

### Blocking API

Write to a key and block until the operation is completed.

```
final Key myKey = Key.of("this.is.a.key");
Long writeVal = csync.blocking.pub(myKey, "My message");
//Specify a custom timeout in milliseconds
Long writeVal = csync.blocking.pub(myKey, "My message",Timeout.of(10000));
```

Delete a key and block until the operation is completed.

```
final Key myKey = Key.of("this.is.a.key");
Long deleteVal = csync.blocking.del(myKey);
```

### Non-Blocking API

The non-blocking API allows for functions to occur as callbacks or futures.

Write to a key without blocking.

```
final Key pqr = Key.of("p.q.r");

csync.pub(pqr,"My Data", (error, vts) -> {
    if (error == null )
        System.out.print("Success!");
    }
);
```

Delete a key without blocking.

```
final Key pqr = Key.of("p.q.r");
//We can delete the same key by calling
csync.del(pqr,"My Data", (error, vts) -> {
    if (error == null )
        System.out.print("Success!");
    }
);
```

Write to a key using futures without blocking.

```
csync.pub("Key.Strin", "hello")
    .thenRun(() -> System.out.print("Success!"))
```

Delete a key using futures without blocking.

```
csync.del("nice.key")
    .thenRun(() -> System.out.print("Success!"))
```

# License
This library is licensed under Apache 2.0. Full license text is
available in [LICENSE](LICENSE).

# Contribution Guide
Want to contribute? Take a look at our [CONTRIBUTING.md](.github/CONTRIBUTING.md)
