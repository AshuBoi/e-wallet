# Deploy for free on Oracle Cloud Always Free

This stack (MySQL + Kafka + 4 Spring services + Next.js) fits best on a small VM. Oracle Cloud's Always Free Ampere A1 shape is enough for a portfolio demo.

## 1. Create the VM

1. Sign up at [Oracle Cloud](https://www.oracle.com/cloud/free/).
2. Create a Compute instance:
   - Image: Ubuntu 22.04
   - Shape: VM.Standard.A1.Flex (Ampere) — e.g. 2 OCPU / 12 GB if capacity allows
   - Assign a public IP
3. In the VCN security list / NSG, allow ingress:
   - TCP 22 (SSH)
   - TCP 9080 (app gateway; or whatever `GATEWAY_PORT` you set)
   - TCP 443 (HTTPS, optional)

## 2. Install Docker

SSH into the VM, then:

```bash
sudo apt update
sudo apt install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo $VERSION_CODENAME) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo usermod -aG docker $USER
# re-login for group change
```

## 3. Run the stack

```bash
git clone <your-fork-url> e-wallet
cd e-wallet
cp .env.example .env
```

Edit `.env`:

```env
MYSQL_ROOT_PASSWORD=<strong-password>
JWT_SECRET=<at-least-32-random-chars>
PAYMENT_PUBLIC_BASE_URL=http://<YOUR_PUBLIC_IP>:9080
APP_CORS_ALLOWED_ORIGINS=http://<YOUR_PUBLIC_IP>:9080
GATEWAY_PORT=9080
SEED_DEMO=true
```

Start:

```bash
docker compose up -d --build
docker compose ps
```

Open `http://<YOUR_PUBLIC_IP>:9080` — Nginx serves the Next.js UI and proxies APIs.

## 4. Optional HTTPS (free)

Point a free domain (DuckDNS / Freenom / Cloudflare) to the VM and put Caddy or Certbot in front of Nginx. Let's Encrypt certificates are free.

## 5. Ops tips

- Logs: `docker compose logs -f wallet-service`
- Restart: `docker compose restart`
- Wipe data: `docker compose down -v` (destructive)
- Mailhog: `http://<IP>:8025` (lock this down or remove the port publish in production demos)

## If Oracle capacity is unavailable

1. Keep the project as a **local Docker Compose demo** (still excellent for interviews).
2. Or try another free/cheap VM (Google Cloud free tier e2-micro is usually too small for Kafka + 4 JVMs; prefer Oracle Ampere or a $5 VPS when needed).

## Interview line

> “I containerized an event-driven wallet with Kafka and a provider-agnostic payment port, then deployed the same Compose file on an Always Free ARM VM so reviewers can hit a live demo.”
