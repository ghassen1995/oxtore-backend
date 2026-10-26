# Deploying to the VPS (Docker + Nginx + HTTPS)

Target: OVH VPS, Ubuntu 24.04, `57.129.77.16`, user `ubuntu`, domain `api.oxtore.com`. Commands run **on the
server** over SSH unless they say "on your PC".

**The server never gets the source code.** The image is built on your PC (later: in CI/CD) and copied over. The
server only holds `docker-compose.prod.yml`, `.env`, the Nginx config and the image, whose final stage contains the
compiled jar only.

What runs where:

```
Internet ──443/80──> Nginx (on the server)
                       ├─ /socket.io/  ──> 127.0.0.1:8082  (Socket.IO, container oxtore_backend)
                       └─ /            ──> 127.0.0.1:8081  (REST API, container oxtore_backend)
                                                └─> oxtore-db (Postgres, Docker network only, no public port)
```

Files used: `Dockerfile` (multi-stage: Maven builds the jar inside Docker), `docker-compose.prod.yml`,
`deploy/nginx/oxtore.conf`, `.env.example`.

---

## 1. Point the domain at the server

HTTPS certificates need a domain name (Let's Encrypt doesn't issue them for a bare IP).

In the OVH DNS zone of `oxtore.com`, add:

| Type | Name | Value |
|---|---|---|
| A | `api` | `57.129.77.16` |

Don't add an AAAA (IPv6) record for `api`. Check from the server:

```bash
dig +short api.oxtore.com
```

It must print `57.129.77.16` before step 9.

## 2. Update the system, firewall, swap

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y nginx

sudo ufw allow OpenSSH
sudo ufw allow 'Nginx Full'
sudo ufw enable
```

(`Nginx Full` exists only once Nginx is installed. Always allow OpenSSH before `ufw enable`.)

Add 2 GB of swap:

```bash
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

## 3. Install Docker

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu
```

Log out and SSH in again so the group change applies, then check with `docker run --rm hello-world`.

## 4. Build the image (on your PC)

In PowerShell, in the project folder, with Docker Desktop running:

```powershell
docker build -t oxtore-backend:latest .
docker save oxtore-backend:latest -o oxtore-backend.tar
```

The Maven stage compiles the jar (tests skipped); only the jar goes into the final image. `oxtore-backend.tar` is
git-ignored.

## 5. Copy the files to the server (on your PC)

```powershell
ssh ubuntu@57.129.77.16 "mkdir -p ~/oxtore"
scp oxtore-backend.tar docker-compose.prod.yml .env.example deploy/nginx/oxtore.conf ubuntu@57.129.77.16:~/oxtore/
```

## 6. Create the production `.env`

```bash
cd ~/oxtore
cp .env.example .env
chmod 600 .env
```

Generate the secrets (run each line, copy each output):

```bash
openssl rand -hex 64   # JWT_SECRET
openssl rand -hex 64   # JWT_REFRESH_SECRET (must differ from JWT_SECRET)
openssl rand -hex 32   # OTP_HMAC_SECRET
openssl rand -hex 24   # DB_PASSWORD
```

Edit with `nano .env` and set at least:

```ini
JWT_SECRET=<first value>
JWT_REFRESH_SECRET=<second value>
OTP_HMAC_SECRET=<third value>
DB_PASSWORD=<fourth value>
APP_PUBLIC_BASE_URL=https://api.oxtore.com
SPRING_PROFILES_ACTIVE=
OTP_DEV_LOG_CODES=false
```

Fill in the `MAIL_*` values (SMTP) for verification emails, and `GOOGLE_CLIENT_ID` for Google sign-in. Without
`MAIL_HOST` the app still works, but verification and password-reset codes can't be sent.

`DB_PASSWORD` is applied only the first time the database volume is created. To change it later, change it inside
Postgres too (`ALTER USER oxtore PASSWORD '...'`).

## 7. Load the image and start

```bash
cd ~/oxtore
docker load -i oxtore-backend.tar && rm oxtore-backend.tar
docker compose -f docker-compose.prod.yml up -d
docker compose -f docker-compose.prod.yml logs -f backend
```

Wait for `Started BackendApplication` (`Ctrl+C` stops following the logs, not the app). Then:

```bash
curl http://127.0.0.1:8081/actuator/health
```

It should answer `{"groups":["liveness","readiness"],"status":"UP"}`. The endpoint is public and shows no
details; `DOWN` (HTTP 503) means the database isn't reachable.

The containers restart by themselves after a crash or a reboot (`restart: unless-stopped`).

## 8. Configure Nginx

```bash
sudo cp ~/oxtore/oxtore.conf /etc/nginx/sites-available/oxtore
sudo sed -i 's/api.example.com/api.oxtore.com/' /etc/nginx/sites-available/oxtore
grep server_name /etc/nginx/sites-available/oxtore   # must print: server_name api.oxtore.com;
sudo ln -s /etc/nginx/sites-available/oxtore /etc/nginx/sites-enabled/oxtore
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
```

Test: `curl http://api.oxtore.com/actuator/health`.

