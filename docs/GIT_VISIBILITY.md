# Git görünürlüğü — uygulanan ilk adım ve kalan kapsam

Güncelleme: **2026-11-13 — Europe/Istanbul**. GitHub branch ve commit geçmişi için ilk uygulama adımı eklendi. Yerel Git ve çalışan Docker sürümünün metadata bağlantısı henüz uygulanmadı. Aşağıdaki geniş kapsamın tamamı bitmiş sayılmaz.

## 13 Kasım — GitHub'dan branch ve commit okuma

Kullanıcı, [paylaştığı yazıdaki](<../javada dockerda çalışan uygulamanın ve o uygulaman.md>) `HttpClient` yaklaşımını yeni öğrendiğini ve hocaya anlatabileceği basitlikte uygulamaya başlamamızı istedi. İlk adım aynı yaklaşımı mevcut katmanlara yerleştirir:

1. `GitHistory.jsx` branch/sayfa seçimini `projectService.getGitHistory` ile backend'e gönderir.
2. `GitControllerImpl`, isteği `GitServiceImpl.getGitHistory` metoduna iletir.
3. Servis, proje profilini repository'den bulur ve `repositoryUrl` içinden `kullanici/depo` kısmını alır.
4. Java `HttpClient`, `https://api.github.com/repos/kullanici/depo/branches` ve `/commits?sha=branch` adreslerine GET isteği gönderir.
5. JSON içindeki kayıtlar açık `for` döngüleriyle okunur. `GitCommitResponse` her commit'in SHA, mesaj, yazar ve tarihini; `GitHistoryResponse` branch listesini, seçimi, sayfa durumlarını ve başarılı okuma zamanını taşır.

Örnek: `GET /api/projects/{id}/git?branch=feature%2Fgit&page=1&branchPage=1`. `branch` verilmezse GitHub'ın o sayfada döndürdüğü ilk branch seçilir; `main` varsayılmaz. İki liste de 20 kayıtla sayfalanır; devam bilgisi GitHub'ın `Link` header'ından alınır. Branch geçmişi ayrı bir SQL tablosuna kopyalanmaz.

`GitHubConfig` yalnızca bağlantı zaman aşımı ayarlanmış `HttpClient` nesnesini oluşturur. Böylece testlerde aynı nesnenin yerine taklit HTTP yanıtları verilebilir. Token isteğe bağlı `GITHUB_TOKEN` ortam değişkeninden okunur ve tarayıcıya gönderilmez. Proje adresi yalnızca HTTPS `github.com/kullanici/depo` biçiminde kabul edilir; istek hedefi sabit `api.github.com` adresidir ve yönlendirmeler takip edilmez.

Pasif proje, hatalı repository adresi ve geçersiz sayfa için 400; bulunamayan proje için 404 döner. GitHub'ın erişim, limit, bağlantı veya JSON hataları açıklayıcı mesajla 502 döner; GitHub'ın 401 yanıtı OpsBridge oturumunu kapatmaz. Arayüz istek değişince eski sonucu temizler; iptal edilmiş isteğin geç gelen yanıtını göstermez. **Yenile**, iki listeyi de ilk sayfadan açar.

Bu adımda adapter/factory, yeni HTTP kütüphanesi, Git geçmişi tablosu veya yeni capability eklenmedi. İzlenen uygulamanın kaynakları ve Jenkins değiştirilmedi. Test ve canlı doğrulama sonuçları [.wiki/STATUS.md](../.wiki/STATUS.md) içindedir.

## Kullanıcının belirlediği kapsam

Kullanıcı, mevcut roadmap'e dönmeden önce projelerin commit geçmişi ve branch'lerini OpsBridge'de görmek istiyor. Kaynak seçimini **yerel repo + GitHub; gönderilmemiş commit ve branch'ler dahil** olarak netleştirdi. Ayrı bir DB'de geçmiş tutmanın ve değişince onay istemenin gerekli olup olmadığını sordu; bunları uygulama kararı olarak belirtmedi.

Bu istek Git'e yazma işlemleri için talimat değildir. 8081 Jenkins üzerindeki kullanıcı testine müdahale etmeme sınırı devam eder.

**7 Kasım kullanıcı kararı:** OpsBridge izlenen uygulamayla aynı sunucuda çalışacak; kullanıcı uzaktan tarayıcıyla OpsBridge'e admin girişi yapacak. Bu nedenle “yerel repo” sunucudaki depodur. Gönderilmemiş commit/branch kapsamı bu depoda bulunanlarla sınırlıdır; uzaktaki kullanıcının bilgisayarındaki gönderilmemiş çalışmalar tarayıcı üzerinden kendiliğinden alınmaz. GitHub kaynağı korunur. URL üzerinden yayınlama en son yapılacak ayrı iştir. Ayrıntı: [çalışma modeli](ARCHITECTURE.md).

