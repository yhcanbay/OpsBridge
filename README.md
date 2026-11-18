# OpsBridge

DevOps süreçleri için Spring Boot + React paneli. Bu ilk sürüm, [sekiz haftalık planın](<OpsBridge — Revize 8 Haftalık DevSecOps Proje Planı 1.md>) 1. haftasındaki uygulama iskeletini kurar.

Mevcut ilerleme, vize kapsamı ve kalan işlerin sırası [güncel devam planında](docs/ROADMAP.md) tutulur. Vize haftası 23–29 Kasım; temel DevOps vizeden önce, güvenlik geliştirmeleri vizeden sonra hedeflenir. Kubernetes bu teslimin dışında.

## Çalışma modeli

OpsBridge, takip edilen uygulamanın çalıştığı **aynı sunucuda ayrı bir uygulama** olarak çalışacak. Kullanıcı kendi bilgisayarından OpsBridge'in URL'sini açıp admin hesabıyla giriş yaparak bu sunucudaki projeleri kontrol edecek. Yerel Git deposu ve Docker kaynakları sunucuya aittir; kullanıcının tarayıcıyı açtığı bilgisayara ait değildir.

URL/HTTPS üzerinden yayınlama en son ele alınacak ayrı iştir. Mevcut kurulum localhost erişimine açıktır; dış erişim henüz kurulmadı. Yerleşim, entegrasyon sınırları ve mevcut durum [mimari belgesinde](docs/ARCHITECTURE.md).

## İlk sürümde neler var?

- Basit `ADMIN` girişi ve çıkışı; sunucuda oturum, BCrypt parola doğrulama ve CSRF koruması.
- PostgreSQL üzerinde proje profili oluşturma, listeleme, görüntüleme, güncelleme ve silme.
- İlk kurulumda eklenen `Personal_Task_App` profili.
- Proje profilinin aktifliği ve yeteneklerine göre üretilen Jenkins, Docker ve HTTP health bağlantı durumları.
- React üzerinde giriş, proje arama/listeleme, profil düzenleme ve bağlantı durumları.
- GitHub branch listesi ve seçilen branch'in sayfalanmış commit geçmişi; SHA, mesaj, yazar ve tarih.
- Kayıtlı GitHub hesap/token bağlantıları; proje eklerken bağlantı seçimi, yeni token ekleme, yenileme ve kullanılmayan bağlantıyı silme.
- Jenkins bağlantı kontrolü ve ayrıntı ekranı; son/seçili build, aşamalar, geçmiş, commit/değişiklik ve yayımlanmış test raporu özeti.
- Docker bağlantı kontrolü ve Compose projesine ait container’ların çalışma, sağlık, image, kapanma ve port bilgileri.

**Jenkins ve Docker verisi salt okunur.** Jenkins kartı job erişimini, Docker kartı yerel Docker Engine bağlantısını kontrol eder. Bağlı kartlar ilgili ayrıntı ekranını açar. HTTP Health kartı, `HEALTH_VIEW` yetenekli aktif profillerde OpsBridge backend’in `/actuator/health` yanıtını aynı ekranda gösterir. `SECURITY_VIEW` profil yetenekleri arasında tanımlanabilir.

## Klasör yapısı

```text
OpsBridge/
├── backend/                 Spring Boot 4.0.9 / Java 21 / Maven
│   └── src/main/
│       ├── java/com/yhcanbay/opsbridge/
│       │   ├── config/      ADMIN oturumu ve CSRF yapılandırması
│       │   ├── controller/  I…Controller ve impl/ uygulamaları
│       │   ├── dto/         request/ ve response/ sözleşmeleri
│       │   ├── entity/      JPA modeli
│       │   ├── enums/       Capability ve IntegrationState
│       │   ├── exception/   Uygulama ve API hataları
│       │   ├── mapper/      Açık veri dönüşümleri
│       │   ├── repository/  Veritabanı erişimi
│       │   └── service/     I…Service ve impl/ uygulamaları
│       └── resources/db/migration/  Flyway şema ve örnek profil
├── frontend/                React 19.3.1 / Vite 7.3.7 / Router / Axios
│   └── src/                 app, api, components, constants, context,
│                            hooks, layouts, pages, services
├── skills/                  Üç geliştirme becerisinin kaynakları
├── .reference-code/         Sozluk_Uygulamasi yazım örnekleri (yerel, Git dışında)
├── compose.yaml             OpsBridge DB + API + arayüz
├── .env.example             Yerel yapılandırma örneği
├── docs/                    Referans uygulamanın teknik envanteri
└── .wiki/                   Proje hafızası
```

