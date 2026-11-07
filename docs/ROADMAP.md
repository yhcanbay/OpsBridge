# OpsBridge — vize ve devam planı

Güncelleme: **2026-11-07 — Europe/Istanbul**.

Bu belge, [ilk sekiz haftalık planın](<../OpsBridge — Revize 8 Haftalık DevSecOps Proje Planı 1.md>) mevcut kaynaklar ve kullanıcı açıklamalarına göre güncellenmiş devam önerisidir. İlk plan tarihsel kaynak olarak korunur. Planlanan işler tamamlanmış özellik olarak değerlendirilmez.

## Kullanıcının kesinleştirdiği çerçeve

- **Vize haftası: 23–29 Kasım 2026.** Hocanın beklentisi, temel DevOps işlerinin vizeye kadar bitmesi; güvenlik çalışmalarına vizeden sonra geçilmesi.
- **Kubernetes bu teslimde gerekmiyor.** Kullanıcının uzun vadeli ürün hedefinden kaldırıldığı varsayılmaz.
- **7 Kasım çalışma modeli:** OpsBridge, takip edilen uygulamayla aynı sunucuda ayrı bir uygulama olarak çalışacak. Kullanıcı uzaktan tarayıcıdaki URL'ye admin girişi yapacak. Kaynak işlemleri bu sunucuda yürütülecek; yerel Git yolu sunucu deposunu ifade eder. [Mimari](ARCHITECTURE.md).
- **URL üzerinden yayınlama en son yapılacak.** Dış URL, HTTPS ve proxy/oturum ayarları ayrı son aşamadır; bu karar hemen port açma veya deployment talimatı değildir. Yayın tarihi henüz belirlenmedi.
- **22 Kasım**, vize haftasına çalışan bir sürümle girmek için önerilen hazırlık hedefidir; hocanın bildirdiği ayrı bir teslim tarihi değildir.
- Güvenlik geliştirmeleri için önerilen başlangıç **30 Kasım**. Final teslim tarihi ve haftalık ayrılabilecek süre henüz bilinmiyor. Aralık tarihleri çalışma hedefidir; final tarihi öğrenildiğinde yeniden düzenlenir.
- İlerleme, çalışan akış ve kabul ölçütüyle takip edilir. Dosya veya ekranın bulunması, entegrasyonun tamamlandığını göstermez.
- **7 Kasım karşılaştırma sonucu:** Kullanıcı mimari/CI-CD açıklamalarını okuyup genel modelde çakışma görmediğini belirtti ve durumun güncellenmesini istedi. İki uygulama ayrı Compose/servis gruplarıdır; Jenkins otomasyonu yürütür, OpsBridge takip/yönetim panelidir. Hedef CI/CD akışı ve mevcut uygulama sınırları [mimaride](ARCHITECTURE.md) birlikte kaydedildi. Bu sonuç canlı entegrasyonların tamamlandığı anlamına gelmez.

## Şu anda neredeyiz?

**7 Kasım itibarıyla takvimde 2. haftadayız. OpsBridge uygulama temeli ve Sozluk_Uygulamasi yazımına uyarlama tamamlandı; son kaynak kontrollerinde 11 backend ve 11 frontend testi geçti. Aynı sunucuda ayrı OpsBridge + tarayıcıdan admin erişimi modeli kesinleşti. Sıradaki özellik işi sunucudaki yerel depo ve GitHub için commit/branch görünürlüğüdür. Gerçek Git/Jenkins/Docker/health entegrasyonları henüz yok; URL/HTTPS yayını son aşamada yapılacak.**

