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

Falls der Build lokal zu langsam ist (QEMU-Emulation), alternativ auf dem Server bauen:

```bash
scp projectforge-application/build/libs/projectforge-application-8.2-SNAPSHOT.jar user@server:~/gateway/
scp Dockerfile docker/entrypoint.sh docker/environment.sh user@server:~/gateway/
ssh user@server "cd ~/gateway && podman build \
  --build-arg JAR_FILE=projectforge-application-8.2-SNAPSHOT.jar \
  -t micromata/projectforge-gateway:test ."
```

### 2. Image auf Server übertragen

```bash
podman save micromata/projectforge-gateway:test | ssh user@server podman load

scp docker/compose/gateway/docker-compose-gateway.yml user@server:~/gateway/
scp docker/compose/gateway/projectforge.properties user@server:~/gateway/
```

### 3. Auf Server starten

```bash
ssh user@server
cd ~/gateway
podman-compose -f docker-compose-gateway.yml up -d
```

Alternativ mit `podman compose` (Podman 4.7+):

```bash
podman compose -f docker-compose-gateway.yml up -d
```

### 4. Main-Instanz auf Remote zeigen

In `~/.ProjectForge/projectforge.properties`:

```properties
projectforge.gateway.push.enabled=true
projectforge.gateway.push.url=http://REMOTE_IP:8090/api/gateway/sync
projectforge.gateway.push.secret=test-secret-12345
projectforge.gateway.push.syncIntervalMs=60000
```

### 5. Logs und Status prüfen

```bash
# Auf dem Server
podman logs -f gateway-projectforge-gateway-1
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

## Phase 2: OAuth/Authentik hinzufügen

Nach erfolgreichem Sync-Test in `projectforge.properties` der Gateway-Instanz ergänzen:

```properties
spring.security.oauth2.client.registration.authentik.client-id=YOUR_CLIENT_ID
spring.security.oauth2.client.registration.authentik.client-secret=YOUR_CLIENT_SECRET
spring.security.oauth2.client.registration.authentik.scope=openid,profile,email
spring.security.oauth2.client.registration.authentik.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.authentik.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
spring.security.oauth2.client.provider.authentik.issuer-uri=https://auth.example.com/application/o/projectforge/
```

Redirect-URI im Authentik-Provider registrieren:
- Lokal: `http://localhost:8090/login/oauth2/code/authentik`
- Remote: `https://gateway.example.com/login/oauth2/code/authentik`
