# Setting up a gateway test installation

## Variant A: Local with HSQLDB (fastest way)

### 1. Gateway data directory

```bash
mkdir -p ~/ProjectForgeGateway
```

Create the file `~/ProjectForgeGateway/projectforge.properties`:

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
# Same key as the main instance: the DAV and calendar tokens are pushed encrypted and stored 1:1
projectforge.security.authenticationTokenEncryptionKey=<key of the main instance>
```

Without the main instance's key the gateway generates a random one on first start (appended to this file), and
every CardDAV or ICS login on the gateway fails with "Can't authenticate user ... by given token".

### 2. Configure the main instance

Add to `~/ProjectForge/projectforge.properties`:

```properties
projectforge.gateway.push.enabled=true
projectforge.gateway.push.url=http://localhost:8090/api/gateway/sync
projectforge.gateway.push.secret=test-secret-12345
projectforge.gateway.push.syncIntervalMs=60000
```

With gateway push enabled, the main instance no longer allows data transfer areas with external access
(download/upload): they are administered on the gateway, and the server refuses to turn external access on.
The admin form shows a switch only for an area that already has it on, and only for switching it off. To allow
external access on the main instance as well, set:

```properties
projectforge.datatransfer.externalAccess.localAllowed=true
```

(`false` forbids it on any instance, even without a gateway.)

### 3. Start

**Terminal 1 – gateway:**

```bash
./gradlew :projectforge-application:bootJar
java -Dprojectforge.base.dir=$HOME/ProjectForgeGateway \
  -jar projectforge-application/build/libs/projectforge-application-{VERSION}.jar \
  --spring.profiles.active=external-gateway
```

**Terminal 2 – main instance:**

Start with ```-Dprojectforge.base.dir=$HOME/ProjectForge```

### 4. Verify the sync

Sync requests appear in the gateway logs after at most 60s. Manual test:

```bash
# Public heartbeat, available in every mode; the main instance checks it before every sync
# (200 with {"status":"UP","mode":"gateway"}, 503 while starting)
curl -i http://localhost:8090/rsPublic/heartbeat

# Simulate a user sync
curl -X POST http://localhost:8090/api/gateway/sync/users \
  -H "X-Gateway-Secret: test-secret-12345" \
  -H "Content-Type: application/json" \
  -d '[{"username":"testuser","email":"test@example.com","active":true}]'

# Check the endpoint filter (without login 403: Spring Security denies the request before the filter answers 404)
curl -s -o /dev/null -w "%{http_code}" http://localhost:8090/wa/

# CardDAV reachable
curl -X PROPFIND http://localhost:8090/.well-known/carddav
```

**Timeouts and statistics:** the main instance gives up a push after
`projectforge.integration.clients.gateway.responseTimeoutMs` (default 120s) and stops the whole sync with one
warning, if the gateway isn't reachable or answers too slowly. Addresses are pushed in batches of 500; a full sync
deletes missing addresses afterwards (`/api/gateway/sync/addressbooks/retain`), only if all batches succeeded.
Duration and counts of every run are logged (`Sync 'gateway-push' (delta) success in ...`) and shown on the system
statistics page (group *Sync*, admins only): `gateway-push` on the main instance, `gateway-receive-*` on the gateway.

Test of the timeout: let the push url point to a port which accepts connections but never answers, e.g.
`nc -lk 8091` and `projectforge.gateway.push.url=http://localhost:8091/api/gateway/sync` together with
`projectforge.integration.clients.gateway.responseTimeoutMs=3000`. Expected: one warning per sync, status
*aborted* and `timeouts=1` on the statistics page.

---

## Variant B: Podman + Postgres on a Debian server

### Prerequisites on the server

```bash
sudo apt update && sudo apt install -y podman podman-compose
```

### 1. Build the JAR locally and create the image on the server

**Locally: build the fat JAR and copy the build context to the server**

```bash
./gradlew clean build

ssh user@server "mkdir -p ~/build/docker"
rsync -P -e ssh projectforge-application/build/libs/projectforge-application-8.2-SNAPSHOT.jar user@server:~/build/
scp Dockerfile user@server:~/build/
scp docker/entrypoint.sh docker/environment.sh user@server:~/build/docker/
```

**On the server: build the Docker image**

