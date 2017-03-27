Atlassian Connect Play
======================

[![Build Status](https://travis-ci.org/toolsplus/atlassian-connect-play.svg?branch=master)](https://travis-ci.org/toolsplus/atlassian-connect-play)
[![codecov](https://codecov.io/gh/toolsplus/atlassian-connect-play/branch/master/graph/badge.svg)](https://codecov.io/gh/toolsplus/atlassian-connect-play)
[![Maven Central](https://img.shields.io/maven-central/v/io.toolsplus/atlassian-connect-play-core_2.11.svg)](https://maven-badges.herokuapp.com/maven-central/io.toolsplus/atlassian-connect-play-core_2.11)


This project contains a [Play Scala](https://www.playframework.com/) based implementation of the [Atlassian Connect](https://connect.atlassian.com/) framework. It serves as a starter for building Atlassian Connect add-ons for JIRA and Confluence.

## Quick start

atlassian-connect-play is published to Maven Central for Scala 2.11 so you can just add the following to your build:

    libraryDependencies += "io.toolsplus" %% "atlassian-connect-play" % "0.0.2"

## Basics

To get started, there are two options. You can generate a fresh project by cloning the seed project, which
will provide you with the basic project structure. Alternatively if you have an existing Scala project, you can manually
add a the framework dependency to your project to turn it into an Atlassian Connect add-on.

### Creating a project from the seed project

...

### Modifying an existing project

...

## Responding to requests
 
### Requests from an Atlassian Host

### Requests from the add-on


## Making API requests to the product

### Send requests as the add-on

### Send requests as a user

## Authenticating requests from iframe content back to the add-on


## Reacting to add-on lifecycle events


## Configuration

### Referencing configuration values in the add-on


## Making your add-on production ready
  

## Contributing
 
Pull requests are always welcome. Please follow the [contribution guidelines](CONTRIBUTING.md).

## License

atlassian-connect-play is licensed under the **[Apache License, Version 2.0][apache]** (the
"License"); you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[apache]: http://www.apache.org/licenses/LICENSE-2.0