5 Kasım kaynak incelemesinde OpsBridge HEAD'i `47313af`, Personal_Task_App HEAD'i `faae3d0` idi; bunlar tarihsel kayıtlardır. Kullanıcı, kullandığı Jenkins'in **Windows üzerinde `localhost:8081`** olduğunu doğruladı; o günkü salt okunur job incelemesinde `taskapp-cicd`, `*/main` ve `Jenkinsfile` belirlendi. 7 Kasım'da referans Jenkinsfile yeniden okundu: frontend test/lint, feature/PR ayrımı ve deploy branch koşulu yok. Kimlik doğrulanmış API, webhook/ngrok üzerinden otomatik tetikleme ve güncel başarılı deployment ayrıca doğrulanmadı. Referans uygulamanın canlı kurulum kanıtı açık kaldığından 1. haftanın tamamı bitmiş sayılmaz.

**Etkin çalışma sınırı:** Kullanıcı 8081 Jenkins üzerinden test yapıyor ve müdahale istemiyor. Jenkins servisi/job/ayar değiştirme, yeniden başlatma veya build tetikleme yapılmaz. Başlatılan referans Compose derlemesi bu açıklama üzerine uygulama container'ları yeniden oluşturulmadan iptal edildi; kullanıcı testi sürerken bu deployment'a devam edilmez. OpsBridge doğrulaması kendi `opsbridge` Compose projesi ve 5174/8082/5433 portlarıyla tamamlandı.

| Alan | Durum ve kanıt |
|---|---|
| Spring Boot–React–PostgreSQL temeli | Mevcut; kaynaklar ve kurulum belgeleri hazır |
| ADMIN girişi, session/CSRF, profil CRUD | Mevcut; 5 Kasım'da Docker/Nginx üzerinden gerçek Chrome ile giriş/çıkış ve profil ekleme–düzenleme–silme doğrulandı |
| Katmanlar, DTO/mapper, Router/Axios, migration'lar | Mevcut; 7 Kasım'da Sozluk_Uygulamasi yazımına uyarlandı: controller/service arayüzleri ve impl sınıfları, Lombok DTO, açık React form alanları |
| Otomatik kontroller | 7 Kasım kaynak refaktöründe temiz Maven verify ile 11 backend testi ve jar; 11 frontend testi, Prettier kontrolü ve üretim build'i başarılı. Bu durum güncellemesinde kontroller tekrarlanmadı |
| OpsBridge Docker kurulumu ve gerçek tarayıcı akışı | 5 Kasım'da üç servis, DB healthy/health UP, Java 21, giriş/CRUD/doğrudan URL/dar ekran ve yeniden başlatma sonrası kalıcılık doğrulandı. 7 Kasım kaynak refaktörü yeniden deploy edilmedi; yeni kodun canlı tarayıcı kanıtı yok |
| Personal_Task_App temiz kurulum ve gerçek Jenkins bağlantısı | Kurulum denemesi kullanıcı testi nedeniyle iptal edildi. Yerel Jenkins/job/branch ve mevcut Compose label/DB volume'u belirlendi; başarılı deployment/API/ağ doğrulaması açık |
| 2. haftanın pipeline iyileştirmeleri | Referans kaynaklarda lint aşaması, JUnit yayınlama, commit/build image kimliği, özel label'lar ve Actuator health eksik |
| OpsBridge canlı veri kartları | Henüz yok; adapter'lar dış istek yapmadan başlangıç durumları döndürüyor |
| Build tetikleme, queue/build eşlemesi, işlem geçmişi | Henüz yok |
| Çoklu roller, tarama, rapor ve security gate | Henüz yok; mevcut ADMIN/CSRF/environment temeli bu teslimlerin tamamlandığı anlamına gelmez |

Ayrıntılı mevcut durum ve önceki doğrulamanın sınırları: [STATUS](../.wiki/STATUS.md). Referans uygulama kaynak envanteri: [PERSONAL_TASK_APP](PERSONAL_TASK_APP.md).

## 5 Kasım öncelik güncellemesi — Git görünürlüğü ara adımı

