PLA
---------------------

Bit no.     V  Meaning      (V = bit value)
---------------------------------------------------
00 00000001 0 "Netz aus" "Netz aus"
00 00000001 1 "Netz ein" "Netz ein"
01 00000002 0 "manuell " "Handsteuerung"
01 00000002 1 "remote  " "Rechnersteuerung"
04 00000010 0 "Emergenc" "Emergency ausgelöst"
04 00000010 1 "no Emerg" "kein Emergency-Status"
05 00000020 0 "Interlck" "Interlock ausgelöst"
05 00000020 1 "no Intlk" "kein Interlock aufgetreten"
06 00000040 0 "Hardware" "Hardwarefehler aufgetreten"
06 00000040 1 "HW ok   " "Hardware in Ordnung"
07 00000080 0 "Software" "Software-Warnung aufgetreten"
07 00000080 1 "SW ok   " "keine Software-Warnung aufgetreten"
08 00000100 0 "extBlock" "externe Blockierung"
08 00000100 1 "no extBl" "keine externe Blockierung"
09 00000200 0 "intBlock" "interne Blockierung"
09 00000200 1 "no intBl" "keine interne Blockierung"
10 00000400 0 "Temp ?? " "Temperatur ??"
10 00000400 1 "Temp ok " "Temperatur ok"
11 00000800 0 "End-In  " "Endlage innen"
11 00000800 1 "        " "Innere Endlage verlassen"
12 00001000 0 "End-Out " "Endlage außen"
12 00001000 1 "        " "Äußere Endlage verlassen"
13 00002000 0 "extInter" "externes Interlock"
13 00002000 1 "no extIn" "kein externes Interlock"
14 00004000 0 "lokAktiv" "lokal aktiviert"
14 00004000 1 "remAktiv" "remote aktiviert"
15 00008000 0 "SW block" "Antrieb softwaremäßig blockiert"
15 00008000 1 "SW frei " "Antrieb softwaremäßig frei"
16 00010000 0 "Netz aus" "Netz aus"
16 00010000 1 "Netz ein" "Netz ein"


DS
---------------------

Bit no.     V  Meaning      (V = bit value)
---------------------------------------------------
00 00000001 0 "Netz aus" "Netz aus"
00 00000001 1 "Netz ein" "Netz ein"
01 00000002 0 "manuell " "Handsteuerung"
01 00000002 1 "remote  " "Rechnersteuerung"
04 00000010 0 "Emergenc" "Emergency ausgelöst"
04 00000010 1 "no Emerg" "kein Emergency-Status"
05 00000020 0 "Interlck" "Interlock ausgelöst"
05 00000020 1 "no Intlk" "kein Interlock aufgetreten"
06 00000040 0 "Hardware" "Hardwarefehler aufgetreten"
06 00000040 1 "HW ok   " "Hardware in Ordnung"
07 00000080 0 "Software" "Software-Warnung aufgetreten"
07 00000080 1 "SW ok   " "keine Software-Warnung aufgetreten"
08 00000100 0 "noFahrb " "Antrieb(e) nicht fahrbereit"
08 00000100 1 " Fahrb  " "Antrieb(e) fahrbereit (Park, no Interlock)"
09 00000200 0 "AntrFahr" "dieser Antrieb fährt"
09 00000200 1 "AntrPark" "dieser Antrieb steht"
10 00000400 0 "FahrErr " "Antrieb hat Fahrfehler"
10 00000400 1 "Fahr OK " "Antrieb hat keine Fahrfehler"
11 00000800 0 "Intlck B" "Interlock B ausgelöst"
11 00000800 1 "noIntl B" "kein Interlock B"
12 00001000 0 "Intlck A" "Interlock A ausgelöst"
12 00001000 1 "noIntl A" "kein Interlock A"
13 00002000 0 "Motfahrt" "mindestens ein Antrieb fährt"
13 00002000 1 "allePark" "alle Antriebe in Parkstellung"
!14 frei
!15 frei
16 00010000 0 "#minAbst" "Mindestabstand nicht eingehalten"
16 00010000 1 "min Abst" "Mindestabstand eingehalten"
17 00020000 1 "End In  " "Antrieb(e) in innerer Endlage"
18 00040000 1 "End Aus " "Antrieb(e) in äußerer Endlage"
! Bit 19 bezieht sich auf die gesamte Elektronik mit Schlitzen,
!        die der Benutzer nicht sieht/nichts von weiß/usw., deshalb
!	 wird dieses Bit nicht angezeigt...
!19 00080000 0 "some In " "mindestens ein Antrieb nicht in äußerer Endlage"
!19 00080000 1 "AlEndOut" "alle Antriebe äußerer Endlage"
!21 frei
!22 frei
!23 frei
!24 frei
!25 frei
!26 frei
!27 frei
!28 frei
!29 frei
!30 frei
31 80000000 0 "Therapie" "Therapie läuft + Gerät hat Soll/Ist-Wertabweichung"
31 80000000 1 "no thera" "weder Therapievorb. noch -teilnehmer, Gerät frei"