```bash
ssh user@server
cd ~/build
podman build \
  --build-arg JAR_FILE=projectforge-application-8.2-SNAPSHOT.jar \
  -t micromata/projectforge-gateway:test .
```

Rebuild both the JAR and the image after code changes, otherwise a stale build keeps
running. The commit of the running build is logged at startup (`git=<branch>@<hash>`) and
can be compared against the branch.

### 2. Copy compose and nginx files to the server

```bash
scp docker/compose/gateway/docker-compose-gateway.yml user@server:~/gateway/
scp -r docker/compose/gateway/nginx user@server:~/gateway/
scp docker/compose/gateway/projectforge.properties user@server:~/gateway/ProjectForge/
```

The compose file terminates TLS via nginx, which is the recommended setup. For a quick
test without TLS, drop the `nginx` and `certbot` services, publish the application port
directly (`ports: - "8090:8080"`) and skip step 3. In that case `projectforge.domain` and
the redirect URI registered in Authentik must use `http://<host>:8090` instead of the
HTTPS host name.

### 3. Create a TLS certificate (Let's Encrypt)

On first start the certificate has to be created initially. Replace `gateway.example.com`
with the actual, publicly resolvable host name (in `nginx/nginx.conf` as well) and
`admin@example.com` with a real address — Let's Encrypt rejects `example.com`. Port 80 of
the host must be reachable from the internet for the HTTP-01 challenge.

The temporary nginx uses `nginx/nginx-init.conf` (HTTP only), not `nginx/nginx.conf`: the
latter contains the 443 server block, which refers to the not yet existing certificate, so
nginx would refuse to start. Make sure step 2 copied both files (`ls nginx/`).

```bash
ssh user@server
cd ~/gateway
mkdir -p nginx/certs nginx/webroot

# Actual host name, used in nginx.conf and for certbot below
GW_HOST=gateway.example.com
sed -i "s/gateway.example.com/$GW_HOST/g" nginx/nginx.conf

# Rootless Podman may not bind port 80 by default (see troubleshooting)
sudo sysctl net.ipv4.ip_unprivileged_port_start=80

# Temporarily start nginx without SSL (for the ACME challenge)
podman run --rm -d --name nginx-init \
  -p 80:80 \
  -v ./nginx/nginx-init.conf:/etc/nginx/nginx.conf:ro \
  -v ./nginx/webroot:/var/www/certbot \
  docker.io/library/nginx:alpine

# Check that it is running (otherwise: podman logs nginx-init — run without --rm to keep it)
podman ps

# Obtain the certificate
podman run --rm \
  -v ./nginx/certs:/etc/letsencrypt \
  -v ./nginx/webroot:/var/www/certbot \
  docker.io/certbot/certbot certonly \
    --webroot -w /var/www/certbot \
    -d $GW_HOST \
    --agree-tos --non-interactive -m admin@example.com

podman stop nginx-init
```

### 4. Set up the ProjectForge home on the server

The directory `~/gateway/ProjectForge` is bind-mounted into the container. Properties,
logs, the Lucene index and uploads live directly in the file system:

```bash
ssh user@server
mkdir -p ~/gateway/ProjectForge
```

Create `~/gateway/ProjectForge/projectforge.properties`. Only the settings below are needed —
CardDAV and the menu visibility come from the `external-gateway` profile automatically.

`projectforge.domain` must match the URL the gateway is actually reached at, including the
port when the container port is published directly (the default from `application.properties`
is `http://localhost:8080`). It is the `{baseUrl}` of the OAuth2 redirect URI and therefore
has to match what is registered in Authentik.

Set `projectforge.gateway.sync.secret` here as a literal value and drop `GATEWAY_SYNC_SECRET`
from the compose file. The `external-gateway` profile defaults the property to
`${GATEWAY_SYNC_SECRET:}`, but since this file has the higher priority it simply wins, so the
environment variable is not needed. Keeping the secret out of the compose file also keeps it
out of version control and out of the container environment, where `podman inspect` would
expose it.

