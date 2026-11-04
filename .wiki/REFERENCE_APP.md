# Referans uygulama: Personal_Task_App

Kaynak inceleme tarihi: **2026-11-03 — Europe/Istanbul**. Bu kayıt OpsBridge hafızasına 2026-11-04 tarihinde aktarıldı.

Bu teknik bulgular ayrı bir depoya aittir: `C:\Yazılım\Personal_Task_App`. Aşağıdaki uygulama ve pipeline dosyaları OpsBridge'in kendi kaynakları değildir. Bilgiler dosya incelemesinden gelir; çalışma zamanında doğrulanmadı.

## Kodda gözlenen başlangıç

İnceleme sırasında Git branch'i `main`, HEAD kısa kimliği `faae3d0` olarak okundu. Bu değer kalıcı varsayım değildir; yeni kod işinde güncel durumu tekrar kontrol et.

| Alan | Dosyalarda gözlenen durum | Kaynak |
|---|---|---|
| Backend | Java 21, Spring Boot 4.0.5, JPA ve PostgreSQL | [pom.xml](../../../Personal_Task_App/taskApp/pom.xml) |
| Frontend | React 19, Vite 7; karşılama, ayarlar, haftalık plan ve yapılacaklar ekranları | [package.json](../../../Personal_Task_App/taskApp/frontend/Ders_Programi/package.json), [App.jsx](../../../Personal_Task_App/taskApp/frontend/Ders_Programi/src/App.jsx) |
| Kalıcı veriler | Ayarlar ve haftalık plan GET/PUT API'leriyle DB'ye yazılıyor; servisler tek sabit kayıt ID'si kullanıyor | [SettingsService.java](../../../Personal_Task_App/taskApp/src/main/java/com/yhcanbay/taskApp/service/SettingsService.java), [WeeklyScheduleService.java](../../../Personal_Task_App/taskApp/src/main/java/com/yhcanbay/taskApp/service/WeeklyScheduleService.java) |
| Yapılacaklar | Backend yerine tarayıcı `localStorage` alanında saklanıyor | [todoService.js](../../../Personal_Task_App/taskApp/frontend/Ders_Programi/src/services/todoService.js) |
| Uygulama ortamı | DB, backend ve frontend; kalıcı PostgreSQL volume'u | [docker-compose.yml](../../../Personal_Task_App/docker-compose.yml) |
| Jenkins ortamı | Ayrı Compose; Docker socket bağlantısı ve root kullanıcı ayarı | [docker-compose.jenkins.yml](../../../Personal_Task_App/docker-compose.jenkins.yml), [Dockerfile](../../../Personal_Task_App/jenkins/Dockerfile) |
| Ek Compose | `taskApp/compose.yaml` yalnızca PostgreSQL servisini tanımlıyor; kökteki tam uygulama Compose'u ile karıştırma | [compose.yaml](../../../Personal_Task_App/taskApp/compose.yaml) |
| OpsBridge | İncelenen depoda panel/backend, proje profili, adapter veya güvenlik raporu entegrasyonu bulunmadı | Önceki kaynak dosya incelemesi |

## Mevcut teslimat hattı

[Jenkinsfile](../../../Personal_Task_App/Jenkinsfile) aşamaları:

`Checkout → Backend Test → Frontend Build → Docker Deploy → Verify`

- Backend testi Maven wrapper üzerinden çalıştırılacak şekilde tanımlı. Mevcut test yalnızca `contextLoads()` içeriyor; iş kurallarını test eden geniş bir test seti değil.
- Frontend için `npm ci` ve `npm run build` var; pipeline'da lint veya frontend testi yok.
- Frontend'de `ScheduleLogic.test.js` dosyası var, fakat `package.json` içinde test komutu ve Vitest bağımlılığı yok. Dosyanın bulunması çalışan test altyapısı olduğu anlamına gelmiyor.
- Deploy adımı kökteki Compose'u `up -d --build --remove-orphans` ile uyguluyor. Commit/build kimliği taşıyan açık image etiketleme akışı bulunmadı.
- Verify adımı `http://localhost:8080/api/settings` adresini kontrol ediyor. Actuator bağımlılığı mevcut Maven dosyasında yok.
- Mevcut Maven dosyasında Spring Security bağımlılığı görülmedi. OpsBridge kimlik doğrulaması henüz uygulanmış değil.
- Compose'da geliştirme DB bilgileri düz metin, container adları sabit; özel `opsbridge.*` etiketleri ve backend healthcheck'i yok. Değerleri hafıza dosyalarına kopyalama.

## Doğrulama sınırları

- **Yapılan:** Raporu ve ilgili kaynak/yapılandırma dosyalarını okuma; Git durumunu/branch'ini inceleme; önerilerin uygulanabilirliği için resmî dokümantasyon araştırması.
- **Yapılmayan:** Uygulamayı başlatma, Maven testi, npm build/lint/test, Docker build/deploy, Jenkins API bağlantısı, webhook/ngrok veya Kubernetes çalıştırma.
- Pipeline dosyasının varlığı, hattın şu anda başarıyla çalıştığına kanıt değildir.
- Jenkins container içinde çalışıyorsa Verify adımındaki `localhost` adresinin hangi makineyi/container'ı gösterdiği ilk çalıştırmada kontrol edilmeli. Bu konuşmada ağ düzeniyle denenmedi.

## 2026-11-04 — Başlangıç uygulaması için yeniden inceleme

Kaynak Maven, package.json, Jenkinsfile, Compose ve Dockerfile'lar yeniden okundu. Güncel kısa envanter ve komutlar [docs/PERSONAL_TASK_APP.md](../docs/PERSONAL_TASK_APP.md) içinde tutuluyor. Önceki kaynak bulguları geçerli; Docker Engine erişilemediği için referans uygulama bu görevde de çalıştırılmadı. Uygulama dosyaları değiştirilmedi.

OpsBridge'in ayrı başlangıç uygulaması artık mevcut; önceki “OpsBridge kodu bulunmadı” tespiti bu dosyada **o tarihte incelenen referans depoya** aittir. OpsBridge'in güncel kaynak ve test durumu [STATUS.md](STATUS.md) içindedir.
