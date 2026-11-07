# Personal_Task_App — 1. hafta teknik envanteri

Kaynak envanteri: **2026-11-04**; ortam güncellemesi: **2026-11-05 — Europe/Istanbul**. Kaynak depo: `C:\Yazılım\Personal_Task_App`. Bu uygulama OpsBridge'den ayrı geliştirilir. Aşağıdaki yapı dosyalardan gözlendi; temiz Docker kurulumu henüz doğrulanmadı.

## 5 Kasım — doğrulanan Jenkins ayrımı ve çalışma sınırı

Kullanıcı, kullandığı Jenkins'in **Windows servisi olarak `http://localhost:8081`** üzerinde çalıştığını doğruladı. Servis `Running`; job yolu **`taskapp-cicd`**, kaynak **`https://github.com/yhcanbay/Personal_Task_App`**, branch **`*/main`**, script yolu **`Jenkinsfile`**. Bu alanlar yerel job yapılandırmasından salt okunur alındı. Kimliksiz Jenkins API isteği **403** döndü; token veya kimlik doğrulanmış API erişimi henüz kurulmadı. Eski başarılı build kayıtları güncel pipeline/deployment kanıtı sayılmaz.

Ayrı bir **`jenkins-server`** Docker container'ı `8080` portunu kullanıyordu; onun da aynı adlı bir job'ı var. Kullanıcının yerel Jenkins seçimi sonrasında port çakışmasını gidermek için yalnızca bu container durduruldu; container ve `jenkins_home` volume'u silinmedi. `docker-compose.jenkins.yml` içindeki `taskapp-jenkins` tanımı mevcut yerel Windows servisiyle aynı kurulum değildir; yeni Jenkins başlatmak gerekmez.

**Kullanıcı bu sırada 8081 Jenkins üzerinden test yapıyor ve müdahale istemiyor.** Jenkins servisi/job/ayarları değiştirilmez, yeniden başlatılmaz ve yeni build tetiklenmez. Codex'in başlattığı referans Compose derlemesi bu açıklama üzerine iptal edildi; iptal sırasında henüz uygulama container'ları yeniden oluşturulmamıştı. Kullanıcının testi sürerken referans uygulamaya başka Compose/deploy komutu uygulanmaz. Aşağıdaki kurulum komutları bu koşul dikkate alınarak sonraki çalışmada kullanılmalıdır.

Mevcut referans container'larında proje label'ı **`com.docker.compose.project=personal_task_app`**, DB volume'u **`personal_task_app_postgres_data`** olarak görüldü. Bu volume korunur. `opsbridge.*` label'ları kaynaklarda hâlâ yok. Job yolu bilinse de OpsBridge profilinde otomatik güncelleme veya gerçek adapter bağlantısı bu çalışma kapsamında yapılmadı.

## OpsBridge ile erişim adresleri

| İsteği yapan | Hedef adres | 5 Kasım kanıtı |
|---|---|---|
| Host tarayıcı | `http://localhost:5174` → OpsBridge Nginx | Gerçek Chrome giriş/CRUD akışı geçti; otomasyonda loopback `127.0.0.1` kullanıldı |
| OpsBridge Nginx container'ı | `http://backend:8082` | Proxy üzerinden health ve oturum/API istekleri geçti |
| OpsBridge backend container'ı | `db:5432`, veritabanı `opsbridge` | Migration kontrolü, gerçek CRUD ve yeniden başlatma sonrası kalıcılık başarılı |
| Host / yerel Jenkins agent'ı | `http://localhost:8080/api/settings` → referans API | Kaynak yapılandırmasının beklediği adres; bu turda canlı doğrulanmadı |
| OpsBridge backend container'ı | `http://host.docker.internal:8081` → yerel Jenkins | Sonraki entegrasyon için adres adayı; kullanıcı testi nedeniyle denenmedi |
| OpsBridge backend container'ı | `http://host.docker.internal:8080/api/settings` → referans API | Sonraki entegrasyon için adres adayı; kullanıcı testi nedeniyle denenmedi |

Container içindeki `localhost`, o container'ın kendisidir. Bu nedenle OpsBridge backend'inin Windows'taki Jenkins'e `localhost:8081` ile erişmesi beklenmez. Docker Desktop host servisleri için `host.docker.internal` adını sağlar; yukarıdaki iki adres bu kurala göre çıkarılmıştır ve bu ortamda ayrıca sınanmalıdır. Bu adresler profile veya adapter yapılandırmasına henüz yazılmadı. [Docker Desktop: host servisine bağlanma](https://docs.docker.com/desktop/features/networking/networking-how-tos/).

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

4 Kasım envanterinde bu komutlar çalıştırılmadı. 5 Kasım'da mevcut `personal_task_app` proje adı korunarak `up -d --build --wait` başlatıldı; kullanıcının eşzamanlı Jenkins testi nedeniyle image indirme/derleme aşamasında iptal edildi. Başarılı kurulum/deployment sonucu yok. Jenkins başlatma komutu uygulanmadı; yerel Jenkins kurulumu, oturumu ve job'ları değiştirilmedi. Job yolu yukarıda doğrulandı; token, webhook/ngrok ve canlı deployment/ağ kontrolleri açık.

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

Örnek profil [V2 migration](../backend/src/main/resources/db/migration/V2__seed_reference_project.sql) ile eklenir. Repository URL ve `/api/settings` adresi kaynak dosyalardan gelir. `local-jenkins` yalnızca bağlantı referansıdır; gerçek bağlantı yapılandırılmaz. İlk kurulumda job yolu bilinmediği için `null` bırakıldı; 5 Kasım'da belirlenen `taskapp-cicd` değeri profile otomatik yazılmadı. Build görüntüleme/tetikleme, container görüntüleme ve HTTP health yetenekleri başlangıç profiline yazılır; bu, canlı bağlantının doğrulandığı anlamına gelmez.

## 4 Kasım ortam bulguları ve 5 Kasım güncellemesi

Java 25.0.2, Maven 3.9.11, Node 24.21.0, npm 11.19.0 ve Docker Compose 5.6.1 komutları bulundu. Java hedefi iki projede de 21'dir. 4 Kasım'da Docker API pipe'ı bulunamadı. 5 Kasım'da Docker Desktop başlatıldı ve Engine 29.9.2 erişimi doğrulandı. Referans kurulum denemesi yukarıdaki kullanıcı talimatı nedeniyle tamamlanmadı; temiz kurulum kanıtı hâlâ yok. Native PostgreSQL 17 yalnızca OpsBridge doğrulaması için ayrı geçici instance olarak kullanılabilir; Personal_Task_App verilerine bağlanılmaz.
