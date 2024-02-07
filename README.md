
# APP CAMBUSA

Applicazione sviluppata come progetto durante il corso di Ambienti di Programmazione e Programmazione Mobile durante AA2023/24 presso l'Università della Calabria (UNICAL) cdl Ing. Informatica;

Il progetto consiste in una semplice app di food managing o inventario (da qui il nome "Cambusa") che permetta di tenere traccia dei prodotti presenti a casa e della loro scadenza, in modo da limitare gli sprechi di cibo dovuti a "dimenticanze";

Non solo l'app propone di tenere traccia dei prodotti ma è in grado di suggerire eventuali ricette per consumarli tramite generazione di ricette basata sul modello ChatGPT3.5Turbo ;

L'app permette di ricevere un reminder giornaliero su eventuali prodotti scaduti o in scadenza (sotto una soglia di giorni impostata dall'utente) ed inoltre di ricevere un recap dei prodotti tramite notifiche "location based" impostabili da apposita interfaccia creata tramite Api GoogleMaps e gestite tramite Geofences.


# Componenti Android utilizzate

- Ui basata su Material3 tematizzata, con supporto modalità chiara/scura e realizzata in ibrido con Layout, Fragments e widget costruti tramite api Jetpack Compose;
- Gestione dati persistenti tramite SharedPreferences (impostazioni) e Database Sqlite (prodotti, ricette salvate, posizioni);
- Recupero / Salvataggio / modifica dati tramite WorkManager, ViewModel e coroutine in maniera asincrona;
- Gestione di Geofences e notifiche basate su posizione tramite BroadCast receiver;
- Vista aggiunta posizioni generata tramite GoogleMap e recupero posizione tramite FusedLocationProvider;
- Creazione ricette e collegamento api tramite OKHTTP ad OpenAi con apposito prompt di controllo e modalità json-response-only del modello GPT3.5;
- Gestione tasto share per condividere le ricette tramite Intent Impliciti identificati da MIME;


Per eventuali informazioni:

giulianiadriano3@gmail.com

https://www.linkedin.com/in/adriano-giuliani-733413209






