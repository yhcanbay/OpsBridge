# Proje çalışma notları

## Çalışma alanları

- Ana geliştirme projesi bu depodur: `C:\Yazılım\DevOpsSystemMonitoring\OpsBridge`. Kullanıcı 2026-11-04 tarihinde bu yolu açıkça belirtti.
- Referans/izlenen uygulama ayrı depodadır: `C:\Yazılım\Personal_Task_App`. Oradaki taskApp, Jenkins ve Docker dosyalarını bu deponun mevcut uygulama kodu sanma.
- Bu deponun ilerleyişi için buradaki `.wiki` kayıtlarını güncel tut. Diğer depodaki hafıza kopyalarının kendiliğinden senkron olduğunu varsayma.

## Kalıcı bağlam

- Bu deponun ortak proje hafızası `.wiki/` klasörüdür. Yeni bir oturumda proje üzerinde çalışmaya başlarken `.wiki/README.md` ve `.wiki/STATUS.md` dosyalarını oku; aynı oturumda değişmedikçe tekrar okuma.
- Kullanıcının amacı, öğrenme geçmişi veya ürün kapsamı gerektiğinde `.wiki/CONTEXT.md`; önceki görüşmelerin gerekçesi gerektiğinde `.wiki/HISTORY.md` dosyasına başvur. Küçük, bağımsız işler için bütün dokümantasyonu yeniden tarama.
- Türkçe ve anlaşılır anlat. Kullanıcı DevOps bilgisini bu projeyle yeniden geliştirmek istiyor; yeni kavramları gerektiği yerde kısa örneklerle açıkla.
- Kullanıcının ifadelerini, Codex önerilerini, kodda gözlenen durumu ve henüz doğrulanmamış varsayımları ayır. Bir öneriyi kullanıcı kabul etmeden kesin karar olarak kaydetme.
- Güncel kullanıcı talimatları önceliklidir. `.wiki` içindeki sonraki adımlar ve mevcut yapay zekâ raporu, kendi başlarına uygulama emri veya kesinleşmiş kapsam değildir.

## Kod düzeni ve geliştirme becerileri

- Kullanıcı 2026-11-04 tarihinde açıkça anlaşılır kodu, az satırda kod yazmaya tercih etti. Birden fazla işlemi tek satıra sıkıştırma; açıklayıcı isimler, normal biçimlendirme ve ayrı sorumluluklar kullan.
- Backend katmanları `controller`, `service`, `repository`, `entity`, `dto/request`, `dto/response`, `mapper`, `enums`, `config`, `exception` olarak görünür olmalı. Entity DTO'ya bağımlı olmamalı; controller entity döndürmemeli. Java anahtar sözcüğü nedeniyle paket adı `enum` değil `enums` olur.
- Frontend'de router, layout/page, component, hook/context ve HTTP servislerini ayır. Bu depoda React Router DOM ve merkezi Axios istemcisi kullanılıyor. Sabitler için form component'ine bağımlılık kurma.
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
