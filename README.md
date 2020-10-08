# Azure Datalake connected system plugin for Appian.

This is an Azure Datalake connected system plugin for Appian. After building, follow the Appian instructions on
installing the plugin.

## ADLS Gen 2 URLS

The standard format for ADLS Gen2 URLS is

https://accountname.blob.core.windows.net/filesystem

## Unit testing

In order to test this, you need to supply a set of [credentials](src/test/resources/credentials.properties). Currently the plugin
only supports a shared key credential. Future versions will look to support SAS URLs and username/password credentials.

```
accountName=<account name>
accountKey=<shared key>
fileSystem=<filesystem>
```

