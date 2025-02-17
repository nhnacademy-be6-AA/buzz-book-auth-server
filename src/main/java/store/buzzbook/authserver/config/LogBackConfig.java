package store.buzzbook.authserver.config;

import static ch.qos.logback.classic.Level.*;
import static java.io.File.*;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.LevelFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.RollingPolicy;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.util.FileSize;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import store.buzzbook.authserver.appender.LogNCrashAppender;
import store.buzzbook.authserver.client.LogNCrashAdapter;

@Configuration
@RequiredArgsConstructor
public class LogBackConfig {

	@Value("${logncrash.version}")
	private String version;

	@Value("${logncrash.host}")
	private String host;

	@Value("${logncrash.platform}")
	private String platform;

	@Value("${logncrash.log.version}")
	private String logVersion;

	@Value("${logncrash.log.source}")
	private String logSource;

	@Value("${logncrash.log.type}")
	private String logType;

	@Value("${logncrash.app-key}")
	private String appKey;

	@Value("${logncrash.config.file-path}")
	private String filePath;

	@Value("${logncrash.config.file-name}")
	private String fileName;

	private final ResourceLoader resourceLoader;
	private final LogNCrashAdapter logNCrashAdapter;

	private final LoggerContext logCtx = (LoggerContext)LoggerFactory.getILoggerFactory();
	private static final String PATTERN = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-3level %logger{5} - %msg %n";
	private static final String FILENAMEPATTERN = ".%d{yyyy-MM-dd}_%i";
	private static final String EXT = ".log";
	private static final String MAXFILESIZE = "10MB";
	private static final int MAXHISTORY = 30;

	private ConsoleAppender<ILoggingEvent> consoleAppender;
	private RollingFileAppender<ILoggingEvent> fileAppender;
	private RollingFileAppender<ILoggingEvent> filterAppender;
	private AppenderBase<ILoggingEvent> logNCrashAppender;

	@PostConstruct
	public void logConfig() {
		consoleAppender = getLogConsoleAppender();
		fileAppender = getLogFileAppender();
		filterAppender = getFilterLogFileAppender();
		logNCrashAppender = getLogNCrashAppender();
		createLoggers();
	}

	private void createLoggers() {
		// 로거 이름, 로깅 레벨, 상위 로깅 설정 상속 여부
		createLogger("root", INFO, true);
		createLogger("jdbc", INFO, false);
		createLogger("jdbc.sqlonly", INFO, false);
		createLogger("jdbc.sqltiming", INFO, false);
		createLogger("store.buzzbook.authserver", DEBUG, false);
	}

	// 어펜더 추가 시 로거 등록 필요
	private void createLogger(String loggerName, Level logLevel, Boolean additive) {
		Logger logger = logCtx.getLogger(loggerName);
		logger.setAdditive(additive);
		logger.setLevel(logLevel);

		logger.addAppender(consoleAppender);
		logger.addAppender(fileAppender);
		logger.addAppender(filterAppender);

		if (logLevel.equals(DEBUG)) {
			logger.addAppender(logNCrashAppender);
		}
	}

	private LogNCrashAppender getLogNCrashAppender() {
		LogNCrashAppender appender = new LogNCrashAppender(version, host, platform, logVersion, logSource,
			logType, appKey, logNCrashAdapter);
		appender.start();
		return appender;
	}

	// 콘솔 로그 어펜더 생성
	private ConsoleAppender<ILoggingEvent> getLogConsoleAppender() {
		final String appenderName = "STDOUT";

		PatternLayoutEncoder consoleLogEncoder = createLogEncoder(PATTERN);
		return createLogConsoleAppender(appenderName, consoleLogEncoder);
	}