```properties
projectforge.domain=https://gateway.example.com
projectforge.gateway.enabled=true
projectforge.gateway.sync.secret=<your-secret>

# PostgreSQL
spring.datasource.url=jdbc:postgresql://postgres:5432/projectforge
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=projectforge
spring.datasource.password=projectforge-gw-pass

# Encryption key for the users' authentication tokens
projectforge.security.authenticationTokenEncryptionKey=CHANGE_ME

# OAuth2/OIDC — only required for the DataTransfer UI (see note below)
spring.security.oauth2.client.registration.authentik.client-id=YOUR_CLIENT_ID
spring.security.oauth2.client.registration.authentik.client-secret=YOUR_CLIENT_SECRET
spring.security.oauth2.client.registration.authentik.scope=openid,profile,email
spring.security.oauth2.client.registration.authentik.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.authentik.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
spring.security.oauth2.client.provider.authentik.issuer-uri=https://auth.example.com/application/o/projectforge/
```

Note that `#` does not start a comment in the middle of a properties line — a comment
appended to a value becomes part of that value. Always put comments on their own line.

`projectforge.security.authenticationTokenEncryptionKey` is required: the DAV and calendar
tokens pushed by the main instance are stored encrypted, so without the key they cannot be
decrypted. If the key is lost or changed later, all users have to renew their authentication
passwords.

**Precedence:** ProjectForge adds the home `projectforge.properties` as
`--spring.config.additional-location`, which Spring loads with the *highest* priority — it
therefore overrides the `external-gateway` profile. Setting for example
`projectforge.carddav.server.enable=false` here silently wins over the profile, so keep this
file limited to the settings above.

**OAuth2 note:** the OAuth2 block is optional. Without `client-id` the `OAuth2UserService`
bean is not created and the gateway starts without any login option — CardDAV and ICS still
work, since they authenticate via tokens. Only the DataTransfer UI requires OAuth2: after the
login at the IdP the user lands on `/next/datatransfer/`. The DataTransfer plugin is always
active in gateway mode. Of the projectforge-next app only the data transfer pages
(`/next/datatransfer/...`), its assets (`/next/_next/...`) and the REST calls of these pages and
of the app's shell are served; the attachment calls only for category `datatransfer`. Any other
page shows the gateway's error page (`/error`), which tells whether and as whom you are logged
in; the login page is `/next/login` (also the target after logout, and where the app goes when
the session has ended). A user must exist and be active on the gateway (synced from the main
instance).

**Important — Spring profile and `environment.sh`:** in docker mode ProjectForge creates an
`environment.sh` in the ProjectForge home on first start, containing an empty
`export JAVA_ARGS=`. Since `entrypoint.sh` sources that file, a `JAVA_ARGS=--spring.profiles.active=external-gateway`
passed via the container environment used to be lost from the second start onwards (visible
in the log as `No active profile set`). After the fix in `docker/entrypoint.sh`, values from
the container environment take precedence. With older images, set the profile directly in
`~/gateway/ProjectForge/environment.sh` instead:

```bash
export JAVA_ARGS=--spring.profiles.active=external-gateway
```

Set permissions (the container runs as user `projectforge`, UID 101):

```bash
podman unshare chown -R 101:101 ~/gateway/ProjectForge
```

After this, the files are no longer editable directly — see the troubleshooting entry below.

### 5. Start on the server

Enable lingering for the user once, otherwise systemd-logind kills all of the user's
processes — including rootless containers — when the last login session ends (logout, SSH
timeout), even with `-d`:

```bash
sudo loginctl enable-linger $USER
loginctl show-user $USER --property=Linger   # Linger=yes
```

Then start the stack detached (`-d`). Without `-d` it is bound to the terminal: Ctrl-C, a
closed terminal or a dropped SSH connection stops all containers.

```bash
cd ~/gateway
podman-compose -f docker-compose-gateway.yml up -d
```

`podman compose` (with a blank) is the same: it delegates to `podman-compose`.

The gateway is now reachable at `https://gateway.example.com`. Nginx terminates TLS and
forwards internally to the Spring Boot container. Certbot renews the certificate
automatically every 12h.

Day-to-day operation (in `~/gateway`, `-f docker-compose-gateway.yml` omitted for brevity):

| Command | Effect |
|---------|--------|
| `podman-compose start` / `stop` | starts/stops the existing containers |
| `podman-compose down` | stops and removes the containers; the database (named volume `postgres-data`) and `./ProjectForge` are kept. Never add `-v`, it deletes the volume |
| `podman-compose down && podman-compose up -d` | recreates the containers, e.g. after a new image was built |
| `podman ps -a --filter name=gateway_` | status of all gateway containers |

