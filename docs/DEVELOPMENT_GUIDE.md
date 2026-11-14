# OpsBridge geliştirme rehberi

Araştırma: **2026-11-04**; son düzenleme: **2026-11-14 — Europe/Istanbul**.

Kullanıcının açık tercihi: **az satır yerine anlaşılır kod**. Dosya adı, bulunduğu paket ve çağrı akışı üzerinden bir işin nerede yapıldığını takip edebilmek temel ölçüt. Bu rehberdeki teknoloji seçimleri bu projenin mevcut kapsamına aittir; her uygulama için tek doğru teknoloji listesi olduğu iddia edilmez.

**7 Kasım kullanıcı talimatıyla yazım referansı Sozluk_Uygulamasi oldu.** Önceki record DTO ve tek sınıflı servis tercihleri bu talebe göre değişti. Yerel örnekler, biçim ve güncel kurallar [kod yazım rehberinde](CODING_STYLE.md).

**Çalışma modeli:** OpsBridge, izlenen uygulamayla aynı sunucuda ayrı servisler olarak çalışır. Uzak kullanıcı tarayıcıdan URL'ye admin girişi yapar; kaynak işlemlerini o sunucudaki backend yürütür. URL/HTTPS yayını en son yapılacak ayrı iştir. [Mimari ve kaynak sınırları](ARCHITECTURE.md).

## Spring Boot: katmanların sorumluluğu

**14 Kasım — GitHub tokenlarının saklanması:** GitHub'a istek gönderirken asıl token gerektiği için admin parolasındaki BCrypt hash'i bu amaçla kullanılamaz. Mevcut Spring Security 7.0.8 içindeki `AesGcmBytesEncryptor` ile şifreleme kullanılır; yeni bağımlılık veya özel kriptografi eklenmez. 32 baytlık anahtar yalnız backend ortamında kalır, DB'de şifreli token bulunur. Bağlantı yanıtı `id/name/accountLogin`, proje ise yalnız bağlantı kimliği taşır. Yeni tokenın sahibi GitHub `/user`, seçilen projeye okuma erişimi `/branches` ile doğrulanır. Mevcut Java `HttpClient` yaklaşımı ve sabit GitHub hedefi korunur. [Spring Security Crypto](https://docs.spring.io/spring-security/reference/7.0/features/integrations/cryptography.html), [GitHub hesap doğrulama](https://docs.github.com/en/rest/users/users#get-the-authenticated-user), [branch okuma izinleri](https://docs.github.com/en/rest/branches/branches#list-branches). Akış ve API sözleşmesi [Git görünürlüğü belgesindedir](GIT_VISIBILITY.md).

```text
com.yhcanbay.opsbridge
├── config/                   Spring Security yapılandırması
├── controller/               I…Controller sözleşmeleri ve HTTP mapping
│   └── impl/                 …ControllerImpl uygulamaları
├── dto/
│   ├── request/              Gelen veri ve validation
│   └── response/             API'nin döndürdüğü sözleşme
├── entity/                   JPA/veritabanı modeli
├── enums/                    Capability, IntegrationState
├── exception/                Uygulama hataları ve HTTP hata çevirisi
├── mapper/                   DTO–entity dönüşümü
├── repository/               Veritabanı sorguları
└── service/                  I…Service sözleşmeleri
    └── impl/                 Kullanım senaryoları ve transaction
```

**11 Kasım sadeleştirmesi:** Jenkins, Docker ve HTTP health başlangıç durumları `IntegrationServiceImpl` içinde doğrudan `IntegrationStatusResponse` olarak oluşturulur. Henüz dış istek yapılmadığı için adapter/stub/factory ve ayrı sonuç modeli kaldırıldı. Akış controller → service → response DTO şeklindedir; gerçek bağlantılar ihtiyaç oldukça tanıdık servis düzeninde eklenir.

