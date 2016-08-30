# GLiTR: GraphQL Lightweight Type Registry

[![][travis img]][travis]
[![][maven img]][maven]
[![][license img]][license]

GLiTR is a library that greatly simplifies the creation of a GraphQL schema.

## How does it work?

#### Todo

todo

## Binaries

Example for Maven:

```xml
<dependency>
    <groupId>com.nfl.dm.shield</groupId>
    <artifactId>glitr</artifactId>
    <version>x.y.z</version>
</dependency>
```

Example for gradle:

```gradle
compile("com.nfl.dm.shield:glitr:x.y.z")
```

Change history and version numbers => [CHANGELOG.md](https://github.com/NFL/GLiTR/blob/master/CHANGELOG.md)

## Build

To build:

```
$ git clone git@github.com:NFL/GLiTR.git
$ cd GLiTR/
$ ./gradlew build
```

Futher details on building can be found on the [Getting Started](https://github.com/NFL/GLiTR/wiki/Getting-Started) page of the wiki.

## How to use it

```java
return GlitrBuilder.newGlitrWithRelaySupport()
        .withNodeFetcherService(nodeFetcherService)
        .withQueryRoot(new Viewer())
        .withMutationRoot(new MutationType())
        // add potential custom data fetchers
        .addCustomDataFetcherFactory(SomeAnnotation.class, someAnnotationBasedDataFetcherFactory)
        .build();
```

```java
@GraphQLDescription("Global node. Entry point needed for Relay.")
public class Viewer {
    public Root getViewer(DataFetchingEnvironment environment) {
        return new Root();
    }
}
```

```java
@GraphQLDescription("Where it all begins.")
public class Root {

    @Argument(name = "someDomainObject", type = String.class , nullable = true)
    private SomeDomainObject someDomainObject;

    public SomeDomainObject getSomeDomainObject() {
        return someDomainObject;
    }
```

```java
@GraphQLDescription("Where to persist something.")
public class MutationType {

    @Argument(name = "input", type = SomeMutationInput.class, nullable = false)
    public SomeMutationPayload getSomeMutation(DataFetchingEnvironment env) {
        SaveSomethingFunc mutation = new SaveSomethingFunc();
        MutationDataFetcher mutationDataFetcher = new MutationDataFetcher(SomeMutationInput.class, new SomeValidator(), mutation);
        return (SomeMutationPayload) mutationDataFetcher.get(env);
    }
}
```

```java
public class SaveSomethingFunc implements RelayMutation<SomeMutationInput, SomeMutationPayload> {

    public SomeMutation call(SomeMutationInput mtnInput, DataFetchingEnvironment env) {
        SomeDomainObject someDomainObject = persist(mtnInput);
        return new SomeMutationPayload().setSomeDomainObject(someDomainObject);
    }

    public SomeDomainObject persist(SomeMutationInput mtnInput) {
        // persistenc logic goes here
    }
}
```

```java
public class SomeMutationPayload extends RelayMutationType {

    private SomeDomainObject someDomainObject;

    public SomeDomainObject getSomeDomainObject() {
        return someDomainObject;
    }

    public SomeMutationPayload setSomeDomainObject(SomeDomainObject someDomainObject) {
        this.someDomainObject = someDomainObject;
        return this;
    }
}
```

```java
public class SomeMutationInput extends RelayMutationType {

    private SomeMutation someMutation;

    public SomeMutation getVideo() {
        return video;
    }

    public SomeMutationInput setSomeDomainObject(VideoMtn someMutation) {
        this.someMutation = someMutation;
        return this;
    }

    public static class SomeMutation {

        private String someValue;

        public String getSomeValue() {
            return someValue;
        }

        public SomeMutation setSomeValue(String someValue) {
            this.someValue = someValue;
            return this;
        }
    }
}
```

## Full Documentation

See the [Wiki](https://github.com/NFL/GLiTR/wiki/) for full documentation, examples, operational details and other information.

See the [Javadoc](https://github.com/NFL/GLiTR/javadoc) for the API.

See [hystrix-examples](https://github.com/NFL/GLiTR/tree/master/glitr-examples/src/main/java/com/nfl/dm/glitr/examples) for example implementation

## Contact Info

- Twitter: [@nflengineers](http://twitter.com/nflengineers)
- [GitHub Issues](https://github.com/NFL/GLiTR/issues)


## LICENSE

Copyright 2016 NFL Enterprises LLC. NFL and the NFL shield design are
registered trademarks of the National Football League.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.