Once the stack runs via systemd (step 5a), use `systemctl --user start|stop|restart
projectforge-gateway` instead, so that systemd's state stays in line with the containers.

### 5a. Survive reboots (systemd user service)

Podman has no daemon: `restart: unless-stopped` only restarts a container that crashes while
Podman is around, nothing starts the stack after a reboot. A systemd user service does this,
together with lingering from step 5 (which starts the user's systemd instance at boot,
without anyone logging in).

Create `~/.config/systemd/user/projectforge-gateway.service`:

```ini
[Unit]
Description=ProjectForge gateway (podman-compose stack)

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=%h/gateway
# Removes containers left over from an unclean stop (otherwise "container name ... is already in use").
ExecStartPre=-/usr/bin/podman-compose -f docker-compose-gateway.yml down
ExecStart=/usr/bin/podman-compose -f docker-compose-gateway.yml up -d
# Gives ProjectForge 60s to shut down cleanly (default 10s, then SIGKILL).
ExecStop=/usr/bin/podman-compose -f docker-compose-gateway.yml down -t 60
TimeoutStartSec=600
TimeoutStopSec=180

[Install]
WantedBy=default.target
```

`TimeoutStartSec` is generous because the first start may pull images and waits for the
Postgres health check. Activate it — stop a manually started stack first, so systemd takes
over:

```bash
cd ~/gateway && podman-compose -f docker-compose-gateway.yml down
systemctl --user daemon-reload
systemctl --user enable --now projectforge-gateway
systemctl --user status projectforge-gateway
podman ps
```

If `systemctl --user` fails with `Failed to connect to bus` (e.g. after `su`/`sudo -u`), log in
directly as the user via SSH, or set `export XDG_RUNTIME_DIR=/run/user/$(id -u)`.

Test it with `sudo reboot`; afterwards `podman ps` must show all four containers, without
having to log in first. Output of the unit:

```bash
journalctl --user -u projectforge-gateway
```

Alternatives: `podman-compose systemd -a register` generates a similar unit
(`podman-compose@.service`), but requires the stack to run in a pod. Quadlets
(`~/.config/containers/systemd/*.container`) are the native Podman way, but the compose file
would have to be rewritten into one unit file per container.

### 6. Point the main instance at the remote gateway

In `~/ProjectForge/projectforge.properties`:

```properties
projectforge.gateway.push.enabled=true
projectforge.gateway.push.url=https://gateway.example.com/api/gateway/sync
projectforge.gateway.push.secret=test-secret-12345
projectforge.gateway.push.syncIntervalMs=60000
```

### 7. Check logs and status

```bash
# Logs directly in the file system
tail -f ~/gateway/ProjectForge/logs/ProjectForge.log

# Or via Podman
podman logs -f gateway_projectforge-gateway_1
podman ps
```

---

## Synced data (need-to-know principle)

The main instance pushes only the minimum necessary data to the gateway:

### Users
| Field | Description |
|------|-------------|
| `username` | Unique user name (identification) |
| `idpExternalId` | IdP id for OAuth2/OIDC login at the gateway |
| `davToken` | Token for CardDAV/CalDAV authentication |
| `calendarRestToken` | Token for ICS calendar subscriptions |
| `active` | Active state (access control) |

**Not synced:** email, first name, last name, password hashes, locale, etc.

### Groups
| Field | Description |
|------|-------------|
| `name` | Group name |
| `memberUsernames` | List of members (user names) |

**Not synced:** description, permission details.

### Addresses
Full contact data for CardDAV (name, organization, email, phone).

### CardDAV favorites
CardDAV serves only the addresses a user marked as favorite. The main instance pushes the
complete list of favorites (user name and address uid), but only if it changed since the last
push (on a full sync always). The gateway replaces its favorites with this list, so favorites
created locally on the gateway are removed.

### ICS data
Pre-computed ICS calendar exports per user and calendar.

### Delta sync and nightly full sync

| Run | When | What is pushed |
|-----|------|----------------|
| Delta sync | every `syncIntervalMs` (default 15 min) | all users and groups; only addresses (incl. images), favorites and ICS calendars changed since the last push |
| Full sync | nightly (`fullSyncCron`, default `0 0 3 * * *`) and on the first run after a restart of the main instance | everything |