**13 Kasım GitHub adımı:** `GitServiceImpl`, kullanıcının öğrendiği Java `HttpClient` ile branch/commit listelerini okur ve açık döngülerle yanıt DTO'larını oluşturur. Yeni adapter veya HTTP kütüphanesi eklenmez. Katmanlar arasındaki kısa örnek akış [Git görünürlüğü belgesinde](GIT_VISIBILITY.md).

Örneğin proje oluşturma akışı:

1. `IProjectController`, `ProjectRequest` verisini doğrulatır; `ProjectControllerImpl` servisi çağırır.
2. `IProjectService` uygulaması `ProjectServiceImpl`, kullanım senaryosunun transaction'ını yönetir.
3. `ProjectMapper.createEntity`, gelen veriyi `ProjectProfile` entity'sine çevirir.
4. `ProjectRepository`, entity'yi kaydeder.
5. Mapper, kaydedilmiş veriyi `ProjectResponse` olarak döndürür; controller 201 ve Location header'ını üretir.

Entity, HTTP isteğini veya DTO'yu bilmez. Service HTTP 404 üretmez; `ProjectNotFoundException` fırlatır, `ApiExceptionHandler` bunu HTTP hata yanıtına çevirir. Auth DTO'ları ve enum'lar da kendi dosyalarındadır. Bu katman düzeni kullanıcının istediği proje standardıdır. Spring Boot belirli bir paket şemasını zorunlu tutmaz; ana uygulama sınıfının diğer paketlerin üzerinde bulunmasını önerir. [Spring Boot 4.0 kod düzeni](https://docs.spring.io/spring-boot/4.0/reference/using/structuring-your-code.html).

### DTO, mapper ve entity tercihleri

| Teknoloji / yaklaşım | OpsBridge'deki seçim ve gerekçe |
|---|---|
| Request/response DTO | Gelen ve dönen sözleşmeler ayrı; entity doğrudan API'ye açılmaz |
| DTO sınıfı | Referanstaki gibi Lombok `@Data`, `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor`; normal alanlar ve getter/setter'lar |
| Mapper | Entity–DTO dönüşümü `ProjectMapper` içinde `toDto`, `createEntity`, `updateEntity` ile yapılır; bağlantı durumları serviste doğrudan yanıt DTO'su olarak oluşturulur |
| MapStruct | Araştırıldı; çok sayıda tekrar eden dönüşüm oluştuğunda değerlendirilecek. Şu an tek küçük mapper için ek bağımlılık eklenmedi |
| Constructor injection | `@RequiredArgsConstructor` ve `private final` bağımlılıklar |
| Controller/service interface | Kullanıcının referansı doğrultusunda `I…` arayüzü ve `impl/` altındaki uygulama sınıfı |
| Lombok | DTO, entity erişim metotları ve constructor üretimi için eklendi; sürümü Boot yönetir, Maven annotation processor açık |

