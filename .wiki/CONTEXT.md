# Kullanıcı ve ürün bağlamı

Son güncelleme: **2026-11-04 — Europe/Istanbul**

## Kullanıcının anlattığı geçmiş ve amaç

Kaynak: Bu proje sohbetindeki kullanıcı açıklamaları.

- Kullanıcı `taskApp` uygulamasını önceki dönem, hocası Jenkins ve Docker öğretirken yaptı. Anlattığı akışta gönderilen commit'in otomatik kontrolü için ngrok kullanılıyordu.
- Konuların çoğunu şu anda hatırlamadığını söylüyor. DevOps süreçlerine hâkim olduğunu varsayma; bu uygulamayı tekrar öğrenmek ve bilgisini geliştirmek için araç olarak seçti.
- Büyük projeler geliştirip yayınlama deneyimi sınırlı. Gerçek operasyon ihtiyaçlarının ve hangi sistemden hangi bilgilerin alınabileceğinin sınırlarını anlamak istiyor.
- Yalnızca çalışan kod değil, neyin neden yapıldığını anlayabileceği bir ilerleme istiyor. Somut uygulama akışları ve küçük adımlar açıklamayı kolaylaştırır.
- Mevcut sekiz haftalık raporu yapay zekâ oluşturmuş. Kullanıcı raporun doğru/yanlış ve uygulanabilir kısımlarını kendi fikri üzerinden değerlendirmemizi istiyor; raporun tamamını onaylamış değil.
- Sohbet bağlamını `.wiki` altında saklamamızı açıkça istedi. Amaç aynı geçmişi tekrar yazmamak ve ilerleyişi Codex ile senkron yürütmek.

## Kesinleşen çalışma alanları

- **2026-11-04 kullanıcı düzeltmesi:** Asıl OpsBridge geliştirme ortamı `C:\Yazılım\DevOpsSystemMonitoring\OpsBridge` dizinidir. Kullanıcı hafıza dosyalarının bu klasöre de eklenmesini istedi.
- `C:\Yazılım\Personal_Task_App`, önceki konuşmanın ve kaynak incelemesinin yapıldığı ayrı referans uygulama deposudur.
- Önceki yanıtta açık oturum dizini ana geliştirme ortamı sanılmıştı. Kullanıcı bu değişiklikleri geri aldığını söyledi. Bu kurulumda kaynak depoya yeniden değişiklik yapılmadı.
- OpsBridge ayrı bir proje dizininde geliştiriliyor. 2026-11-04 başlangıç uygulamasında Codex `backend/` ve `frontend/` klasörlerini oluşturdu; kurulum kökteki Compose ile tanımlandı.

## Kullanıcının hedeflediği OpsBridge

Kullanıcının hedefi, proje ve ortam için temel operasyonları bir araya getiren bir arayüz:

1. **Yerel bilgisayarda:** Container, Git ve CI/CD durumunu temel düzeyde görmek ve gerekli temel işlemleri başlatmak/değişiklikleri yapmak.
2. **Sunucuda:** Aynı arayüzden Git, Jenkins ve Kubernetes kontrollerini sunmak.
3. **Ayrıntı seviyesi:** Her aracın bütün ayrıntılarını göstermek zorunda değil. Sorun olduğunda Docker veya Jenkins gibi asıl araçlarda ayrıntılı inceleme yapılabilir.
4. **Örnek değişiklik isteği:** Kullanıcı panelden bir DB image'ı seçsin; sistem Docker Compose yapılandırmasını buna göre değiştirsin. Bu, konuşmada akla gelen bir kullanım örneği; ayrıntılı kapsamı kesinleşmiş bir özellik değil.

Kubernetes kullanıcının ifade ettiği ürün hedefinin parçasıdır. Codex'in onu sonraki aşamaya bırakma önerisi, bu hedefin kullanıcı tarafından kaldırıldığı anlamına gelmez.

## 2026-11-04 — Kullanıcının ilk uygulama talimatı

- Kullanıcı, raporun **1. haftasından yola çıkarak basit Spring–React yapısını oluşturmamızı** istedi; Personal_Task_App yolunu tekrar belirtti ve gerektiğinde soru sorulabileceğini söyledi.
- Bu istek doğrultusunda proje profili CRUD, PostgreSQL, basit ADMIN oturumu ve dış servisler için adapter arayüzleri uygulandı. `backend/`–`frontend/` düzeni, session/CSRF yaklaşımı ve çakışmayan yerel portlar Codex'in teknik uygulama seçimleridir.
- Bu talimat, raporun bütün tarihlerinin ve sekiz haftalık kapsamının topluca kabulü veya Kubernetes hedefinin kaldırılması olarak kaydedilmedi.
- Uygulama ve doğrulama ayrıntıları [STATUS.md](STATUS.md), çalıştırma adımları [README](../README.md) içindedir.

## 2026-11-04 — Kullanıcının kod kalitesi ve beceri talimatı

