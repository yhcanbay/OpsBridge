# OpsBridge — Revize 8 Haftalık DevSecOps Proje Planı

## Net değerlendirme

Mevcut planın ana fikri doğru, ancak vize öncesi ve sonrası ayrımı hocanın söylediği sırayla tam örtüşmüyor. Önceki planda runtime/health görünürlüğü 6. haftaya bırakılırken güvenlik hazırlığı ve rol modeli daha erken başlıyor; ayrıca 5–7. haftalarda aynı anda tarama, rapor ayrıştırma, güvenlik politikası, runtime metrikleri, ikinci proje ve saldırı yüzeyi testleri isteniyor. Siber güvenliğe yeni başlayan tek geliştirici için bu dağılım gereğinden yoğun ve öğrenme sırası da ters.[^1]

Planın yeniden kurulması gerekir: **ilk dört hafta çalışan DevOps paneli**, sonraki dört hafta ise **bu panelin ve yönettiği teslimat hattının ölçülebilir güvenlik kontrolleriyle DevSecOps’a dönüştürülmesi** olmalıdır. “Her projeye uyar” iddiası da daraltılmalı; doğru ifade, **Jenkins ile build edilen, Docker veya HTTP health bilgisi sağlayan projeleri yapılandırma tabanlı adapter’larla destekleyen genişletilebilir panel** olmalıdır.

## Depo incelemesi