**Kullanıcı isteği:** Ana roadmap'e dönmeden önce projelerin commit geçmişi ve branch'lerini OpsBridge içinde görmek. Kaynak **yerel repo + GitHub**, gönderilmemiş commit ve branch'ler dahil olarak netleşti. Bu ara adım henüz uygulanmadı; ayrı DB, yenilemede onay veya Git'e yazma işlemleri kullanıcı kararı olarak eklenmedi.

[Git görünürlüğü tasarım önerisi](GIT_VISIBILITY.md): yerel repo ve GitHub için backend okuma adapter'ları, Docker backend'e salt okunur repo bağlantısı ve proje detayında kaynak/branch seçimi ile sayfalanmış commit listesi. Ayrı DB yerine ihtiyaç halinde mevcut OpsBridge PostgreSQL'inde önbellek/metadata tutulması; normal veri yenilenmesinin onay istememesi Codex önerisidir. Yereldeki upstream bilgisinin son fetch'e bağlı olduğu gösterilmeli; yeni SHA tek başına force push kanıtı sayılmamalı.

8081 Jenkins'e ve kullanıcının eşzamanlı referans deployment'ına müdahale etmeme sınırı devam eder. İlk yerel repo bağlantısı, kütüphane/komut seçimi ve kalıcı önbellek ihtiyacı uygulama öncesinde netleştirilmeli. Bu ara adım için henüz süre ölçülmedi; mevcut vize hedeflerinin etkilenmediği varsayılmaz ve bu turda takvim tarihleri değiştirilmedi.

## Güncellenmiş sekiz geliştirme haftası

Vize haftası araya alındığından, sekiz geliştirme haftasının takvim bitişi ilk rapordan farklıdır. İlk dört hafta temel DevOps, son dört hafta güvenlik ve final hazırlığı olarak korunur.

| Hafta / hedef aralığı | Mevcut durum | Yapılacak iş | Bitiş kanıtı |
|---|---|---|---|
| 1 — 28 Ekim–3 Kasım; kalanını 5–6 Kasım'da kapatma hedefi | OpsBridge ortam kanıtı tamam; referans uygulama kanıtı açık | Referans uygulama kurulumunu ve erişimini, kullanıcı Jenkins testi sonrasında doğrulama | OpsBridge giriş/CRUD/kalıcılık doğrulandı; ayrıca referans uygulama belgeli kurulumla açılmalı |
| 2 — 4–10 Kasım; pipeline için 7–10 Kasım hedefi | İyileştirmeler kaynaklarda eksik | Tekrarlanabilir pipeline, image kimliği, label ve health | Başarılı kaynak sürümü deploy oluyor; bozuk test/lint deploy'u durduruyor |
| 3 — 11–17 Kasım | Adapter sözleşmeleri hazır; canlı veri yok | Jenkins geçmişi, Docker durumu ve HTTP health okuma | Gerçek veriler ve doğru hata/son veri zamanı panelde görünüyor |
| 4 — 18–22 Kasım | İşlem akışı yok | Yetkili build tetikleme, queue/build eşleme, işlem kaydı ve demo | Panelden başlatılan build, deploy edilen image ve health ile izlenebiliyor |
| Vize — 23–29 Kasım | Kullanıcı tarafından doğrulanan hafta | Demo, anlatım ve gerekli hata düzeltmeleri | Temel DevOps akışı gösterilebiliyor |
| 5 — 30 Kasım–6 Aralık | Temel kimlik doğrulama mevcut | Tehdit modeli, rol/yetki ve erişim sınırları | Yetkisiz API işlemi reddediliyor ve kayda giriyor |
| 6 — 7–13 Aralık | Tarama yok | Trivy raporları, build/image ile ilişkilendirme, gözlem modu | Her build için tarama sonucu veya açık hata durumu saklanıyor |
| 7 — 14–20 Aralık | Gate ve rapor ekranı yok | Bulguları gösterme, açıklanabilir deployment gate'i | Kontrollü bulgu deploy'u durduruyor; kararın nedeni izlenebiliyor |
| 8 — 21–27 Aralık | Belge/test temeli mevcut; final akışı eksik | İkinci proje, temiz kurulum, kabul testleri ve final demosu | İkinci proje yapılandırmayla ekleniyor; final senaryoları doğrulanıyor |

