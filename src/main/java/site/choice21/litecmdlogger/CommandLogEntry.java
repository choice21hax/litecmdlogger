package site.choice21.litecmdlogger;

import java.sql.Timestamp;

public record CommandLogEntry(String player, String command, Timestamp timestamp) {}
