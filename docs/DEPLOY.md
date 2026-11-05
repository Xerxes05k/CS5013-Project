# Deploying the live site

Step by step, for the setup chosen in [ADR 0007](decisions/0007-host-on-azure-for-students-vm.md) (host) and [ADR 0006](decisions/0006-host-on-oracle-always-free-vm.md) (everything else): an Azure for Students VM, the jar run by `systemd`, and Caddy in front for HTTPS. Commands after step 1 run on the VM over `ssh` unless marked **laptop**.

Placeholders: `jamuna-cycles.centralindia.cloudapp.azure.com` stands for the VM's DNS name (step 1). `azureuser` is the admin username you pick when creating the VM.

---

## 1. Create the VM (once, in a browser)

1. **Sign up at azure.microsoft.com/free/students** with your smail address. Microsoft checks you are a student through that address. No credit card is asked for. You get $100 of credit for 12 months.
2. **laptop:** make an SSH key if you don't have one (`ls ~/.ssh/id_ed25519.pub`):

   ```bash
   ssh-keygen -t ed25519
   ```

3. **Portal → Virtual machines → Create:**
   - Region: **Central India**
   - Image: **Ubuntu Server 24.04 LTS - x64 Gen2**
   - Size: **Standard_B1s** (it says "Free services eligible")
   - Authentication: **SSH public key**, username `azureuser`, and paste the contents of `~/.ssh/id_ed25519.pub`
   - Public inbound ports: **SSH (22), HTTP (80), HTTPS (443)**
   - Disks: keep the default OS disk
   - Management: check that **auto-shutdown is off**

4. **Give it a name:** VM → Overview → click the public IP → Configuration.
   - Set **DNS name label** to `jamuna-cycles`. The VM is then reachable at `jamuna-cycles.centralindia.cloudapp.azure.com`.
   - Check that the IP assignment is **Static**, so the name keeps pointing at the VM.

If Azure won't accept the student sign-up, see the fallback in ADR 0007: a laptop and a Cloudflare quick tunnel, for the demo only.

## 2. Install Java 25, Tesseract, Caddy (once)

```bash
ssh azureuser@jamuna-cycles.centralindia.cloudapp.azure.com

# B1s has 1 GiB of RAM; a 2 GB swap file keeps Java and Tesseract from running out.
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Java 25 from Adoptium (Ubuntu's own repos may not have 25 yet).
sudo apt update && sudo apt install -y wget gpg
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor | sudo tee /etc/apt/trusted.gpg.d/adoptium.gpg > /dev/null
echo "deb https://packages.adoptium.net/artifactory/deb $(. /etc/os-release; echo $VERSION_CODENAME) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt update

# Tesseract with English data; the -dev packages add the plain libtesseract.so and
# libleptonica.so names that Tess4j and Lept4j load. sqlite3 for adding cycles and backups.
sudo apt install -y temurin-25-jre tesseract-ocr tesseract-ocr-eng libtesseract-dev libleptonica-dev sqlite3 caddy
java -version          # expect 25
ls /usr/share/tesseract-ocr/5/tessdata/eng.traineddata

# The app's own user and folder.
sudo useradd --system --home /opt/cyclebooking --shell /usr/sbin/nologin cyclebooking
sudo mkdir -p /opt/cyclebooking && sudo chown cyclebooking: /opt/cyclebooking
```

## 3. Build and copy the jar (every deploy)

**laptop**, from the repo root on an up-to-date `main`:

```bash
cd app && ./mvnw test && ./mvnw package -DskipTests
scp target/cyclebooking-0.0.1-SNAPSHOT.jar ../deploy/cyclebooking.service ../deploy/Caddyfile azureuser@jamuna-cycles.centralindia.cloudapp.azure.com:/tmp/
```

On the VM:

```bash
sudo install -o cyclebooking -m 644 /tmp/cyclebooking-0.0.1-SNAPSHOT.jar /opt/cyclebooking/cyclebooking.jar
```

## 4. Start the service and HTTPS (first deploy)

