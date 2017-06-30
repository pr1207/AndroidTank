U prilogu je source kod aplikacije (gradle fajl i src folder) za android kao i par pomoćnih skripti koje sam koristio prilikom pripremanja
podataka za pravljene sopstvenog klasifikatora u opencv_traincascade. 

Prvi klasifikator koji sam napravio na oko 80 pozitivnih (gdje su slike samo tenka i neke pozadine -trava,pustinja itd) i 500 negativnih je uglavnom prepoznavao objekte ali ima mnogo
false-positives zbog nedovoljnog broja uzoraka.

Nakon toga sam pokrenuo sa 400 pozitivnih i 20 stageova i u toku noci je negdje pukao jer je spisak negativnih nije bio
cisti utf8. Zbog nedostatka vremena jer mislim da bi bar dan jos trebalo, moram da posaljem zadatak sa prvim klasifikatorom
koji sam napravio, a koji ne radi kako bi trebalo (prepoznaje objekte ali brlja).

Pomoćni alat koji sam koristio omogućava da se napravi .txt fajl koji sadrži spisak pozitivnih kao i koordinate objekta 
koji nas zanima. Primjenom 2 bat skripte su dodate apsolutne putanje u taj fajl i kreirani su sampleovi kao vec. fajl
pomoću opencv_createsamples. Nakon toga je počeo proces treniranja klasifikatora.

OPENCV je ukljucen unutar projekta, ne inicjalizuje se preko asyncInit().
Što se same implementacije tiče, postoje 3 načina korišćenja aplikacije. 

1. Korisnik odabere fotografiju,koja zatim prolazi kroz opencv i kao rezultat dobija se fotografija koja ima pravougaonik u dijelu gdje je traženi objekat
(ukoliko postoji na slici).

2. Korisnik odabere video, iz kojega prvo izvadimo frejmove na svakih 1sec i onda te frejmove pojedinačno provlačimo kroz
opencv, i na svaki sekund ih u View-u u androidu prikazujemo.

3. Korisnik odabere Real-Time, otvara se prikaz kamere koja u realnom vremenu detektuje tražene objekte. 

Problemi:
- Osim problema sa klasifikatorom (koji bih riješio da sam se ranije posvetio pisanju rada), postoji problem i sa obradom 
videa. Naime, bibiloteke koje sam pokušavao koristiti često su rezultovale ili null frameovima ili OutOfMemory greškama, 
sad da li je vezano i do konkretnog uredjaja na kome sam testirao nisam siguran.
Na kraju sam riješio stvar tako što se iz videa skida frejm na svaki sekund, provlači kroz opencv 
i rezultujuća slika skalira kao bitmapa na maksimalnu dimenziju od 250px (po širini ili dužini), 
i to nativnom bibliotekom koja ne vadi konkretan frejm već prvi reprezentativni u njegovoj okolini 
(pretpostavljam da se ovo koristi kod generisanja thumbnailova i da nije baš najsrećnije
 rešenje ali je jedino što sam uspio da natjeram da radi a da ne dobijam gore pomenute exceptione).