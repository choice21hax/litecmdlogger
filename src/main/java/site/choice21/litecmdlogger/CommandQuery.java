package site.choice21.litecmdlogger;

import java.time.Instant;
import java.util.List;

public class CommandQuery {
    private final String player;          // exact player match, nullable
    private final Instant since;          // only commands at or after this time, nullable
    private final Boolean modOnly;        // true to include only mod commands; false to include only non-mod; null for all
    private final List<String> modPrefixes; // used when filtering modOnly

    public CommandQuery(String player, Instant since, Boolean modOnly, List<String> modPrefixes) {
        this.player = player;
        this.since = since;
        this.modOnly = modOnly;
        this.modPrefixes = modPrefixes;
    }

    public String player() { return player; }
    public Instant since() { return since; }
    public Boolean modOnly() { return modOnly; }
    public List<String> modPrefixes() { return modPrefixes; }

    public static CommandQuery empty() {
        return new CommandQuery(null, null, null, java.util.Collections.emptyList());
    }
}