```properties
projectforge.gateway.push.syncIntervalMs=900000
projectforge.gateway.push.fullSyncCron=0 0 3 * * *
```

Deletions on the main instance:

- **Users** are always pushed completely, deactivated and deleted ones with `active=false`. The
  gateway revokes their DAV and calendar tokens immediately (with the next delta sync).
- **Addresses** marked as deleted are part of the delta sync and are deleted on the gateway.
  Addresses missing completely (e.g. removed from the database) are deleted on the gateway by the
  full sync.
- **Groups** missing on the main instance are deleted on the gateway by the full sync (members
  removed, group marked as deleted).

After each sync with changed addresses or favorites, the gateway clears its address caches, so
CardDAV clients get the changes with their next request.

If the gateway restarts, it loses its in-memory ICS cache. It reports this in the response of
the ICS push, and the main instance then pushes all calendars again at once.

### Note: automatically generated tokens on the gateway

On a user's first sync the gateway automatically creates **all** token types
(`CALENDAR_REST`, `DAV_TOKEN`, `REST_CLIENT`, `STAY_LOGGED_IN_KEY`) in its local
`T_USER_AUTHENTICATIONS` table. The tokens for `REST_CLIENT` and `STAY_LOGGED_IN_KEY` are
**not** transferred by the main instance but generated locally as a side effect of
`UserAuthenticationsDao` initialization. They are not used functionally on the gateway and
can be ignored.

---

## Troubleshooting

### Startup fails: no bean of type `OAuth2UserService`

```
Field oAuth2UserService in org.projectforge.gateway.GatewaySecurityConfig required a bean
of type 'org.projectforge.security.OAuth2UserService' that could not be found.
```

Older builds injected the service as mandatory, so the gateway refused to start without
OAuth2 configuration. This was fixed by commit `08b59438` ("Improve gateway resilience");
the dependency is optional now. Compare the commit hash in the startup log against the
branch — if it predates the fix, rebuild the JAR and the image.

### Authentik: "Redirect URI Error" (`redirect_uri=http://...`)

The authorize URL in the browser shows `redirect_uri=http://<host>/login/oauth2/code/authentik`
although the gateway is reached via HTTPS. nginx forwards to the application via plain HTTP;
unless Spring evaluates `X-Forwarded-Proto`, `{baseUrl}` resolves to `http://...`, which does
not match the `https://` URI registered in Authentik (strict mode).

The `external-gateway` profile sets `server.forward-headers-strategy=framework`. With older
builds, add the line to the home `projectforge.properties` and restart the container. Make
sure nginx sends `proxy_set_header X-Forwarded-Proto https;` (or `$scheme`) and
`proxy_set_header Host $host;`.

### Main instance: `Sync push to /users failed ... PKIX path building failed`

The JVM of the main instance does not trust the gateway's TLS certificate (self-signed or
private CA). Import it into a dedicated truststore, see Variant C step 8.

### `No active profile set` although `JAVA_ARGS` is set in compose

The auto-generated `environment.sh` in the ProjectForge home overrides `JAVA_ARGS`. See the
note in step 4.

### Files in the ProjectForge home cannot be edited (and `chown` fails)

After `podman unshare chown -R 101:101`, `ls -l` shows an owner like `689924` and editing
`projectforge.properties` is denied — as is `chown`, even though the files nominally belong
to you.

With rootless Podman, container UIDs are mapped into your subordinate UID range from
`/etc/subuid`: container UID 0 becomes your own UID, and everything above it lands in the
subuid range (`689924` above is the mapping of container UID 101, the `projectforge` user from
the Dockerfile). Those UIDs are allocated to you but are not your login user, so you have
neither write access nor the `CAP_CHOWN` needed to change ownership — that capability only
exists inside the namespace.

Edit inside the namespace, where you are root:

```bash
podman unshare vi ~/gateway/ProjectForge/projectforge.properties
```

Alternatively hand a single file back to yourself, edit it normally, then return it (container
UID 0 maps to your own user on the host):

```bash
podman unshare chown 0:0 ~/gateway/ProjectForge/projectforge.properties
# edit
podman unshare chown 101:101 ~/gateway/ProjectForge/projectforge.properties
```

### Gateway stops by itself (`Commencing graceful shutdown`)

The log ends with a normal shutdown without any error before it:

