| Öğrenmek istediğin | `docker-java` komutu |
|---|---|
| Docker Engine erişilebilir mi? | `pingCmd()` |
| Docker Engine ve API sürümü ne? | `versionCmd()` |
| Engine hangi işletim sistemi ve mimaride çalışıyor? | `infoCmd()` |
| Projeye ait hangi container’lar var? | `listContainersCmd()` |
| Bu container’ın durumu, portları ve çıkış kodu ne? | `inspectContainerCmd(containerId)` |
| Bu container’ın son logları ne? | `logContainerCmd(containerId).withTail(100)` |
| Bu container ne kadar CPU ve bellek kullanıyor? | `statsCmd(containerId)` |