	// 롤링 파일 어펜더 생성
	private RollingFileAppender<ILoggingEvent> getLogFileAppender() {
		final String appenderName = "LOGS";

		final String logFilePath = filePath + separator + fileName;
		final String archiveLogFile = filePath + separator + appenderName + separator + fileName + FILENAMEPATTERN;

		PatternLayoutEncoder fileLogEncoder = createLogEncoder(PATTERN);
		RollingFileAppender<ILoggingEvent> logFileAppender = createLogFileAppender(appenderName, logFilePath,
			fileLogEncoder);
		SizeAndTimeBasedRollingPolicy<RollingPolicy> logFilePolicy = createLogFilePolicy(MAXFILESIZE, MAXHISTORY,
			archiveLogFile, logFileAppender);

		logFileAppender.setRollingPolicy(logFilePolicy);
		logFileAppender.start();

		return logFileAppender;
	}

	// 롤링 파일 어펜더 생성 (필터 적용)
	private RollingFileAppender<ILoggingEvent> getFilterLogFileAppender() {
		final String appenderName = "ERROR";

		final String errorLogFilePath = filePath + separator + appenderName + separator + fileName;
		final String errorLogFile = errorLogFilePath + FILENAMEPATTERN;

		PatternLayoutEncoder fileLogEncoder = createLogEncoder(PATTERN);
		RollingFileAppender<ILoggingEvent> logFileAppender = createLogFileAppender(appenderName, errorLogFilePath,
			fileLogEncoder);
		SizeAndTimeBasedRollingPolicy<RollingPolicy> logFilePolicy = createLogFilePolicy(MAXFILESIZE, MAXHISTORY,
			errorLogFile, logFileAppender);
		LevelFilter levelFilter = createLevelFilter(ERROR);

		logFileAppender.setRollingPolicy(logFilePolicy);
		logFileAppender.addFilter(levelFilter);
		logFileAppender.start();

		return logFileAppender;
	}

	private PatternLayoutEncoder createLogEncoder(String pattern) {
		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(logCtx);
		encoder.setPattern(pattern);
		encoder.start();
		return encoder;
	}

	private ConsoleAppender<ILoggingEvent> createLogConsoleAppender(String appenderName,
		PatternLayoutEncoder consoleLogEncoder) {
		ConsoleAppender<ILoggingEvent> logConsoleAppender = new ConsoleAppender<>();
		logConsoleAppender.setName(appenderName);
		logConsoleAppender.setContext(logCtx);
		logConsoleAppender.setEncoder(consoleLogEncoder);
		logConsoleAppender.start();
		return logConsoleAppender;
	}

	private RollingFileAppender<ILoggingEvent> createLogFileAppender(String appenderName, String logFilePath,
		PatternLayoutEncoder logEncoder) {
		RollingFileAppender<ILoggingEvent> logFileAppender = new RollingFileAppender<>();
		logFileAppender.setName(appenderName);
		logFileAppender.setContext(logCtx);
		logFileAppender.setEncoder(logEncoder);
		logFileAppender.setAppend(true);
		logFileAppender.setFile(logFilePath + EXT);
		return logFileAppender;
	}

	private SizeAndTimeBasedRollingPolicy<RollingPolicy> createLogFilePolicy(String maxFileSize, int maxHistory,
		String fileNamePattern, RollingFileAppender<ILoggingEvent> logFileAppender) {
		SizeAndTimeBasedRollingPolicy<RollingPolicy> logFilePolicy = new SizeAndTimeBasedRollingPolicy<>();
		logFilePolicy.setContext(logCtx);
		logFilePolicy.setParent(logFileAppender);
		logFilePolicy.setFileNamePattern(fileNamePattern + EXT);
		logFilePolicy.setMaxHistory(maxHistory);
		logFilePolicy.setMaxFileSize(FileSize.valueOf(maxFileSize));
		logFilePolicy.start();
		return logFilePolicy;
	}

	private LevelFilter createLevelFilter(Level level) {
		LevelFilter levelFilter = new LevelFilter();
		levelFilter.setLevel(level);
		levelFilter.setOnMatch(FilterReply.ACCEPT);
		levelFilter.setOnMismatch(FilterReply.DENY);
		levelFilter.start();
		return levelFilter;
	}

}