## İlk yapılacaklar: 1. haftayı kapatmak

5 Kasım uygulama sonucu ve kalan sıra:

1. **Tamamlandı:** Docker Engine 29.9.2 ve Compose erişimi, yerel Java/Node ortamı ve iki Jenkins kurulumu ayrıştırıldı. Kullanıcının seçimi 8081 Windows Jenkins.
2. **Tamamlandı:** Mevcut `.env` ve `opsbridge_opsbridge-db` korunarak `docker compose up -d --build --wait` başarılı oldu. API 8082, arayüz 5174, DB 5433; Java container sürümü 21.0.13.
3. **Tamamlandı:** Gerçek Chrome ile giriş/çıkış, profil CRUD, doğrudan proje URL'si, giriş sonrası dönüş, 390px görünüm ve üç OpsBridge servisi yeniden başladıktan sonra veri kalıcılığı doğrulandı. Geçici test profili silindi; önceki profiller korundu.
4. **Açık:** Personal_Task_App'in belgelenmiş kurulumla açıldığını ve DB/API/arayüzün çalıştığını doğrulamak. Kullanıcının Jenkins testi sürerken eşzamanlı Compose/deploy uygulanmaz.
5. **Kısmen tamamlandı:** Jenkins `8081`, job `taskapp-cicd`, branch `main`; mevcut container seçicisi `com.docker.compose.project=personal_task_app`, DB volume'u `personal_task_app_postgres_data`. Kimlik doğrulanmış API ve güncel başarılı pipeline sonucu henüz doğrulanmadı; secret'lar kaydedilmez.
6. **Kısmen tamamlandı:** OpsBridge Nginx → backend → PostgreSQL yolu gerçek isteklerle doğrulandı. Jenkins agent'ı ve referans uygulama arasındaki güncel erişim ile OpsBridge container'ından bu hedeflere erişim kullanıcı testi sonrasında doğrulanmalı.

**Kapanış:** OpsBridge ve referans uygulama gerçekten açılmalı; kurulum ve erişim bilgileri belgelenmeli. Bu koşullar sağlanmadan 1. hafta tamamlandı olarak işaretlenmez. Docker erişimi bekleniyorsa örnek yanıtlarla adapter geliştirmesine hazırlık yapılabilir; canlı doğrulama açık kalır.

## 2. hafta: izlenen uygulamanın pipeline'ı

Bu işlerin asıl uygulama alanı ayrı **Personal_Task_App** deposudur. OpsBridge'in kendi API'sinde Actuator bulunması, referans uygulamanın health ihtiyacını karşılamaz.

- Frontend lint komutunu pipeline'a eklemek; çalışmasını engelleyen mevcut lint hatalarını düzeltmek.
- Backend test raporlarını, başarısız testlerde de Jenkins'te JUnit sonucu olarak yayımlamak.
- Backend/frontend image'larını commit SHA ve Jenkins build numarasıyla ilişkilendirmek. Deployment, o build'de üretilen image'ları kullanmalı; çalışan sürümün hangi commit'ten geldiği bulunabilmeli.
- Compose servislerine `opsbridge.project`, `opsbridge.environment`, `opsbridge.component` label'larını eklemek.
- Referans backend'e Actuator health eklemek; pipeline doğrulamasını ve uygun healthcheck'i buna bağlamak. Erişim adresini gerçek Jenkins ağ düzeninde sınamak.
- Normal build/deploy'u ve kontrollü başarısız test/lint senaryolarını göstermek. Hatalı kaynakta deploy aşamasının çalışmadığına dair sonuç kaydetmek.