```
GracefulShutdown : Commencing graceful shutdown. Waiting for active requests to complete
ProjectForgeApp  : Shutdown...
HikariPool-1 - Failed to validate connection ... (This connection has been closed.)
```

The application did not crash, the container received SIGTERM from outside. If Hikari reports
closed connections at the same time, Postgres was stopped together with it — the whole stack
was stopped. Typical causes: the stack was started without `-d` and the terminal/SSH session
ended, or lingering is off and the last login session of the user ended. Check:

```bash
loginctl show-user $USER --property=Linger
journalctl -b --since "<time of the shutdown minus 1 min>"   # "Removed session", "Stopping User Manager"
podman events --since "<time>" --until "<time + 1 min>"
```

Fix: step 5 (lingering, `-d`) and step 5a (systemd service).

### `the container name "gateway_postgres_1" is already in use`

`up -d` tries to create containers that still exist (stopped) from the last run. Either start
them with `podman-compose -f docker-compose-gateway.yml start`, or remove and recreate them
with `down` followed by `up -d` (the data is kept, see step 5). The systemd service from
step 5a does this automatically before every start.

### Base image not found

The Dockerfile uses `docker.io/eclipse-temurin:17-jre-jammy`. If Podman cannot resolve the
registry:

```bash
podman pull docker.io/eclipse-temurin:17-jre-jammy
```

### JAR_FILE build argument

The build argument must contain the relative path to the fat JAR (not the `-plain.jar`):

```bash
podman build \
  --platform linux/amd64 \
  --build-arg JAR_FILE=projectforge-application/build/libs/projectforge-application-8.2-SNAPSHOT.jar \
  -t micromata/projectforge-gateway:test .
```

### Podman rootless: port < 1024

If the gateway should listen on port 80/443:

```bash
sudo sysctl net.ipv4.ip_unprivileged_port_start=80
```

---

## Variant C: Native nginx + self-signed certificate with a `.priv` domain (test only)

Use this variant when nginx runs natively on the Debian gateway server (no Podman/Docker
for the reverse proxy) and the host is not publicly reachable, so Let's Encrypt is not an
option. ProjectForge itself still runs as a Podman container or directly from the JAR.

### 1. Install nginx

```bash
sudo apt update && sudo apt install -y nginx
```

### 2. Generate the certificate

The certificate must carry a `subjectAltName`; modern browsers and Java's HTTPS client
reject certificates that only set the hostname in the `CN` field.

```bash
sudo openssl req -x509 -nodes -days 825 \
  -newkey rsa:2048 \
  -keyout /etc/ssl/projectforge.key \
  -out /etc/ssl/projectforge.crt \
  -subj "/CN=gateway.priv" \
  -addext "subjectAltName=DNS:gateway.priv"
```

### 3. DH parameters (for TLS 1.2 fallback)

2048 bit is sufficient for a test installation and much faster to generate than 4096:

```bash
sudo openssl dhparam -out /etc/nginx/dhparam.pem 2048
```

### 4. Configure nginx

Copy the template from the repository and replace the placeholder domain:

```bash
sudo cp doc/misc/nginx_sites-available_projectforge \
  /etc/nginx/sites-available/projectforge
sudo sed -i 's/projectforge.example.com/gateway.priv/g' \
  /etc/nginx/sites-available/projectforge
```

The template already points to `proxy_pass http://localhost:8080` — adjust the port if
ProjectForge listens elsewhere (e.g. 8090 for Variant A/B setups).

Activate and reload:

```bash
sudo ln -sf /etc/nginx/sites-available/projectforge \
            /etc/nginx/sites-enabled/projectforge
sudo nginx -t && sudo systemctl reload nginx
```

### 5. DNS resolution for `.priv`

`.priv` is not a public TLD, so every host that needs to reach the gateway must resolve it
via `/etc/hosts`. Add the following on the gateway server itself, on the Authentik host,
and on every developer machine:

```
<gateway-ip>  gateway.priv
```

### 6. Tell Authentik to trust the certificate

Authentik verifies the TLS certificate when it contacts the gateway's OIDC endpoints
(e.g. the token endpoint). A self-signed certificate is not trusted by default.

**Option A — import the certificate into Authentik (recommended):**

1. In the Authentik Admin UI go to *System → Certificates → Import*.
2. Paste the contents of `/etc/ssl/projectforge.crt`.
3. Open the provider that points to the gateway and select the imported certificate
   under *Verification certificate*.