## Geniş kapsam için önceki öneriler ve kalan işler

1. Seçili projenin yerel deposundan commit'leri, yerel branch'leri, HEAD ve bilinen upstream referanslarını backend okur. `git log` ve `git for-each-ref` bu veriler için kullanılabilir; komutlar shell metni birleştirmeden ayrı argümanlarla ve yapılandırılmış depo hedefleriyle çalıştırılır. Git çalıştırılabilirinin backend ortamında bulunması uygulama sırasında sağlanmalıdır; mevcut Dockerfile Git kurmuyor.
2. GitHub branch ve commit okuması 13 Kasım'da doğrudan `GitServiceImpl` içinde uygulandı. İlk önerideki ayrı adapter katmanı kurulmadı. Özel depolar için salt okuma yetkili kimlik bilgisi backend yapılandırmasında tutulur; tarayıcıya taşınmaz.
3. React'te proje detayına Git görünümü eklenir: kaynak seçimi, branch seçimi, commit SHA/mesaj/yazar/tarih, sayfalama ve son başarılı okuma zamanı. Kaynaklardan biri erişilemezse diğeri çalışabilir; eski sonuç güncel veri gibi sunulmaz.
4. Upstream farkı gösterilecekse yereldeki `origin/main` gibi referansların son fetch'ten kalan bilgi olduğu açıkça belirtilir. Yalnızca yerel/uzak son SHA'nın farklı olması, kaç commit'in gönderilmediğini veya geçmişin yeniden yazıldığını kanıtlamaz. Upstream bulunmaması ve karşılaştırma için yeterli commit nesnesinin bulunmaması ayrı durumdur. İlk sürümde kullanıcı deposunda otomatik fetch/pull yapılması önerilmez; güncel iki kaynak arasında kesin karşılaştırma gerekiyorsa OpsBridge'e ait ayrı Git kopyası ve nesne eşitlemesi ayrıca tasarlanır.

Mevcut backend Docker'da olduğu için host'taki depo kendiliğinden görünmez. İlgili sunucu deposu, backend container'ına **salt okunur bind mount** ile bağlanabilir; örneğin yapılandırılmış bir `/repositories/...` yolu. Geliştirme makinesindeki Windows yolu bu host bağlantısının yerel denemesidir. GitHub'dan clone edilen ayrı bir kopya, sunucudaki depoya ait gönderilmemiş commit'leri içermez; o yerel kaynağın ayrıca okunması gerekir. Bu model için başka bir bilgisayara erişen host agent tasarlanmaz.

Mevcut projede `repositoryUrl` ile GitHub okuması var; yerel depo bağlantısı yok. Yerel okuma mevcut controller/service ve response DTO düzeninde geliştirilebilir. Yeni adapter katmanı, capability, kütüphane veya migration peşinen gerekli kabul edilmez.

Çalışan Docker sürümü için build sırasında üretilen tam commit SHA'sı ve build branch'i ayrıca okunmalıdır. GitHub branch ucunu veya sunucudaki yerel HEAD'i çalışan sürüm diye göstermek doğru değildir. İzlenen uygulamanın build metadata'sı bağlanmadan sürüm eşitliği, ileride/geride olma veya kaç commit fark bulunduğu sonucu üretilmez.

## Klasör bağlantısı ve Jenkins tutarlılığı

Kullanıcı, uygulama açılışında klasör seçimi gerekip gerekmediğini ve bunun Jenkins tutarlılığını sağlayıp sağlamayacağını sordu. **Codex önerisi:** Yerel repo bağlantısını proje profiline bir kez tanımlayıp sonraki açılışlarda kullanmak; her açılışta klasör seçimini zorunlu tutmamak. Geçerli Git deposu ve seçili GitHub repository kimliğiyle eşleşen remote kontrolü yapılabilir. Mevcut Docker kurulumunda backend'e salt okunur mount ayrıca gerekir; tarayıcıdaki klasör seçimi tek başına container'a dosya erişimi sağlamaz. Bu kullanıcı akışı henüz uygulanmadı veya kesinleştirilmedi.

Klasör bağlantısı tek başına Jenkins tutarlılık kanıtı değildir. Aynı repository ve hedef branch için yerel HEAD/upstream, GitHub branch ucu ve ilgili Jenkins build'inin gerçekten checkout ettiği commit karşılaştırılmalıdır. Jenkins Git plugin'i build commit'ini `GIT_COMMIT` ile sağlar; verinin gerçek build/checkout kaydından alınması gerekir. Yerelde C3, GitHub'da C2 ve build kaydında C2 varsa gönderilmemiş yerel çalışma olabilir; bu Jenkins hatası değildir. Build kaydı C1 iken GitHub C2'ye ilerlediyse daha yeni commit için build henüz görülmüyor denebilir; bu da tek başına Jenkins arızası kanıtı değildir. Karşılaştırma kaynaklarının okuma zamanları ve build başlangıcından sonra branch'in ilerlemiş olabileceği hesaba katılır. Çalışan uygulama için ayrıca image/deployment kimliği eşlemesi gerekir.

