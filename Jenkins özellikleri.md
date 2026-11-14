Bir DevOps mühendisi böyle bir paneli açtığında öncelikle şu soruların cevabını görmek ister:

“Son değişikliğin pipelineʼı başarılı mı? Şu anda ne çalışıyor? Nerede hata oldu? Hangi kod build

edildi? Ayrıntıya nereden ulaşabilirim?”

OpsBridge için Jenkins bölümünü bu sorular etrafında tasarlamanı öneririm. Amaç Jenkins

ekranının tamamını kopyalamak değil; projenin teslimat sürecini hızlıca anlaşılır kılmak

olmalı.

1. Son build ve devam eden işlem

İlk bakışta şu bilgileri göster:

Bilgi

Cevapladığı soru

Job adı ve bağlantısı

Hangi pipeline’ı takip ediyorum?

Son build numarası ve sonucu

Son çalışmanın sonucu ne?

Devam eden build

Şu anda yeni bir çalışma var mı?

Başlangıç zamanı

Ne zaman başladı?

Süre / geçen süre

Ne kadar sürdü veya ne kadar zamandır çalışıyor?

Son başarılı build

En son ne zaman başarılı çalıştı?

Build numarası, sonuç, zaman ve süre, Jenkins pipeline izleme araçlarında da kullanılan temel
verilerdir.

[1]

Burada önerdiğim önemli ayrım: Son build devam ediyorsa, önceki tamamlanmış buildʼin
sonucunu da göstermek. Böylece kullanıcı hem “şu anda çalışıyor” bilgisini hem de en son

tamamlanan çalışmanın sonucunu görür.

2. Pipeline hangi aşamada?

Senin Personal_Task_App akışında en anlamlı görünüm şu olur:

Kodu alma          Tamamlandı       4 sn
Backend testleri   Tamamlandı      32 sn
Frontend derleme   Çalışıyor       18 sn
Docker deploy      Henüz başlamadı
Doğrulama          Henüz başlamadı

Bu, örnek bir ekran; gerçek çalışma sonucu değil.

Kullanıcının yalnızca “build devam ediyor” değil, “frontend derlemesi devam ediyor”

diyebilmesini istiyoruz. Jenkinsʼin Stage View yaklaşımı da build geçmişini aşama bazında
[2]

gösterir; aşama sürelerini ve geçmiş ortalamalarına dayalı süre beklentilerini sunabilir.

OpsBridgeʼde başlangıç için şunlar yeterli:

Aşama adı.

Durumu.

Süresi.

Başarısızsa hangi aşama olduğu.

Geçmiş ortalamalar ve tahmini bitiş zamanı daha sonra eklenebilir.

3. Başarısızsa nereden inceleyeceğim?

Panelde hatayı fark etmek kadar incelemeye geçebilmek de önemli. Şunları öneririm:

Başarısız buildʼe bağlantı.

Başarısız aşamanın adı.

Jenkins konsol çıktısına bağlantı.

Test raporu varsa rapora bağlantı.

İlk sürümde tüm konsol logunu OpsBridgeʼe taşıman gerekmez. Özeti panelde gösterip
ayrıntıda Jenkinsʼe yönlendirmek, senin belirlediğin kapsamla daha uyumlu.

Ayrıca “backend test aşaması başarısız” ile “hangi test başarısız” farklı ayrıntı seviyeleri.
İkincisini sunmak için test sonuçlarının Jenkinsʼe rapor olarak yayımlanması gerekir;
Jenkinsʼin test analiz araçları bu yayımlanmış raporları kullanır.

[3]

Bu nedenle rapor yoksa “0 başarısız test” değil, “Test raporu bulunamadı” göstermeyi tercih et.

4. Hangi kod bu buildʼe girdi?

Burada görmek isteyeceğimiz bilgiler:

İlgili repository.

Branch.

Buildʼin kullandığı commit SHAʼsı.

Varsa bu buildʼe ait değişiklik listesi.

GitHub commit sayfasına bağlantı.

Bunu OpsBridge için özellikle öneriyorum; çünkü GitHub tarafın zaten var. Kullanıcı bir

buildʼden ilgili commitʼe geçebilsin.

Ancak üç alanı birbirine karıştırma:

Branch'in şu anki son commit'i
Jenkins build'inin kullandığı commit
Şu anda çalışan uygulamanın sürümü

