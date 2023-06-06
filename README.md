[![Build Status](https://api.travis-ci.com/magnolia-community/form2db.svg?branch=master)](https://travis-ci.com/github/magnolia-community/form2db) 
[![Magnolia compatibility](https://img.shields.io/badge/magnolia-5.6-brightgreen.svg)](https://www.magnolia-cms.com)
[![Magnolia compatibility](https://img.shields.io/badge/magnolia-5.7-brightgreen.svg)](https://www.magnolia-cms.com)
[![Magnolia compatibility](https://img.shields.io/badge/magnolia-6.1-brightgreen.svg)](https://www.magnolia-cms.com)
[![Magnolia compatibility](https://img.shields.io/badge/magnolia-6.2-brightgreen.svg)](https://www.magnolia-cms.com)

Form2DB
=======

This module extends the Magnolia form module by the capability to persist the submitted form data in a JCR workspace. 
It's recommended to use a clustered JCR workspace to see and export the data on the author instance.

Issue tracking
--------------
Issues are tracked at [GitHub](https://github.com/magnolia-community/form2db/issues).
Any bug reports, improvement or feature requests are welcome! 

Maven artifacts in Magnolia's Nexus
---------------------------------
The code is built on [Travis CI](https://travis-ci.com/github/magnolia-community/form2db).
You can browse available artifacts through [Magnolia's Nexus](https://nexus.magnolia-cms.com/#nexus-search;quick~form2db-app)

Maven dependency
-----------------
```xml
<dependency>
    <groupId>de.marvinkerkhoff</groupId>
    <artifactId>form2db-app</artifactId>
    <version>1.6.0</version>
</dependency>
```

Versions
-----------------
* Version 1.5.0 is compatible with Magnolia 5.6.x, 5.7.x and 6.1.x
* Version 1.6.0 is compatible with Magnolia 6.2.x
