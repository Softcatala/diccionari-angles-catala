# Diccionari anglès-català

El diccionari anglès-català de Softcatalà és una obra totalment nova que vol donar resposta a les necessitats dels usuaris de la llengua en el món digital. La primera versió és d’octubre del 2024, amb més de 47.000 entrades.

A més de coneixements propis, el diccionari incorpora dades revisades de fonts existents amb llicència compatible o permís exprés:

* Wiktionary https://en.wiktionary.org/
* Catalan Dictionary https://www.catalandictionary.org/
* TERMCAT - Terminologia oberta https://www.termcat.cat/ca/terminologia-oberta/
* Open English Wordnet https://en-word.net/
* Xavier Pàmies - Llista complementària del diccionari anglès-català de l'Enciclopèdia https://diccxpamies.xarxa.cat/

## Diccionari

A la carpeta `diccionari` hi ha les dades més recents del diccionari en format XML. El fitxer `eng-cat.xsd` defineix i permet validar l'esquema dels fitxers de dades.

Els fitxers `stopwords.eng.txt` i `stopwords.cat.txt` contenen llistes d'algunes de les paraules més freqüents en anglès i català, respectivament. El servidor limita els resultats de cerca d'aquestes paraules per a mostrar únicament coincidències exactes.

## Servidor

A la carpeta `server` hi ha el codi del servidor. Es pot compilar executant l'ordre següent des de dins de la carpeta:

```
mvn package
```

Per a executar el servidor, és necessari un fitxer de configuració. A la carpeta `server-cfg` n'hi ha un d'exemple.

## Client

A la carpeta `client` hi ha un exemple de client bàsic en PHP.

## Llicència

This software is licensed under:

* GNU General Public License, version 2 https://www.gnu.org/licenses/gpl-2.0.txt

Dictionary data is licensed under:

* Creative Commons CC-BY 4.0 https://creativecommons.org/licenses/by/4.0/

## Crèdits
 
* David Cànovas
* Jaume Ortolà
* Marc Riera
