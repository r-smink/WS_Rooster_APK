# RoosterPlanner Android app (preview)

Native Android client for the RoosterPlanner WordPress plugin. The app talks to the new REST API under `/wp-json/roosterplanner/v1` using a WordPress Application Password per medewerker.

## Vereisten
- Android Studio Hedgehog+ (AGP 8.2 / Kotlin 1.9)
- JDK 17
- Een WordPress gebruiker met Application Password (WordPress → Gebruikers → jouw account → Application Passwords)

## Configuratie
1. Update de plugin op je site zodat de REST endpoints beschikbaar zijn.
2. Maak een Application Password voor de medewerker die inlogt.
3. Start de app en vul:
   - Site URL, bijvoorbeeld `https://jouwdomein.nl/`
   - Gebruikersnaam
   - Application Password (32 tekens)
4. Tik op **Verbind**. De app test `/me` en haalt het rooster op voor de komende 21 dagen.

## Belangrijkste schermen
- **Login:** slaat URL + credentials op in DataStore (lokaal, niet versleuteld).
- **Rooster:** toont diensten, shift-namen, locatie en notities. Pull-to-refresh knop haalt opnieuw op.

## Endpoints die worden gebruikt
- `GET /wp-json/roosterplanner/v1/me`
- `GET /wp-json/roosterplanner/v1/schedules?start_date=YYYY-MM-DD&end_date=YYYY-MM-DD`
- `GET /wp-json/roosterplanner/v1/notifications` (klaar voor gebruik)
- `POST /wp-json/roosterplanner/v1/availability` (hooked via repository, UI nog minimaal)

## Bouwen
```
cd android-app
./gradlew assembleDebug
```
(De Gradle wrapper wordt automatisch gedownload door Android Studio.)

## Volgende stappen
- Push notificatie tokens opslaan via `/notifications` uitbreiden.
- Schermen voor beschikbaarheid invullen en ziekmelding.
- Offline caching (Room) toevoegen.