MapStruct, derleme sırasında mapper uygulaması üretebilir; Spring component modeli ve eşlenmeyen alanlar için derleme kontrolü sunar. Manuel mapper seçimi bu küçük model için proje kararıdır. [MapStruct kararlı sürüm rehberi](https://mapstruct.org/documentation/stable/reference/html/).

Lombok'taki `@Data` erişim metotlarını ve temel nesne metotlarını, `@Builder` isimli alanlarla nesne oluşturmayı üretir. JPA entity'de `@Getter`/`@Setter` kullanılır; koleksiyon güncellemesi ve lifecycle metotları açık kalır. [Lombok Data](https://projectlombok.org/features/Data), [Builder](https://projectlombok.org/features/Builder), [Maven kurulumu](https://projectlombok.org/setup/maven).

### Session ve JWT

Mevcut ürün aynı origin üzerinden çalışan tek bir yönetim paneli ve basit ADMIN hesabı kullanıyor. **Session cookie + CSRF** korunuyor: oturum sunucuda sonlandırılabilir; tarayıcı parola veya bearer token saklamaz. Axios yazma isteğinden önce CSRF token'ını alır, girişten sonra yeni token ister. [Spring Security 7.0 CSRF](https://docs.spring.io/spring-security/reference/7.0/servlet/exploits/csrf.html).

JWT, bağımsız mobil/API istemcisi veya bir kimlik sağlayıcıyla bearer token doğrulama ihtiyacında değerlendirilecek. Bu durumda issuer, audience, imza anahtarları, süre, token yenileme ve çıkış/iptal davranışı birlikte tasarlanmalı. Spring Security Resource Server, JWT doğrulamak için desteklenen bir yoldur; kendi başına kullanıcı girişi ve token üretim sistemi değildir. Bu değerlendirme sonucunda mevcut görevde JWT'ye geçilmedi. [Spring Security 7.0 JWT](https://docs.spring.io/spring-security/reference/7.0/servlet/oauth2/resource-server/jwt.html).

## React: routing, durum ve HTTP erişimi

```text
frontend/src/
├── app/                       Route tanımları ve uygulama bileşimi
├── api/                       Axios instance, CSRF ve hata normalleştirme
├── components/
│   ├── auth/                  Giriş formu ve route koruması
│   ├── common/                Ortak küçük UI parçaları
│   └── projects/              Profil formu, liste, detay ve bağlantı kartları
├── constants/                 Capability etiketleri ve başlangıç form modeli
├── context/                   AuthContext / AuthProvider
├── hooks/                     useAuth, useProjects, useIntegrations
├── layouts/                   Panel çerçevesi ve navigasyon
├── pages/                     Login, projeler ve bulunamadı ekranı
└── services/                  Auth ve proje API işlemleri
```

| Teknoloji | Kullanım |
|---|---|
| React Router DOM 7.18.5 | `/login`, `/projects`, `/projects/:projectId`, bulunamadı sayfası, korumalı layout |
| Axios 1.21.0 | Ortak base URL, 10 saniye timeout, CSRF request interceptor, API hata çevirisi ve 401 oturum olayı |
| Context | Yalnızca oturumun ortak durumu; kullanıcı ve giriş/çıkış işlemleri |
| Custom hooks | Proje yükleme/mutation ve entegrasyon yükleme; iptal edilen isteklerin yönetimi |
| Prettier | Tekrarlanabilir JSX/JS/CSS biçimi; `npm run format` ve `npm run format:check` |
| Vitest + Testing Library + jsdom | HTTP istemci sözleşmesi ve React yönlendirme/oturum davranışı testleri |

Router'ın **declarative** modu bu Vite SPA için seçildi. Nested route ve `Outlet` layout'u paylaşır. Proje seçimi URL'ye yazar; bir linkten gelen proje kimliği bulunamadığında başka projeyi sessizce göstermez. Girişten sonra kullanıcının istediği adrese dönülür. Bu kurgu, router'ın URL eşleştirme ve iç içe sayfa özelliklerini kullanır. [React Router routing](https://reactrouter.com/start/declarative/routing), [mod seçimi](https://reactrouter.com/start/modes).

Component'ler Axios yapılandırması taşımaz; `projectService` ve `authService` çağırır. Axios instance ortak HTTP davranışını, interceptor'lar CSRF ve hata akışını toplar. Mutasyonlar başarısız olduğunda otomatik tekrarlanmaz; sunucu işlemi tamamlamış ama yanıt ulaşmamış olabilir. [Axios instance](https://axios-http.com/docs/instance), [interceptors](https://axios-http.com/docs/interceptors).

Hook'lar tek bir anlaşılır davranış etrafında ayrıldı. Büyük bir App component'i içine bütün durumlar ve işlemler konmadı. Route değişirken/yeni istek başlarken eski isteği iptal etmek için AbortController kullanılır. İlk oturum kontrolünde ağ hatası olması, giriş yapılmamış olmakla aynı sayılmaz; kullanıcıya tekrar deneme sunulur. [React custom hooks](https://react.dev/learn/reusing-logic-with-custom-hooks), [Axios cancellation](https://axios-http.com/docs/cancellation).

Redux/Zustand, TanStack Query veya React Hook Form/Zod şu an eklenmedi. Paylaşılan durum, cache veya karmaşık form ihtiyacı doğduğunda bunlar ayrıca değerlendirilebilir. Bu araçların yokluğu, router/servis/HTTP sınırlarının belirsiz olması için gerekçe değildir.

## PostgreSQL, JPA ve Flyway

- Şema Flyway migration'larıyla yönetilir; Hibernate yalnızca `validate` yapar. Uygulanmış V1/V2 değişmedi; integrity kuralları yeni **V3** ile eklenir. Flyway sürümlü migration'ları sıralı uygular ve checksum izler. [Flyway versioned migrations](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/versioned-migrations).
- API validation'a ek olarak DB, boş proje adı/ortamı ve tanımsız capability değerlerini reddeder. Foreign key, capability'nin var olan projeye ait olmasını sağlar. İsim+ortam için yeni bir uniqueness kuralı uydurulmadı. [PostgreSQL 16 constraints](https://www.postgresql.org/docs/16/ddl-constraints.html).
- Capability değerleri metin olarak saklanır; enum ordinal sırasına bağlı veri tutulmaz.
- Capability koleksiyonu varsayılan LAZY'dir. İlgili repository sorgularında EntityGraph ile istenir ve DTO dönüşümü service transaction'ı içinde yapılır. Her sorguya otomatik EAGER yük bindirilmez. [Spring Data JPA entity graphs](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.entity-graph), [transaction sınırı](https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html).
- Güncelleme yanıtından önce flush yapılması, JPA callback'inin `updatedAt` değerini yenilemesini sağlar.
- Mevcut primary key indekslerine ek, sorgu ihtiyacı gösterilmeden indeks eklenmedi. Yeni filtreleme ve veri hacminde sorgu planı incelenerek karar verilir. [PostgreSQL 16 indexes](https://www.postgresql.org/docs/16/indexes-intro.html).

## Biçim ve doğrulama

Java kodu açık import'larla, dört boşluk girintiyle ve Sozluk_Uygulamasi örneklerine göre yazılır. Önceki AOSP formatter çıktısı güncel yazım ölçütü değildir. Frontend JS/JSX'te dört boşluk, tek tırnak, noktalı virgülsüz yazım ve son elemandan sonra virgül olmaması Prettier ile uygulanır; `.editorconfig` editör temelini belirtir. [Kod yazım rehberi](CODING_STYLE.md), [Prettier options](https://prettier.io/docs/options).

Kontroller:

```powershell
# backend/ içinde
mvn verify

# frontend/ içinde
npm run format:check
npm test
npm run build
```

Backend paketleri taşındığında ilk kontrolde `mvn clean verify` kullanılır; eski sınıfların `target/` içinde kalması önlenir. 11 backend testi API/oturum ve doğrudan DB integrity davranışlarını, 11 frontend testi router/oturum, Axios protokolü ve proje formunu kapsar. jsdom testi gerçek tarayıcı görsel kontrolünün yerine geçmez. Son çalıştırmalar ve doğrulama sınırları [.wiki/STATUS.md](../.wiki/STATUS.md) içinde tutulur.

## Üç geliştirme becerisi

Kaynakları depodaki [skills](../skills/) altında; kurulu kopyaları `C:\Users\yhcan\.codex\skills` içinde:

- [backend-developer](../skills/backend-developer/SKILL.md): Spring katmanları, DTO/mapper ve güvenlik seçimi.
- [frontend-developer](../skills/frontend-developer/SKILL.md): React routing, servisler, state ve erişilebilir component'ler.
- [database-developer](../skills/database-developer/SKILL.md): PostgreSQL, JPA, migration ve veri bütünlüğü.

Her beceri kendi teknik kaynaklarını içerir. Ortak amaç okunabilirlik ve doğru sorumluluk sınırlarıdır. Kurulu ve depodaki kopyalar otomatik senkron değildir; güncellemelerde birlikte ele alınmalıdır.
