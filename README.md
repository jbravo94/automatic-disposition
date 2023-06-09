# Introduction

Because Bahmni Disposition Feature is except for bed management a no-op, this module enables automatic disposition to different bahmni locations and start different visits automatically with minimal configuration.

[![Automatic Disposition](misc/Disposition-Configuration.png?raw=true "Automatic Disposition")](https://youtu.be/8KgqzevWyiI "Automatic Disposition")

# Build
* Run `mvn clean install`

# Testing
When using Bahmni Docker (https://bahmni.atlassian.net/wiki/spaces/BAH/pages/3117744129/Getting+Started+Quickly+with+Bahmni+on+Docker#Running-Bahmni-Standard), following parameter must be enabled to allow omod module upload and remote debugging in `.env` file:

```
OPENMRS_MODULE_WEB_ADMIN='true'
OMRS_DEV_DEBUG_PORT=1044
```

# Setup
* Create `Concept Sources`:  `Disposition Location` & `Disposition Visit Type` and create respectively `Reference Terms` the location and visit type same in code and name.
* Create `Reference Term` of source `org.openmrs.module.emrapi`.
* Create a new dispositon `Concept` and link it to the `Disposition` concept and add mappings of all create reference terms.
* Rebuild Search Index
* Ensure that the referenced location has a `Location Tag` of `Visit Location`.

# Notes
* DB connection seems only to work best if module runs already at start:
  * First compilation => Upload and restart
  * Further compilation => Upgrade (Problems? => See Troubleshooting)
* Troubleshooting: `java.lang.IllegalStateException: EntityManager is closed` => DB connection was not wired correctly => restart openmrs

# Sources
https://github.com/openmrs/openmrs-module-atomfeed/
https://github.com/ICT4H/atomfeed
https://github.com/openmrs/openmrs-module-event
https://github.com/openmrs/openmrs-module-coreapps
https://github.com/openmrs/openmrs-core
https://github.com/ICT4H/openmrs-atomfeed
https://github.com/openmrs/openmrs-module-emrapi
https://wiki.openmrs.org/display/docs/Event+Module
https://wiki.openmrs.org/display/docs/Create+and+Deploy+Your+First+OpenMRS+Module
https://wiki.openmrs.org/display/docs/Set+Up+OpenMRS+Server+with+OpenMRS+SDK+and+Docker#SetUpOpenMRSServerwithOpenMRSSDKandDocker-InstallOpenMRSSDK
https://guide.openmrs.org/en/Configuration/customizing-openmrs-with-plug-in-modules.html
https://www.baeldung.com/spring-programmatic-transaction-management
