# Gateway Teststellung aufbauen

## Variante A: Lokal mit HSQLDB (schnellster Weg)

### 1. Gateway-Datenverzeichnis

```bash
mkdir -p ~/.ProjectForgeGateway
```

Die Datei `~/.ProjectForgeGateway/projectforge.properties` anlegen:

```properties
projectforge.domain=http://localhost:8090
server.port=8090
projectforge.gateway.enabled=true
projectforge.gateway.sync.secret=test-secret-12345
spring.datasource.url=jdbc:hsqldb:file:${projectforge.base.dir}/database/projectforge;shutdown=true
spring.datasource.driver-class-name=org.hsqldb.jdbc.JDBCDriver
spring.datasource.username=sa
spring.datasource.password=
projectforge.carddav.server.enable=true
```

### 2. Main-Instanz konfigurieren

In `~/.ProjectForge/projectforge.properties` ergänzen:

```properties
projectforge.gateway.push.enabled=true
projectforge.gateway.push.url=http://localhost:8090/api/gateway/sync
projectforge.gateway.push.secret=test-secret-12345
projectforge.gateway.push.syncIntervalMs=60000
```

### 3. Starten

**Terminal 1 – Gateway:**

```bash
./gradlew :projectforge-application:bootJar
java -jar projectforge-application/build/libs/projectforge-application-*.jar \
  -Dprojectforge.base.dir=$HOME/.ProjectForgeGateway \
  --spring.profiles.active=external-gateway
```

**Terminal 2 – Main-Instanz:**

```bash
./gradlew bootRun
```

### 4. Sync prüfen

Nach max 60s erscheinen Sync-Requests in den Gateway-Logs. Manuell testen:

```bash
# User-Sync simulieren
curl -X POST http://localhost:8090/api/gateway/sync/users \
  -H "X-Gateway-Secret: test-secret-12345" \
  -H "Content-Type: application/json" \
  -d '[{"username":"testuser","email":"test@example.com","active":true}]'

# Endpoint-Filter prüfen (muss 404 liefern)
curl -s -o /dev/null -w "%{http_code}" http://localhost:8090/wa/

# CardDAV erreichbar
curl -X PROPFIND http://localhost:8090/.well-known/carddav
```

---

## Variante B: Podman + Postgres auf Debian-Server

### Voraussetzungen auf dem Server

```bash
sudo apt update && sudo apt install -y podman podman-compose
```

### 1. Image bauen (lokal mit Podman)

```bash
./gradlew :projectforge-application:bootJar

podman build \
  --platform linux/amd64 \
  --build-arg JAR_FILE=projectforge-application/build/libs/projectforge-application-8.2-SNAPSHOT.jar \
  -t micromata/projectforge-gateway:test .
```

Falls der Build lokal zu langsam ist (QEMU-Emulation), alternativ auf dem Server bauen.
Das Dockerfile erwartet `docker/entrypoint.sh` und `docker/environment.sh` relativ zum Build-Kontext:

```bash
# Lokal: Dateien in korrekter Struktur auf den Server kopieren
ssh user@server "mkdir -p ~/build/docker"
scp projectforge-application/build/libs/projectforge-application-8.2-SNAPSHOT.jar user@server:~/build/
scp Dockerfile user@server:~/build/
scp docker/entrypoint.sh docker/environment.sh user@server:~/build/docker/
```

```bash
# Auf dem Server bauen
ssh user@server
cd ~/build
podman build \
  --build-arg JAR_FILE=projectforge-application-8.2-SNAPSHOT.jar \
  -t micromata/projectforge-gateway:test .
```

### 2. Image auf Server übertragen

```bash
podman save micromata/projectforge-gateway:test | ssh user@server podman load
scp docker/compose/gateway/docker-compose-gateway.yml user@server:~/gateway/
```

### 3. ProjectForge-Home auf dem Server einrichten

Das Verzeichnis `~/gateway/ProjectForge` wird als Bind-Mount ins Container gemappt.
Hier liegen Properties, Logs, Lucene-Index und Uploads direkt im Filesystem:

```bash
ssh user@server
mkdir -p ~/gateway/ProjectForge
```

`~/gateway/ProjectForge/projectforge.properties` anlegen:

```properties
projectforge.gateway.enabled=true
projectforge.gateway.sync.secret=test-secret-12345

# PostgreSQL
spring.datasource.url=jdbc:postgresql://postgres:5432/projectforge
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=projectforge
spring.datasource.password=projectforge-gw-pass

# OAuth2/OIDC (required for gateway mode)
spring.security.oauth2.client.registration.authentik.client-id=YOUR_CLIENT_ID
spring.security.oauth2.client.registration.authentik.client-secret=YOUR_CLIENT_SECRET
spring.security.oauth2.client.registration.authentik.scope=openid,profile,email
spring.security.oauth2.client.registration.authentik.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.authentik.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
spring.security.oauth2.client.provider.authentik.issuer-uri=https://auth.example.com/application/o/projectforge/
```

Permissions setzen (Container läuft als User `projectforge`, UID 101):

```bash
podman unshare chown -R 101:101 ~/gateway/ProjectForge
```

### 4. Auf Server starten

```bash
cd ~/gateway
podman-compose -f docker-compose-gateway.yml up -d
```

### 5. Main-Instanz auf Remote zeigen

In `~/.ProjectForge/projectforge.properties`:

```properties
projectforge.gateway.push.enabled=true
projectforge.gateway.push.url=http://REMOTE_IP:8090/api/gateway/sync
projectforge.gateway.push.secret=test-secret-12345
projectforge.gateway.push.syncIntervalMs=60000
```

### 6. Logs und Status prüfen

```bash
# Logs direkt im Filesystem
tail -f ~/gateway/ProjectForge/logs/ProjectForge.log

# Oder über Podman
podman logs -f gateway_projectforge-gateway_1
podman ps
```

---

## Troubleshooting

### Base-Image nicht gefunden

Das Dockerfile nutzt `docker.io/eclipse-temurin:17-jre-jammy`. Falls Podman die Registry nicht findet:

```bash
podman pull docker.io/eclipse-temurin:17-jre-jammy
```

### JAR_FILE Build-Argument

Das Build-Argument muss den relativen Pfad zum Fat-JAR enthalten (nicht das `-plain.jar`):

```bash
podman build \
  --platform linux/amd64 \
  --build-arg JAR_FILE=projectforge-application/build/libs/projectforge-application-8.2-SNAPSHOT.jar \
  -t micromata/projectforge-gateway:test .
```

### Podman rootless: Port < 1024

Falls der Gateway auf Port 80/443 laufen soll:

```bash
sudo sysctl net.ipv4.ip_unprivileged_port_start=80
```

---

## OAuth/Authentik Redirect-URI

Im Authentik-Provider die Redirect-URI registrieren:
- Lokal: `http://localhost:8090/login/oauth2/code/authentik`
- Remote: `https://gateway.example.com/login/oauth2/code/authentik`
