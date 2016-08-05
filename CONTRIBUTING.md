# Guide for contributors

This project follows a standard fork and pull model for accepting contributions via
GitHub pull requests.

### Code Style

This project uses [Scalafmt](https://olafurpg.github.io/scalafmt/) to ensure consistent code styling.
Code can be reformatted using

    sbt scalafmt
    

### Generate Test Coverage Report

Generate test coverage reports with [scoverage](http://scoverage.org/) plugin using

    sbt clean coverage test coverageReport