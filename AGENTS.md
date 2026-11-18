# Proje çalışma notları

## Çalışma alanları

- Ana geliştirme projesi bu depodur: `C:\Yazılım\DevOpsSystemMonitoring\OpsBridge`. Kullanıcı 2026-11-04 tarihinde bu yolu açıkça belirtti.
- Referans/izlenen uygulama ayrı depodadır: `C:\Yazılım\Personal_Task_App`. Oradaki taskApp, Jenkins ve Docker dosyalarını bu deponun mevcut uygulama kodu sanma.
- Kod yazım referansı `C:\Yazılım\Sozluk_Uygulamasi` projesidir. Kaynak kodunun yerel kopyası `.reference-code/Sozluk_Uygulamasi/` altındadır ve Git tarafından izlenmez. Bu proje ile izlenen Personal_Task_App'in rolleri farklıdır.
- Bu deponun ilerleyişi için buradaki `.wiki` kayıtlarını güncel tut. Diğer depodaki hafıza kopyalarının kendiliğinden senkron olduğunu varsayma.

## Kalıcı bağlam

- Bu deponun ortak proje hafızası `.wiki/` klasörüdür. Yeni bir oturumda proje üzerinde çalışmaya başlarken `.wiki/README.md` ve `.wiki/STATUS.md` dosyalarını oku; aynı oturumda değişmedikçe tekrar okuma.
- Kullanıcının amacı, öğrenme geçmişi veya ürün kapsamı gerektiğinde `.wiki/CONTEXT.md`; önceki görüşmelerin gerekçesi gerektiğinde `.wiki/HISTORY.md` dosyasına başvur. Küçük, bağımsız işler için bütün dokümantasyonu yeniden tarama.
- Türkçe ve anlaşılır anlat. Kullanıcı DevOps bilgisini bu projeyle yeniden geliştirmek istiyor; yeni kavramları gerektiği yerde kısa örneklerle açıkla.
- Kullanıcının ifadelerini, Codex önerilerini, kodda gözlenen durumu ve henüz doğrulanmamış varsayımları ayır. Bir öneriyi kullanıcı kabul etmeden kesin karar olarak kaydetme.
- Güncel kullanıcı talimatları önceliklidir. `.wiki` içindeki sonraki adımlar ve mevcut yapay zekâ raporu, kendi başlarına uygulama emri veya kesinleşmiş kapsam değildir.

## Kod düzeni ve geliştirme becerileri

- **2026-11-07 çalışma modeli:** OpsBridge takip edilen uygulamayla aynı sunucuda ayrı bir servis olarak çalışır; kullanıcı uzaktan yalnızca tarayıcıdaki OpsBridge URL'sine admin girişi yapar. Yerel Git/Docker yolları bu sunucuya aittir. Farklı sunuculara merkezi SSH/agent yönetimi ekleme. URL/HTTPS üzerinden yayınlama en son yapılacak ayrı iştir; bugünkü localhost portlarını bu kararla açma. Ayrıntı: [mimari](docs/ARCHITECTURE.md).

