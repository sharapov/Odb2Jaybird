# Odb2Jaybird
JDBC Driver for LibreOffice Base (odb)
Unpack fdb file from odb file before connect.
After close pack to odb.
Need Jaybird 3.0
Class odbfb.jdbc.OdbFbDriver
URL example:
odbfb.jdbc.OdbFbDriver
Connection connection = DriverManager.getConnection("jdbc:odbfb:local:C:\\work\\CollectionFortest.odb", "SYSDBA", masterkey");