Bunları veri modelinde ayrı tut. Güvenilir şekilde eşleştiremediğin noktada “bilinmiyor”

göster; örneğin yalnızca son başarılı buildʼe bakarak çalışan containerʼın sürümünü belirleme.

5. Son çalışmalar nasıl gidiyor?

Tek bir buildʼin yanında küçük bir geçmiş listesi de öneririm:

Build

Sonuç

Başlangıç

Süre

Detay

#42

#41

#40

Başarısız

Bugün 11.20

1 dk 12 sn

Jenkins’e git

Başarılı

Bugün 10.45

2 dk 8 sn

Jenkins’e git

Başarılı

Dün 18.10

2 dk 3 sn

Jenkins’e git

Bu tablo da örnektir.

İlk sürümde son 10 build yeterli bir kapsam olur. Sonraki aşamada şu sorulara yönelik özetler
ekleyebilirsin:

Seçilen dönemde kaç build başarılı, kaçı başarısız?

Build süreleri uzuyor mu?

En çok hangi aşamada hata oluyor?

Başarı oranı, ortalama build süresi ve aşama bazlı hata sayıları Jenkins izleme panellerinde
kullanılan metriklerdir. Ancak oran gösterirken “son 10 tamamlanmış build” gibi hesaplama
kapsamını açıkça yazmalısın.

[4]

6. Jenkins verisine güvenebiliyor muyum?

OpsBridgeʼe ayrıca şu iki bilgiyi koymanı öneririm:

Jenkinsʼe son erişim başarılı mı?

Ekrandaki veri en son ne zaman güncellendi?

Şu durumlar farklı gösterilmeli:

Build başarısız.
Jenkins'e ulaşılamıyor.
Job bulunamadı.
Henüz build çalışmamış.

Bunların hepsine tek bir “Başarısız” etiketi koyarsan kullanıcı uygulama sorunu ile

entegrasyon sorununu ayıramaz.

OpsBridge için önerdiğim kapsam

Bunlar ekranın nihai hedeﬁ; hepsini ilk kodlama adımında yapmak zorunda değilsin.

Öncelik

Özellik

İlk çalışan entegrasyon

Son build, sonuç, devam ediyor bilgisi, zaman/süre ve Jenkins bağlantısı

İlk kullanılabilir Jenkins ekranı

Build geçmişi, aşama durumları, başarısız aşama, konsol bağlantısı, veri güncelliği

Sonraki geliştirme

Branch/commit eşleştirmesi, test özeti, süre ve başarı eğilimleri

Şimdilik kapsam dışı

Jenkins yapılandırması, agent yönetimi, credential yönetimi ve panelden deploy işlemleri

Senin projen için en değerli ekran, kullanıcının “Personal_Task_Appʼin son pipelineʼı backend

testlerinde durmuş; ilgili buildʼi ve değişikliklerini açabilirim” diyebildiği ekran olur.
Uygulamanın şu anda sağlıklı çalışıp çalışmadığını ise yanında, ayrı Docker/uygulama sağlığı

bölümünde göstermeye devam et.

⁂

1. https://plugins.jenkins.io/pipeline-dora-metrics/

2. https://plugins.jenkins.io/pipeline-stage-view/

3. https://plugins.jenkins.io/test-results-analyzer/

4. https://plugins.jenkins.io/github-autostatus/

5. https://plugins.jenkins.io/gitlab-plugin/

6. https://plugins.jenkins.io/teamconcert/

7. https://plugins.jenkins.io/pipeline-graph-analysis/

8. https://plugins.jenkins.io/workflow-step-api/

9. projects.devsecops_management_platform

10. learning.ai_assisted_development

11. https://www.perplexity.ai/search/e09f285f-e478-4c03-8986-8ee7797a58a7

12. https://www.perplexity.ai/search/5c648b41-9c43-43a9-b929-463549e6745a

13. https://www.perplexity.ai/search/ae9b0880-66c0-45e3-8c5d-4b68591044c0

14. https://plugins.jenkins.io/pipeline-rest-api/

15. https://plugins.jenkins.io/mcp-server

16. https://plugins.jenkins.io/meliora-testlab/

17. https://plugins.jenkins.io/integrity-plugin/

18. https://plugins.jenkins.io/interactive-ci/

19. https://plugins.jenkins.io/build-history-manager/

20. https://plugins.jenkins.io/coverage/

