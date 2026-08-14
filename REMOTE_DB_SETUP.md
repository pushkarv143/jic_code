# Pointing the local backend at a remote MySQL server

## Status

The backend needs **no code change** — `application.yml` already reads the
datasource from environment variables:

```yaml
url:      ${DB_URL:jdbc:mysql://localhost:3306/school_management_system?...}
username: ${DB_USERNAME:root}
password: ${DB_PASSWORD:root}
```

Set those three and it connects wherever you point it.

**However:** `132.226.191.38:3306` is **not reachable** from this machine, while
**port 22 on the same host is open**:

```
Test-NetConnection 132.226.191.38 -Port 22    -> TcpTestSucceeded : True
Test-NetConnection 132.226.191.38 -Port 3306  -> TcpTestSucceeded : False
```

That pair of results is diagnostic, and it rules a lot out:

- the host is up and routable;
- your network is **not** blocking outbound traffic to it, since SSH gets through;
- so the block is specific to port 3306 — on the server side.

That points at the OCI security list, which by default opens 22 and nothing
else. Work through the checklist below, or **skip it entirely and use the SSH
tunnel** — port 22 is already open, so that route works right now.

## Run it

```powershell
cd school-backend
.\run-local-remote-db.ps1
```

The script sets the three variables, checks the port, and fails fast with a
readable message instead of a buried JDBC timeout.

`run-local-remote-db.ps1` holds the real password and is **gitignored**
(`run-local*` in `.gitignore`). `run-local-remote-db.ps1.example` is the tracked
template — keep real credentials out of it.

## Why it is unreachable — in the order worth checking

`132.226.x.x` is Oracle Cloud Infrastructure, so there are two firewalls plus the
database's own bind address, and traffic must pass all three.

Because SSH already works, your own network is **not** the problem and the
checklist starts at the server.

**1. OCI security list / NSG ingress — most likely.** In the OCI console:
`Networking → VCN → Security Lists` (or the NSG attached to the instance) → add
an ingress rule:

| Field | Value |
|---|---|
| Source | your public IP `/32` — not `0.0.0.0/0` |
| Protocol | TCP |
| Destination port | 3306 |

**2. The instance's own firewall.** OCI Ubuntu/Oracle Linux images ship with
iptables rules that drop everything except SSH. On the server:

```bash
sudo iptables -I INPUT 1 -p tcp --dport 3306 -j ACCEPT
sudo netfilter-persistent save        # Ubuntu
# or, on Oracle Linux / firewalld:
sudo firewall-cmd --permanent --add-port=3306/tcp && sudo firewall-cmd --reload
```

**3. MySQL is bound to localhost.** By default it listens on `127.0.0.1` only, so
it refuses non-local connections even with the firewalls open:

```bash
sudo grep bind-address /etc/mysql/mysql.conf.d/mysqld.cnf
# bind-address = 127.0.0.1   <-- change to 0.0.0.0, then:
sudo systemctl restart mysql
```

**4. The `root` user may be `root@localhost` only.** Confirm the account is
allowed to connect remotely:

```sql
SELECT user, host FROM mysql.user WHERE user = 'root';
```

If only `localhost` is listed, remote logins will be rejected as
`Access denied` even once the network path is open.

Verify after each step with `Test-NetConnection` from your machine — that
isolates network problems (steps 1–2) from authentication problems (step 4),
which produce very different errors.

## Recommended: an SSH tunnel — works right now

Port 22 is already open, so this needs **no server changes at all**. It also
avoids exposing 3306 to the internet, which is worth preferring on its own
merits rather than as a fallback.

In one terminal, leave this running:

```powershell
ssh -L 3306:localhost:3306 <user>@132.226.191.38
```

That forwards your machine's port 3306 to the server's own `localhost:3306` —
which means it works even if MySQL is bound to `127.0.0.1`, because from MySQL's
point of view the connection *is* local.

Then start the backend normally in a second terminal:

```powershell
cd school-backend
.\mvnw.cmd spring-boot:run
```

