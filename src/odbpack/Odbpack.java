/*
{{IS_RIGHT
	This program is distributed under LGPL Version 3.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
 */
package odbpack;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.firebirdsql.management.BackupManager;
import org.firebirdsql.management.FBBackupManager;

/**
 *
 * @Sharapov
 *
 */
public class Odbpack {

    private static final ResourceBundle I18N = ResourceBundle.getBundle("odbpack/locale/i18n");
    private static boolean cli = false;

    /**
     * @param args the command line arguments
     * @throws java.io.FileNotFoundException
     * @throws java.nio.file.FileSystemException
     * @throws java.sql.SQLException
     */
    public static void main(String[] args) throws FileNotFoundException, FileSystemException, IOException, SQLException {
        String[][] expectedArgs = {{"o",
            "obak",
            I18N.getString("option.create_backup_odb")},
        /*{"d",
            "dnbak",
            I18N.getString("option.create_backup_fdb")},*/
        {"r",
            "dontremovefdb",
            I18N.getString("option.dont_remove_fdb")}
        };
        cli = true;
        CmdLineArgs cmdLineArgs = new CmdLineArgs(args, expectedArgs);
        if (cmdLineArgs.getMainArgs().size() >= 3) {
            switch (cmdLineArgs.getMainArgs().get(0)) {
                case "unpack":
                    Odbpack.unpackOdb(cmdLineArgs.getMainArgs().get(1), cmdLineArgs.getMainArgs().get(2), cmdLineArgs);
                    break;
                case "pack":
                    Odbpack.packOdb(cmdLineArgs.getMainArgs().get(1), cmdLineArgs.getMainArgs().get(2), cmdLineArgs);
                    break;
                default:
                    System.out.println(I18N.getString("rule.odbpack"));
            }
        } else {
            System.out.println(I18N.getString("rule.odbpack"));
            for (String[] s : expectedArgs) {
                System.out.println("{-" + s[0] + "|--" + s[1] + "} " + s[2]);
            }
        }
    }

