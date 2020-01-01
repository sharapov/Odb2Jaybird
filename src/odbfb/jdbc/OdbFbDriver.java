/*
{{IS_RIGHT
	This program is distributed under LGPL Version 3.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
 */
package odbfb.jdbc;

import java.io.FileNotFoundException;
import java.nio.file.FileSystemException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import odbpack.CmdLineArgs;
import odbpack.Odbpack;
import org.firebirdsql.jdbc.FBConnection;
import org.firebirdsql.jdbc.FBDriver;
import org.firebirdsql.jdbc.FirebirdConnection;
import org.firebirdsql.jdbc.FirebirdConnectionProperties;

/**
 *
 * @author RZP16
 */
public class OdbFbDriver extends FBDriver implements Driver {

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return super.getParentLogger();
    }

    @Override
    public boolean jdbcCompliant() {
        return super.jdbcCompliant();
    }

    @Override
    public int getMinorVersion() {
        return super.getMinorVersion();
    }

    @Override
    public int getMajorVersion() {
        return super.getMajorVersion();
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return super.getPropertyInfo(url, info);
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {

        return url.startsWith("jdbc:odbfb:local:");//super.acceptsURL(url); 
    }

    @Override
    public FirebirdConnectionProperties newConnectionProperties() {
        return super.newConnectionProperties();
    }

    @Override
    public FirebirdConnection connect(FirebirdConnectionProperties properties) throws SQLException {
        return super.connect(properties);
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!url.startsWith("jdbc:odbfb:local:")) {
            throw new SQLException("No");
        }
        /*for (Map.Entry<Object, Object> s : info.entrySet()) {
            System.out.println(s.getKey() + " | " + s.getValue());
        }*/
        String tmpfile;
        CmdLineArgs args = (url.substring(17).lastIndexOf('?') == -1 ? new CmdLineArgs() : new CmdLineArgs(url.substring(url.lastIndexOf('?') + 1).split("&"), /*null*/ new String[][]{}));
        boolean readonly = args.getMainArgs().contains("readonly=true");
        String odbfilename = (url.substring(17).lastIndexOf('?') == -1 ? url.substring(17) : url.substring(17, url.lastIndexOf('?')));
        try {
            tmpfile = Odbpack.unpackOdb(odbfilename, null,
                    args);
        } catch (FileNotFoundException | FileSystemException ex) {
            throw new SQLException(ex);
        }
        return (tmpfile == null ? null : new OdbFirebirdConnection((FBConnection) super.connect("jdbc:firebirdsql:local:" + tmpfile, info),
                odbfilename, tmpfile, args, readonly));
    }

}
