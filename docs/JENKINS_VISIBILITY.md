# Jenkins görünürlüğü

Son güncelleme: **2026-11-14 — Europe/Istanbul**.

OpsBridge, Jenkins'e GET gönderir ve ekrana gereken bilgileri DTO olarak döndürür. Build başlatma, deploy, Jenkins ayarı değiştirme veya logların tamamını kopyalama işlemi yapmaz. Build/aşama/test verisi için yeni tablo veya migration yoktur.

## Proje ayarları

Projeler → ilgili proje → **Profili düzenle → Devam et** içinde iki mevcut alan düzenlenir:

| Alan | Örnek ve davranış |
|---|---|
| `jenkinsBaseUrlRef` / `jenkins_base_url_ref` | Gerçek Jenkins URL'si: `http://localhost:8081` veya context path ile `http://localhost:8081/jenkins`. Mevcut kolon adı ve 100 karakter sınırı korunur. `local-jenkins` eski referansı sunucudaki `JENKINS_BASE_URL` değerini kullanır. |
| `jenkinsJobPath` / `jenkins_job_path` | `taskapp-cicd`, `apps/taskapp-cicd` veya `/job/apps/job/taskapp-cicd/`. En fazla 200 karakter. Adlar URL kodlaması yapılmadan girilir; boşluk ve Türkçe karakterler backend'de kodlanır. Sonuna `api/json` eklenmez. |

Alanlar kaydedildiğinde sonraki bağlantı kontrolü yeni değerleri kullanır. Kayıt için çalışan Jenkins bağlantısı şart değildir; yanlış job yolu kaydedilmişse kartta hata görülür ve formdan düzeltilebilir. URL alanına kullanıcı adı/token yazılamaz. Profil pasif veya Jenkins yeteneği kapalıysa dış istek yapılmaz.

## Ortam ayarları

| Ortam değişkeni | Kullanımı |
|---|---|
| `JENKINS_BASE_URL` | `local-jenkins` için gerçek URL; ortak kimlik bilgisinin gönderilebileceği ana adres. |
| `JENKINS_USERNAME` | Jenkins kullanıcısı. |
| `JENKINS_API_TOKEN` | Aynı kullanıcının API tokenı; yalnız backend'de kalır. |
| `JENKINS_ALLOWED_BASE_URLS` | Ortak token gönderilebilecek ek Jenkins ana adresleri; virgülle ayrılır. |

Kullanıcı adı/token birlikte boşsa anonim GET yapılır. Birinin eksik olması yapılandırma hatasıdır. Ortak kimlik bilgisinin yanlış proje URL'sine gönderilmesini önlemek için kimlik doğrulamalı hedef, ana veya izin verilen adresin altında olmalıdır. Yönlendirmeler takip edilmez; hata yanıtının gövdesi ve token frontend'e verilmez. Ortam ayarları değişince backend yeniden başlatılır; yalnız proje URL/job değişikliği yeniden başlatma gerektirmez.

Yerelde çalışan backend için `localhost` host makinesidir. Docker backend içinde `localhost` kendi container'ıdır; host Jenkins için uygun adres genellikle `host.docker.internal` olur. Bu görev portları dış erişime açmaz.

## İstek akışı

1. Proje detayındaki `IntegrationCards`, `GET /api/projects/{id}/integrations` çağırır. `IntegrationServiceImpl`, Jenkins için `JenkinsServiceImpl.getJenkinsConnectionStatus` kullanır. Job API'sine tek GET gönderilir. Erişim doğrulanırsa `CONNECTED` döner; kartın tamamı Jenkins ayrıntısına bağlantı olur. Bağlantı yoksa geçiş bağlantısı gösterilmez; genel proje ayarları kullanılabilir.
2. `/projects/{id}/jenkins`, korunan mevcut dashboard içinde açılır. `useJenkinsDetails`, proje profilini ve `GET /api/projects/{id}/jenkins` sonucunu okur. İptal edilen/gecikmiş istekler yeni görünümü değiştiremez.
3. `IJenkinsController → JenkinsControllerImpl → IJenkinsService → JenkinsServiceImpl` akışı mevcut katman düzenini korur. Kullanıcının verdiği örnekteki gibi `RestClient.get().uri(...).accept(...).retrieve().body(...)` kullanılır. `JenkinsMapper`, yalnız gereken JSON alanlarını normal Lombok DTO sınıflarına çevirir.
4. Temel job verisi `/job/.../api/json?tree=...`; seçili build verisi `/{number}/api/json`; aşamalar `/{number}/wfapi/describe`; rapor `/{number}/testReport/api/json` üzerinden okunur. Build seçimi `?build=47` ile API'ye ve sayfa adresine yansır. Bağlantıda 3, istekte 4 saniyelik zaman aşımı vardır; sayfa toplam dört ardışık Jenkins GET'i için 25 saniye bekler.
5. Repo sorgusu mevcut `capabilities` entity graph'ını yükler. Jenkins HTTP çağrısı sırasında veritabanı transaction'ı açık tutulmaz. Yeni persistence katmanı veya runtime veri kaydı yoktur.

