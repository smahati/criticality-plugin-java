# Create a crititcality plugin for CAP Java

## Prerequisites

**In case you are using SAP Business Application Studio or SAP Build Code you can skip this section.**

You need to have the following tools installed:

* Java >=17 (internally, we use sapmachine but any other vendor should work, too)
* Maven (some recent 3.x release)
* Your Java IDE of choice. Ideally with CDS Tooling installed. This would be VS Code or IntelliJ Idea Ultimate.

## Requirement

Write a CAP Java plugin that provides a handler that can detect CDS enum values annotated with `@criticality.*` and 
ets the integer value according to the [criticality OData vocabulary](https://sap.github.io/odata-vocabularies/vocabularies/UI.html#CriticalityType)
to an `criticality` element of the same entity.

Bones points: this works for expanded entities, too.

## Create a new plain Java project with Maven

Use the Maven quickstart archetype to generate a plain, empty Java project:

```
mvn archetype:generate -DgroupId= -DartifactId=criticality -DarchetypeGroupId=com.sap.capire -DarchetypeArtifactId=maven-archetype-quickstart -Dversion=1.0-SNAPSHOT -DinteractiveMode=false
```


## Add needed dependencies

Replace the generated pom.xml with the following content. It contains the needed dependencies and the correct Java version:

```
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.sap.capire</groupId>
  <artifactId>criticality</artifactId>
  <packaging>jar</packaging>
  <version>1.0-SNAPSHOT</version>
  <name>criticality</name>
  <url>http://maven.apache.org</url>
  <properties>
	  <maven.compiler.release>17</maven.compiler.release>
	  <cds.services.version>2.10.0</cds.services.version>
  </properties>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>com.sap.cds</groupId>
        <artifactId>cds-services-bom</artifactId>
        <version>${cds.services.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-autoconfigure</artifactId>
      <version>3.2.6</version>
    </dependency>
    <dependency>
      <groupId>com.sap.cds</groupId>
      <artifactId>cds-services-api</artifactId>
      <version>2.10.0</version>
    </dependency>
    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>3.8.1</version>
      <scope>test</scope>
    </dependency>
  </dependencies>
</project>
```

Perform the initial build with `mvn compile`.

## Spring Boot Auto Configuration for the handler class

Use Spring Boot autoconfiguration to register the handler automatically as soon the dependency is added to the pom.xml.

## Implement the handler

Write the handler. Use the following resources as a reference:

* https://cap.cloud.sap/docs/java/reflection-api
* https://cap.cloud.sap/docs/java/cds-data

## Install the handler to the local Maven repo
In order to consume the new plugin from e.g. the Incidents App you need to install it to the local Maven repo. The `source:jar`
goal adds the source code to the jar as well. You might need it for debugging. 😈

```
mvn source:jar install
```

## Adjust the model of the target application

Clone the Incidents App for Java: https://github.com/recap-conf/incidents-app-java

Create a `criticality.cds file` in the `db` module and paste the following content: 

```cds
using {sap.capire.incidents as my} from './schema';

annotate my.Status with {
    code           @criticality {
        new        @criticality.Neutral;
        assigned   @criticality.Critical;
        in_process @criticality.Critical;
        on_hold    @criticality.Negative;
        resolved   @criticality.Positive;
        closed     @criticality.Positive;
    };
};

extend my.Urgency {
    criticality : Integer;
};

annotate my.Urgency with {
    code       @criticality {
        high   @criticality.Negative;
        medium @criticality.Critical;
    };
};
```

Add the dependency of the just created plugin to your `srv/pom.xml`:

```xml
<dependency>
    <groupId>com.sap.capire</groupId>
    <artifactId>criticality</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

Start the application and execute some HTTP requests:

```http
GET http://localhost:8080/odata/v4/ProcessorService/Urgency
Authorization: basic YWxpY2U6

###

GET http://localhost:8080/odata/v4/ProcessorService/Incidents?$expand=urgency
Authorization: basic YWxpY2U6
```