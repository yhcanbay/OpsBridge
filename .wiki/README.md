# OpsBridge proje hafızası

Son güncelleme: **2026-11-07 — Europe/Istanbul**

Bu klasör, kullanıcının aynı proje bağlamını her sohbette yeniden anlatmaması ve Codex ile ilerleyişi ortak kayıtlardan takip edebilmesi için oluşturuldu. Konuşmaların anlamı korunarak özetlenir; sözcüğü sözcüğüne sohbet dökümü değildir.

**Bu hafızanın ait olduğu proje:** `C:\Yazılım\DevOpsSystemMonitoring\OpsBridge`.

Kaynak sohbet ve teknik inceleme `C:\Yazılım\Personal_Task_App` deposunda başladı. Kullanıcı 2026-11-04 tarihinde ana geliştirme ortamının bu OpsBridge dizini olduğunu netleştirdi. Güncel OpsBridge ilerlemesi burada tutulur; iki depodaki hafıza kopyaları otomatik olarak senkronlanmaz.

## Nereden devam edilir?

| Dosya | Sorumluluğu |
|---|---|
| [STATUS.md](STATUS.md) | Şu anda ne var, ne doğrulandı, ne eksik ve nereden devam edilebilir? |
| [CONTEXT.md](CONTEXT.md) | Kullanıcının öğrenme geçmişi, hedeflediği ürün, öneriler ve açık kapsam soruları |
| [REFERENCE_APP.md](REFERENCE_APP.md) | Ayrı Personal_Task_App deposunun ilk teknik incelemesi |
| [HISTORY.md](HISTORY.md) | Önemli görüşmelerin, karar değişimlerinin ve çalışma sonuçlarının tarihçesi |
| [../AGENTS.md](../AGENTS.md) | Codex'in bu hafızayı bulması ve güncel tutması için kısa depo talimatları |
| [Kod yazım rehberi](../docs/CODING_STYLE.md) | Kullanıcının seçtiği Sozluk_Uygulamasi referansı ve mevcut koda uygulanan yazım kuralları |
| [Mimari](../docs/ARCHITECTURE.md) | Aynı sunucuda ayrı OpsBridge kurulumu, tarayıcıdan admin erişimi ve sonraya bırakılan URL yayını |

Önce `STATUS.md` ile güncel durumu al. Ürün ve öğrenme bağlamı için `CONTEXT.md` dosyasını kullan; geçmiş gerekçeler için `HISTORY.md` dosyasına bak.

## Kayıt ilkesi

- **Kullanıcı hedefi/isteği:** Kullanıcının açıkça söylediği şey.
- **Codex önerisi:** Konuşmada sunulmuş, kullanıcı tarafından henüz kesinleştirilmemiş yaklaşım.
- **Kodda gözlendi:** Dosyalarda görülen durum; çalışma zamanında başarı kanıtı değildir.
- **Çalıştırılarak doğrulandı:** Gerçek komut veya sistem gözlemiyle doğrulanan sonuç.
- **Açık konu:** Henüz belirlenmemiş kapsam, karar veya ortam bilgisi.

Yalnızca gelecekteki çalışmayı etkileyen bilgileri tut. Hedef değişikliklerini bağlamda, son durumu durum dosyasında, anlamlı gelişmeleri tarihçede güncelle. Kaynak raporu ve kodu bütünüyle kopyalama; bağlantı ver.

## Codex açısından nasıl işler?

`.wiki` adı bizim seçtiğimiz bir depo düzenidir; tek başına otomatik yüklenen özel bir Codex hafıza klasörü değildir. Kök `AGENTS.md`, sonraki proje oturumlarını bu dosyalara yönlendirir. Bu kayıtlar aynı çalışma alanına erişilebildiğinde kullanılabilir; başka bir bilgisayara taşınırken dosyaların da taşınması gerekir. Bu görevde commit veya push yapılmadı.

OpenAI Docs, gerekli proje talimatları için `AGENTS.md` ve depoda tutulan belgeleri; yerleşik hafızayı ise yardımcı hatırlama katmanı olarak öneriyor. Yerleşik hafıza ayarları veya Codex'in ürettiği global hafıza dosyaları bu görevde değiştirilmedi. [Resmî hafıza rehberi](https://learn.chatgpt.com/docs/customization/memories), [AGENTS.md rehberi](https://learn.chatgpt.com/docs/agent-configuration/agents-md)

Bu klasörün ayrıntılı bağlamı kısa talimatlardan ayırması ve gözlenen sonucu öneriden ayırması, resmî iş akışı örneğinden uyarlanmıştır; örnekteki tüm dosya ve süreçler zorunlu değildir. [OpenAI iş akışı örneği](https://developers.openai.com/cookbook/examples/codex/iterating-development-workflows-with-codex)