- Kullanıcı 2026-11-04 tarihinde açıkça anlaşılır kodu, az satırda kod yazmaya tercih etti. Birden fazla işlemi tek satıra sıkıştırma; açıklayıcı isimler, normal biçimlendirme ve ayrı sorumluluklar kullan.
- **2026-11-07 kullanıcı talimatı:** Yeni ve mevcut kodda Sozluk_Uygulamasi yazımını esas al. İlgili iş için yerel referanstan benzer controller/service/DTO veya React bileşenini incele. Somut örnekler ve uyarlama sınırları [kod yazım rehberinde](docs/CODING_STYLE.md). Genel “clean code” tercihini kullanıcının anlayabildiği yazımın önüne koyma.
- **2026-11-11 kullanıcı geri bildirimi:** Kullanıcı bu kodla öğreniyor ve okul nedeniyle zamanı kısıtlı; çok sayıda sınıf ve dosyalar arası geçiş anlamayı zorlaştırıyor. Referansa uyumu yalnızca biçim olarak ele alma. Mevcut katman ve DTO sınırları içinde doğrudan, adım adım akış kur; gelecekte gerekebilir diye adapter, stub, factory veya ek model katmanları oluşturma. Yeni bir ayrım gerçekten gerekiyorsa bugünkü somut ihtiyacını kısa bir örnekle açıkla; bir kaynak veya taktik önerisini kapsamlı mimari kurma talimatı sayma.
- Backend'de `IProjectController` → `controller/impl/ProjectControllerImpl`, `IProjectService` → `service/impl/ProjectServiceImpl` düzenini kullan. DTO'lar `record` yerine Lombok `@Data`, `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor` içeren normal sınıflardır. Dönüşümlerde `toDto`, `createEntity`, `updateEntity`; servislerde `getAllProjects`, `getProjectById`, `saveProject` gibi açık isimleri kullan. Liste dönüşümünü anlaşılır `for` döngüsüyle yaz. Enjeksiyon `@RequiredArgsConstructor` ve `private final` alanlarla yapılır.
- Backend katmanları `controller`, `service`, `repository`, `entity`, `dto/request`, `dto/response`, `mapper`, `enums`, `config`, `exception` olarak görünür olmalı. Entity DTO'ya bağımlı olmamalı; controller entity döndürmemeli. Java anahtar sözcüğü nedeniyle paket adı `enum` değil `enums` olur.
- Frontend'de router, layout/page, component, hook/context ve HTTP servislerini ayır. Bu depoda React Router DOM ve merkezi Axios istemcisi kullanılıyor. Sabitler için form component'ine bağımlılık kurma.
- React bileşenlerini `function Component() { ... }` ve dosya sonunda `export default Component` biçiminde yaz. JS/JSX dört boşluk, tek tırnak, noktalı virgülsüz ve sonda fazladan virgül olmadan biçimlenir. Form alanlarını JSX içinde açıkça göster; işlemleri `handleChange`, `handleSubmit` gibi isimli fonksiyonlarla yürüt.
- Teknolojiyi gerçek ihtiyaca göre seç. JWT, MapStruct, state veya form kütüphanesi eklemek otomatik bir kalite ölçütü değildir; seçimi mevcut kapsam ve resmî kaynaklarla açıkla.
- İlgili işte bu becerileri kullan: [backend-developer](skills/backend-developer/SKILL.md), [frontend-developer](skills/frontend-developer/SKILL.md), [database-developer](skills/database-developer/SKILL.md). Üçü ayrıca `C:\Users\yhcan\.codex\skills` altında kurulu. Bu depodaki kopyalar inceleme ve proje talimatlarının kaynağıdır; kopyaların otomatik senkron olduğunu varsayma.
- Mimari, teknoloji gerekçeleri ve kaynaklar [geliştirme rehberinde](docs/DEVELOPMENT_GUIDE.md). Frontend biçim kontrolü `npm run format:check`, davranış testleri `npm test`; backend kontrolü `mvn verify`.

## Hafızayı güncel tutma

- Anlamlı bir hedef/kapsam kararı, teknik bulgu veya çalışma sonucu oluştuğunda ilgili `.wiki` dosyasını aynı görev içinde güncelle. Güncel durumu `STATUS.md`, kalıcı hedefleri `CONTEXT.md`, önemli değişimlerin tarihçesini `HISTORY.md` içinde tut.
- Sonraki oturumun devam edebilmesi için tamamlanan işi, gerçekten yapılan doğrulamayı, belirsizlikleri ve sıradaki önerilen işi belirt. Sırf hafıza kaydı oluşturuldu diye ürün geliştirme adımını tamamlandı sayma.
- Eski karar değişirse neyin değiştiğini ve nedenini tarihçeye ekle; geçmiş bir öneriyi sessizce kullanıcı kararı hâline getirme.
- Dosyaların tarihini gerçek güncellemeye göre yaz; bu proje için kullanıcı saat dilimi `Europe/Istanbul`.
- Token, parola, özel anahtar veya gereksiz kişisel bilgi kaydetme. Ham loglar ve kaynak kodu kopyaları yerine kaynak dosyaya bağlantı ver.
- Kod ve canlı sistemler, mevcut davranış için asıl kanıttır. Hafıza eskimişse düzelt; test veya deployment çalıştırılmadığında bunu açıkça belirt.
