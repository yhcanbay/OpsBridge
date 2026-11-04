# OpsBridge

DevOps süreçleri için Spring Boot + React paneli. Bu ilk sürüm, [sekiz haftalık planın](<OpsBridge — Revize 8 Haftalık DevSecOps Proje Planı 1.md>) 1. haftasındaki uygulama iskeletini kurar.

## İlk sürümde neler var?

- Basit `ADMIN` girişi ve çıkışı; sunucuda oturum, BCrypt parola doğrulama ve CSRF koruması.
- PostgreSQL üzerinde proje profili oluşturma, listeleme, görüntüleme, güncelleme ve silme.
- İlk kurulumda eklenen `Personal_Task_App` profili.
- Proje adı yerine profil verileriyle çalışan Jenkins, Docker ve HTTP health adapter arayüzleri.
- React üzerinde giriş, proje arama/listeleme, profil düzenleme ve bağlantı durumları.

**Bu sürüm Jenkins/Docker/health verisi çekmez ve build başlatmaz.** Bağlantılar `NOT_CONNECTED`, yetenek tanımlanmamışsa `NOT_SUPPORTED`, profil pasifse `DISABLED` döner. “Aktif profil”, uygulamanın sağlıklı olduğu anlamına gelmez. `SECURITY_VIEW` tanımlanabilir; rapor ekranı henüz yoktur.

## Klasör yapısı

```text
OpsBridge/
├── backend/                 Spring Boot 4.0.9 / Java 21 / Maven
│   └── src/main/
│       ├── java/com/yhcanbay/opsbridge/
│       │   ├── config/      ADMIN oturumu ve CSRF yapılandırması
│       │   ├── controller/  HTTP endpoint'leri
│       │   ├── dto/         request/ ve response/ sözleşmeleri
│       │   ├── entity/      JPA modeli
│       │   ├── enums/       Capability ve IntegrationState
│       │   ├── exception/   Uygulama ve API hataları
│       │   ├── mapper/      Açık veri dönüşümleri
│       │   ├── repository/  Veritabanı erişimi
│       │   ├── service/     Kullanım senaryoları ve transaction
│       │   └── integration/ Adapter sözleşmeleri ve stub uygulamalar
│       └── resources/db/migration/  Flyway şema ve örnek profil
├── frontend/                React 19.3.1 / Vite 7.3.7 / Router / Axios
│   └── src/                 app, api, components, constants, context,
│                            hooks, layouts, pages, services
├── skills/                  Üç geliştirme becerisinin kaynakları
├── compose.yaml             OpsBridge DB + API + arayüz
├── .env.example             Yerel yapılandırma örneği
├── docs/                    Referans uygulamanın teknik envanteri
└── .wiki/                   Proje hafızası
```

React yalnızca OpsBridge `/api` adreslerini çağırır. Geliştirmede Vite, container ortamında Nginx bu istekleri Spring Boot'a aktarır. Veriler PostgreSQL'de tutulur. Böylece tarayıcıya Jenkins token'ı veya Docker erişim bilgisi verilmez.

Kod düzeni, DTO/mapper, React Router/Axios, session/JWT ve DB tercihlerinin gerekçeleri resmî kaynaklarıyla [geliştirme rehberinde](docs/DEVELOPMENT_GUIDE.md) açıklanır. Giriş `/login`, projeler `/projects`, tek proje `/projects/{id}` adresindedir. Giriş gerektiren bir adresten giriş ekranına yönlendirilirseniz, girişten sonra istediğiniz adrese dönülür.

## Hızlı kurulum — Docker Compose

Gereken: Docker Desktop/Engine çalışıyor olmalı ve Compose kullanılabilmeli.

Proje kökünde PowerShell:

```powershell
Copy-Item .env.example .env
notepad .env
```

`.env` içindeki `POSTGRES_PASSWORD` ve `OPSBRIDGE_ADMIN_PASSWORD` değerlerini kendiniz belirleyin. Örnek `replace-with-...` admin parolasıyla backend başlatılmaz. `.env` Git tarafından yok sayılır; gerçek bilgileri `.env.example` içine yazmayın.

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
npm ci
npm run dev
```

Kurulu Maven yoksa Windows'ta `.\mvnw.cmd spring-boot:run`, Linux/macOS'ta `./mvnw spring-boot:run` kullanılabilir; wrapper ilk kullanımda Maven indirir. Bu oturumda wrapper'ın Windows indirmesi bağlantı hatası verdiği için doğrulamalar kurulu Maven ile yapıldı. `.env` backend başlatılırken okunur; değişiklikten sonra backend'i yeniden başlatın.

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
| GET | `/api/projects/{id}/integrations` | Adapter başlangıç durumları |
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

11 backend testi giriş/çıkış, gerçek oturum ve CSRF token'ları, yetkisiz erişim, ADMIN kontrolü, genel profil CRUD, geçersiz alanlar, adapter durumları ve doğrudan DB integrity kurallarını kapsar. H2 test kapsamındadır; normal uygulama PostgreSQL kullanır. Paketler taşındıktan sonraki ilk kontrolde eski class dosyalarını kaldırmak için `mvn clean verify` kullanın.

Frontend biçim kontrolü, 7 davranış testi ve üretim derlemesi, `frontend/` altında:

```powershell
npm ci
npm run format:check
npm test
npm run build
```

Compose yapılandırma kontrolü, kökte:

```powershell
docker compose --env-file .env.example config --quiet
```

Gerçekte yapılan kontroller ve ortam sınırları [.wiki/STATUS.md](.wiki/STATUS.md) içinde tutulur. Docker build/deploy ve tarayıcı etkileşimi doğrulanmadıysa build/test başarısı bunların yerine geçmez.

## Personal_Task_App ve sonraki adım

İzlenen uygulama ayrı depodadır: `C:\Yazılım\Personal_Task_App`. Çalıştırma komutları, mevcut pipeline ve bağlantı varsayımları [teknik envanterde](docs/PERSONAL_TASK_APP.md) bulunur. Bu görevde o deponun uygulama dosyaları değiştirilmedi.

1. haftanın uygulama temeli hazırlandıktan sonra önerilen devam işi, Personal_Task_App'i Docker ile gerçekten çalıştırmak ve mevcut Jenkins job yolunu doğrulamaktır. Gerçek Jenkins/Docker/health adapter'ları sonraki kapsamdır. Ayrıntılı RBAC, JWT yenileme, tarama, security gate ve Kubernetes bu başlangıç uygulamasında bulunmaz; gelecekteki kapsam kararları ayrıca ele alınır.
