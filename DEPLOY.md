# Deploying to the VPS (Docker + Nginx + HTTPS)

Target: OVH VPS, Ubuntu 24.04, `57.129.77.16`, user `ubuntu`. Everything below runs **on the server** over SSH
unless it says "on your PC".

What runs where:

```
Internet ──443/80──> Nginx (on the server)
                       ├─ /socket.io/  ──> 127.0.0.1:8082  (Socket.IO, container oxtore_backend)
                       └─ /            ──> 127.0.0.1:8081  (REST API, container oxtore_backend)
                                                └─> oxtore-db (Postgres, Docker network only, no public port)
```

Files used: `Dockerfile` (multi-stage: Maven builds the jar inside Docker), `docker-compose.prod.yml`,
`deploy/nginx/oxtore.conf`.

In the commands, replace `api.example.com` with your real domain.

---

## 1. Point a domain at the server

HTTPS certificates need a domain name (Let's Encrypt doesn't issue them for a bare IP).

At your domain registrar, add a DNS record:

| Type | Name | Value |
|---|---|---|
| A | `api` (or `@` for the bare domain) | `57.129.77.16` |

Check it from the server (it can take a few minutes to an hour):

```bash
dig +short api.example.com
```

It must print `57.129.77.16` before step 8.

## 2. Update the system, firewall, swap

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y git nginx

sudo ufw allow OpenSSH
sudo ufw allow 'Nginx Full'
sudo ufw enable
```

Add 2 GB of swap so the JVM and the Maven build don't run out of memory on a small VPS:

```bash
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h
```

## 3. Install Docker

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu
```

Log out (`exit`) and SSH in again so the group change applies, then check:

```bash
docker run --rm hello-world
docker compose version
```

## 4. Get the code on the server

If the GitHub repository is private, give the server a read-only deploy key:

```bash
ssh-keygen -t ed25519 -C "oxtore-vps" -f ~/.ssh/id_ed25519 -N ""
cat ~/.ssh/id_ed25519.pub
```

Copy the printed line into GitHub: repository **Settings → Deploy keys → Add deploy key** (leave "Allow write
access" unchecked). Then:

```bash
cd ~
git clone git@github.com:ghassen1995/oxtore-backend.git
cd oxtore-backend
```

If the deployment files are not merged into `master` yet, switch to their branch:

```bash
git checkout deploy/vps-docker-nginx
```

## 5. Create the production `.env`

```bash
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

Edit the file with `nano .env` and set at least:

```ini
JWT_SECRET=<first value>
JWT_REFRESH_SECRET=<second value>
OTP_HMAC_SECRET=<third value>
DB_PASSWORD=<fourth value>
APP_PUBLIC_BASE_URL=https://api.example.com
SPRING_PROFILES_ACTIVE=
OTP_DEV_LOG_CODES=false
```

Also fill in the `MAIL_*` values (SMTP) so verification emails work, and `GOOGLE_CLIENT_ID` if you use Google
sign-in. Save with `Ctrl+O`, `Enter`, exit with `Ctrl+X`.

Use fresh secrets here, never the ones from your PC.

`DB_PASSWORD` is applied only the first time the database volume is created. To change it later, change it inside
Postgres too (`ALTER USER oxtore PASSWORD '...'`).

## 6. Build the Docker image

The `Dockerfile` has two stages: a Maven image compiles the jar (tests skipped), then only the jar is copied into a
small Java 21 runtime image. So the server needs no Java or Maven installed.

```bash
cd ~/oxtore-backend
docker compose -f docker-compose.prod.yml build
```

The first build downloads Maven dependencies and takes a few minutes; later builds reuse them unless `pom.xml`
changes. Check the image:

```bash
docker images oxtore-backend
```

<details>
<summary>Alternative: build on your PC and copy the image (if the server is too small to build)</summary>

On your PC, in the project folder:

```bash
docker build -t oxtore-backend:latest .
docker save oxtore-backend:latest -o oxtore-backend.tar
scp oxtore-backend.tar ubuntu@57.129.77.16:~
```

On the server:

```bash
docker load -i ~/oxtore-backend.tar
```

Then in step 7 use `up -d` without `--build`.
</details>

## 7. Start the containers

```bash
docker compose -f docker-compose.prod.yml up -d
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f backend
```

Wait for `Started BackendApplication` in the logs (`Ctrl+C` stops following the logs, not the app). Then:

```bash
curl http://127.0.0.1:8081/actuator/health
```

It should answer `{"status":"UP"}` (the endpoint is public and shows no details).

The containers restart by themselves after a crash or a server reboot (`restart: unless-stopped`).

## 8. Configure Nginx

```bash
sudo cp ~/oxtore-backend/deploy/nginx/oxtore.conf /etc/nginx/sites-available/oxtore
sudo sed -i 's/api.example.com/YOUR_DOMAIN/' /etc/nginx/sites-available/oxtore
sudo ln -s /etc/nginx/sites-available/oxtore /etc/nginx/sites-enabled/oxtore
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
```

(Replace `YOUR_DOMAIN` with e.g. `api.example.com`.)

Test from your PC's browser: `http://api.example.com/actuator/health`.

## 9. Enable HTTPS (Let's Encrypt)

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d api.example.com --redirect -m you@example.com --agree-tos --no-eff-email
```

Certbot gets the certificate, adds the `listen 443 ssl` block to the Nginx site and redirects HTTP to HTTPS.
Renewal runs automatically (systemd timer); check it works:

```bash
sudo certbot renew --dry-run
```

Test: `https://api.example.com/actuator/health`.

## 10. Point the app (frontend) at the server

In the app's `environment.prod.ts` (or equivalent):

- `apiBaseUrl`: `https://api.example.com`
- Socket.IO URL: `https://api.example.com`, **without** `:8082`. Nginx forwards `/socket.io/` to the Socket.IO
  server, and the client uses that path by default.
- Google sign-in: add the production web origin to *Authorized JavaScript origins* in Google Cloud Console.

## Updating after new commits

```bash
cd ~/oxtore-backend
git pull
docker compose -f docker-compose.prod.yml up -d --build
docker image prune -f
```

The database and uploaded images live in Docker volumes (`oxtore-db-data`, `oxtore-uploads`), so they survive
rebuilds. Never run `docker compose down -v`: `-v` deletes those volumes.

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

(Check the exact volume name with `docker volume ls`.) Copy backups off the server, e.g. `scp` to your PC.

## Troubleshooting

| Symptom | Check |
|---|---|
| `set JWT_SECRET in .env` (or another variable) when starting | That variable is empty in `.env` |
| `502 Bad Gateway` from Nginx | The backend isn't running or is still starting: `docker compose -f docker-compose.prod.yml logs backend` |
| Backend logs `password authentication failed` | `DB_PASSWORD` changed after the database volume was created (see step 5) |
| `413 Request Entity Too Large` | Image larger than the 5 MB limit |
| Notifications don't arrive | The app connects to `https://api.example.com` without `:8082`; Nginx logs: `sudo tail -f /var/log/nginx/error.log` |
| Build killed / out of memory | Add swap (step 2) or build on your PC (step 6 alternative) |
| Certbot fails | DNS doesn't point to `57.129.77.16` yet, or port 80 is closed (`sudo ufw status`) |
