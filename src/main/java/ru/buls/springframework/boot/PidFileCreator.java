package ru.buls.springframework.boot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

/**
 * Created by Bulgakov Alexander on 06.09.16.
 * This {@link ApplicationRunner} create PID file on starting an application
 * but check on PID file existing. If the file exists then this application will terminate.
 * See {@link org.springframework.boot.system.ApplicationPidFileWriter}
 */
@Component
@Slf4j
public class PidFileCreator implements ApplicationRunner {
    public static final String DEFAULT_FILE_NAME = "application.pid";
    private final File pidFile;
    private final boolean exitIfPidExists;

    public PidFileCreator() {
        this(true);
    }

    public PidFileCreator(boolean exitIfPidExists) {
        this(DEFAULT_FILE_NAME, exitIfPidExists);
    }

    public PidFileCreator(String pidFileName, boolean exitIfPidExists) {
        this(new File(pidFileName), exitIfPidExists);
    }

    public PidFileCreator(File pidFile, boolean exitIfPidExists) {
        this.pidFile = pidFile;
        this.exitIfPidExists = exitIfPidExists;
    }

    private void checkPidFile() {
        if (pidFile.getAbsoluteFile().exists()) {
            log.warn("The PID file {} already has been created. path {}",
                    pidFile.getName(), pidFile.getAbsolutePath());
            if (exitIfPidExists) exitOnError();
        }
    }

    private void whitePidFile() {
        String pid = System.getProperty("PID");
        if (pid == null) {
            log.warn("Cannot retrieve PID from System property, check Spring Boot LoggingSystemProperties.class");
            pid = getPidFromRuntimeMX();
        }
        pidFile.deleteOnExit();
        final File dir = pidFile.getAbsoluteFile().getParentFile();
        final String pidFileAbsolutePath = pidFile.getAbsolutePath();
        if (!dir.canExecute()) {
            final boolean created = dir.mkdirs();
            if (!created) log.error("Cannot create directories for PID file {}", pidFileAbsolutePath);
        }
        try (FileWriter out = new FileWriter(pidFile)) {
            out.write(pid);
        } catch (Exception e) {
            log.warn("Cannot create PID file '{}', PID is {}", pidFileAbsolutePath, pid);
            exitOnError();
        }
        log.info("PID file has been created: {}", pidFileAbsolutePath);
        log.info("Process id is {}", pid);
    }

    public void run(ApplicationArguments args) throws Exception {
        checkPidFile();
        whitePidFile();
    }

    private void exitOnError() {
        log.warn("Exit on error");
        System.exit(-1);
    }

    private String getPidFromRuntimeMX() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        String name = runtimeMXBean.getName();
        return name.split("@")[0];
    }
}
