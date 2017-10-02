Build
=====

```
mvn clean package
```

The shaded artifact including all necessary dependencies
gets stored under `target/config-backup.jar`.

Usage
=====

Making a backup snapshot of the monitored environments:

```
java -jar target/config-backup.jar application.conf
```

Configuration
=============

See `src/main/resources/application.conf.example` for
more information.

If the configured backup directory is an initialized git working
copy, the program will commit and push detected changes to the
data dump.
