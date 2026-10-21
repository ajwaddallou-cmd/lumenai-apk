# Nuskha Server-Cleanup (opt-in) - Sicherheitskonzept, Stand 20.10.2026
Status: ENTWURF, nicht gebaut, keine oeffentliche Route.

## Prinzip
- Default bleibt offline. Kein Text verlaesst das Geraet ohne ausdrueckliches Opt-in pro Installation.
- Setup-App: Schalter "Server-Bereinigung (eigener Oracle-Server)" standardmaessig AUS, mit Klartext-Hinweis: "Zur Bereinigung wird dieser Text an deinen eigenen Server gesendet und dort sofort verworfen. Ohne diesen Schalter bleibt alles auf dem Geraet."
- Kein fest eingebettetes Secret im APK.

## Authentifizierung
- Pro Installation ein zufaelliges Token (32 Bytes, Base64), einmalig an Ajwad ausgehaendigt (WhatsApp) und von ihm in die Setup-App eingefuegt.
- Server speichert nur SHA-256-Hash des Tokens. Token rotierbar und widerrufbar (Hash-Datei loeschen = sofort gesperrt).
- Header: Authorization: Bearer <token>. Ohne gueltiges Token: 401, kein Rate-Limit-Bypass.

## Transport
- Nur HTTPS ueber bestehenden Ultron-Tunnel (TLS von Cloudflare), Pfad /nuskha/cleanup, POST JSON.
- Kein oeffentliches Ollama: Dienst "nuskha-cleanup" lauscht nur auf 127.0.0.1:18081 und ruft selbst localhost:11434.

## Begrenzung
- Max 2000 Zeichen pro Anfrage, max 60 Anfragen/Tag/Token (In-Memory-Zaehler, Tagesreset).
- Feste Prompt-Schablonen (Chat/Poliert/E-Mail/Woertlich), kein freier Prompt vom Client -> kein Prompt-Gateway.
- Timeout 15 s, num_predict 150. Fehlerfall: App faellt still auf lokale Regeln zurueck.

## Logging/Retention
- Kein Text-Logging. Nur Zeitstempel + Zeichenanzahl + Ergebniscode in Ringpuffer (100 Eintraege, RAM).
- Anfrage wird im Speicher verarbeitet und verworfen; kein Schreiben auf Platte.

## Ruecknahme
- systemctl stop nuskha-cleanup + Tunnel-Route entfernen = Feature komplett weg, App laeuft offline weiter.
- Token-Hash-Datei loeschen = alle Installationen gesperrt.

## Modell-Realitaet (gemessen 20.10., qwen2.5:1.5b, 4 synthetische Diktate)
- Gut: leichte Bereinigung kurzer Saetze (Fuellwoerter, Kommas) ~7 s.
- Schlecht: Stil "E-Mail" halluziniert Anrede/Grussformel und formuliert um; Stil "Chat" behielt einmal "aehm"; ein langer Satz wurde grammatikalisch verstümmelt.
- Fazit: KEINE Wispr-Qualitaet. Als "schnelle Bereinigung" vertretbar, fuer Stilwechsel unzuverlaessig. Groesseres Modell oder on-device-LLM waere naechster Schritt.