React yalnızca OpsBridge `/api` adreslerini çağırır. Geliştirmede Vite, container ortamında Nginx bu istekleri Spring Boot'a aktarır. Proje profilleri ve GitHub bağlantıları PostgreSQL'de tutulur; Jenkins build/aşama/test verisi her okumada Jenkins'ten alınır ve veritabanına kaydedilmez. Tarayıcıya Jenkins token'ı veya Docker erişim bilgisi verilmez.

Kod düzeni, DTO/mapper, React Router/Axios, session/JWT ve DB tercihlerinin gerekçeleri resmî kaynaklarıyla [geliştirme rehberinde](docs/DEVELOPMENT_GUIDE.md) açıklanır. Giriş `/login`, projeler `/projects`, tek proje `/projects/{id}` adresindedir. Giriş gerektiren bir adresten giriş ekranına yönlendirilirseniz, girişten sonra istediğiniz adrese dönülür.

Kod yazımı kullanıcının **Sozluk_Uygulamasi** projesini esas alır. Yerel referansın konumu ve controller/service, Lombok DTO, React biçim kuralları [kod yazım rehberinde](docs/CODING_STYLE.md) tutulur.

## GitHub geçmişini kullanma

Aktif bir projenin profilinde `https://github.com/kullanici/depo` biçiminde repository adresi tanımlayın. Proje detayındaki **GitHub geçmişi** bölümünde branch seçin; branch'ler ve commit'ler 20'şer kayıtla sayfalanır. İlk açılışta dönen ilk branch seçilir. **Yenile** branch ve commit sayfalarını başa alır.

**+ Proje ekle** formunda önce GitHub bağlantısını seçin. Kayıtlar `hesap adı — bağlantı adı` olarak gösterilir. **Yeni bağlantı ekle** ile bağlantıya bir ad ve GitHub tokenı girin; hesap adı backend tarafından GitHub'dan doğrulanır. Ardından **Devam et** ile proje bilgilerini doldurun. Açık depolar için **Tokensız devam et** seçilebilir. Özel depolarda ilgili depoya **Contents: read** izni veren bir fine-grained token kullanın.

Aynı hesaba ait farklı tokenlar ayrı bağlantılar olarak kaydedilebilir; bir bağlantıyı birkaç proje kullanabilir. Proje kaydedilirken seçili bağlantının depodaki branch'leri okuyabildiği kontrol edilir. Formdaki **Bağlantıyı düzenle**, token boş bırakılırsa yalnız adı değiştirir; yeni token aynı hesaba ait olmalıdır. Token yenilemesi bu bağlantıyı kullanan bütün projelere uygulanır. Projelerin kullandığı bağlantı silinemez; önce projelerin bağlantısını değiştirin. Bağlantı kaydı proje kaydından bağımsızdır: proje formunu iptal etmek, az önce eklenen bağlantıyı silmez.

Tokenlar veritabanında AES-GCM ile şifrelenir. Liste yanıtında veya proje yanıtında token bulunmaz. Tarayıcı tokenı yalnız ekleme/yenileme isteğinde backend'e gönderir; kalıcı tarayıcı depolamasına yazmaz. `GITHUB_TOKEN` ortam değişkeni artık kullanılmaz; mevcut tokenı panelden bir bağlantı olarak ekleyin. Mevcut projeler migration sonrasında bağlantısız kalır; özel depolar için profil düzenlemeden bağlantı seçin.

Bu bölüm GitHub verisini gösterir. Sunucudaki gönderilmemiş commit'ler ve çalışan Docker sürümünün commit'i henüz okunmaz. Çalışan sürümü belirlemek için izlenen uygulamanın build sırasında kaydettiği metadata ayrıca bağlanmalıdır. Kod akışının kısa açıklaması [Git görünürlüğü belgesinde](docs/GIT_VISIBILITY.md).

## Jenkins bağlantısını kullanma