**Option B — disable verification in Authentik (quick test only):**

In `authentik.env` (or the Authentik compose environment block):

```env
AUTHENTIK_OUTPOSTS__DISABLE_EMBEDDED_OUTPOST_SSL_VERIFY=true
```

Restart Authentik after the change.

### 7. Trust the Authentik certificate in the ProjectForge JVM

ProjectForge calls Authentik's OIDC discovery endpoint on startup. If Authentik itself uses
a self-signed or private-CA certificate, import it into the JVM truststore:

```bash
# Find the JDK in use, e.g.:
JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))

sudo keytool -import -alias authentik-priv \
  -file /path/to/authentik.crt \
  -keystore $JAVA_HOME/lib/security/cacerts \
  -storepass changeit -noprompt
```

Alternatively, pass a JVM flag (test setups only — disables all hostname verification):

```bash
# In ~/ProjectForge/environment.sh or the service unit:
export JAVA_ARGS="--spring.profiles.active=external-gateway \
  -Djdk.internal.httpclient.disableHostnameVerification=true"
```

### 8. Trust the gateway certificate in the main instance JVM

The main instance pushes its sync data via HTTPS to the gateway. With a self-signed
certificate the push fails with:

```
Sync push to /users failed
... PKIX path building failed: ... unable to find valid certification path to requested target
```

Point `projectforge.gateway.push.url` at the internal host name (e.g.
`https://gateway.priv/api/gateway/sync`), not at a public name that is not resolvable
internally or is protected by an SSO proxy.

**1. Fetch and check the certificate** on the host of the main instance:

```bash
openssl s_client -connect gateway.priv:443 -servername gateway.priv </dev/null 2>/dev/null \
  | openssl x509 > gateway.crt
openssl x509 -in gateway.crt -noout -subject -ext subjectAltName
```

The output must contain `DNS:gateway.priv`. Without a matching `subjectAltName` Java fails the
hostname verification even after the import — regenerate the certificate as in step 2.

**2. Create a dedicated truststore.** Prefer this over modifying the JDK's `cacerts`: on Debian
`ca-certificates-java` regenerates the system `cacerts` on updates, which silently drops the
import. It also works without `sudo`. Start from a *copy* of `cacerts` — an empty truststore
would replace all public CAs, and calls to e.g. Authentik would fail.

```bash
# Find the JDK the main instance runs with (path and current -D options):
ps -o args= -C java | head -1

JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))   # or the JDK from above
cp $JAVA_HOME/lib/security/cacerts ~/pf-truststore.jks
keytool -importcert -alias gateway-priv -file gateway.crt \
  -keystore ~/pf-truststore.jks -storepass changeit -noprompt
keytool -list -keystore ~/pf-truststore.jks -storepass changeit -alias gateway-priv
```

**3. Start the main instance JVM with this truststore** — wherever its Java options are set
(start script, systemd unit, `JAVA_OPTS`), using the absolute path:

```
-Djavax.net.ssl.trustStore=/home/<user>/pf-truststore.jks -Djavax.net.ssl.trustStorePassword=changeit
```

If the main instance runs in a container, the truststore has to be available inside the
container. Restart the main instance; the sync (a full sync, being the first run after the
restart) appears in the gateway log after at most `syncIntervalMs`.

Note that the truststore is a snapshot of the JDK's `cacerts`: after a JDK update, recreate it
so that new or renewed public CAs are included.

### 9. `projectforge.properties` for the `.priv` domain

Same as Variant B step 4, with the host names replaced:

```properties
projectforge.domain=https://gateway.priv
spring.security.oauth2.client.registration.authentik.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
spring.security.oauth2.client.provider.authentik.issuer-uri=https://auth.priv/application/o/projectforge/
```

### 10. Register the redirect URI in Authentik

In the Authentik provider settings set the allowed redirect URI to:

```
https://gateway.priv/login/oauth2/code/authentik
```

---

## OAuth/Authentik redirect URI

Register the redirect URI in the Authentik provider:
- Local: `http://localhost:8090/login/oauth2/code/authentik`
- Remote: `https://gateway.example.com/login/oauth2/code/authentik`
- `.priv` (self-signed): `https://gateway.priv/login/oauth2/code/authentik`