**Kapanış:** Başarılı build'in test sonucu, commit'i, image kimliği ve çalışan uygulaması eşleştirilmeli. Frontend test altyapısı bu haftaya ek zorunluluk yapılmaz.

## 3. hafta: OpsBridge'den gerçek veri okumak

Önerilen uygulama sırası Jenkins okuma → HTTP health → Docker durumudur. Her entegrasyon API ve basit ekranıyla tamamlanır.

- Jenkins bağlantısını backend yapılandırmasında tanımlamak; kayıtlı proje/job için son build'leri, sonuç, süre ve kuyruk/çalışma durumunu almak.
- Health isteğini backend'den yapmak; sağlıklı yanıt, sağlıksız yanıt, erişilememe ve zaman aşımını ayırmak.
- Docker servislerini label'larla bulmak; container durumunu health sonucundan ayrı göstermek.
- Her veri kaynağı için son başarılı veri zamanını tutmak; eski sonucu veya bağlantı hatasını anlaşılır göstermek.
- Bağlantı hedeflerini yapılandırılmış sınırlar içinde tutmak ve erişim bilgilerini backend'de kullanmak. Mevcut session/CSRF korumasını sürdürmek.
- Kayıtlı örnek yanıtlarla başarı, geçersiz token, bulunamayan job, erişim hatası ve timeout davranışlarını test etmek; ardından gerçek sistemle doğrulamak.

**Kapanış:** Jenkins/uygulama kapatıldığında panel doğru hata durumunu göstermeli. Başarılı build, çalışan container ve sağlıklı uygulama ayrı bilgiler olarak okunabilmeli.

## 4. hafta: çalışan DevOps demosu

- Yalnızca kayıtlı ve izin verilen job için panelden build başlatmak.
- Kullanıcı isteğini Jenkins'in döndürdüğü queue kaydıyla, ardından gerçek build numarasıyla ilişkilendirmek.
- Çift tıklama/tekrarlanan istek için backend'de tek işlem üretme davranışı kurmak.
- Kullanıcı, proje, job, queue ID, build numarası, zaman ve sonucu işlem geçmişine kaydetmek. Sonucu henüz alınamayan işlemi açıkça göstermek.
- Panelden build başlatma → pipeline → deploy edilen image → container/health akışını birlikte doğrulamak.
- Kurulum belgelerini güncellemek; kısa mimari açıklama ve 3–5 dakikalık demo hazırlamak. Yeni görsel özellikler için bu akışın bitmesini beklemek.

**22 Kasım hazırlık hedefi:** Giriş → proje → canlı build geçmişi → build tetikleme → queue/build ilişkisi → deployment/runtime/health → işlem kaydı akışı çalışmalı. Ayrıca bir bağlantı hatası kontrollü gösterilebilmeli. 23–29 Kasım için yeni büyük özellik planlanmaz.

## Vizeden sonra: güvenlik ve final

**5. hafta — erişim modeli:** Mevcut sistem üzerinden kısa tehdit modeli çıkarmak; VIEWER/OPERATOR/ADMIN davranışlarını ve proje/job erişim sınırlarını tanımlayıp backend'de uygulamak. Secret, ağ/bağlantı sınırları ve container yetkilerini gözden geçirmek. Vize öncesi oluşan işlem geçmişini, reddedilen işlemleri de kapsayacak şekilde genişletmek. Kabul: VIEWER doğrudan API çağrısıyla build başlatamıyor; izin dışı hedef isteği reddediliyor ve kayda giriyor.

**6. hafta — tarama ve kayıt:** İlk plandaki Trivy yaklaşımıyla kaynak ve image taramalarını pipeline'a eklemek. JSON raporlarını artifact olarak saklamak; commit/build/image ve araç sürümüyle ilişkilendirmek. Önce gözlem modunda çalışmak. Kabul: bulgu yok, tarama hatası ve taramanın hiç çalışmaması farklı sonuçlar olarak kaydediliyor.

