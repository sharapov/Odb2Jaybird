# Odb2Jaybird
JDBC Driver for LibreOffice Base (odb)

Unpack fdb file from odb file before connect. 
After close pack to odb. 
Need Jaybird 3.0. Does not require Libreoffice installation.

Class odbfb.jdbc.OdbFbDriver

URL example:

odbfb.jdbc.OdbFbDriver

Connection connection = DriverManager.getConnection("jdbc:odbfb:local:C:\\work\\CollectionFortest.odb", "SYSDBA", masterkey");

For readonly
Connection connection = DriverManager.getConnection("jdbc:odbfb:local:C:\\work\\CollectionFortest.odb?readonly=true", "SYSDBA", masterkey");

Command line interface

odbpack {unpack|pack} name.odb name.fdb

{-o|--obak} create backup file for odb (default=no)

{-r|--dontremovefdb} dont remove fdb (default=remove)

java -jar Odb2Jaybird.jar {unpack|pack} name.odb name.fdb
