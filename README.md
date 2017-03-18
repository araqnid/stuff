Stuff!
======

This imaginatively-named application doesn't do much of anything
useful. It's just a "template" app intended to be used for cribbing
bits of wiring that may not be an artifact in their own right.

* Wired up with Guice
* "Request activity" times events within a request and outputs the data for a graph
* JAX-RS (RESTEasy) resources
  * Includes a resource to display the routing in a vaguely comprehensible way
* Integration tests run the server and provides HTTP client access
* Browser tests (very few, but they can run)
* Inclusion of runtime dependency *list* in built jar with scripts to fetch dependencies for deployment (alternative to fat jars)

[![Build Status](https://travis-ci.org/araqnid/stuff.svg?branch=master)](https://travis-ci.org/araqnid/stuff)