[Personal_Task_App](https://github.com/yhcanbay/Personal_Task_App) uygun bir izlenen proje adayıdır. Depoda Jenkinsfile, iki Compose dosyası, Jenkins klasörü ve `taskApp` uygulaması bulunuyor; backend Spring Boot/Java 21, frontend ise React/Vite tabanlıdır.

Mevcut Jenkins pipeline’ı checkout, backend test, frontend build, Docker Compose deployment ve `/api/settings` üzerinden doğrulama aşamalarını zaten içeriyor. Aşamaların sırası, backend testi veya frontend build başarısız olduğunda deployment’a geçilmesini engelliyor; `disableConcurrentBuilds()` ise aynı job’ın eşzamanlı çalışmasını kapatıyor.

Bu başlangıç noktası iyi olsa da aşağıdaki açıklar proje için doğrudan çalışma maddesidir:

- Frontend’de `lint` komutu bulunmasına rağmen Jenkins pipeline’ında çalıştırılmıyor; ayrıca `package.json` içinde frontend test komutu yok.
- Doğrulama bir işlev endpoint’i olan `/api/settings` üzerinden yapılıyor; backend’de Actuator bağımlılığı yok. Spring Boot Actuator’ın `/actuator/health` endpoint’i uygulama sağlığı için tasarlanmıştır ve ayrıntıların kimlere gösterileceği sınırlandırılabilir.[^2]
- Compose dosyasında veritabanı kullanıcı adı ve parolası düz metin olarak bulunuyor; servislerde proje/ortam etiketi ve backend healthcheck yok, ayrıca sabit container adları kullanılıyor.
- Backend ve frontend runtime imajlarında ayrıcalıksız `USER` tanımlanmamış; taban imajlar sürüm etiketiyle kullanılıyor fakat digest ile sabitlenmemiş.
- Spring Security bağımlılığı izlenen uygulamanın Maven yapılandırmasında görünmüyor. Ancak OpsBridge ayrı bir ürün olacağı için panelin kimlik doğrulaması izlenen uygulamaya eklenmemeli; OpsBridge backend’inde uygulanmalıdır.

## Ürün sınırı

İki farklı ürün birbirine karıştırılmamalıdır:

| Bileşen | Sorumluluk | Bu projede yapılmayacak |
|---|---|---|
| **OpsBridge** | Proje profili, Jenkins görünürlüğü/tetikleme, Docker ve health görünürlüğü, güvenlik raporlarının sunulması, yetkilendirme ve audit | Jenkins veya Docker’ın yerine geçmek |
| **Personal_Task_App** | OpsBridge tarafından izlenen ve pipeline’dan geçen birincil örnek uygulama | OpsBridge’e dönüştürülmek |
| **İkinci proje** | Yapılandırma tabanlı genellenebilirliği finalde doğrulamak | Tüm özellikleri desteklemek zorunda olmak |

Başarı iddiası şu olmalıdır:

> OpsBridge, koduna proje adına özel koşul eklenmeden, Jenkins job yolu, Docker label seçicisi, health URL’si ve desteklenen yetenekler tanımlanarak ikinci bir projeyi ekleyebiliyor.

Bu ifade gerçekçi ve savunulabilirdir. “Her teknoloji ve her CI/CD sistemi destekleniyor” iddiası ise sekiz haftada kanıtlanamaz.

## Önerilen mimari

Tek Spring Boot backend ve tek React frontend yeterlidir; mikroservis mimarisi bu süre için gereksizdir.

```text
React Panel
    |
    v
OpsBridge Spring Boot API
    ├── Auth/RBAC
    ├── Project Profile Service
    ├── Jenkins Adapter
    ├── Runtime/Health Adapter
    ├── Security Report Parser
    └── Audit Service
              |
          PostgreSQL

Yönetilen ortam
    ├── Jenkins
    ├── Docker Engine / Compose
    └── Personal_Task_App
```

Frontend hiçbir zaman Jenkins token’ı veya Docker bağlantı bilgisini almamalıdır. Jenkins’in REST-benzeri Remote Access API’si build bilgisi okumayı, build tetiklemeyi ve queue kayıtlarını sorgulamayı destekler; bu nedenle bütün Jenkins çağrıları OpsBridge backend’indeki adapter üzerinden yürütülmelidir.[^3]

Docker erişimi laboratuvar ortamıyla sınırlandırılmalıdır. Docker, daemon kontrolünün yalnızca güvenilir kullanıcılara verilmesini; uzaktan erişimde SSH veya karşılıklı TLS kullanılmasını önerir. Docker API’sini kimlik doğrulamasız TCP ile açmak, panelden keyfî container yönetimi sunmak veya kontrolsüz socket erişimi vermek bu projenin kapsamına girmemelidir.[^4][^5][^6]

### Temel veri modeli

```text
ProjectProfile
- id
- name
- repositoryUrl
- environment
- jenkinsBaseUrlRef
- jenkinsJobPath
- containerLabelSelector
- healthUrl
- capabilities
- enabled

OperationRecord
- id
- userId
- projectId
- operationType
- queueId
- buildNumber
- result
- requestedAt
- completedAt

SecurityScan
- id
- projectId
- commitSha
- buildNumber
- imageRef
- scanType
- tool
- toolVersion
- status
- reportPath
- createdAt
```

`capabilities` alanı en az `BUILD_VIEW`, `BUILD_TRIGGER`, `CONTAINER_VIEW`, `HEALTH_VIEW` ve `SECURITY_VIEW` değerlerini desteklemelidir. İkinci projede bir yetenek yoksa ekranda “desteklenmiyor” gösterilmeli; “hata” gösterilmemelidir.

## Sekiz haftalık plan

### Hafta 1 — 28 Ekim–3 Kasım

**Hedef:** İzlenen uygulamayı doğrulamak ve OpsBridge’in en küçük iskeletini kurmak.

- Personal_Task_App’i temiz ortamda çalıştır; Java, Node, Docker ve gerekli environment değişkenlerini kaydet.
- Jenkinsfile, Compose ve Dockerfile’ların mevcut davranışını kısa bir teknik envantere dönüştür.
- OpsBridge için Spring Boot + React + PostgreSQL iskeleti oluştur.
- `ProjectProfile` CRUD ve tek bir Personal_Task_App profili ekle.
- Jenkins/Docker/health erişimlerini interface arkasına al; henüz tam özellik yazma.
- Basit bir `ADMIN` girişi kullan; bu aşamada ayrıntılı RBAC ve JWT yenileme akışını büyütme.

**Teslim kanıtı:** OpsBridge açılıyor, kullanıcı giriş yapıyor, kayıtlı proje listeleniyor ve Personal_Task_App belgelenmiş komutla çalışıyor.

**Kabul ölçütü:** Yeni bir proje adı backend koduna `if/else` eklenmeden veri tabanına kaydedilebiliyor.

### Hafta 2 — 4–10 Kasım

**Hedef:** Personal_Task_App pipeline’ını tekrarlanabilir ve izlenebilir hâle getirmek.

- Pipeline’a frontend lint ekle; frontend test altyapısını bu haftanın zorunlu kapsamına alma.
- Backend test raporlarını Jenkins artifact/JUnit sonucu olarak sakla.
- Docker imajlarını en az `commit SHA + build number` ile etiketle; yalnızca `latest` kullanma.
- Compose servislerine `opsbridge.project`, `opsbridge.environment` ve `opsbridge.component` etiketlerini ekle.
- `/actuator/health` ekle ve yalnızca gerekli health bilgisini dışarı aç.
- Başarısız backend testi ve frontend lint durumunda deployment’ın çalışmadığını göster.

**Teslim kanıtı:** Başarılı commit deploy olur; bozuk test veya lint deployment’ı durdurur; çalışan sürüm commit/build numarasıyla bulunabilir.

**Kabul ölçütü:** Pipeline aynı commit için tekrar çalıştırıldığında aynı kaynak sürümüyle ilişkilendirilebilir çıktı üretir.

### Hafta 3 — 11–17 Kasım

**Hedef:** OpsBridge’den gerçek Jenkins ve uygulama durumunu okumak.

- Jenkins adapter ile son build’leri, sonucu, süresini ve çalışıyor/kuyrukta durumunu getir.
- Docker servislerini sabit container adına göre değil Compose label’larıyla bul.
- Container durumu ile HTTP health durumunu ayrı göster.
- Jenkins erişilemiyor, token geçersiz, job bulunamadı ve health timeout durumlarını ayır.
- Her kartta “son başarılı veri alma zamanı” göster; eski veriyi güncelmiş gibi sunma.
- Adapter katmanını kayıtlı örnek JSON yanıtlarıyla birim test et.

**Teslim kanıtı:** Panel, gerçek Jenkins build geçmişini ve Personal_Task_App runtime/health durumunu gösteriyor.

**Kabul ölçütü:** Jenkins veya uygulama kapatıldığında panel yanlış biçimde yeşil durum göstermiyor.

### Hafta 4 — 18–24 Kasım

**Hedef:** Vize için çalışan DevOps MVP’sini tamamlamak.

- Panelden yalnızca izin verilen job’ı build etme özelliği ekle.
- POST isteğinin döndürdüğü Jenkins queue kaydını izle; son build numarasını tahmin ederek işlemle eşleştirme yapma.
- Çift tıklamaya karşı idempotency/işlem kilidi uygula.
- Kullanıcı, proje, job, queue ID, build numarası ve sonucu audit tablosuna yaz.
- Kurulum README’si, mimari şema ve 3–5 dakikalık vize demosu hazırla.
- Görsel ayrıntıları yalnızca çekirdek akış tamamlandıktan sonra düzenle.

Jenkins API, build tetikleme ve build queue bilgisini programatik olarak sunar; bu nedenle kullanıcı isteği → queue ID → build number zinciri savunmada gösterilebilir.[^3]

**Vize demosu:** Proje profilini aç → mevcut build’leri göster → panelden build başlat → queue/build ilişkisini izle → deploy sonucunu ve health durumunu göster → Jenkins’i kapatıp doğru hata durumunu göster.

**Vize bitiş tanımı:** OpsBridge henüz DevSecOps ürünü olmak zorunda değildir; ancak yapılandırma tabanlı çalışan, gerçek Jenkins ve Docker/health verisi kullanan bir DevOps paneli olmalıdır.

### Hafta 5 — 25 Kasım–1 Aralık

**Hedef:** Güvenlik öğrenme tabanını kurmak ve saldırı yüzeyini küçültmek.

- Kısa tehdit modeli çıkar: varlıklar, aktörler, güven sınırları, kötüye kullanılabilecek işlemler ve karşılık gelen kontroller.
- `VIEWER`, `OPERATOR`, `ADMIN` rollerini backend’de uygula; yalnızca buton gizlemeye güvenme.
- Jenkins token’ını environment/secret mekanizmasına taşı; veritabanında tutulacaksa düz metin saklama.
- Proje dışı job yolu, keyfî health URL’si ve başka projeye ait ID gönderme senaryolarını reddet.
- Compose içindeki geliştirme parolalarını `.env`/secret yaklaşımına taşı ve örnek yapılandırmayı gerçek değerden ayır.
- Dockerfile’larda uygun olduğunda ayrıcalıksız kullanıcıya geç; gereksiz port ve yetkileri azalt.

**Öğrenme çıktısı:** “Zafiyet taraması yaptım” demeden önce tehdit, risk, kontrol, bulgu, yanlış pozitif ve istisna kavramlarını kendi proje örnekleriyle açıklayabilmek.

**Kabul ölçütü:** VIEWER doğrudan API çağrısıyla build başlatamıyor; reddedilen istek audit kaydına dönüşüyor.

### Hafta 6 — 2–8 Aralık

**Hedef:** Tek bir araç ailesiyle anlaşılır güvenlik taramaları eklemek.

İlk sürümde ana araç olarak **Trivy** kullanılması önerilir. Trivy; repository/filesystem, container image, dependency/CVE, secret ve IaC/Dockerfile yanlış yapılandırma taramalarını destekler. Misconfiguration taramasının bazı hedeflerde açıkça etkinleştirilmesi gerektiği için kullanılan komut ve Trivy sürümü rapora kaydedilmelidir.[^7][^8][^9]

- Kaynak ağacında `trivy fs` ile vulnerability, secret ve misconfiguration taraması çalıştır.
- Build edilen backend ve frontend imajlarını `trivy image` ile tara.
- JSON raporlarını Jenkins artifact olarak sakla.
- Raporu `commitSha`, `buildNumber`, `imageRef`, araç ve araç sürümüyle ilişkilendir.
- Önce “gözlem modu”nda çalıştır: bulguları kaydet fakat hemen tüm pipeline’ı engelleme.
- Gerçek kimlik bilgisi commit etmeden, aracın güvenli test girdisiyle secret tespitini göster.

Ayrı bir Gitleaks entegrasyonu ancak Trivy akışı bitmişse eklenmelidir. Gitleaks git geçmişi ve dosyalardaki hardcoded secret’ları tespit etmek için uygundur; ancak sekiz haftalık projede ikinci tarayıcı zorunlu değildir.[^10][^11]

**Teslim kanıtı:** Aynı build için kaynak ve image raporları üretilebiliyor; “tarama çalışmadı” ile “bulgu yok” farklı durumlar olarak kaydediliyor.

**Kabul ölçütü:** Rapor dosyası bozuk veya eksikse sistem güvenli sonuç üretmiş gibi davranmıyor.

### Hafta 7 — 9–15 Aralık

**Hedef:** Bulguları panelde anlamlandırmak ve dar bir security gate uygulamak.

- Trivy JSON çıktısını OpsBridge’de ortak bulgu modeline dönüştür.
- Panelde tarama durumu, önem seviyesi, hedef, paket/dosya, CVE veya kural, bulunan ve düzeltilmiş sürümü göster.
- İlk gate politikasını dar tut: doğrulanmış secret, `CRITICAL` ve düzeltmesi bulunan image vulnerability veya seçilmiş ciddi Dockerfile misconfiguration deployment’ı durdursun.
- `HIGH` bulguları ilk etapta uyarı olarak göster; bütün eski teknik borcu bir anda engelleme.
- İstisna gerekiyorsa gerekçe, veren kullanıcı ve son geçerlilik tarihi zorunlu olsun.
- Hatalı/çalışmamış taramada korunan deployment için **fail closed** davranışı uygula.

**Kabul ölçütü:** Kontrollü test bulgusu deployment’ı durduruyor; istisna kaldırıldığında aynı bulgu yeniden engel oluyor; bu karar panelde ve audit kaydında görülebiliyor.

**Sınır:** Bu çalışma penetration test, sıfır-gün keşfi veya tam SAST ürünü değildir. Hazır araç bulgularını güvenilir biçimde pipeline kararı ve izlenebilir kanıta dönüştüren bir DevSecOps entegrasyonudur.

### Hafta 8 — 16–22 Aralık

**Hedef:** Genellenebilirliği ve teslim edilebilirliği kanıtlamak.

- React_Ders veya Melodify’dan teknik olarak daha kolay olanı ikinci proje seç.
- Yalnızca desteklediği yetenekleri tanımla; örneğin build + security report varsa container/health zorunlu olmasın.
- OpsBridge koduna proje adını içeren özel koşul eklemeden profille bağla.
- Temiz kurulum testi yap; mümkünse farklı dizin veya VM kullan.
- Otomatik testleri, yetkisiz istekleri, bozuk raporu ve security gate senaryolarını yeniden çalıştır.
- README, mimari kararlar, tehdit modeli, güvenlik politikası, bilinen sınırlar ve demo videosunu tamamla.

**Final demosu:** Birinci projede normal build/deploy → health görünürlüğü → güvenli test bulgusuyla deployment engeli → yetkisiz tetiklemenin reddi → ikinci projenin farklı yetenek setiyle eklenmesi.

**Final bitiş tanımı:** Mevcut bir uygulama test, build, image, tarama, gate ve deployment zincirinden geçiyor; OpsBridge bu zinciri ve runtime/health durumunu gösteriyor; yetkisiz işlemleri reddediyor; ikinci proje yapılandırmayla eklenebiliyor.

## Vize ve final kapsamı

| Alan | Vizeye kadar zorunlu | Finalde zorunlu | Vakit kalırsa |
|---|---|---|---|
| Proje profili | Tek proje, yapılandırma tabanlı | İkinci proje | Profil içe/dışa aktarma |
| Jenkins | Geçmiş, durum, tetikleme, queue/build eşleme | Güvenlik gate sonucu | Log streaming |
| Runtime | Container durum + health | Güvenli bağlantı sınırları | CPU/RAM geçmiş grafikleri |
| Kimlik | Basit admin girişi | VIEWER/OPERATOR/ADMIN + audit | Refresh token ayrıntıları |
| Güvenlik | Yalnızca güvenli varsayımlar | Trivy fs/image, JSON rapor, dar gate | Gitleaks, ZAP baseline |
| Operasyon | Görüntüleme ve build tetikleme | Yetkili işlemler | Container restart |
| Gözlemlenebilirlik | Son veri zamanı ve hata ayrımı | Operasyon + güvenlik geçmişi | Prometheus/Grafana |

## Güvenlik öğrenme yolu

Siber güvenlik bilgisi olmaması bu proje için engel değildir; fakat araç sayısını artırmak yerine her kontrolde şu beş soru cevaplanmalıdır:

1. **Neyi koruyor?** Örneğin Jenkins token’ı, deployment yetkisi veya Docker host’u.
2. **Tehdit nedir?** Token sızıntısı, yetkisiz build, SSRF ile keyfî health URL’si veya tehlikeli Docker erişimi.
3. **Kontrol nedir?** Allowlist, backend yetkilendirmesi, secret saklama, ağ sınırı veya security gate.
4. **Kontrol nasıl bozulur?** Geçersiz token, başka proje ID’si, bozuk rapor veya zararsız test secret’ı.
5. **Kanıt nedir?** Otomatik test, audit kaydı, Jenkins artifact’ı ve demo ekranı.

Her hafta yalnızca gereken konuyu öğrenmek daha verimlidir: 5. hafta tehdit modeli ve erişim kontrolü, 6. hafta tarayıcı ve rapor biçimi, 7. hafta risk politikası ve istisna yönetimi, 8. hafta doğrulama ve kalan riskler.

## Kritik riskler

| Risk | Etki | Önlem |
|---|---|---|
| Paneli “her projeyi destekler” diye büyütmek | Sekiz haftada tamamlanamama | Jenkins + Docker/HTTP uyumluluğuyla sınırla |
| İlk dört haftayı UI tasarımına harcamak | Vizede gerçek entegrasyon gösterememe | Backend → adapter → basit UI sırasını koru |
| Çok sayıda güvenlik aracı eklemek | Bulguları anlayamama, demo kırılganlığı | Önce Trivy; Gitleaks ve ZAP opsiyonel |
| Docker socket’i kontrolsüz açmak | Host üzerinde yüksek yetkili saldırı yüzeyi | İzole lab, yerel socket izinleri veya SSH/TLS; internetten erişim yok[^4][^5] |
| Her `HIGH/CRITICAL` bulguyu doğrudan engellemek | Mevcut teknik borç nedeniyle pipeline’ın sürekli kırılması | Önce gözlem modu, sonra dar ve açıklanabilir gate |
| Tarama hatasını “temiz” saymak | Yanlış güven hissi | `PASSED`, `FAILED`, `ERROR`, `NOT_RUN` durumlarını ayır |
| İkinci projeyi erken eklemek | Ana akış tamamlanmadan bağlam sorunu | Yalnızca 8. haftada genellenebilirlik kanıtı olarak ekle |

## Yapılmayacaklar

- Kubernetes, Helm, çoklu cloud veya ikinci CI sağlayıcısı
- Sıfırdan zafiyet tarayıcı geliştirmek
- Otomatik exploit veya aktif saldırı sistemi
- Docker yönetim portunu internete açmak
- Tam SIEM, dağıtık tracing veya kurumsal SSO
- Yapay zekânın bulguları otomatik kapatması ya da deployment politikasını tek başına değiştirmesi
- Vize öncesi ayrıntılı dashboard grafikleri ve animasyonlar

## Haftalık çalışma düzeni

Haftada 10–12 saat ayrılabiliyorsa her hafta yaklaşık şu düzen uygulanabilir:

- 2 saat: Konuya yönelik resmi dokümantasyon ve küçük deney.
- 5–6 saat: Backend, adapter ve pipeline geliştirmesi.
- 2 saat: Basit frontend görünümü.
- 1–2 saat: Hata senaryosu, otomatik test, ekran görüntüsü ve kısa not.

Her pazar haftayı kapatmak için yalnızca dört soruya cevap verilmelidir: Ne çalışıyor? Hangi hata kontrollü ele alınıyor? Bunu hangi test veya demo kanıtlıyor? Sonraki haftaya taşınan tek kritik eksik nedir?

## Öncelik sırası

Zaman daralırsa aşağıdaki sıra korunmalıdır:

1. Personal_Task_App’in güvenilir pipeline’ı.
2. Jenkins geçmişi ve queue/build eşleme.
3. Container + health görünürlüğü.
4. Backend yetkilendirmesi ve audit.
5. Trivy raporu ve açıklanabilir deployment gate’i.
6. İkinci projenin profille eklenmesi.
7. Gelişmiş grafikler, restart, bildirim, ZAP, Gitleaks ve Prometheus/Grafana.

Bu revizyonla dönem projesinin hikâyesi nettir: **vizede çalışan bir DevOps kontrol paneli; finalde güvenlik kontrolleri, izlenebilir kararlar ve kontrollü yetkilendirme eklenmiş DevSecOps paneli.**

---

## References

1. [opsbridge-8-haftalik-devsecops-plani.md](https://ppl-ai-file-upload.s3.amazonaws.com/web/direct-files/attachments/157103789/50c563e4-5752-4cf8-915c-7ad8bd5f948d/opsbridge-8-haftalik-devsecops-plani.md?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Checksum-Mode=ENABLED&X-Amz-Credential=ASIA2F3EMEYEX7CP45UH%2F20261027%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20261027T202541Z&X-Amz-Expires=3254&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEC0aCXVzLWVhc3QtMSJHMEUCIQD4xXnaeerzLfSVszQxU9%2BN4T5ge6iyQjMm0qExcUplkgIgIcz4Mc5TuwcD%2BP%2BkSqslajeNxfhckh8dYPyCHm0EgywqiQUI9f%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FARABGgw2OTk3NTMzMDk3MDUiDLNkJtClJpb6GMyMdCrdBFGNfdt1yDxN1BWC5NmZfZc2AQpDKMBm0z19S7K8fpMeMHR523Q0WVCeKEU8rlbRLo79iA46A%2B%2BPtWRde8dyjpJy0UvEV08jFBp2Yu4msOqDej21zasM%2FYQQd06PR3WUSkyOC1%2Bap7D8bUWlq6nzzD6HvmAvVO1RfrwU5Nn2gSOUrLrv5dvMPBc5mP7KwL9%2Flgm8%2BiGwqnt6BhTXjgeBve8HMTsPsCellUvMoNvBweXIL%2Fd7NHCZKUI8HYrwGGFSdF6CqFzG1RO1Zc3nID40N6riltGOxtABa0DBkmLkMTOlYAkYw9RYVirBkeblsvLMJ%2FaQJ0otSMLU30tSv78dDuGX5sx1czwWUaJiifLGtiJQAL1FFZZgih5fLIGjvc3LiNAo7L2Bcj1aCfx1r406GqjeEN2OdrETYuAoctpLV2Ke%2FV9kwWQ0ZcDG3m5YnRU2GBxdkGSiz81dRIRn2%2FT%2BcbJ3qPRtDXmimFrgh1wSPRv64V2rbXmMwFBg6fe9UIjbWILLdJxV7BtTfsKJfIxHI1Rf%2BgUKLISmQPyVbvTzinKws3J2VeH7s%2FtdG3hBKjhrOZIjP%2B%2FrHHA9%2FDwLxc3XTLNGlLH7imqVtD3b2Fz7ktJjCWEmAzgJT4xpd5Z2b%2BszhE3%2FpqEnB6Mjw3uLPY9O8wmBy3uPa8XDg%2Ffs4BnXznXSfGphbrxuWEjECDN2sE3QR5SsXNpZLnf8YDX1KqLFtqfmVXLOwLi6th5TCVdvc8DR8bkjJwrrJOezFfgR7uQ5MHWvg%2FKFEK%2BXiHxsNk10eTVZrasy0%2BfCbJhTSOyRMKiPhNcGOpgBBb36pu3ej%2FetlkT1iUgNn47nSnki%2FLXaAqccBQ6%2FGT9MK5ESdScWHl1m3%2B%2FS%2FN83O8BGRRhPBv6pFGi0jcfJe8LSv3gRosFMMdYfvKVdeRju%2FUGrFceB0dVJesl%2FyRgX0fuMt8Doc13AFpYQZs1GE5taEnTeCdwngu70YBg7q9v%2F9miQC5%2Bnl91u%2Bn9QaEPcZv7B0HIRS48%3D&X-Amz-SignedHeaders=host&x-id=GetObject&X-Amz-Signature=98ed5145c10606b4ed7a71b3e1f94d6cf13ae21ba75aec4b77d7bf87a8b171e6)

2. [Endpoints :: Spring Boot](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html) - For example, the `health` endpoint provides basic application health information.

3. [Remote Access API](https://www.jenkins.io/doc/book/using/remote-access-api/) - Adding/removing Jenkins agents, querying details of agents. Obtaining the tasks in build queue, and ...

4. [Docker Documentation](https://docs.docker.com/engine/security/protect-access.md) - you can enable TLS (HTTPS) by specifying the `tlsverify` flag and pointing Docker's

5. [Docker Engine security](https://docs.docker.com/engine/security/) - Docker security non-events · Isolate containers with a user namespace · Protect the Docker daemon so...

6. [Configure remote access for Docker daemon](https://docs.docker.com/engine/daemon/remote-access/) - You can configure Docker to accept requests from remote clients by configuring it to listen on an IP...

7. [Trivy - Overview](https://trivy.dev/docs/dev/guide/scanner/misconfiguration/) - You can specify `--scanners vuln,misconfig,secret` to enable vulnerability and secret detection as w...

8. [Trivy](https://trivy.dev/) - Use Trivy to find vulnerabilities (CVE) & misconfigurations (IaC) across code repositories, binary a...

9. [GitHub - aquasecurity/trivy: Find vulnerabilities ...](https://github.com/aquasecurity/trivy) - Find vulnerabilities, misconfigurations, secrets, SBOM in containers, Kubernetes, code repositories,...

10. [GitHub - gitleaks/gitleaks-action: Protect your secrets using ...](https://github.com/gitleaks/gitleaks-action) - Protect your secrets using Gitleaks-Action. Contribute to gitleaks/gitleaks-action development by cr...

11. [Gitleaks](https://gitleaks.io/) - Gitleaks-Action is the official Gitleaks GitHub Action. You can use it to automatically run a gitlea...