Jenkins bağlantısı için proje detayındaki **Profili düzenle → Devam et** yolundan mevcut **Jenkins URL / referansı** ve **Jenkins job yolu** alanlarını doldurun. Yanlış URL/job aynı yerden düzeltilebilir; yeniden backend başlatmak gerekmez. URL alanı gerçek HTTP(S) adresi veya `local-jenkins` olabilir. `local-jenkins`, sunucudaki `JENKINS_BASE_URL` ayarını kullanır. Job yolu örneği: `taskapp-cicd` veya `apps/taskapp-cicd`.

Jenkins kimlik doğrulaması gerekiyorsa `.env` içindeki `JENKINS_USERNAME` ve `JENKINS_API_TOKEN` değerlerini birlikte tanımlayın. `JENKINS_BASE_URL` bu tokenın gönderilebileceği ana adrestir; ek adresler `JENKINS_ALLOWED_BASE_URLS` içinde virgülle ayrılır. Ortam ayarı değiştiğinde backend yeniden başlatılır. Yerel backend host Jenkins'e `http://localhost:8081`, Docker backend ise `http://host.docker.internal:8081` gibi container'dan erişilebilen adresle bağlanır.

Bağlantı doğrulandığında projelerdeki **Jenkins** kartına tıklayın. `/projects/{id}/jenkins` ekranı açılır; **Yenile** yeniden okur, geçmişteki build numarası o çalışmayı seçer. Kurulum, hata durumları, aşama/test raporu gereksinimleri ve kısa kod akışı [Jenkins görünürlüğü belgesinde](docs/JENKINS_VISIBILITY.md).

## Docker bağlantısını kullanma

