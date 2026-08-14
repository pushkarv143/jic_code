# Deploying the backend to `132.226.191.38`

Target: Ubuntu instance `instance-20260807-1559`, SSH user `ubuntu`, MySQL on
the same host.

## Why this is simpler than the local setup

Port 3306 is closed to the internet, which blocked connecting from your laptop.
The deployed backend is unaffected: it runs **on the same machine as MySQL**, so
it connects over `localhost:3306` and never crosses a firewall.

The port that now matters is **8080** — the one the browser has to reach. See
*Exposing the API* below; it is the same OCI security-list problem in a new place.

## One-time setup

**1. Convert your key** (only if it is a PuTTY `.ppk` — `scp` and `ssh` cannot
read those):

PuTTYgen → **Load** your `.ppk` → **Conversions → Export OpenSSH key** →
save as e.g. `school-server.pem`.

**2. Copy the deploy files up:**

```powershell
scp -i school-server.pem deploy\setup-server.sh deploy\school-backend.service deploy\school-backend.env.example ubuntu@132.226.191.38:/tmp/
```

**3. Prepare the server:**

```bash
ssh -i school-server.pem ubuntu@132.226.191.38
cd /tmp && sudo bash setup-server.sh
```

That installs Java 21 if missing, creates `/opt/school-backend/{uploads,logs}`,
installs and enables the systemd unit, and writes
`/etc/school-backend/school-backend.env` with a **freshly generated
`JWT_SECRET`**. It is idempotent — re-running it will not overwrite an existing
env file.

**4. Fill in the two values it cannot know:**

```bash
sudo nano /etc/school-backend/school-backend.env
```

- `DB_PASSWORD=310fc71474395c494d8c7237a42234a4`
- `FRONTEND_URL` / `CORS_ALLOWED_ORIGINS` — the exact browser origin, no
  trailing slash

**5. Load the database schema**, if this database is empty. Check first:

```bash
mysql -u root -p school_management_system -e "SHOW TABLES;"
```

If it comes back empty, run `00`–`10` from `database/` in order (see
`database/README.md`). If it already has tables, run only `08`, `09` and `10` —
those are individually guarded and safe to re-run, whereas `01`–`07` are not.

## Deploying

From the repository root, every time:

```powershell
.\deploy\deploy.ps1 -KeyFile C:\path\to\school-server.pem
```

It builds the jar, uploads it to a staging path, stops the service, keeps the
previous jar as `school-backend.jar.prev`, swaps the new one in, restarts, and
checks the service came up. Add `-SkipBuild` to redeploy the jar you already
built.

**Rollback**, if a deploy goes bad:

```bash
sudo systemctl stop school-backend
sudo mv /opt/school-backend/school-backend.jar.prev /opt/school-backend/school-backend.jar
sudo systemctl start school-backend
```

## Exposing the API

The service listens on `8080`, which is closed to the internet exactly as 3306
is. Two options.

**Option A — open 8080 directly.** Quickest, but publishes an unencrypted API.

1. OCI console → the instance's Security List / NSG → ingress rule: TCP `8080`
   from your IP (avoid `0.0.0.0/0`).
2. On the server:
   ```bash
   sudo iptables -I INPUT 1 -p tcp --dport 8080 -j ACCEPT
   sudo netfilter-persistent save
   ```

> Ubuntu images on OCI ship iptables rules that drop unlisted ports. Opening the
> security list alone is not enough, and the failure looks identical either way —
> a connection timeout — so do both before concluding it is broken.

**Option B — nginx on 80/443 (recommended).** Serve the built frontend and
reverse-proxy `/api` to 8080, so only port 80/443 is exposed and the two share
an origin, which removes the CORS problem entirely:

```nginx
server {
    listen 80;
    server_name 132.226.191.38;

    root /var/www/school;          # frontend `npm run build` output
    index index.html;
    location / { try_files $uri $uri/ /index.html; }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Uploaded photos, documents and study materials are served from disk.
    # Trailing slashes on both sides matter: without them nginx concatenates
    # rather than substitutes, and every file 404s.
    location /uploads/ { alias /home/ubuntu/app/uploads/; }
}
```

nginx runs as `www-data`, so it needs traverse permission on `/home/ubuntu`:

```bash
sudo chmod o+x /home/ubuntu /home/ubuntu/app
```

Without it every upload URL returns 403 while the file is plainly on disk —
an unhelpfully confusing failure.

There is already an `app/` directory in `~ubuntu` — check what it holds before
pointing nginx anywhere, in case something is already running on 80.

## Where uploaded files live

```
/home/ubuntu/app/uploads/
├── student-id-pictures/   <- student photos      (jpg/png/webp, max 2MB)
├── documents/             <- student documents   (pdf/jpg/png, max 5MB)
└── materials/             <- study materials     (office/pdf/zip, max 25MB)
```

Set by `UPLOAD_DIR` plus `UPLOAD_PHOTOS_SUBDIR` / `UPLOAD_DOCUMENTS_SUBDIR` /
`UPLOAD_MATERIALS_SUBDIR` in the env file. `setup-server.sh` creates all three
and chowns them to `ubuntu`.

Files are stored under a random UUID (`<uuid>.jpg`) — the original filename never
reaches disk, which rules out path traversal through a crafted filename and
collisions between two uploads both called `photo.jpg`. The database stores only
the relative URL, e.g. `students.photo_url = /uploads/student-id-pictures/<uuid>.jpg`.

**`UPLOAD_DIR` must be an absolute path.** systemd does not expand `~`, so
`~/app/uploads` would be taken literally and create a directory named `~`.

**Renaming a subdir does not move existing files.** Reads resolve
`UPLOAD_DIR` + whatever path is in the database, so rows written before a rename
keep pointing at the old folder — which still works, as long as you leave that
folder in place. To make old files follow a rename, move them on disk and update
the stored paths:

```sql
UPDATE students
SET photo_url = REPLACE(photo_url, '/uploads/photos/', '/uploads/student-id-pictures/')
WHERE photo_url LIKE '/uploads/photos/%';
```

## Operating it

```bash
sudo systemctl status school-backend      # is it up
journalctl -u school-backend -f           # follow logs
journalctl -u school-backend -n 100       # recent logs
sudo systemctl restart school-backend     # restart
curl http://localhost:8080/actuator/health
```

## Things that will actually go wrong

| Symptom | Cause |
|---|---|
| `UnsupportedClassVersionError` at startup | JRE older than 21. `java -version`. |
| `Access denied for user 'root'@'localhost'` | Wrong `DB_PASSWORD`, or the account is `root@%` only. |
| `Unknown database` | Schema not loaded — see step 5. |
| Starts, then browser calls fail with CORS | `CORS_ALLOWED_ORIGINS` must match the origin **exactly** — scheme, host, port, no trailing slash. |
| Service active but unreachable from your laptop | Port 8080 not opened in *both* the OCI security list and iptables. |
| `Public Key Retrieval is not allowed` | `allowPublicKeyRetrieval=true` missing from `DB_URL`. |

## Security notes

- `/etc/school-backend/school-backend.env` is `chmod 600`, root-owned. Secrets
  are kept out of the systemd unit deliberately: unit files are world-readable
  and their contents appear in `systemctl cat`.
- `setup-server.sh` generates a real `JWT_SECRET`. If you ever bypass it, do not
  leave the default from `application.yml` in place — it is in this repository,
  so anyone who has read it could forge an admin token.
- The DB password was shared in a chat transcript; rotate it, and prefer a
  non-root MySQL user scoped to `school_management_system`.