    static public void packOdb(String odbname, String dbname, CmdLineArgs cmdLineArgs) throws FileNotFoundException, FileAlreadyExistsException, IOException, SQLException {
        File tmpfile;
        SimpleDateFormat dateFormet = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        File fOdbname = new File(odbname);
        if (!new File(dbname).exists()) {
            if (cli) {
                System.out.println(java.text.MessageFormat.format(I18N.getString("msg.file_not_found"), new Object[]{
                    new File(dbname).getAbsolutePath()}));
            } else {
                throw new FileNotFoundException(java.text.MessageFormat.format(I18N.getString("msg.file_not_found"), new Object[]{
                    new File(dbname).getAbsolutePath()}));
            }
            return;
        }
        if (!fOdbname.exists()) {
            if (cli) {
                System.out.println(java.text.MessageFormat.format(I18N.getString("msg.file_odb_not_found"), new Object[]{
                    fOdbname.getAbsolutePath()}));
            } else {
                throw new FileNotFoundException(java.text.MessageFormat.format(I18N.getString("msg.file_odb_not_found"), new Object[]{
                    fOdbname.getAbsolutePath()}));
            }
            return;
        }
        /*if(!fOdbname.canWrite()){
                if (cli) {
                    System.out.println(java.text.MessageFormat.format(I18N.getString("msg.file_odb_canot_not_write"), new Object[]{
                        fOdbname.getAbsolutePath()}));
                } else {
                    throw new FileNotFoundException(java.text.MessageFormat.format(I18N.getString("msg.file_odb_canot_not_write"), new Object[]{
                        fOdbname.getAbsolutePath()}));
                }
                return;
            }*/
        String odbpath = new File(fOdbname.getAbsolutePath()).getParent() + File.separatorChar
                + fOdbname.getName();
        String odbpathbak = new File(fOdbname.getAbsolutePath()).getParent() + File.separatorChar
                + StringUtils.removeExtension(fOdbname.getName()) + "_" + dateFormet.format(new Date()) + "." + StringUtils.getExtension(fOdbname.getName());
        if (new File(odbpathbak).exists()) {
            if (cli) {
                System.out.println(java.text.MessageFormat.format(I18N.getString("msg.file_exists"), new Object[]{
                    odbpathbak}));
            } else {
                throw new FileAlreadyExistsException(java.text.MessageFormat.format(I18N.getString("msg.file_exists"), new Object[]{
                    odbpathbak}));
            }
            return;
        }
        Files.copy(Paths.get(odbpath), Paths.get(odbpathbak));

        tmpfile = Files.createTempFile(Paths.get(System.getProperty("java.io.tmpdir")), "fb", ".fbk").toFile();

        BackupManager backupManager = new FBBackupManager("EMBEDDED");
        //backupManager.setHost("localhost");
        //backupManager.setPort(3050);
        //backupManager.getServerVersion();
        backupManager.setUser("SYSDBA");
        backupManager.setPassword("masterkey");
        backupManager.setVerbose(false);
        backupManager.setRestoreReplace(true);
        backupManager.setDatabase(dbname);
        backupManager.setBackupPath(tmpfile.getPath());
        backupManager.backupDatabase();
        ZipFile obdbackzip = new ZipFile(odbpathbak);
        FileInputStream fi = null;
        try (
                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(fOdbname))) {
            Enumeration<? extends ZipEntry> entries = obdbackzip.entries();
            byte[] buf1 = new byte[8192];
            int readed;
            while (entries.hasMoreElements()) {
                ZipEntry ze = entries.nextElement();
                if (ze.getName().equals("database/firebird.fbk")) {
                    continue;
                }
                zos.putNextEntry(ze);
                InputStream inputStream = obdbackzip.getInputStream(ze);
                //if(!ze.isDirectory()){
                while ((readed = inputStream.read(buf1)) > 0) {
                    zos.write(buf1, 0, readed);
                }
                zos.closeEntry();
            }
            ZipEntry ze = new ZipEntry("database/firebird.fbk");
            zos.putNextEntry(ze);
            fi = new FileInputStream(tmpfile);
            byte[] buf = new byte[fi.available()];
            fi.read(buf);
            zos.write(buf);
            zos.closeEntry();
            zos.close();
            obdbackzip.close();
            fi.close();
            tmpfile.delete();
        } catch (IOException ex) {
            //ex.printStackTrace()
            obdbackzip.close();
            fOdbname.delete();
            Files.copy(Paths.get(odbpathbak), Paths.get(odbpath));
            if (fi != null) {
                fi.close();
            }
            if (tmpfile.exists()) {
                tmpfile.delete();
            }
            if (!cmdLineArgs.getShortArgs().containsKey("o") && !cmdLineArgs.getLongArgs().containsKey("obak")) {
                new File(odbpathbak).delete();
            }
            throw new IOException(ex);
        }
        if (!cmdLineArgs.getShortArgs().containsKey("o") && !cmdLineArgs.getLongArgs().containsKey("obak")) {
            new File(odbpathbak).delete();
        }
        if (!cmdLineArgs.getShortArgs().containsKey("r") && !cmdLineArgs.getLongArgs().containsKey("dontremovefdb")) {
            new File(dbname).delete();
        }
    }

    static public String unpackOdb(String odbname, String dbname, CmdLineArgs cmdLineArgs) throws FileNotFoundException, FileAlreadyExistsException, FileSystemException, SQLException, IOException {
        if (dbname != null && new File(dbname).exists()) {
            if (cli) {
                System.out.println(java.text.MessageFormat.format(I18N.getString("msg.file_exists"), new Object[]{
                    dbname}));
            } else {
                throw new FileAlreadyExistsException(java.text.MessageFormat.format(I18N.getString("msg.file_exists"), new Object[]{
                    dbname}));
            }
            return null;
        }
        if (!new File(odbname).exists()) {
            if (cli) {
                System.out.println(java.text.MessageFormat.format(I18N.getString("msg.file_odb_not_found"), new Object[]{
                    new File(odbname).getAbsolutePath()}));
            } else {
                throw new FileNotFoundException(java.text.MessageFormat.format(I18N.getString("msg.file_odb_not_found"), new Object[]{
                    new File(odbname).getAbsolutePath()}));
            }
            return null;
        }
        if (!new File(odbname).canWrite() || !new File(odbname).canRead()) {
            if (cli) {
                System.out.println(java.text.MessageFormat.format(I18N.getString("msg.file_locked"), new Object[]{
                    odbname}));
            } else {
                throw new FileSystemException(java.text.MessageFormat.format(I18N.getString("msg.file_locked"), new Object[]{
                    odbname}));
            }
            return null;
        }
        File tmpfile = null;
        try (ZipFile zf = new ZipFile(odbname)) {
            ZipEntry ze = zf.getEntry("database/firebird.fbk");
            if (!ze.isDirectory()) {

                write(zf.getInputStream(ze),
                        new BufferedOutputStream(new FileOutputStream(
                                tmpfile = Files.createTempFile(Paths.get(System.getProperty("java.io.tmpdir")), "fb", ".fbk").toFile()
                        )));
                BackupManager restoreManager = new FBBackupManager("EMBEDDED");
                restoreManager.setUser("SYSDBA");
                restoreManager.setPassword("masterkey");
                restoreManager.setVerbose(false);
                restoreManager.setRestoreReplace(false);
                if (dbname == null) {
                    dbname = File.createTempFile("fbd", ".fdb", Paths.get(System.getProperty("java.io.tmpdir")).toFile()).getAbsolutePath();
                    new File(dbname).delete();
                }
                restoreManager.setDatabase(dbname);
                restoreManager.setBackupPath(tmpfile.getPath());
                //restoreManager.setLogger(System.out);
                //try {
                restoreManager.restoreDatabase();
                //} catch (SQLException ex) {
                //    ex.printStackTrace();
                //fbFileName.delete();
                //fbFileName = null;
                //}
            }
        } catch (SQLException ex) {
            if (tmpfile != null) {
                tmpfile.delete();
            }
            throw new SQLException(ex);
        }
        if (tmpfile != null) {
            tmpfile.delete();
        }
        return dbname;
    }

    static void write(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) {
            out.write(buffer, 0, len);
        }
        out.close();
        in.close();
    }

}
