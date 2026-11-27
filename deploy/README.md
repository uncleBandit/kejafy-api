# Deploying Kejafy

Kejafy uses the same pipeline shape as HolidayHub:

| Workflow | When | Where |
|---|---|---|
| `tests` | every push / PR to `main` or `develop` | GitHub runner with Postgres 17 + Redis 7 |
| `deploy-mvp` | after `tests` passes on `main`, or by hand | the shared MVP server (systemd + nginx) |
| `deploy-production` | by hand only | Kejafy's own server (Docker stack in `compose.prod.yaml`) |

Every deploy confirms the release with `GET /actuator/health` (it must report `UP`).

---

## Shared MVP server (`deploy-mvp`)

The same server as HolidayHub. Kejafy runs as a jar under systemd, with nginx in front:

```
/var/www/kejafy/
  releases/<id>/app.jar     the last 5 releases
  current -> releases/<id>  switched in one step on each deploy
  shared/.env               settings, read by systemd (EnvironmentFile)
  shared/logs/app.log       application log, rotated daily
```

### How a deploy works
1. **tests** passes on `main`.
2. **deploy-mvp** builds the jar on GitHub with `./mvnw package`, so the server never builds anything.
3. It uploads the jar to `/var/www/kejafy/releases/<id>`, then [`mvp/release.sh`](mvp/release.sh):
   - switches `current` to the new release
   - restarts `kejafy.service`. Hibernate updates the schema on startup.
   - waits up to about 2 minutes for `/actuator/health` to report `UP`. If it never does, it prints the end of the log, **switches back** to the previous release, and the run fails.
   - keeps the last 5 releases
4. The workflow checks the public URL.

### Adding Kejafy to the server (one time)
The server must already be set up with HolidayHub's `deploy/mvp/server-setup.sh`.

1. **DNS:** add an **A record** `kejafy` → the server's static IP.
2. **On the server:**
   ```bash
   curl -fsSLO https://raw.githubusercontent.com/uncleBandit/kejafy-api/main/deploy/mvp/add-app.sh
   sudo DEPLOY_USER=ubuntu bash add-app.sh kejafy kejafy.unclecanvas.com 8081
   nano /var/www/kejafy/shared/.env
   ```
   - The script installs Java 21 if it's missing.
   - It also creates the database and user, a systemd service, nginx, HTTPS, log rotation, and a sudoers rule so the deploy user can restart the service.
   - The port (here `8081`) must be unused by any other app on the server.
   - In `.env`, fill in the Google, M-Pesa and Stripe keys. The database password, Redis database index, CORS origin and callback URL are already filled in.
3. **Google Cloud console:** add `https://kejafy.unclecanvas.com/login/oauth2/code/google` as an authorised redirect URI.
4. **GitHub repo:** Settings → Secrets and variables → Actions:

   | Kind | Name | Value |
   |---|---|---|
   | secret | `DEPLOY_SSH_KEY` | the same `mvp_deploy` private key HolidayHub uses |
   | variable | `DEPLOY_HOST` | the static IP |
   | variable | `DEPLOY_USER` | `ubuntu` |
   | variable | `MVP_APP` | `kejafy` |
   | variable | `APP_URL` | `https://kejafy.unclecanvas.com` |

   Until `DEPLOY_HOST` is set, `deploy-mvp` is skipped, not failed.

### Memory
Spring Boot needs far more RAM than an idle PHP-FPM pool. The service starts with `-Xmx384m` (`JAVA_OPTS` in `shared/.env`).
- On a 1 GB server shared with other MVPs, rely on the swap that `server-setup.sh` adds.
- On a 2 GB server, it's comfortable.

### Day to day
```bash
sudo systemctl status kejafy
tail -f /var/www/kejafy/shared/logs/app.log
sudo systemctl restart kejafy           # after editing shared/.env
```

---

## Own server (`deploy-production`)

For when Kejafy graduates off the shared server.

1. Create an Ubuntu 24.04 server with a static IP, and open ports 80 and 443.
2. Run `sudo bash deploy/server-setup.sh` on it. This installs Docker and creates `/opt/kejafy`.
3. Copy [`.env.production.example`](.env.production.example) to `/opt/kejafy/.env` and fill it in.
4. In GitHub, set secret `PROD_SSH_KEY` and variables `PROD_HOST`, `PROD_USER` and `PROD_URL`.
5. Actions → **deploy-production** → Run workflow.

The workflow builds the image, pushes it to `ghcr.io/unclebandit/kejafy-api`, and copies `compose.prod.yaml` and the Caddyfile to the server. The server then pulls the image and restarts the stack. Caddy handles HTTPS. A `backup` container writes a nightly `pg_dump` and keeps 14 days.