## 9. Enable HTTPS (Let's Encrypt)

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d api.oxtore.com --redirect
sudo certbot renew --dry-run
```

Certbot asks for an email (expiry warnings), adds the HTTPS block to the Nginx site and redirects HTTP to HTTPS.
Renewal is automatic. Test: `curl https://api.oxtore.com/actuator/health`.

## 10. Point the app (frontend) at the server

- API URL: `https://api.oxtore.com`
- Socket.IO URL: `https://api.oxtore.com`, **without** `:8082` (Nginx forwards `/socket.io/`)
- Google sign-in: add the production web origin to *Authorized JavaScript origins* in Google Cloud Console

## Updating to a new version

On your PC:

```powershell
docker build -t oxtore-backend:latest .
docker save oxtore-backend:latest -o oxtore-backend.tar
scp oxtore-backend.tar ubuntu@57.129.77.16:~/oxtore/
```

(Also `scp` `docker-compose.prod.yml` if it changed.) On the server:

```bash
cd ~/oxtore
docker load -i oxtore-backend.tar && rm oxtore-backend.tar
docker compose -f docker-compose.prod.yml up -d
docker image prune -f
```

The database and uploaded images live in Docker volumes (`oxtore-backend_oxtore-db-data`,
`oxtore-backend_oxtore-uploads`), so they survive updates. Never run `docker compose down -v`: `-v` deletes them.

## Backups

Database dump:

```bash
docker exec oxtore-db pg_dump -U oxtore oxtore-db | gzip > ~/oxtore-db-$(date +%F).sql.gz
```

Restore:

```bash
gunzip -c ~/oxtore-db-YYYY-MM-DD.sql.gz | docker exec -i oxtore-db psql -U oxtore -d oxtore-db
```

Uploaded images:

```bash
docker run --rm -v oxtore-backend_oxtore-uploads:/data -v ~:/backup alpine tar czf /backup/uploads-$(date +%F).tar.gz -C /data .
```

Copy backups off the server, e.g. `scp` to your PC.

## Troubleshooting

| Symptom | Check |
|---|---|
| `set JWT_SECRET in .env` (or another variable) when starting | That variable is empty in `.env` |
| `pull access denied for oxtore-backend` | The image wasn't loaded: `docker load -i oxtore-backend.tar` |
| `502 Bad Gateway` from Nginx | The backend isn't running or is still starting: `docker compose -f docker-compose.prod.yml logs backend` |
| Backend logs `password authentication failed` | `DB_PASSWORD` changed after the database volume was created (see step 6) |
| `413 Request Entity Too Large` | Image larger than the 5 MB limit |
| Notifications don't arrive | The app connects to `https://api.oxtore.com` without `:8082`; `sudo tail -f /var/log/nginx/error.log` |
| Certbot fails | DNS doesn't point to `57.129.77.16` yet, or port 80 is closed (`sudo ufw status`) |