Ana kod: [servis](../backend/src/main/java/com/yhcanbay/opsbridge/service/impl/JenkinsServiceImpl.java), [mapper](../backend/src/main/java/com/yhcanbay/opsbridge/mapper/JenkinsMapper.java), [istemci ayarı](../backend/src/main/java/com/yhcanbay/opsbridge/config/JenkinsConfig.java), [sayfa](../frontend/src/pages/JenkinsDetailsPage.jsx), [hook](../frontend/src/hooks/useJenkinsDetails.js).

## Veri ve hata sınırları

- Son build sürüyorsa son tamamlanan ve son başarılı build ayrı gösterilir. Jenkins'in son 30 kaydından en fazla 10 tamamlanmış çalışma alınır. Oran, bu dönen listenin tamamını; ortalama, süresi bilinen kayıtları kapsar. İptal/kararsız/bilinmeyen sonuçlar başarıya çevrilmez.
- Aşama adı, durumu ve süresi Jenkins'in bildirdiği gibidir. Stage API yoksa diğer build bilgileri kullanılabilir; ileride çalışacak aşamalar tahmin edilmez. Pipeline REST API eklentisi/kayıt gereksinimi açıklanır. [Jenkins Pipeline REST API](https://plugins.jenkins.io/pipeline-rest-api/)
- Test raporu yayımlanmamışsa veya okunamıyorsa sayılar gösterilmez. Yayımlanmış raporda başarılı/başarısız/atlanan sayıları ve ilk beş başarısız test adı gösterilir; ham hata logları taşınmaz. Jenkins'te testlerin çalışması, raporun yayımlandığını tek başına göstermez. [Jenkins test sonucu API'si](https://javadoc.jenkins.io/plugin/junit/hudson/tasks/test/TestResult.html)
- Build SHA/branch, Git eklentisinin proje repository'siyle eşleşen `BuildData.lastBuiltRevision` kaydından alınır. Başka repository veya shared library'nin SHA'sı seçilmez. Eşleştirilemeyen değer bilinmiyor kalır. Değişiklik listesi en fazla 20 kayıtla sınırlıdır. Branch'in güncel HEAD'i ve çalışan container sürümü bu SHA'dan çıkarılmaz. [Git BuildData kaynak kodu](https://github.com/jenkinsci/git-plugin/blob/master/src/main/java/hudson/plugins/git/util/BuildData.java)
- Jenkins 401/403 yanıtı OpsBridge oturum hatasına çevrilmez; ayrıntı API'sinde 502 ve açıklayıcı mesaj döner. Job/build 404, erişim/timeout, geçersiz yanıt ve eksik yapılandırma ayrılır. Yenileme hatasında eski build verisi güncelmiş gibi bırakılmaz.
- Jenkins ve GitHub dış bağlantıları yeni sekmede açılır. Kullanıcının tarayıcısının bu adreslere erişebilmesi gerekir; backend'in erişebilmesi bunu tek başına sağlamaz.

İlk görsel tasarım ayrı `jenkins-preview.html` girişinde, `previews/JenkinsDesignPreview.jsx` içinde örnek verilerle korunur. Üretim sayfası bu veriyi veya önizleme senaryosu menüsünü kullanmaz.

## Doğrulama

`backend/` içinde `mvn verify`; `frontend/` içinde `npm.cmd test`, `npm.cmd run format:check`, `npm.cmd run build`. Backend testleri Jenkins HTTP yanıtlarını taklit eder. Tarayıcı kontrolünde ayrıca gerçek Spring Boot/RestClient ve geçici H2 ile yerel Jenkins HTTP fixture'ı kullanıldı; gerçek Jenkins tokenı/sunucusuna bağlantı doğrulaması değildir. Son sonuçlar [.wiki/STATUS.md](../.wiki/STATUS.md) içindedir.

Temel kaynaklar: [Spring RestClient](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html), [Jenkins Remote Access API](https://www.jenkins.io/doc/book/using/remote-access-api/).