No environment variables and no run script are needed on this path: the
backend's default URL already points at `localhost:3306`, so the tunnel makes
the remote database look exactly like a local one.

If your local MySQL is already using 3306, forward a different local port and
point `DB_URL` at it:

```powershell
ssh -L 3307:localhost:3306 <user>@132.226.191.38
$env:DB_URL = "jdbc:mysql://localhost:3307/school_management_system?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
```

## Connecting with DBeaver

DBeaver has an SSH tunnel built in, so it needs no server changes either. The
one thing people get wrong is **which host goes in which tab** — see the note
below the table.

**Database → New Database Connection → MySQL**

### Main tab

| Field | Value |
|---|---|
| Server Host | `localhost` |
| Port | `3306` |
| Database | `school_management_system` |
| Username | `root` |
| Password | *(the DB password)* — tick **Save password** |

> **This is the part that trips people up.** Do **not** put `132.226.191.38`
> here. With a tunnel, the Main tab describes how to reach MySQL *from the SSH
> server*, and MySQL is running on that same machine — so it is `localhost`.
> Putting the public IP here bypasses the tunnel and fails with a connection
> timeout, which looks identical to the tunnel being broken.

### SSH tab

Tick **Use SSH Tunnel**, then:

| Field | Value |
|---|---|
| Host/IP | `132.226.191.38` |
| Port | `22` |
| User Name | `opc` for Oracle Linux, `ubuntu` for Ubuntu images |
| Authentication Method | **Public Key** |
| Private Key | your key file — see the format note below |
| Passphrase | if the key has one |

Use **Test tunnel configuration** on that tab before testing the connection
itself. It isolates the two halves: if the tunnel passes and the connection
fails, the problem is MySQL credentials, not networking.

### Key format: convert `.ppk` first

DBeaver uses a Java SSH library that reads **OpenSSH** keys. It cannot read
PuTTY `.ppk` files — the symptom is `invalid privatekey` or
`Cannot read private key`, which does not hint at the real cause.

Convert with PuTTYgen (already on this machine):

1. Open **PuTTYgen** → **Load** → select your `.ppk` (set the file filter to
   *All Files*, since Load defaults to `.ppk` only in some versions)
2. Menu **Conversions → Export OpenSSH key**
3. Save as e.g. `oracle-db.pem`
4. Point DBeaver's *Private Key* field at that `.pem`

### Driver properties

MySQL 8 defaults to `caching_sha2_password`, which refuses to hand over its
public key on an unencrypted connection unless told otherwise. On the
**Driver properties** tab set:

| Property | Value |
|---|---|
| `allowPublicKeyRetrieval` | `true` |
| `useSSL` | `false` |

Without the first one the connection fails with *"Public Key Retrieval is not
allowed"* even though the username and password are correct.

### If you would rather not use DBeaver's tunnel

Run the tunnel yourself in a terminal and point DBeaver at plain
`localhost:3306` with no SSH tab configured at all:

```powershell
ssh -L 3306:localhost:3306 <user>@132.226.191.38
```

Same result; the only difference is that the tunnel dies when you close the
terminal.

## Before running the schema scripts

`database/01_schema.sql` creates tables and `07_sample_data.sql` inserts hundreds
of rows. If the remote database already holds real data, run the read-only check
first to see what is actually there:

```sql
SELECT table_name, table_rows FROM information_schema.tables
WHERE table_schema = 'school_management_system' ORDER BY table_name;
```

Files `08`, `09` and `10` are individually guarded and safe to re-run. Files
`01`–`07` are not — `01_schema.sql` uses bare `CREATE TABLE`, which fails on an
existing schema rather than merging into it.

## Credential note

The password for this server was shared in a chat transcript, so treat it as
disclosed and rotate it once the connection is working:

```sql
ALTER USER 'root'@'%' IDENTIFIED BY '<new-password>';
```

Then update `run-local-remote-db.ps1`. Separately, a database whose `root`
account is exposed directly to the internet is worth replacing with a
non-root application user restricted to `school_management_system`.
