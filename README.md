# LiteCMDLogger
[![Maven Package](https://github.com/choice21hax/litecmdlogger/actions/workflows/maven-publish.yml/badge.svg?event=release)](https://github.com/choice21hax/litecmdlogger/actions/workflows/maven-publish.yml)
A lightweight Paper/Spigot plugin that logs player commands to a database (SQLite by default, MySQL optional) and provides an in-game paginated chest GUI to browse logs.

## Features
- SQLite by default (file stored in plugin data folder); optional MySQL with HikariCP
- Asynchronous command logging for performance
- Beautiful chest GUI with pagination (21 entries per page)
- Quick open command: `/commandlogs` or alias `/cl`
- Powerful filters right from the command:
  - `/cl [PLAYER] [TIMEFRAME] [MODCOMMANDS]`
  - Player filter, timeframe (e.g., `15m`, `24h`, `7d`), and moderation-only toggle
- Configurable moderation-command prefixes for filtering

## Commands
- `/commandlogs` — open the GUI
- `/cl` — alias for the above
- `/cl [PLAYER] [TIMEFRAME] [MODCOMMANDS]` — open a filtered GUI

Examples:
- `/cl` — all logs
- `/cl Notch` — only Notch’s logs
- `/cl Notch 24h` — Notch’s logs in the last 24h
- `/cl * 7d true` — all moderation commands over the last 7 days
- `/cl all all false` — all non-moderation commands, all time

Timeframe accepts:
- `Ns`, `Nm`, `Nh`, `Nd`, `Nw` (seconds, minutes, hours, days, weeks)
- `all` or `*` for no time filter

MODCOMMANDS accepts:
- `true` — only moderation commands
- `false` — only non-moderation commands
- Omit — include all

## Permissions
- `litecmdlogger.view` — allows opening the GUI (default: OP)

## Configuration
File: `src/main/resources/config.yml`

Storage selection:
```yaml
storage:
  type: sqlite  # sqlite (default) or mysql

sqlite:
  file: data.db  # created in the plugin data folder

mysql:
  host: "localhost"
  port: 3306
  database: "your_database"
  username: "your_user"
  password: "your_pass"
  pool:
    maximumPoolSize: 10
    minimumIdle: 2
    connectionTimeoutMs: 30000
```

GUI settings:
```yaml
gui:
  pageSize: 20
  title: "Command Logs"
```

Moderation command prefixes (without `/`):
```yaml
moderation:
  modPrefixes:
    - op
    - deop
    - gamemode
    - ban
    - tempban
    - mute
    - unmute
    - kick
    - tp
    - teleport
```

## How it works
- `CommandListener` captures `PlayerCommandPreprocessEvent` and logs asynchronously via `DatabaseManager`.
- `DatabaseManager` uses HikariCP for pooling and supports:
  - SQLite (default): file-based DB in the plugin data folder
  - MySQL: configured in `config.yml`
- `GUIManager` builds a 54-slot inventory (21 content slots + navigation) and handles:
  - Prev/Next page buttons
  - Close button
  - Filter persistence across pagination
- `CommandQuery` carries filters (player, since timestamp, mod-only) from command parsing into the data layer.

## Database schema
Table auto-creates on startup.

SQLite:
```sql
CREATE TABLE IF NOT EXISTS command_logs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  player TEXT NOT NULL,
  command TEXT NOT NULL,
  timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

MySQL:
```sql
CREATE TABLE IF NOT EXISTS command_logs (
  id INT AUTO_INCREMENT PRIMARY KEY,
  player VARCHAR(16) NOT NULL,
  command TEXT NOT NULL,
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Build
Prerequisites:
- JDK 21
- Maven 3.8+

Build command:
```bash
mvn clean package
```

Artifacts:
- `target/litecmdlogger-1.0-SNAPSHOT-shaded.jar` — shaded with required dependencies (HikariCP, JDBC drivers)

## Install
1. Build the plugin or download the jar.
2. Place the jar in your server `plugins/` folder.
3. Start the server once to generate the default `config.yml`.
4. (Optional) Edit `config.yml` to switch to MySQL and/or adjust GUI and moderation settings.
5. Restart the server.

## Usage
- Use `/commandlogs` or `/cl` to open the GUI.
- Use arrows to paginate, barrier to close.
- To filter, pass args as documented in the Commands section.

## Troubleshooting
- If you see database driver not found errors, ensure you are using the shaded jar (`-shaded.jar`).
- SQLite file location: `plugins/litecmdlogger/data.db` (configurable via `sqlite.file`).
- Ensure you’re running Java 21 if targeting Paper 1.21.x.

## Roadmap / Ideas
- Add server name column and filtering
- Export logs to CSV
- Per-player view command (e.g., `/clview <player>`) that opens directly in filtered mode
- MiniMessage/Adventure-based GUI text (remove deprecated ChatColor usage)

## License
There is no license, use at your own risk. This is fully open source and open to the public.