- Kullanıcı controller, service, entity, DTO, mapper ve enum sorumluluklarının klasörlerde açıkça ayrılmasını istedi. Az satırda kod yazmak yerine **anlaşılır kod** talep etti.
- Backend-developer, frontend-developer ve veritabanıyla ilgili üç becerinin eklenmesini; her teknoloji için kaynak araştırılması ve iyi uygulama yöntemlerinin belirlenmesini istedi.
- React Router DOM/Axios ve Spring DTO/mapper/JWT örneklerini teknoloji değerlendirmesi bağlamında verdi. Bütün örneklerin koşulsuz bağımlılık olarak eklenmesi veya JWT'ye mutlaka geçilmesi şeklinde yorumlanmadı.
- Bu talimatla katmanlar ayrıldı; frontend Router DOM ve Axios'a taşındı. Küçük model için açık manuel mapper ve mevcut session+CSRF tercihleri gerekçelendirildi. Üç beceri kaynaklarıyla oluşturulup kullanıcı beceri klasörüne kuruldu.
- Kalıcı kurallar kök `AGENTS.md`, teknoloji tercihleri ve kaynaklar [geliştirme rehberi](../docs/DEVELOPMENT_GUIDE.md) içinde. Yeni adımlarda bu okunabilirlik talimatını koru; yeniden sıkıştırılmış kod üretme.

## Codex'in sunduğu yaklaşım — henüz kesin karar değil

- OpsBridge'i ayrı bir yönetim paneli; Personal_Task_App'i ilk izlenen örnek uygulama olarak ele almak. Bu ayrım raporda da var; ayrı geliştirme dizini kullanıcı tarafından belirlendi ve başlangıç iskeleti bu ayrımla oluşturuldu.
- Ürünün değerini şu ilişki üzerinden kurmak: **commit → build/test → deploy edilen sürüm → mevcut çalışma ve health durumu**.
- Git/Jenkins/Docker/Kubernetes bağlantılarını backend'deki ayrı bağlantı modülleri üzerinden yürütmek; erişim bilgilerini tarayıcıya taşımamak.
- Proje, ortam ve çalıştırma altyapısını ayrı tanımlamak. Sunucunun mutlaka Kubernetes kullanması gerekmez; Compose sunucuda da kullanılabilir.
- Başarılı build, çalışan container ve sağlıklı uygulamayı ayrı durumlar olarak göstermek; birini diğerinin kanıtı saymamak.
- İlk aşamayı yerel Jenkins + Docker Compose ile dar tutmak. Önerilen ilk akış: build geçmişi + container/health görünümü + yetkili build tetikleme.
- Yapılandırma değişikliğini dosya farkı ve doğrulama ile sunmak, Git'te izlemek, Jenkins üzerinden uygulamak ve çalışan sonucu kontrol etmek.
- Image değiştirmeyi önce hazırlanmış backend/frontend sürümleriyle denemek. DB ana sürüm geçişi veri taşıma gerektirebilir; yalnızca image etiketini değiştirmek yeterli değildir. DB motorunu değiştirmek ayrıca uygulama uyumluluğu gerektirir.
- Sekiz haftalık kapsam için Kubernetes'i sonraki aşamaya bırakmak önerildi; kullanıcı bu öneriyi henüz kabul etmedi.

## Mevcut raporla ilişki

Kaynak: [OpsBridge — Revize 8 Haftalık DevSecOps Proje Planı.md](<../OpsBridge — Revize 8 Haftalık DevSecOps Proje Planı 1.md>)

- Raporda ilk dört hafta DevOps paneli, sonraki dört hafta güvenlik kontrolleri/DevSecOps öneriliyor.
- Raporun kapsamı Jenkins + Docker/HTTP health ile sınırlı; Kubernetes açıkça yapılmayacaklar arasında. Bu, kullanıcının daha geniş sunucu hedefiyle henüz çözülmemiş bir kapsam farkı.
- Rapor Trivy taramaları, güvenlik gate'i, yetkilendirme ve audit öneriyor. Bunların uygulanmış olduğu varsayılmamalı.
- Panelden image/Compose değiştirme, son konuşmada ayrıca ele alınan bir fikir; rapor bunu tamamlanmış tasarım olarak tanımlamıyor.
- Rapordaki tarihler, vize/final kapsamı, haftalık 10–12 saat ve hocanın beklentileri kullanıcı tarafından bu sohbette ayrıca doğrulanmadı. Bunları kesin teslim tarihi veya kullanıcının zaman taahhüdü sayma.

## Açık konular

- Kubernetes dönem projesinde zorunlu mu, yoksa ürünün sonraki sürüm hedefi mi?
- Vize/final için gerçek teslim tarihleri, zorunlu kapsam ve ayrılabilecek süre ne?
- İlk sürümde Git kontrolü son commit/branch görünümüyle mi sınırlı; yerel değişiklik, commit/push veya değişiklik önerisi de isteniyor mu?
- Başlangıç düzeni `backend/` ve `frontend/` olarak uygulandı; sonraki kapsam için ek altyapı ihtiyaçları ayrıca değerlendirilecek.
- Mevcut Jenkins job'ı, webhook/ngrok, Docker Desktop/WSL ve erişim düzeninin şu anki çalışan durumu ne?
- Compose düzenlemesinde doğrudan dosya güncelleme, override dosyası veya Git üzerinden değişiklik akışından hangisi seçilecek?

Bu sorular her görevde topluca yeniden sorulacak bir anket değildir. Mevcut dosyaları ve konuşma kayıtlarını kullan; yalnızca yapılacak işi gerçekten etkileyen belirsizliği gerektiğinde netleştir.
