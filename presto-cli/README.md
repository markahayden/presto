## Tableau Data Extract (TDE) support for the presto CLI

1. Follow the [directions here](https://onlinehelp.tableau.com/current/api/sdk/en-us/help.htm#SDK/tableau_sdk_installing.htm) to install the C/C++/Java libraries on your machine: 
  * [Mac OS X 64bit]: Copy the Frameworks directory to /Library/Frameworks.  You will need to enter your password 
  * [Debian 64bit]: `sudo dpkg --install Tableau-SDK-Linux-64Bit-9-3-3.deb` 
2. Download the [prestotde] executable jar file (the required tableau jar files are bundled in)

### Usage
```
./prestotde --server localhost:8080 --catalog hive --schema default --execute "select * from blah;" --output-format TDE --tdefile test.tde
``` 
OR  
```
./prestotde --server localhost:8080 --catalog hive --schema default --execute "select * from blah;" --output-format TABLEAU_SERVER --tab-host "https://tableau.xyz.com" --tab-u "your-username" --tab-p "your-password" --tab-datasource "test-datasource"
```

### Enviornment variables 
You may want to set the following environment variables to control where tableau writes temp files.  `TMPDIR` is used if they are not specified and the folder must exist (tableau cannot create folders).
```
TAB_SDK_LOGDIR
TAB_SDK_TMPDIR
```

### Notes

There is a [bug](https://community.tableau.com/thread/211263) with the Linux libraries that sometimes generates an error when querying large datasets:
```
terminate called after throwing an instance of 'boost::exception_detail::clone_impl<boost::exception_detail::error_info_injector<boost::lock_error> >'
what():  boost: mutex unlock failed in pthread_mutex_unlock: Invalid argument
```
This is a bug in the Tableau API that only affects Linux builds, usually re-running the query will resolve it.  I am waiting on Tableau for a fix.  In the meantime, I found a workaround by downgrading to 9.2. 
```
wget https://downloads.tableau.com/tssoftware/Tableau-SDK-Linux-64Bit-9-2-4.tar.gz
tar xf Tableau-SDK-Linux-64Bit-9-2-4.tar.gz 
sudo mv tableausdk-linux64-9200.0.0.0/bin/tdeserver64 /usr/bin/tdeserver64
```

Unfortunately the SDK does not support SSL on Linux, so I've only gotten the `TABLEAU_SERVER` output format to work on Mac OS X.

### Installation

If you want to build this on your own, it's a standard maven project but you will need to install the jar files to your local maven repository.  I've included them in /lib for convenience 
```
cd lib
mvn install:install-file -Dfile=jna.jar -DgroupId=com.sun.jna -DartifactId=jna -Dversion=3.5.1 -Dpackaging=jar
mvn install:install-file -Dfile=tableauextract.jar -DgroupId=com.tableausoftware -DartifactId=tableau-extract -Dversion=9.3.3 -Dpackaging=jar
mvn install:install-file -Dfile=tableaucommon.jar -DgroupId=com.tableausoftware -DartifactId=tableau-common -Dversion=9.3.3 -Dpackaging=jar
mvn install:install-file -Dfile=tableauserver.jar -DgroupId=com.tableausoftware -DartifactId=tableau-server -Dversion=9.3.3 -Dpackaging=jar
``` 
then: 
`mvn clean install`


[Mac OS X 64bit]: https://downloads.tableau.com/tssoftware/Tableau-SDK-9-3-3.dmg
[Debian 64bit]: https://downloads.tableau.com/tssoftware/Tableau-SDK-Linux-64Bit-9-3-3.deb
[prestotde]: https://github.com/Mark-Hayden/presto/blob/master/presto-cli/prestotde
