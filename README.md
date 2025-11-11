Atlassian Connect Play
======================

[![Continuous integration](https://github.com/toolsplus/atlassian-connect-play/actions/workflows/ci.yml/badge.svg)](https://github.com/toolsplus/atlassian-connect-play/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/toolsplus/atlassian-connect-play/branch/master/graph/badge.svg)](https://codecov.io/gh/toolsplus/atlassian-connect-play)
[![Maven Central](https://img.shields.io/maven-central/v/io.toolsplus/atlassian-connect-play-core_3.svg)](https://maven-badges.herokuapp.com/maven-central/io.toolsplus/atlassian-connect-play-core_3)


This project contains a [Play Scala](https://www.playframework.com/) based 
implementation of the [Atlassian Connect](https://connect.atlassian.com/) framework. 
It serves as a starter for building Atlassian Connect add-ons for JIRA and Confluence.

## Quick start

atlassian-connect-play is published to Maven Central for Scala 3 and Play 3, 
so you can just add the following to your build:

    libraryDependencies += "io.toolsplus" %% "atlassian-connect-play" % "x.x.x"

## Basics

You can generate a fresh project by cloning the seed project, which will provide 
you with the basic project structure. Alternatively if you have an existing Scala 
project, you can manually add the framework dependency to your project to turn 
it into an Atlassian Connect add-on.

### Creating a project from the seed project

The easiest way to get started is by cloning the [Atlassian Connect Play Seed](atlassian-connect-play-seed) 
project.

## Responding to requests
 
### Requests from an Atlassian Host

### Requests from the add-on


## Making API requests to the product

### Send requests as the add-on

atlassian-connect-play will automatically sign requests from your add-on to an 
installed host product with JSON Web Tokens. To make a request, inject an 
`AtlassianConnectHttpClient` object into your class.
Then call `authenticatedAsAddon(url)` with a relative URL. Make sure you have a 
implicit instance of `AtlassianHost` available in the calling function. It is 
required to sign and determine the absolute URL for the outgoing request.

    class RestClient @Inject()(httpClient: AtlassianConnectHttpClient) {
   
        def fetchIssue(issueKey: String)(implicit host: AtlassianHost): Future[WSResponse] = {
            httpClient.authenticatedAsAddon(s"/rest/api/2/issue/$issueKey").get
        }
    }

### Send requests as a user

Not yet implemented.

## Authenticating requests from iframe content back to the add-on

The initial request to load iframe content served by the add-on is secured by 
JWT, as described above. However, add-ons often need to make authenticated 
requests back to the add-on from within the iframe. Using sessions is not 
recommended, since some browsers block third-party cookies by default. Also, the 
JWT token issued by the Atlassian host cannot be used for other requests to the 
add-on since it contains the `qsh` (query-string hash) claim.

Instead, add-ons can use the JWT *self-authentication token* - provided by 
atlassian-connect-play. 
Pages (iframes) rendered by a add-on may include a meta tag containing an 
initial self-authentication token.

    <meta name="token" content="@token">
 
A client-side script can easily extract the token
 
    document.head.querySelector("[name=token]").content
 
and use it to sign the request back to the add-on. Whenever possible, e.g. for 
AJAX requests, the token should be sent in the Authorization HTTP header:

    beforeSend: function (request) {
        request.setRequestHeader("Authorization", "JWT " + token);
    }
You can also send the token in the `jwt` query parameter:

    <a href="/protected-resource?jwt=...">See more</a>
    
Add-ons may respond to client-side requests with refreshed token provided in the
Authorization HTTP header. You may save the refreshed token for the next request
back to the add-on.

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

[atlassian-connect-play-seed]: https://github.com/toolsplus/atlassian-connect-play-seed
[apache]: http://www.apache.org/licenses/LICENSE-2.0