First set the GS: in `cyclebooking.service`, replace `gs-smail@smail.iitm.ac.in` on the `Environment=CYCLEBOOKING_GS=` line with the GS's smail address (add the Warden's after a comma). Only those residents see the Report tab and the Repaired button. They must also be in the residents CSV (§5) to log in.

```bash
sudo cp /tmp/cyclebooking.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now cyclebooking
journalctl -u cyclebooking -f      # wait for "Started CyclebookingApplication", then Ctrl+C

# Put the VM's DNS name in the Caddyfile, then:
sudo cp /tmp/Caddyfile /etc/caddy/Caddyfile
sudo systemctl reload caddy
```

Open `https://jamuna-cycles.centralindia.cloudapp.azure.com` on a phone. You should see the login page with a padlock.

The first start creates the empty tables in `/var/lib/cyclebooking/cyclebooking.db`. There is **no** dev login on the server: `data.sql` is switched off in the `prod` profile.

## 5. Load cycles and residents (first deploy, then whenever the list changes)

**Cycles**: one line each, with the label painted on the cycle:

```bash
sudo -u cyclebooking sqlite3 /var/lib/cyclebooking/cyclebooking.db \
  "INSERT INTO cycles (label, state, version) VALUES ('JAM-01','AVAILABLE',0), ('JAM-02','AVAILABLE',0);"
```

**Residents**: a CSV from the hostel list, `roll_number,name,smail_address`, one per line, with no commas inside names:

```bash
# laptop
scp residents.csv azureuser@jamuna-cycles.centralindia.cloudapp.azure.com:/tmp/

# VM: stop the service so only one program writes the database, then run the importer.
sudo systemctl stop cyclebooking
sudo install -o cyclebooking -m 600 /tmp/residents.csv /var/lib/cyclebooking/residents.csv
sudo -u cyclebooking java -jar /opt/cyclebooking/cyclebooking.jar --spring.profiles.active=prod \
  --cyclebooking.import.residents=/var/lib/cyclebooking/residents.csv \
  --cyclebooking.import.passcodes-out=/var/lib/cyclebooking/passcodes.csv
# wait for "Imported N new residents", then Ctrl+C
sudo systemctl start cyclebooking
```

How the importer behaves:
- **Residents already present are skipped,** so their passcodes stay the same. Running it again with a longer list only adds the new names.
- **It never overwrites an existing `passcodes.csv`.** Move the old one away first.

**`passcodes.csv` holds every new passcode in plain text.** The importer creates it readable only by the `cyclebooking` user, and it is all or nothing: a bad line anywhere in the CSV stops the import before anyone is saved, with the line number in the error.
- Copy it to the GS:

  ```bash
  sudo cat /var/lib/cyclebooking/passcodes.csv > /tmp/p.csv
  ```

  then from your laptop:

  ```bash
  scp azureuser@jamuna-cycles.centralindia.cloudapp.azure.com:/tmp/p.csv .
  ```

- Then delete both copies on the VM:

  ```bash
  sudo rm /var/lib/cyclebooking/passcodes.csv /tmp/p.csv
  ```

- The GS hands each resident their own passcode.

## 6. Smoke test (every deploy)

From a phone on mobile data, not hostel Wi-Fi, so it really comes over the internet:

1. Log in with a real resident's smail address and passcode.
2. Book a cycle and scan your ID card.
   - **If the scan never reads,** check `journalctl -u cyclebooking | grep "ID card scan failed"`. A missing Tesseract library shows there.
   - The typed-roll fallback after 3 tries still works, so booking isn't blocked.
3. Confirm pickup, then return with one part ticked.
4. Restart the service, then check the booking is still in History:

   ```bash
   sudo systemctl restart cyclebooking
   ```

Write the result, and the live URL, in the README and PROGRESS.md.

## Later deploys

Step 3, then:

```bash
sudo systemctl restart cyclebooking
```

The database is untouched.

## Backups

One file holds everything. A dated copy every night at 03:00:

```bash
echo '0 3 * * * cyclebooking sqlite3 /var/lib/cyclebooking/cyclebooking.db ".backup /var/lib/cyclebooking/backup-$(date +\%F).db"' | sudo tee /etc/cron.d/cyclebooking-backup
```
