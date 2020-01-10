# SYM - Laboratoire 4

> Rueff soit Rouff Christophe, Gabrielli Alexandre, Póvoa Tiago

## 1.2 Question

> Une fois la manipulation effectuée, vous constaterez que les animations de la flèche ne sont pas fluides, il va y avoir un tremblement plus ou moins important même si le téléphone ne bouge pas. Veuillez expliquer quelle est la cause la plus probable de ce tremblement et donner une manière (sans forcément l’implémenter) d’y remédier.

En effet, nous avons constaté ce comportement.

Les senseurs étant très précis, il suffit de quelques micro mouvements impercetibles de la main pour provoquer des changements numériques et donc créer un tremblement. 

Il y a plusieurs solutions à explorer qui peuvent venir à l'esprit:

* Diminuer le rafraichissement. 
* Actualiser que lorsque la valeur a un changement suffisamment significatif.
* Appliquer un sorte d'arrondi.

En fouillant un peu, nous avons trouvé la suggestion suivante qui est d'utiliser un low-pass filter:

```java
private static final float ALPHA = 0.5f;
//lower alpha should equal smoother movement
...
private float[] applyLowPassFilter(float[] input, float[] output) {
    if ( output == null ) return input;

    for ( int i=0; i<input.length; i++ ) {
        output[i] = output[i] + ALPHA * (input[i] - output[i]);
    }
    return output;
}
```

Source: 

https://stackoverflow.com/questions/27846604/how-to-get-smooth-orientation-data-in-android

## 2.2 Question

> La caractéristique permettant de lire la température retourne la valeur en degrés Celsius, multipliée par 10, sous la forme d’un entier non-signé de 16 bits. Quel est l’intérêt de procéder de la sorte ? Pourquoi ne pas échanger un nombre à virgule flottante de type float par exemple ? 

Un float est généralement fait sur 32 bits. Dans notre cas, on a pas besoin de toute la précision. Donc le compromis mémoire de 16 bits est intéressant. Surtout sachant que le BLE n'a que 100 kbit/s de débit.

> Le niveau de charge de la pile est à présent indiqué uniquement sur l’écran du périphérique, mais nous souhaiterions que celui-ci puisse informer le smartphone sur son niveau de charge restante. 
>
> Veuillez spécifier la(les) caractéristique(s) qui composerai(en)t un tel service, mis à disposition par le périphérique et permettant de communiquer le niveau de batterie restant via Bluetooth Low Energy. Pour chaque caractéristique, vous indiquerez les opérations supportées (lecture, écriture, notification, indication, etc.) ainsi que les données échangées et leur format.

Cette caractéristique lié à ce service hypothétique existe en effet déjà. Ce qu'on pourrait imaginer est la représentation en pourcentage stockée dans un uint8.

Sur la page officiel de bluetooth on peut voir le Battery Service *0x180F* . 

Il retourne un niveau de 0 à 100. 0 Étant vide et 100 plein. (0x64 sera convertit en 100 décimale)

Il supporte les opérations suivantes: (selon le xml)

| Opération             |                                                |
| --------------------- | ---------------------------------------------- |
| Lecture               | Obligatoire (il est de toute façon disponible) |
| Écriture              | Exclu                                          |
| Écriture sans réponse | Exclu                                          |
| Écriture fiable       | Exclu                                          |
| Notification          | Optionnel                                      |
| Indication            | Exclu                                          |
| Écritures auxiliaires | Exclu                                          |
| Broadcast             | Exclu                                          |

Évidemment, l'utilisation d'un tel service n'est pas forcément disponible ou à jour. Contrairement à l'affichage sur l'appareil qui est toujours à jour.

sources: 

https://www.bluetooth.com/specifications/gatt/services/

https://www.bluetooth.com/wp-content/uploads/Sitecore-Media-Library/Gatt/Xml/Services/org.bluetooth.service.battery_service.xml