## Stand der lokalen Umsetzung (20.10. abend)
- Dienst ~/nuskha-cleanup/server.py laeuft lokal auf 127.0.0.1:18081 (nohup, noch KEIN systemd, kein Tunnel, keine oeffentliche Route).
- Getestet: 401 ohne/falschem Token, 404 falscher Pfad, 400 freier/unbekannter Stil, 413 bei >8000 Bytes Body, Rate-Limit 60/Tag greift (429), Auth vor Verarbeitung.
- Log-Pruefung: Dienst loggt nichts (log_message deaktiviert); ollama-Journal zeigt nur Request-Metadaten (GIN-Zeilen), kein Prompt-Text (grep-Verifikation). Versprechen "kein Text-Logging" damit fuer Dienst+Ollama gedeckt; Tunnel-/Proxy-Logs erst bei Aktivierung pruefen.
- Token liegt NUR als SHA-256 in ~/nuskha-cleanup/tokens.sha256 (600) und temporaer /tmp/nuskha-token.txt (600). Einrichtung am Geraet: Token in Instinct-Vault ablegen (Eintrag "Nuskha Server-Cleanup"), Ajwad fuegt ihn beim Einrichten selbst aus dem Vault in die App ein - kein Chat-Versand, kein APK-Einbau.
- Modell-Benchmarks (2 vCPU, ARM, synthetische Diktate):
  - qwen2.5:1.5b: ~2-7 s, Stilwechsel unzuverlaessig (Halluzination in E-Mail, "aehm" uebersehen, langer Satz verstuemmelt).
  - qwen2.5:3b (jetzt installiert): ~8-9 s, deutlich besser, aber immer noch erfundene Floskeln moeglich ("Danke fuer Ihre Nachforschung") -> KEINE Wispr-Qualitaet, Stil E-Mail weiterhin riskant.
  - qwen2.5:7b: NICHT installiert - auf 2 vCPU unbrauchbar langsam, RAM-Reserve fuer Hermes/Lumen wuerde knapp.
- Fazit: lokaler Fallback bleibt Primaerweg; Server-Cleanup als sichtbarer Opt-in-Zusatz ("Server" im Ergebnis kennzeichnen), Stil "woertlich"/"poliert" brauchbar, "E-Mail" vorerst nicht anbieten.

## App-Verdrahtung (20.10. abend, lokal, nicht oeffentlich)
- Opt-in-Karte auf der Act-Seite: Toggle (default AUS), Server-URL, Token-Feld, Klartext-Offenlegung. Gespeichert nur lokal im Geraet (localStorage), kein Einbau in Builds.
- IME: Server-Knopf (Wolke) erscheint NUR wenn URL+Token konfiguriert. Ergebnis geht nur in die Entwurfs-Vorschau, Einfuegen bleibt manuell. Statuszeile unterscheidet: "Server-Entwurf (Oracle) - bitte pruefen" vs "Lokal, ohne KI oder Netzwerk" vs "Server nicht erreichbar - lokale Regeln genutzt".
- Stile Wörtlich und E-Mail rufen den Server NIE auf (woertlich bleibt regelbasiert lokal, E-Mail vorerst nicht angeboten).
- Verifikation im Browser-Harness: Server-Knopf ohne Konfiguration unsichtbar (Screenshot), Opt-in-Karte rendert im hellen Setup-Theme korrekt.
- WICHTIG: CSP in app.html hat connect-src none - blockiert jede Netzabfrage. Bei Aktivierung muss connect-src explizit auf die eigene Server-URL geoeffnet werden; bewusst so belassen als zusaetzliche Sicherung, dass ohne Freigabe nichts funkt.

## Offene Produktluecken / Auflagen vor Freischaltung (Stand 20.10. 19:10)
1. "Nichts gespeichert"-Aussage erst gegen den vollstaendigen Pfad pruefen (App, WebView, Tunnel/Cloudflare-Logs, Dienst, Ollama, systemd-Journal), nicht vor Freischaltung versprechen.
2. Token-Ablage: NICHT ungeschuetzt in WebView-localStorage/normalen Preferences, nicht ueber JS-Bridge auslesbar. Native Android-Ablage (EncryptedSharedPreferences/Keystore), Bridge-Oberflaeche minimal halten, Navigation im WebView auf eigene Origin beschraenken.
3. CSP ersetzt keine native Pruefung: zusaetzlich URL-Allowlist + Zertifikatspruefung (HTTPS, gueltige Chain, ggf. Pinning) auf Android-Seite.
4. Echte KI-Qualitaet offen: 3b besser als 1.5b, aber erfundene Floskeln moeglich -> Ergebnis immer als pruefbarer Entwurf, nie auto-einfuegen.
5. Sichere Kopplung (Token-Uebergabe ueber Vault, Widerruf) und Geraetetest auf Pixel 8 stehen noch aus.
