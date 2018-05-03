# Nexus Repository Manager 3 Maven/Rundeck Plugin
A NXRM3 Plugin which adds REST endpoints to
* Search & Retrieve artifacts using Maven GAV parameters
* Search & Retrieve artifacts with the LATEST version keyword
* Maven-style version [Rundeck Option Model Provider](http://rundeck.org/docs/manual/jobs.html#option-model-provider)

## Endpoints
* All endpoints are relative to the base REST URL, ie `http://nexus:8081/service/rest`

### `/maven/versions` (GET)
* Returns an array of Maven 'baseVersions', along with corresponding Nexus asset metadata.
* Array is sorted by baseVersion, assets sorted descending by last updated (most recent)

##### Query Parameters
| Query Parameter | Description | Default | Required |
|---|---|---|---|
| l | The (numeric) search result limit | 10 | False |
| r | The name of the repository to search | | False |
| g | The GAV group id | | False |
| a | The GAV artifact id | | False |
| c | The Maven classifier | | False |
| e | The Maven extension | | False |

##### Sample Response
 ```json
[
  {
    "baseVersion": "1.1.1-SNAPSHOT",
    "assets": [
      {
        "version": "1.1.1-20180430.040018-77",
        "lastUpdated": "2018-04-30T04:00:19.069+0000"
      },
      {
        "version": "1.1.1-20180423.040013-76",
        "lastUpdated": "2018-04-23T04:00:13.000+0000"
      }
    ]
  }
]
```

### `/maven/download` (GET)
* Initiates a download of latest artifact found, sorted descending by last updated (most recent)

##### Query Parameters
| Query Parameter | Description | Default | Required |
|---|---|---|---|
| l | The (numeric) search result limit | 10 | False |
| r | The name of the repository to search | | True |
| g | The GAV group id | | True |
| a | The GAV artifact id | | True |
| v | The GAV version, LATEST keyword is supported | | True |
| c | The Maven classifier | | False |
| e | The Maven extension | | False |

##### Error Response Codes
| Http Code | Notes |
|---|---|
| 400 | Invalid request, check query parameters |
| 404 | Artifact with given attributes not found |


### `/maven/rundeck/versions` (GET)
* Returns an array of versions in the [Rundeck Option Model Provider](http://rundeck.org/docs/manual/jobs.html#option-model-provider) format
* The name will contain the time of the latest asset found, sorted descending by last updated (most recent)

##### Query Parameters
| Query Parameter | Description | Default | Required |
|---|---|---|---|
| l | The (numeric) search result limit | 10 | False |
| r | The name of the repository to search | | False |
| g | The GAV group id | | False |
| a | The GAV artifact id | | False |
| v | The GAV version, LATEST keyword is supported | | False |
| c | The Maven classifier | | False |
| e | The Maven extension | | False |

##### Sample Response
```json
[
  {
    "name": "1.1.1-SNAPSHOT (2018-04-30 00:01:26)",
    "value": "1.1.1-SNAPSHOT"
  }
]
```

## Deployment
### Simple
1. Drop the jar into the nexus (karaf) deploy dir
   * This directory is located under the nexus home dir, ie: `/opt/nexus/deploy`
2. Confirm that the bundle has been deployed and is active
   * Nexus Administrator > System > Bundles > nexus3-maven-rundeck-plugin > State = Active

## Contributing
### Prerequisites
* Git
* JDK 8

### Getting Started
#### Eclipse IDE
1. Open a command window and run `gradlew eclipse`
2. Import the project into eclipse
3. [Configure](https://help.eclipse.org/oxygen/index.jsp?topic=%2Forg.eclipse.jdt.doc.user%2Freference%2Fpreferences%2Fjava%2Fcodestyle%2Fref-preferences-formatter.htm) the Java Formatter and Import Order using the configurations found under [resources directory](./gradle/resources)
   * [vestmark_format.xml](./gradle/resources/vestmark_format.xml) is used for Java formatting
   * [vestmark.importorder](./gradle/resources/vestmark.importorder) is used for import ordering

#### IntelliJ IDEA
1. Import the root project from IntelliJ using the Gradle project model
2. Install the [Eclipse Code Formatter](https://plugins.jetbrains.com/plugin/6546-eclipse-code-formatter) plugin
3. [Configure](https://github.com/krasa/EclipseCodeFormatter#instructions) the Eclipse Code Formatter with the configurations found under [resources directory](./gradle/resources)
  * [vestmark_format.xml](./gradle/resources/vestmark_format.xml) is used for Java formatting
  * [vestmark.importorder](./gradle/resources/vestmark.importorder) is used for import ordering

### Development
1. Branch
2. Make your changes
3. Update the version appropriately, version property is located in [gradle.properties](gradle.properties)
4. Run `gradlew spotlessApply`
5. Commit, including the issue number in your message...push
6. Open a pull request to master

### Built With
* [Gradle 4.7](https://docs.gradle.org/4.7/userguide/userguide.html) - Compilation and packaging
* [Spotless](https://github.com/diffplug/spotless) - Formatter and Checkstyle

### Versioning
* [OSGi Semantic Versioning](https://www.osgi.org/wp-content/uploads/SemanticVersioning.pdf)

## Authors
* **Kevin Brooks** - *Initial development*

## Special Thanks
[Nexus3 Rundeck plugin](https://github.com/nongfenqi/nexus3-rundeck-plugin) was used as a reference during initial development
