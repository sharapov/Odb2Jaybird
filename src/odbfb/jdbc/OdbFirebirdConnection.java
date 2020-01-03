/*
{{IS_RIGHT
	This program is distributed under LGPL Version 3.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
 */
package odbfb.jdbc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import odbpack.CmdLineArgs;
import odbpack.Odbpack;
import org.firebirdsql.gds.DatabaseParameterBuffer;
import org.firebirdsql.gds.TransactionParameterBuffer;
import org.firebirdsql.gds.impl.GDSHelper;
import org.firebirdsql.gds.ng.FbDatabase;
import org.firebirdsql.jca.FBManagedConnection;
import org.firebirdsql.jca.FirebirdLocalTransaction;
import org.firebirdsql.jdbc.FBConnection;
import org.firebirdsql.jdbc.FBObjectListener;
import org.firebirdsql.jdbc.FirebirdConnection;
import org.firebirdsql.jdbc.Synchronizable;

/**
 *
 * @author RZP16
 */
public class OdbFirebirdConnection implements FirebirdConnection, Synchronizable {

    private FBConnection fbconn;
    private String odbname;
    private String fbname;
    private CmdLineArgs cmdLineArgs;
    private boolean odbReadOnly = false;
    private RandomAccessFile lockedFile;
    private FileChannel lockedFileChannel;
    private FileLock lockFile;
    private static final ResourceBundle I18N = ResourceBundle.getBundle("odbpack/locale/i18n");

    private OdbFirebirdConnection() {

    }

    public OdbFirebirdConnection(FBConnection fbconn, String odbname, String fbname, CmdLineArgs args, boolean odbReadOnly) throws FileSystemException {
        this.fbconn = fbconn;
        this.odbname = odbname;
        this.fbname = fbname;
        this.cmdLineArgs = args;
        this.odbReadOnly = odbReadOnly;
        if (!odbReadOnly) {
            if (!lock(odbname)) {
                throw new FileSystemException(java.text.MessageFormat.format(I18N.getString("msg.file_locked"), new Object[]{
                    odbname}));
            }
        }
    }

    public OdbFirebirdConnection(FBConnection fbconn, String odbname, String fbname, CmdLineArgs args) {
        this.fbconn = fbconn;
        this.odbname = odbname;
        this.fbname = fbname;
        this.cmdLineArgs = args;
        this.odbReadOnly = false;
    }

    public OdbFirebirdConnection(FBConnection fbconn, String odbname, String fbname, boolean readOnly) throws FileSystemException {
        this.fbconn = fbconn;
        this.odbname = odbname;
        this.fbname = fbname;
        this.cmdLineArgs = new CmdLineArgs();
        this.odbReadOnly = readOnly;
        if (!odbReadOnly) {
            if (!lock(odbname)) {
                throw new FileSystemException(java.text.MessageFormat.format(I18N.getString("msg.file_locked"), new Object[]{
                    odbname}));
            }
        }
    }

    public OdbFirebirdConnection(FBConnection fbconn, String odbname, String fbname) {
        this.fbconn = fbconn;
        this.odbname = odbname;
        this.fbname = fbname;
        this.cmdLineArgs = new CmdLineArgs();
        this.odbReadOnly = false;
    }

    public boolean isOdbReadOnly() {
        return odbReadOnly;
    }

    public void setOdbReadOnly(boolean odbReadOnly) {
        this.odbReadOnly = odbReadOnly;
    }

    public FBObjectListener.StatementListener getStatementListener() {
        return fbconn.getStatementListener();
    }

    @Override
    public int getHoldability() throws SQLException {
        return fbconn.getHoldability();
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        fbconn.setHoldability(holdability);
    }

    public void setManagedConnection(FBManagedConnection mc) {
        fbconn.setManagedConnection(mc);
    }

    public FBManagedConnection getManagedConnection() {
        return fbconn.getManagedConnection();
    }

    @Override
    public FbDatabase getFbDatabase() throws SQLException {
        return fbconn.getFbDatabase();
    }

    public DatabaseParameterBuffer getDatabaseParameterBuffer() {
        return fbconn.getDatabaseParameterBuffer();
    }

    @Override
    @Deprecated
    public void setTransactionParameters(int isolationLevel, int[] parameters) throws SQLException {
        fbconn.setTransactionParameters(isolationLevel, parameters);
    }

    @Override
    public TransactionParameterBuffer getTransactionParameters(int isolationLevel) throws SQLException {
        return fbconn.getTransactionParameters(isolationLevel);
    }

    @Override
    public TransactionParameterBuffer createTransactionParameterBuffer() throws SQLException {
        return fbconn.createTransactionParameterBuffer();
    }