Kaynak: [Jenkins Git plugin commit/branch değişkenleri](https://plugins.jenkins.io/git/#environment-variables). Bu görüşmede Jenkins'e istek veya yapılandırma işlemi yapılmadı.

## Veritabanı ve yenileme önerisi

**Sadece listeleme için ayrı veritabanı veya bütün Git geçmişinin SQL kopyası gerekli değil.** İlk sürümde asıl kaynaklar yerel Git deposu ve GitHub olur; ekran açılışında/yenilemede okuma ve kısa süreli önbellek yeterli olabilir. Kullanım sıklığı veya geçmişte gözlenen durumu saklama ihtiyacı oluşursa OpsBridge'in kendi PostgreSQL'i içinde ek tablolar düşünülebilir. İzlenen uygulamanın DB'sine yazmak gerekmez.

Kalıcı önbellek gerekirse repository kimliği, tam commit SHA'sı ve gerekli commit metadatası; branch adı/son SHA; son başarılı eşitleme zamanı/hata bilgisi tutulabilir. Aynı commit birden fazla branch geçmişinde bulunabilir; commit'e tek bir branch aitmiş gibi kolon eklemek doğru bir model değildir. Bu kayıtlar kaynak geçmişin yerine geçmez ve tek başına Git yedeği sayılmaz.

GitHub isteklerinde kısa önbellek ve desteklenen endpoint'lerde ETag/koşullu GET önerilir. Daha sonra push/create/delete webhook'ları yenileme sinyali olabilir; webhook'u tüm geçmişin eksiksiz kopyası olarak değerlendirmemek gerekir. İlk sürüm için yeni webhook/ngrok kurulumu zorunlu tutulması önerilmez.

## “Git geçmişi değişti, onaylıyor musunuz?” davranışı

| Durum | Önerilen davranış |
|---|---|
| Yeni commit veya branch geldi | Görüntüyü/önbelleği yenile; onay isteme |
| Branch silindi | Güncel kaynak görünümünü yenile; gerekiyorsa gözlenen silinmeyi kaydet |
| Force push veya geçmişin yeniden yazılması doğrulandı | Bilgilendirme/olay kaydı göster; kaynak geçmişi kendi DB kopyamızla geçersiz sayma |
| OpsBridge kullanıcının isteğiyle push, reset, rebase, branch silme gibi işlem yapacak | İşlemin etkisine uygun yetki ve onay akışını ayrı tasarla; bu okuma talebinin kapsamına ekleme |

Yalnızca branch ucunun SHA'sının değişmesi normal yeni commit'te de olur; bunu otomatik “geçmiş bozuldu” uyarısına çevirmemek gerekir. Ekranın yeni veriyi göstermesi için onay istemek, Git'te daha önce yapılmış bir değişikliği durdurmaz veya geri almaz.

## Kabul örnekleri — uygulama öncesi öneri

- Push edilmemiş yerel branch/commit yerel kaynakta görünür; GitHub görünümüyle karıştırılmaz.
- Seçili branch'in geçmişi sayfalanır; geçmiş tek seferde belleğe yüklenmez.
- Geçersiz yerel yol, özel depoya yetkisiz erişim, GitHub rate limit/ağ hatası ayrı ve anlaşılır gösterilir.
- Görüntüleme sırasında kaynak repo, çalışma dosyaları ve Jenkins değişmez.
- Verinin kaynağı ve son başarılı okuma zamanı görünür; eski upstream bilgisi güncel GitHub karşılaştırması diye sunulmaz.

## Resmî kaynaklar

- [Git log](https://git-scm.com/docs/git-log), [ref listeleme](https://git-scm.com/docs/git-for-each-ref), [fetch ve upstream](https://git-scm.com/docs/git-fetch).
- [GitHub commit API](https://docs.github.com/en/rest/commits/commits), [branch API](https://docs.github.com/en/rest/branches/branches).
- [GitHub sayfalama ve Link header'ı](https://docs.github.com/en/rest/using-the-rest-api/using-pagination-in-the-rest-api).
- [GitHub önbellek/koşullu istek ve webhook önerileri](https://docs.github.com/en/rest/using-the-rest-api/best-practices-for-using-the-rest-api), [push olayı ve forced/deleted alanları](https://docs.github.com/en/webhooks/webhook-events-and-payloads#push).
- [Docker salt okunur bind mount](https://docs.docker.com/engine/storage/bind-mounts/#use-a-read-only-bind-mount).
