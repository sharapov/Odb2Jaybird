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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.firebirdsql.management.BackupManager;
import org.firebirdsql.management.FBBackupManager;

/**
 *
 * @Sharapov
 */
public class Odbpack {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String[][] expectedArgs = {{"o",
            "onbak",
            "not create backup file for odb (default=yes)"},
        {"d",
            "dnbak",
            "not create backup file for fdb (default=yes)"},
        {"r",
            "removefdb",
            "remove fdb (default=no)"}
        };

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
                    System.out.println("odbpack {unpack|pack} name.odb name.fdb");
            }
        } else {
            System.out.println("odbpack {unpack|pack} name.odb name.fdb");
        }
    }

    static public void packOdb(String odbname, String dbname, CmdLineArgs cmdLineArgs) {
        File tmpfile = null;
        SimpleDateFormat dateFormet = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        File fOdbname = new File(odbname);
        try {
            if (!new File(dbname).exists()) {
                System.out.println("File not found " + new File(dbname).getAbsolutePath());
                return;
            }
            if (!fOdbname.exists()) {
                System.out.println("File not found " + fOdbname.getAbsolutePath());
                return;
            }
            String odbpath = new File(fOdbname.getAbsolutePath()).getParent() + File.separatorChar
                    + fOdbname.getName();
            String odbpathbak = new File(fOdbname.getAbsolutePath()).getParent() + File.separatorChar
                    + StringUtils.removeExtension(fOdbname.getName()) + "_" + dateFormet.format(new Date()) + "." + StringUtils.getExtension(fOdbname.getName());
            if (new File(odbpathbak).exists()) {
                System.out.println("File exists " + odbpathbak);
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
            try (
                    ZipFile newzip = new ZipFile(odbpathbak)) {
                try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(fOdbname))) {
                    Enumeration<? extends ZipEntry> entries = newzip.entries();
                    byte[] buf1 = new byte[8192];
                    int readed;
                    while (entries.hasMoreElements()) {
                        ZipEntry ze = entries.nextElement();
                        if (ze.getName().equals("database/firebird.fbk")) {
                            continue;
                        }
                        zos.putNextEntry(ze);
                        InputStream inputStream = newzip.getInputStream(ze);
                        //if(!ze.isDirectory()){
                        while ((readed = inputStream.read(buf1)) > 0) {
                            zos.write(buf1, 0, readed);
                        }
                        zos.closeEntry();
                    }
                    ZipEntry ze = new ZipEntry("database/firebird.fbk");
                    zos.putNextEntry(ze);
                    try (FileInputStream fi = new FileInputStream(tmpfile)) {
                        byte[] buf = new byte[fi.available()];
                        fi.read(buf);
                        zos.write(buf);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    zos.closeEntry();
                    zos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                tmpfile.delete();
            }
            if (!cmdLineArgs.getShortArgs().containsKey("o")) {
                new File(odbpathbak).delete();
            }
        } catch (SQLException | IOException ex) {
            ex.printStackTrace();
            if (tmpfile != null) {
                if (tmpfile.exists()) {
                    tmpfile.delete();
                }
            }
        }
    }

    static public String unpackOdb(String odbname, String dbname, CmdLineArgs cmdLineArgs) {
        if (dbname != null && new File(dbname).exists()) {
            System.out.println("File exists " + dbname);
            return null;
        }
        if (!new File(odbname).exists()) {
            System.out.println("Not found " + new File(odbname).getAbsolutePath());
            return null;
        }
        if (!new File(odbname).canWrite() || !new File(odbname).canRead()) {
            System.out.println("Locked " + odbname);
            return null;
        }
        try {
            try (ZipFile zf = new ZipFile(odbname)) {
                File tmpfile;
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
                    restoreManager.setDatabase(dbname = (dbname == null ? Files.createTempDirectory(Paths.get(System.getProperty("java.io.tmpdir")), "fbtmp").toAbsolutePath().toString() + File.separatorChar + "tmpfb" : dbname));
                    restoreManager.setBackupPath(tmpfile.getPath());
                    //restoreManager.setLogger(System.out);
                    try {
                        restoreManager.restoreDatabase();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        tmpfile.delete();
                        //fbFileName.delete();
                        //fbFileName = null;
                    }
                    tmpfile.delete();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
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