    @Override
    public void setTransactionParameters(int isolationLevel, TransactionParameterBuffer tpb) throws SQLException {
        fbconn.setTransactionParameters(isolationLevel, tpb);
    }

    @Override
    public void setTransactionParameters(TransactionParameterBuffer tpb) throws SQLException {
        fbconn.setTransactionParameters(tpb);
    }

    @Override
    public Statement createStatement() throws SQLException {
        return fbconn.createStatement();
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return fbconn.prepareStatement(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        return fbconn.prepareCall(sql);
    }

    @Override
    public Blob createBlob() throws SQLException {
        return fbconn.createBlob();
    }

    @Override
    public Clob createClob() throws SQLException {
        return fbconn.createClob();
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return fbconn.createStruct(typeName, attributes);
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return fbconn.createArrayOf(typeName, elements);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return fbconn.nativeSQL(sql);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        fbconn.setAutoCommit(autoCommit);
    }

    public void setManagedEnvironment(boolean managedConnection) throws SQLException {
        fbconn.setManagedEnvironment(managedConnection);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return fbconn.getAutoCommit();
    }

    @Override
    public void commit() throws SQLException {
        fbconn.commit();
    }

    @Override
    public void rollback() throws SQLException {
        fbconn.rollback();
    }

    @Override
    public void close() throws SQLException {
        fbconn.close();
        if (!odbReadOnly) {
            unlock();
            try {
                Odbpack.packOdb(odbname, fbname, cmdLineArgs);
            } catch (FileAlreadyExistsException ex) {
                throw new SQLException(ex);
            } catch (IOException ex) {
                throw new SQLException(ex);
            }
        }
        new File(fbname).delete();
    }

    @Override
    public boolean isClosed() {
        return fbconn.isClosed();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return fbconn.isValid(timeout);
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return fbconn.getMetaData();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        fbconn.setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return fbconn.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        fbconn.setCatalog(catalog);
    }

    @Override
    public String getCatalog() throws SQLException {
        return fbconn.getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        fbconn.setTransactionIsolation(level);
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return fbconn.getTransactionIsolation();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return fbconn.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        fbconn.clearWarnings();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return fbconn.createStatement(resultSetType, resultSetConcurrency);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return fbconn.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return fbconn.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return fbconn.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return fbconn.prepareStatement(sql, autoGeneratedKeys);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return fbconn.prepareStatement(sql, columnIndexes);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return fbconn.prepareStatement(sql, columnNames);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return fbconn.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return fbconn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return fbconn.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        fbconn.setTypeMap(map);
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return fbconn.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return fbconn.setSavepoint(name);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        fbconn.rollback(savepoint);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        fbconn.releaseSavepoint(savepoint);
    }

    public FirebirdLocalTransaction getLocalTransaction() {
        return fbconn.getLocalTransaction();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return fbconn.isWrapperFor(iface);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return fbconn.unwrap(iface);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        fbconn.setSchema(schema);
    }

    @Override
    public String getSchema() throws SQLException {
        return fbconn.getSchema();
    }

    public boolean inTransaction() throws SQLException {
        return fbconn.inTransaction();
    }

    @Override
    public String getIscEncoding() throws SQLException {
        return fbconn.getIscEncoding();
    }

    public void addWarning(SQLWarning warning) {
        fbconn.addWarning(warning);
    }

    @Override
    public NClob createNClob() throws SQLException {
        return fbconn.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return fbconn.createSQLXML();
    }

    public GDSHelper getGDSHelper() throws SQLException {
        return fbconn.getGDSHelper();
    }

    @Override
    public boolean isUseFirebirdAutoCommit() {
        return fbconn.isUseFirebirdAutoCommit();
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return fbconn.getClientInfo();
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return fbconn.getClientInfo(name);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        fbconn.setClientInfo(properties);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        fbconn.setClientInfo(name, value);
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        fbconn.abort(executor);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        fbconn.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return fbconn.getNetworkTimeout();
    }

    @Override
    public final Object getSynchronizationObject() {
        return fbconn.getSynchronizationObject();
    }

    private boolean lock(String odbname) {
        try {
            File f = new File(odbname);
            lockedFile = new RandomAccessFile(f, "rw");
            lockedFileChannel = lockedFile.getChannel();
            lockFile = lockedFileChannel.lock();
        } catch (FileNotFoundException ex) {
            return false;
        } catch (IOException ex) {
            try {
                lockedFile.close();
            } catch (IOException ex1) {
            }
            return false;
        }
        return true;
    }

    private void unlock() {
        try {
            lockFile.release();
            lockedFileChannel.close();
            lockedFile.close();
        } catch (IOException ex) {
            //Logger.getLogger(OdbFirebirdConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
