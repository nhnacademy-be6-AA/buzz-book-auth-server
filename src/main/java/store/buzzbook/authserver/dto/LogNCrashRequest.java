package store.buzzbook.authserver.dto;

import lombok.Builder;

@Builder
public record LogNCrashRequest(
	String projectName,
	String projectVersion,
	String logVersion,
	String body,
	String logSource,
	String logType,
	String host,
	String Platform,
	String logLevel
) {
}
