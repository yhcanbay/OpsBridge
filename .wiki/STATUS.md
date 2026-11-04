# Güncel durum ve devam noktası

Son güncelleme: **2026-11-04 — Europe/Istanbul**

## Kullanıcının bu görevde istediği

Sekiz haftalık planın **1. haftasından yola çıkarak basit Spring Boot–React yapısını oluşturmak**. Ardından kullanıcı, açık backend katmanları ve az satır yerine anlaşılır kod istedi; backend/frontend/veritabanı için üç beceri oluşturulmasını ve teknoloji seçimlerinin araştırılmasını talep etti. İzlenen uygulama `C:\Yazılım\Personal_Task_App`; ana geliştirme deposu `C:\Yazılım\DevOpsSystemMonitoring\OpsBridge`. Bütün sekiz haftalık kapsamın veya Kubernetes'i dışlama önerisinin onaylandığı varsayılmadı.

## Uygulamada artık ne var?

- [backend](../backend/pom.xml): Java 21 hedefi, Spring Boot 4.0.9, Maven, JPA, validation, Spring Security, Actuator ve Flyway.
- [frontend](../frontend/package.json): React 19.3.1, Vite 7.3.7, React Router DOM 7.18.5, Axios 1.21.0; Türkçe giriş, URL tabanlı proje listesi/detayı, profil ekleme–düzenleme–silme ve bağlantı kartları.
- [ADMIN oturumu](../backend/src/main/java/com/yhcanbay/opsbridge/config/SecurityConfig.java): environment üzerinden hesap, BCrypt doğrulama, session cookie, açık CSRF koruması. JWT araştırıldı; mevcut aynı-origin yönetim paneli için session akışı korundu, gerekçesi rehberde.
- [Proje profili API'si](../backend/src/main/java/com/yhcanbay/opsbridge/controller/ProjectController.java): genel CRUD, UUID, alan doğrulama ve capability kümesi. Controller/service/repository/entity, request/response DTO, mapper, enum, config ve exception paketleri ayrı. Entity DTO bilmez; controller entity döndürmez. Dönüşümler açık mapper sınıflarında.
- [Flyway migration'ları](../backend/src/main/resources/db/migration/): PostgreSQL şeması ve bir Personal_Task_App profili. V1/V2 değiştirilmedi; yeni V3 boş ad/ortam ve tanımsız capability için CHECK kuralları ekler. Silinen örnek profil her başlangıçta tekrar eklenmez.
- [Adapter arayüzleri](../backend/src/main/java/com/yhcanbay/opsbridge/integration/): Jenkins, runtime/Docker ve HTTP health için ayrı sözleşmeler. Şimdilik dış istek yapmayan başlangıç uygulamaları `NOT_CONNECTED`, `NOT_SUPPORTED`, `DISABLED` döndürür; son başarılı veri zamanı yoktur.
- [Compose](../compose.yaml), iki Dockerfile, Nginx/Vite API proxy, [.env.example](../.env.example), [kurulum README'si](../README.md) ve [referans uygulama envanteri](../docs/PERSONAL_TASK_APP.md).
- [Geliştirme rehberi](../docs/DEVELOPMENT_GUIDE.md): Spring/DTO/mapper, session/JWT, React Router/Axios, PostgreSQL/JPA/Flyway tercihleri ve resmî kaynaklar. Frontend app/api/components/constants/context/hooks/layouts/pages/services olarak ayrıldı; Prettier ve `.editorconfig` eklendi. Java ana kaynakları ve testler google-java-format 2.0.0 AOSP biçiminde.
- [Üç beceri](../skills/): `backend-developer`, `frontend-developer`, `database-developer`. Doğrulayıcıdan geçti ve `C:\Users\yhcan\.codex\skills` altına kuruldu. Depodaki kopyalar inceleme için tutulur; iki konum otomatik senkron değil. Kök AGENTS.md kullanıcı kod tercihlerini ve bu kaynakları belirtir.

Varsayılan OpsBridge portları: arayüz **5174**, backend **8082**, DB **5433**. Referans uygulamanın 5173/8080/5432 ve Jenkins'in 8081 portlarıyla çakışmayı önlemek için bu değerler seçildi. Bunlar Codex'in başlangıç uygulama seçimleridir.

## Çalıştırılarak doğrulananlar

- Kurulu Maven ile `mvn clean verify`: **11 test, 0 hata, 0 başarısızlık**, çalıştırılabilir jar üretimi başarılı. Testlerde H2'nin PostgreSQL modu ve üç Flyway migration'ı kullanıldı. Paket taşıma sonrası eski class dosyaları temizlendi.
- Aynı **11 test**, bu depoya ait geçici PostgreSQL **17.5** üzerinde datasource override edilerek `mvn verify` ile tekrar geçti. Önceden V1/V2 uygulanmış DB'ye V3 ileri migration'ı uygulandı. Mevcut kullanıcı/veritabanlarına bağlanılmadı.
- `npm test`: **7 test geçti**. Axios CSRF edinme/yenileme, 401, alan hataları, mutasyonun otomatik tekrarlanmaması; login sonrası istenen URL'ye dönüş, ağ hatasında tekrar deneme ve 404 route'u kapsanıyor. React testi jsdom ortamında; gerçek tarayıcı görsel testi değil.
- `npm run format:check`, Java formatter `--aosp --dry-run --set-exit-if-changed` ve `npm run build`: başarılı.
- `docker compose --env-file .env.example config --quiet`: yapılandırma geçerli. Bu kontrol Docker build/deploy değildir.
- Son paketlenmiş Spring Boot jar + derlenmiş React'i sunan Vite preview + gerçek PostgreSQL ile HTTP üzerinden giriş, CSRF, profil CRUD, capability durumları ve çıkış yeniden kontrol edildi. Doğrudan `/projects/{id}` URL'si SPA HTML'ini sundu. HTTP istekleri frontend proxy üzerinden geçti. JVM'nin gerçekten durduğu kontrol edilerek yeniden başlatma sonrası profilin DB'de kalması ayrıca sınandı.

Kontroller geçici QA veritabanı ve rastgele test hesabıyla yapıldı; gerçek `.env` veya kullanıcı parolası oluşturulmadı. Test logları/çıktıları `.cache/` ve `target/` altında, Git dışında tutulur. H2 yalnızca test bağımlılığıdır; normal uygulama PostgreSQL kullanır. Doğrulama sonunda geçici backend, frontend preview ve QA PostgreSQL durduruldu; 8082/5174/55433 portlarının kapandığı kontrol edildi.

## Ortam ve doğrulama sınırları

- Yerelde gözlenen araçlar: Java 25.0.2, Maven 3.9.11, Node 24.21.0, npm 11.19.0, Compose 5.6.1. Kaynak Java hedefi 21'dir; Docker runtime'ı Temurin 21 olarak tanımlıdır. Java 21 container'ı bu görevde çalıştırılmadı.
- Docker Engine API pipe'ı erişilebilir değildi. OpsBridge veya Personal_Task_App Docker image build/deploy yapılmadı; referans uygulamanın temiz ortam teslim kanıtı henüz tamamlanmadı.
- Windows Maven wrapper'ın ilk Maven indirmesi ağ/bağlantı hatası verdi. Wrapper dosyaları mevcut; başarılı testler kurulu `mvn` ile yapıldı. README bu ayrımı belirtir.
- Yerleşik Node REPL/tarayıcı otomasyon aracı oturumda sunulmadı. jsdom ile yönlendirme/giriş formu davranışı, frontend build ve HTTP sunumu doğrulandı; gerçek tarayıcıdaki görsel düzen, uçtan uca form etkileşimi ve mobil görünüm denenmedi.
- Personal_Task_App uygulama/pipeline/Compose dosyaları değiştirilmedi. Mevcut Jenkins job yolu, token, webhook/ngrok, Docker label'ları ve ağ düzeni canlı sistemde doğrulanmadı. Örnek profilde job yolu bu nedenle boş; label seçicisi Jenkinsfile'daki Compose proje adına dayanır.
- Gerçek Jenkins/Docker/health veri çekme, build tetikleme, audit, ayrıntılı RBAC ve tarama/security gate henüz yok. “Aktif profil” canlı sistemin sağlıklı olduğunu iddia etmez.
- Commit, push veya haricî yayın yapılmadı.

## Önerilen devam noktası — ayrıca kullanıcı talimatıyla

1. README ile kullanıcı hesabını yapılandırıp Docker Engine çalışırken OpsBridge Compose kurulumunu doğrulamak; tarayıcıda giriş ve profil akışını görmek.
2. [Envanterdeki komutlarla](../docs/PERSONAL_TASK_APP.md) Personal_Task_App'i çalıştırmak; gerçek Jenkins job yolunu ve container label'larını doğrulamak. Bu olmadan 1. haftanın tamamı bitmiş sayılmaz.
3. Kullanıcının sonraki kapsam seçimine göre pipeline hazırlığına veya gerçek adapter'lara geçmek. Sekiz haftalık planın ileri adımları kendiliğinden uygulama emri değildir.
