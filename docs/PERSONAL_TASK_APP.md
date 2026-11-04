# Personal_Task_App — 1. hafta teknik envanteri

İnceleme: **2026-11-04 — Europe/Istanbul**. Kaynak depo: `C:\Yazılım\Personal_Task_App`. Bu uygulama OpsBridge'den ayrı geliştirilir. Aşağıdaki yapı dosyalardan gözlendi; bu görevde temiz Docker ortamında çalıştırıldığı iddia edilmez.

## Kaynak ve bileşenler

| Alan | Mevcut kaynak / davranış |
|---|---|
| Spring Boot | `taskApp/pom.xml`: Spring Boot 4.0.5, Java 21, JPA, PostgreSQL, validation |
| React | `taskApp/frontend/Ders_Programi/package.json`: React 19, Vite 7, `dev`, `build`, `lint`, `preview` komutları |
| Uygulama Compose | Kök `docker-compose.yml`: PostgreSQL 16, backend, Nginx üzerinde frontend |
| Backend image | `taskApp/Dockerfile`: Maven + Temurin 21 build; Temurin 21 JRE runtime |
| Frontend image | `taskApp/frontend/Ders_Programi/Dockerfile`: Node 22 build; Nginx 1.27 runtime |
| Jenkins | `docker-compose.jenkins.yml`, `jenkins/Dockerfile`: ayrı Jenkins servisi; Docker socket bağlantısı |
| Ek DB Compose | `taskApp/compose.yaml`: yalnızca DB; kökteki tam uygulama Compose'u ile aynı şey değildir |

Kaynak dosyaları: [pom.xml](../../../Personal_Task_App/taskApp/pom.xml), [package.json](../../../Personal_Task_App/taskApp/frontend/Ders_Programi/package.json), [Compose](../../../Personal_Task_App/docker-compose.yml), [Jenkinsfile](../../../Personal_Task_App/Jenkinsfile).

## Mevcut portlar ve ortam bilgileri

| Bileşen | Host adresi |
|---|---|
| Frontend | `http://localhost:5173` |
| Backend | `http://localhost:8080` |
| PostgreSQL | `localhost:5432`, Compose veritabanı `taskapp` |
| Jenkins | `http://localhost:8081` |

Backend `SERVER_PORT`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` ve `APP_FRONTEND_URL` yapılandırmalarını okur. Compose bunların bir kısmını kendisi verir. Mevcut geliştirme parolaları kaynak dosyalarda bulunuyor; burada kopyalanmadı. Yerel Maven ile çalıştırmada varsayılan DB hedefi Compose hedefinden farklıdır; açık datasource ayarı gerekir. Tekrarlanabilir ilk deneme için kök Compose tercih edilir.

## Belgelenmiş çalıştırma komutları

Docker Engine çalışırken, PowerShell:

```powershell
cd C:\Yazılım\Personal_Task_App
docker compose config --quiet
docker compose up -d --build
docker compose ps
Invoke-RestMethod http://localhost:8080/api/settings
```

Arayüzü `http://localhost:5173` adresinden açın. `/api/settings` yanıtı işlevsel kontrol sağlar; mevcut backend'de Actuator bulunmadığı için `/actuator/health` bu uygulama için henüz geçerli değildir.

Jenkins'i ayrıca başlatmak gerekiyorsa mevcut tanımın komutu:

```powershell
docker compose -f docker-compose.jenkins.yml up -d --build
```

Bu komutlar bu görevde **çalıştırılmadı**. Mevcut Jenkins kurulumu, oturumu veya job'ları yeniden oluşturulmadı. Job adı/yolu, token, webhook/ngrok adresi ve gerçekten çalışan ağ düzeni henüz doğrulanmadı.

## Pipeline envanteri

`Checkout → Backend Test → Frontend Build → Docker Deploy → Verify`

- Checkout öncesi workspace temizlenir; aynı job'ın eşzamanlı build'i kapalıdır.
- Backend testi `taskApp` altında Maven wrapper ile çalışır.
- Frontend aşaması `npm ci --no-audit --no-fund` ve `npm run build` çalıştırır. Lint komutu pipeline'da çağrılmaz.
- Deployment kök Compose üzerinde `up -d --build --remove-orphans` kullanır.
- Verify, `http://localhost:8080/api/settings` için tekrar deneme yapar. Jenkins container içindeyse `localhost` hedefinin doğru olduğu ayrıca doğrulanmalıdır.
- Pipeline `COMPOSE_PROJECT_NAME=personal_task_app` ayarlar. OpsBridge'in ilk profilindeki `com.docker.compose.project=personal_task_app` seçicisi bu dosya bilgisine dayanır; farklı komutla başlatılmış mevcut container'larda aynı label olduğu henüz kontrol edilmedi.
- `opsbridge.*` label'ları, commit/build ile image etiketleme ve Actuator health mevcut dosyalarda bulunmaz; bunlar planın sonraki haftalarına aittir.

## OpsBridge başlangıç profili

Örnek profil [V2 migration](../backend/src/main/resources/db/migration/V2__seed_reference_project.sql) ile eklenir. Repository URL ve `/api/settings` adresi kaynak dosyalardan gelir. `local-jenkins` yalnızca bağlantı referansıdır; gerçek bağlantı yapılandırılmaz. Job yolu bilinmediği için `null` bırakılır. Build görüntüleme/tetikleme, container görüntüleme ve HTTP health yetenekleri başlangıç profiline yazılır; bu, canlı bağlantının doğrulandığı anlamına gelmez.

## Bu oturumdaki ortam bulguları

Java 25.0.2, Maven 3.9.11, Node 24.21.0, npm 11.19.0 ve Docker Compose 5.6.1 komutları bulundu. Java hedefi iki projede de 21'dir. Docker API pipe'ı bulunamadı; bu nedenle referans uygulamanın container build/deploy ve temiz kurulum kanıtı henüz yoktur. Native PostgreSQL 17 yalnızca OpsBridge doğrulaması için ayrı geçici instance olarak kullanılabilir; Personal_Task_App verilerine bağlanılmaz.