**7. hafta — rapor ve gate:** Ortak bulgu modelini ve panel görünümünü oluşturmak. İlk politika önerisi, doğrulanmış secret ve düzeltmesi bulunan CRITICAL image bulgularının deployment'ı durdurması; HIGH bulguların başlangıçta uyarı olmasıdır. Kesin kural kümesi uygulama öncesinde belirlenir. Korunan deployment'ta bozuk/eksik tarama sonucu geçiş izni vermemeli. İstisna gerekiyorsa gerekçe, yetki, son tarih ve audit birlikte ele alınır. Kabul: kontrollü bulgu deploy'u durduruyor ve panel kararın nedenini gösteriyor.

**8. hafta — genellenebilirlik ve teslim:** Teknik olarak uygun ikinci projeyi seçmek; desteklediği yeteneklerle ve proje adına özel backend koşulu eklemeden bağlamak. Temiz kurulum, yetkisiz istek, bozuk rapor ve gate senaryolarını yeniden doğrulamak. Belgeler, bilinen sınırlar ve final demosunu tamamlamak. Kabul: ilk projede build/deploy/güvenlik zinciri, ikinci projede yapılandırmayla ekleme gösterilebiliyor.

## Kapsam ve takip

- Entegrasyonlar aynı sunucudaki kaynaklar için geliştirilecek. Farklı sunuculara merkezi SSH/agent erişimi bu modelin dışında; tarayıcıdan uzak kullanım sunucudaki OpsBridge'e yapılır. URL ile yayınlama, özellikler hazırlandıktan sonra ayrı kabul kontrolüyle tamamlanacak.

- Bu teslim: Jenkins + Docker/HTTP health üzerinden çalışan temel DevOps paneli; vizeden sonra rol/yetki, tarama, rapor ve deployment gate'i.
- Kubernetes bu teslimin dışında. Ayrıntılı grafikler, log streaming, Prometheus/Grafana, ek tarayıcılar ve container restart isteğe bağlı işler olarak bekler.
- İlk planda Git görünürlüğü commit/build/image ilişkisiyle sınırlıydı. Kullanıcı 5 Kasım'da önceliği yerel ve GitHub commit/branch görünümüne genişletti; bu ara adım önce ele alınacak. Sonraki build/image eşlemesi korunur. Panelden Git yazma işlemleri ve Compose/image düzenleme fikri ayrı kapsam kararları olarak kalır.
- Her iş kapanırken değişen depo/dosyalar, geçen kontroller, canlı doğrulama ve kalan belirsizlik `.wiki/STATUS.md` içine kaydedilir. OpsBridge değişikliklerinde ilgili `mvn verify`, `npm test`, biçim/build kontrolleri; entegrasyon değişikliklerinde anlamlı başarı/hata senaryoları çalıştırılır.
- Önerilen kontrol noktaları: **10 Kasım pipeline**, **17 Kasım canlı veri**, **22 Kasım tam DevOps demosu**. İlk iki hedefte gecikme görülürse vize kapsamı ve kalan süre yeniden değerlendirilir; görsel ve isteğe bağlı işler öncelik almaz.
- Güvenlik için Aralık takvimi, final teslim tarihi ve haftalık çalışma süresi öğrenildiğinde yeniden ölçülür. Buradaki tarihler kesin süre taahhüdü değildir.

**Sıradaki somut iş:** Kullanıcının yeni önceliği olan yerel/GitHub commit ve branch görünürlüğünün ilk sürümünü tasarım önerisi üzerinden netleştirip uygulamak. Git ara adımı sonrasında ve kullanıcının 8081 Jenkins testi de tamamlandığında, referans uygulamanın DB/API/arayüz ve başarılı deployment kanıtıyla 1. haftayı kapatmak; sonra 2. haftanın lint/JUnit işleri. Mevcut Jenkins kurulumu yeniden oluşturulmaz.
