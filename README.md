# Build
* `mvn -DskipTests clean install`

# Notes

* DB connection seems only to work best if module runs already at start:
  * First compilation => Upload and restart
  * Further compilation => Upgrade
* Troubleshooting: `java.lang.IllegalStateException: EntityManager is closed` => DB connection was not wired correctly => restart openmrs

* Setup
  * Create `Concept Sources`:  `Disposition Location` & `Disposition Visit Type` and create respectively `Reference Terms` the location and visit type same in code and name.
  * Create `Reference Term` of source `org.openmrs.module.emrapi`.
  * Create a new dispositon `Concept` and link it to the `Disposition` concept and add mappings of all create reference terms.

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