1. Docker Desktop veya Docker Engine çalışırken OpsBridge backend’ini [yerel geliştirme](#yerel-geliştirme--spring-boot-ve-react-ayrı) adımlarındaki gibi aynı makinede başlatın. Yerel bağlantı otomatik bulunamazsa `.env` içinde `OPSBRIDGE_DOCKER_HOST` ayarlayıp backend’i yeniden başlatın. Linux için örnek `unix:///var/run/docker.sock`, Windows Docker Desktop için örnek `npipe:////./pipe/dockerDesktopLinuxEngine`. Uygulamanın Docker erişimi yerel socket veya named pipe ile sınırlıdır.
2. Proje profilini etkinleştirin, **Container görüntüleme** yeteneğini seçin ve **Container label seçicisi** alanını ayarlayın. Hazır Personal_Task_App profilinde `com.docker.compose.project=personal_task_app` değeri bulunur.
3. Proje ayrıntısındaki **Docker ayrıntıları** bağlantısını açın. Tablodaki servis adına tıklayarak ayrıntılarını görün. **Yenile**, son durumu Docker’dan tekrar okur.

Admin oturumuyla açılan `GET /api/projects/{id}/docker/containers` adresinde servis, profilin Compose etiketiyle çalışan ve durmuş container’ları listeler. Ardından her container’ı inceleyip ekrana gereken alanları döndürür. Mevcut Compose kurulumunda backend container’ına host Docker socket'i bağlanmaz. Docker izleme için bu aşamada yerel backend çalıştırma yolu kullanılır.

## Hızlı kurulum — Docker Compose

Gereken: Docker Desktop/Engine çalışıyor olmalı ve Compose kullanılabilmeli.

Proje kökünde PowerShell:

```powershell
if (-not (Test-Path .env)) {
    Copy-Item .env.example .env
}
notepad .env
```

`.env` içindeki `POSTGRES_PASSWORD` ve `OPSBRIDGE_ADMIN_PASSWORD` değerlerini kendiniz belirleyin. Örnek `replace-with-...` admin parolasıyla backend başlatılmaz. `.env` Git tarafından yok sayılır; gerçek bilgileri `.env.example` içine yazmayın.

`OPSBRIDGE_TOKEN_ENCRYPTION_KEY` için aşağıdaki komutla 32 baytlık rastgele anahtar üretin ve çıktıyı `.env` içindeki bu alana yazın. Örnek anahtarla veya boş anahtarla backend başlatılmaz.

```powershell
$githubKeyBytes = New-Object byte[] 32
$githubKeyGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$githubKeyGenerator.GetBytes($githubKeyBytes)
[Convert]::ToBase64String($githubKeyBytes)
$githubKeyGenerator.Dispose()
```

Bu anahtarı yeniden başlatmalarda koruyun ve veritabanı yedeğinden ayrı, güvenli biçimde yedekleyin. Anahtar kaybolursa kayıtlı tokenlar çözülemez; bağlantıların tokenlarını yeniden girmeniz gerekir. Anahtar değişikliği otomatik token taşıma işlemi yapmaz. Docker'da yeni kod/yapılandırma için servislerin yeniden oluşturulması gerekir; yerelde backend yeniden başlatılır.

```powershell
docker compose up -d --build
docker compose ps
```

Arayüz: **http://localhost:5174**. Kullanıcı adı `.env` içindeki `OPSBRIDGE_ADMIN_USERNAME` (varsayılan `admin`), parola sizin belirlediğiniz değerdir. İlk açılışta PostgreSQL şeması ve örnek proje Flyway ile oluşturulur.

```powershell
docker compose logs -f backend
docker compose down
```

Normal `down` veritabanı volume'unu korur. Profili panelden silmek, Personal_Task_App'i veya container'larını silmez. Silinen örnek profil uygulama yeniden açıldığında kendiliğinden eklenmez; örnek veri yalnızca ilk migration sırasında yazılır.

## Yerel geliştirme — Spring Boot ve React ayrı

Gereken: JDK 21 veya uyumlu daha yeni JDK, Maven 3.9, Node.js **20.19+ veya 22.12+**, PostgreSQL (Compose'da 16). Maven wrapper da depoya dahildir. [Vite Node gereksinimleri](https://vite.dev/guide/).

Önce yukarıdaki `.env` hazırlığını yapın. DB'yi başlatın:

```powershell
docker compose up -d db
```

Birinci terminal, proje kökünden:

```powershell
cd backend
mvn spring-boot:run
```

İkinci terminal, proje kökünden:

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

Kurulu Maven yoksa Windows'ta `.\mvnw.cmd spring-boot:run`, Linux/macOS'ta `./mvnw spring-boot:run` kullanılabilir; wrapper ilk kullanımda Maven indirir. Bu oturumda wrapper'ın Windows indirmesi bağlantı hatası verdiği için doğrulamalar kurulu Maven ile yapıldı. `.env` backend başlatılırken okunur; değişiklikten sonra backend'i yeniden başlatın.

PowerShell örnekleri `npm.cmd` kullanır; böylece `npm.ps1` için script yürütme izni gerekmez. Bash veya CMD'de normal `npm` komutu kullanılabilir.

Docker kullanmadan mevcut PostgreSQL'e bağlanmak için `.env` içine uygun `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/opsbridge` ekleyin ve `opsbridge` veritabanını önceden oluşturun. Kullanıcı/parolayı `POSTGRES_USER` ve `POSTGRES_PASSWORD` ile ayarlayın. Aynı portları kullanan Compose uygulaması ile yerel uygulamayı birlikte başlatmayın.

| Bileşen | OpsBridge varsayılan adresi |
|---|---|
| React | `http://localhost:5174` |
| Spring Boot API | `http://localhost:8082/api` |
| OpsBridge health | `http://localhost:8082/actuator/health` |
| PostgreSQL | `localhost:5433`, veritabanı `opsbridge` |

Bu portlar referans uygulamanın 5173/8080/5432 portlarından ve Jenkins'in 8081 portundan ayrıdır. Yerel backend yalnızca loopback üzerinde dinler; Compose'da servis içi erişim açılır, host portları yine `127.0.0.1` ile sınırlıdır.

## API ve oturum akışı

| Yöntem | Adres | İşlev |
|---|---|---|
| GET | `/api/auth/csrf` | CSRF token'ı ve header adı; girişsiz erişilebilir |
| POST | `/api/auth/login` | Form verisi: `username`, `password`; başarılıysa 204 |
| GET | `/api/auth/me` | Oturumdaki kullanıcı |
| POST | `/api/auth/logout` | Oturumu sonlandır; başarılıysa 204 |
| GET / POST | `/api/projects` | Listele / oluştur |
| GET / PUT / DELETE | `/api/projects/{id}` | Görüntüle / güncelle / sil |
| GET | `/api/projects/{id}/integrations` | Projenin bağlantı durumları |
| GET | `/api/projects/{id}/git?branch=main&page=1&branchPage=1` | GitHub branch ve commit geçmişi; branch verilmezse dönen ilk branch |
| GET | `/api/projects/{id}/jenkins` | Jenkins çalışma ve build ayrıntıları |
| GET | `/api/projects/{id}/docker/containers` | Compose projesine ait container’ların listesi ve ayrıntıları |
| GET | `/actuator/health` | OpsBridge'in kendi sağlığı; yalnızca özet |

CSRF endpoint'inden alınan token, yanıttaki `headerName` ile POST/PUT/DELETE isteklerine eklenir. Aynı oturum cookie'si korunur. Girişten sonra ve çıkıştan sonraki yeni girişte token yeniden alınır. React bu akışı yönetir; parola veya oturum token'ı `localStorage` içine yazılmaz. [Spring Security CSRF ve JavaScript](https://docs.spring.io/spring-security/reference/7.0/servlet/exploits/csrf.html).

Yeni profil örneği:

```json
{
  "name": "Ikinci_Proje",
  "repositoryUrl": "https://github.com/example/project",
  "environment": "local",
  "jenkinsBaseUrlRef": "local-jenkins",
  "jenkinsJobPath": null,
  "containerLabelSelector": null,
  "healthUrl": null,
  "capabilities": ["BUILD_VIEW"],
  "enabled": true
}
```

`environment` küçük harfle başlar; küçük harf, rakam, `_` ve `-` içerebilir (en fazla 30 karakter). URL alanları kullanıcı adı/parola içermeyen HTTP(S) adresleri olmalıdır. Desteklenen yetenekler: `BUILD_VIEW`, `BUILD_TRIGGER`, `CONTAINER_VIEW`, `HEALTH_VIEW`, `SECURITY_VIEW`. Yeteneklerin boş olması geçerlidir. Jenkins bağlantı referansı bir yapılandırma adıdır; secret alanı değildir. Yeni proje eklemek backend'e proje adına özel kod eklemeyi gerektirmez.

## Doğrulama

Backend testleri Docker/PostgreSQL gerektirmeden, yalnızca test kapsamındaki H2 üzerinde aynı Flyway migration'larını çalıştırır:

```powershell
cd backend
mvn verify
```

Backend testleri giriş/çıkış, gerçek oturum ve CSRF token'ları, yetkisiz erişim, ADMIN kontrolü, genel profil CRUD, geçersiz alanlar, bağlantı durumları, DB integrity ve GitHub okuma akışını kapsar. GitHub HTTP yanıtları testlerde taklit edilir; testler gerçek GitHub'a bağlanmaz. Mockito'nun test ayarı `mock-maker-subclass` olduğundan JVM'e agent bağlanması gerekmez. H2 test kapsamındadır; normal uygulama PostgreSQL kullanır. Paketler taşındıktan sonraki ilk kontrolde eski class dosyalarını kaldırmak için `mvn clean verify` kullanın.

Frontend biçim kontrolü, davranış testleri ve üretim derlemesi, `frontend/` altında:

```powershell
npm.cmd ci
npm.cmd run format:check
npm.cmd test
npm.cmd run build
```

Compose yapılandırma kontrolü, kökte:

```powershell
docker compose --env-file .env.example config --quiet
```

Gerçekte yapılan kontroller ve ortam sınırları [.wiki/STATUS.md](.wiki/STATUS.md) içinde tutulur. Docker build/deploy ve tarayıcı etkileşimi doğrulanmadıysa build/test başarısı bunların yerine geçmez.

## Personal_Task_App ve sonraki adım

İzlenen uygulama ayrı depodadır: `C:\Yazılım\Personal_Task_App`. Çalıştırma komutları, mevcut pipeline ve bağlantı varsayımları [teknik envanterde](docs/PERSONAL_TASK_APP.md) bulunur. Bu görevde o deponun uygulama dosyaları değiştirilmedi.

Kullanılan Jenkins, `http://localhost:8081` üzerindeki mevcut Windows servisidir; job yolu `taskapp-cicd`, branch `main`. Kullanıcı bu Jenkins'te test yaparken servis/job/ayar değiştirilmez ve referans uygulamaya eşzamanlı Compose/deploy uygulanmaz. Ayrı Docker Jenkins tanımı mevcut servisin yerine başlatılmaz. Güncel ortam ayrımı ve doğrulama sınırları teknik envanterde kayıtlıdır.

Personal_Task_App'in canlı Docker görünümü ve Jenkins job erişimi [proje durum kaydından](.wiki/STATUS.md) takip edilir.
