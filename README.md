Stuff!
======

This imaginatively-named application doesn't do much of anything
useful. It's just a "template" app intended to be used for cribbing
bits of wiring that may not be an artifact in their own right.

* Wired up with Guice
* Runs either Standalone with Jetty or as a webapp (specfically, within Tomcat)
* Most things are Guava services, aggregated into a ServiceManager
  * Service activator allows services to be stopped/started dynamically
  * Provider service allows a service to provide an implementation of some interface only when it is running
* "Request activity" times events within a request and outputs the data for a graph
* JAX-RS (RESTEasy) resources
  * Includes a resource to display the routing in a vaguely comprehensible way
* Services to listen on Redis lists/Beanstalk tubes and handle message delivery
* A work-in-progress attempt to add auditing/logging around message delivery ("work queue")
* Integration tests run the server and provides HTTP client access
* Browser tests (very few, but they can run)

[![Build Status](https://travis-ci.org/araqnid/stuff.svg?branch=master)](https://travis-ci.org/araqnid/stuff)
